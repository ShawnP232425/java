/*

Copyright (c) 2022 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
The MIT License (MIT)
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/
package com.github.lindenb.jvarkit.tools.uniprot;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.github.lindenb.jvarkit.io.ArchiveFactory;
import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.lang.AttributeMap;
import com.github.lindenb.jvarkit.lang.StringUtils;
import com.github.lindenb.jvarkit.pedigree.Pedigree;
import com.github.lindenb.jvarkit.pedigree.PedigreeParser;
import com.github.lindenb.jvarkit.util.jcommander.Launcher;
import com.github.lindenb.jvarkit.util.jcommander.Program;
import com.github.lindenb.jvarkit.util.log.Logger;
import com.github.lindenb.jvarkit.util.svg.SVG;
import com.github.lindenb.jvarkit.util.vcf.predictions.VepPredictionParser;
import com.github.lindenb.jvarkit.util.vcf.predictions.VepPredictionParserFactory;

import htsjdk.samtools.util.IOUtil;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFIterator;
import htsjdk.variant.vcf.VCFIteratorBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


@Program(name="uniprot2svg",
description="plot uniprot to SVG",
keywords={"uniprot","svg"},
creationDate="20220608",
modificationDate="20220608"
)
public class UniprotToSvg extends Launcher {
	private static final Logger LOG = Logger.build(UniprotToSvg.class).make();
	private static final String UNIPROT_NS = "http://uniprot.org/uniprot";
	@Parameter(names={"-o","--output"},description=ArchiveFactory.OPT_DESC,required = true)
	private Path outputFile = null;
	@Parameter(names = "--vcf", description = "annotated VCF")
	private Path vcfPath  = null;
	@Parameter(names = "--pedigree", description = PedigreeParser.OPT_DESC)
	private Path pedigreePath  = null;
	@Parameter(names = "--exclude-type", description = "Exclude feature/@type matching that regular expression")
	private String excludeFeatureTye = null;
	@Parameter(names = "--include-type", description = "Only feature/@type matching that regular expression")
	private String includeFeatureTye = null;
	@Parameter(names = "--svg", description = "produce SVG only (default is HTML+SVG)")
	private boolean svg_only=false;

	
	@DynamicParameter(names = "-D", description = "extra parameters. Undocumented.",hidden=true)
	private Map<String, String> __dynaParams = new HashMap<>();

	private XPath xpath = null;
	private Document svgDoc =null;
	private final Set<String> casesSamples = new HashSet<>();
	private final Set<String> ctrlsSamples = new HashSet<>();
	private final Set<String> accepted_features = new HashSet<>();

	
	private final AttributeMap attMap = AttributeMap.verbose(AttributeMap.wrap(__dynaParams), S->{
		LOG.info("undefined property "+S+". Using default");
		});
	
	
	private static class UniprotNsContext implements NamespaceContext {	 
	    //The lookup for the namespace uris is delegated to the stored document.
	    public String getNamespaceURI(String prefix) {
	    	return prefix.equals("u") || prefix.equals("uniprot") ? UNIPROT_NS:null;
	    }
	 
	    public String getPrefix(String namespaceURI) {
	        if(UNIPROT_NS.equals(namespaceURI)) return "u";
	        return "";
	    }
	 
	    @SuppressWarnings({"rawtypes" })
	    public Iterator getPrefixes(String namespaceURI) {
	    	 if(UNIPROT_NS.equals(namespaceURI)) return Arrays.asList("u","uniprot").iterator();
	    	return Collections.emptyIterator();
	    	}
	}
	
	
	private Element element(final String localName) {
		return this.svgDoc.createElementNS(SVG.NS, localName);
	}
	private Element html(final String localName) {
		return this.svgDoc.createElement(localName);
		}
	private Element html(final String localName,final String content) {
		final Element e= html(localName);
		e.appendChild(text(content));
		return e;
		}

	
	
	private Element element(final String localName,final String content) {
		final Element e= element(localName);
		e.appendChild(text(content));
		return e;
		}
	
	private Node text(final String s) {
		return this.svgDoc.createTextNode(s);
		}
	
	private String format(double v) {
		return String.valueOf(v);
		}
	
	private static class Variant {
		int pos = -1;
		String title="";
		int numCases = 0;
		int numControls = 0;
		}
	
	private static class Feature {
		String type=null;
		String description;
		int start;
		int end;
		String getURL() {
			return null;
			}
		String getTitle() {
			return this.description;
			}
		String getLabel() {
			return this.type;
			}
		}
	
	private Element stopCreate(String offset,String stop_color)
			{
			Element stop= element("stop");
			stop.setAttribute("offset",offset);
			stop.setAttribute("stop-color",stop_color);
			return stop;
			};
	private String normalizeENS(final String enst) {
		int dot = enst.indexOf('.');
		return dot==-1?enst:enst.substring(0,dot);
		}
	
	private List<Variant> fetchVariants(final String rawEnsemblId) throws IOException {
		if(this.vcfPath==null || (this.casesSamples.isEmpty() && this.ctrlsSamples.isEmpty())) return Collections.emptyList();
		String annotator = this.attMap.getAttribute("variant.annotator","CSQ").toUpperCase();
		VepPredictionParser  vepParser = null;
		final String ensemblId = normalizeENS(rawEnsemblId);
		final List<Variant> L = new ArrayList<>();
	
		try(VCFIterator iter = new VCFIteratorBuilder().open(this.vcfPath)) {
			VCFHeader header = iter.getHeader();
			if(annotator.equals("CSQ")) {
				vepParser = new VepPredictionParserFactory().header(header).get();
				}
			while(iter.hasNext()) {
				final VariantContext ctx = iter.next();
				Variant variant = new Variant();
				if(vepParser!=null) {
					for(final VepPredictionParser.VepPrediction pred : vepParser.getPredictions(ctx)) {
						if(!ensemblId.equals(normalizeENS(pred.getFeature()))) continue;
						final String protPosition = pred.get("Protein_position");
						if(StringUtils.isBlank(protPosition) || !StringUtils.isInteger(protPosition)) continue;
						variant.pos = Integer.parseInt(protPosition);
						for(final Genotype g:ctx.getGenotypes()) {
							if(g.getAlleles().stream().noneMatch(A->!(A.isReference() || A.isNoCall()))) continue;
							if(this.casesSamples.contains(g.getSampleName())) {
								variant.numCases++;
								}
							if(this.ctrlsSamples.contains(g.getSampleName())) {
								variant.numControls++;
								}
							}
						}
					}
				if(variant.pos<0) continue;
				variant.title=String.valueOf(variant.pos)+" "+ctx.getContig()+":"+ctx.getStart()+":"+ctx.getReference().getDisplayString();
				L.add(variant);
				}
			}
		return L;
		}
	
	private boolean acceptFeature(final Feature feat) {
		if(!StringUtils.isBlank(this.excludeFeatureTye)) {
			final Pattern regex = Pattern.compile(this.excludeFeatureTye,Pattern.CASE_INSENSITIVE);
			if(regex.matcher(feat.type).find()) {
				LOG.warn("exclude type "+feat.type+" matching "+regex.pattern());
				return false;
				}
			}
		if(!StringUtils.isBlank(this.includeFeatureTye)) {
			final Pattern regex = Pattern.compile(this.includeFeatureTye,Pattern.CASE_INSENSITIVE);
			if(!regex.matcher(feat.type).find()) {
				LOG.warn("exclude type "+feat.type+" not matching "+regex.pattern());
				return false;
				}
			}
		this.accepted_features.add(feat.type);
		return true;
		}
	
	private void toSVG(final ArchiveFactory archive, final Element uEntry) {
			try {
			final int length = Integer.parseInt(Objects.requireNonNull((String)this.xpath.evaluate("u:sequence/@length", uEntry, XPathConstants.STRING)));
			final String accession = (String)this.xpath.evaluate("u:accession[1]/text()", uEntry, XPathConstants.STRING);
			final String entryName = (String)this.xpath.evaluate("u:name/text()", uEntry, XPathConstants.STRING);
			final String enst = (String)this.xpath.evaluate("u:dbReference[@type='Ensembl']/@id", uEntry, XPathConstants.STRING);
			LOG.info("Ensembl:"+enst);
			final NodeList uFeatures = (NodeList)this.xpath.evaluate("u:feature", uEntry, XPathConstants.NODESET);
			final List<Feature>  features = new ArrayList<>();
			for(int i=0;i< uFeatures.getLength();i++) {
				final Element uFeature = (Element)uFeatures.item(i);
				final Element uLocation = (Element)this.xpath.evaluate("u:location", uFeature, XPathConstants.NODE);
				if(uLocation==null) {
					LOG.info("missing location");
					continue;
					}
				final Feature feat = new Feature();
				final Element positionE = (Element)this.xpath.evaluate("u:position", uLocation, XPathConstants.NODE);
				if(positionE!=null) {
					feat.start = Integer.parseInt(positionE.getAttribute("position"));
					feat.end = feat.start;
					}
				else
					{
					feat.start = Integer.parseInt((String)this.xpath.evaluate("u:begin/@position", uLocation, XPathConstants.STRING));
					feat.end = Integer.parseInt((String)this.xpath.evaluate("u:end/@position", uLocation, XPathConstants.STRING));
					}
				if(feat.start==1 && feat.end==length) continue;//whole protein
				feat.type = uFeature.getAttribute("type");
				if(feat.type==null) feat.type="";
				feat.description = uFeature.getAttribute("description");
				if(feat.description==null) feat.description="";
				
				if(!acceptFeature(feat)) continue;
				features.add(feat);
				}
			
			final List<String> all_types = features.stream().
					map(F->F.type).
					filter(S->!StringUtils.isBlank(S)).
					collect(Collectors.toSet()).
					stream().
					collect(Collectors.toList());
			final Map<String,String> type2fill = new HashMap<>();
			for(int i=0;i< all_types.size();i++) {
				  final Color c = Color.getHSBColor((float) i / (float)all_types.size(), 0.85f, 1.0f);
				  type2fill.put(all_types.get(i), "rgb("+c.getRed()+","+c.getBlue()+","+c.getGreen()+")");
				}
			final int width = this.attMap.getIntAttribute("width").orElse(700);
			final IntToDoubleFunction pos2pix = POS->((double)(POS-1)/length)*width;

			Collections.sort(features,(A,B)->Integer.compare(A.start,B.start));
			final List<List<Feature>> rows = new ArrayList<>();
			while(!features.isEmpty()) {
				final Feature first = features.remove(0);
				int y=0;
				for(y=0;y< rows.size();y++) {
					final List<Feature> row = rows.get(y);
					if(!row.stream().allMatch(X->{
						double x1 = pos2pix.applyAsDouble(X.start)-5;
						double x4 = pos2pix.applyAsDouble(first.end)+5;
						if(x4 < x1) return true;
						double x2 = pos2pix.applyAsDouble(X.end)+5;
						double x3 = pos2pix.applyAsDouble(first.start)-5;
						if(x2 < x3 ) return true;
						return false;
						})) continue;
					row.add(first);
					break;
					}
				if(y==rows.size()) {
					final List<Feature> row = new ArrayList<>();
					row.add(first);
					rows.add(row);
					}
				}
			final List<Variant> variants= fetchVariants(enst);
			
			
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			this.svgDoc = db.newDocument();

			final double featureHeight = this.attMap.getIntAttribute("feature.height").orElse(30);
			final boolean hidelabels  = this.attMap.getBooleanAttribute("hide.labels");
			final int fontSize = this.attMap.getIntAttribute("font.size").orElse(12);
			final int variantsHeight =  this.attMap.getIntAttribute("variant.height").orElse(30);
			//final int line_height = this.attMap.getIntAttribute("line.height").orElse(20);
			final int margin_left = this.attMap.getIntAttribute("margin.left").orElse(50);
			final int margin_right = this.attMap.getIntAttribute("margin.right").orElse(100);
			final int margin_top = this.attMap.getIntAttribute("margin.top").orElse(50);
			final int margin_bottom = this.attMap.getIntAttribute("margin.bottom").orElse(50);
			final int distanceBetweenFeatures = this.attMap.getIntAttribute("feature.margin").orElse(2);

			
			
		
			final Element svgRoot = element("svg");
			svgRoot.setAttribute("version", "1.0");
			
			final String suffix;

			if(svg_only) {
				suffix = ".svg";
				this.svgDoc.appendChild(svgRoot);
				}
			else
				{
				suffix = ".html";
				final Element htmlRoot = html("html");
				final Element head = html("head");
				htmlRoot.appendChild(head);
				final Element body = html("body");
				htmlRoot.appendChild(body);
				final Element div1 = html("div");
				body.appendChild(div1);
				div1.appendChild(svgRoot);
				this.svgDoc.appendChild(htmlRoot);
				}

			
			svgRoot.appendChild(element("title",entryName));
	
			final Element g_style = element("style");
			svgRoot.appendChild(g_style);
			g_style.appendChild(text(
				".featlabel {text-anchor:start;font-size:"+fontSize+"px}\n" +
				".varlabel {text-anchor:start;font-size:8px;stroke:none;}\n" +
				".maintitle {font-size:18px}\n" +
				".legend {font-size:8px}\n"
				));
			
			final Element defs = element("defs");
			svgRoot.appendChild(defs);
			final String[] gradients=new String[]{
	   			"white","gray"
	   			};
			
			for(int i=0;i+1< gradients.length;i+=2)
				{
				Element linearGradient= element("linearGradient");
				defs.appendChild(linearGradient);
				linearGradient.setAttribute("x1","0%");
				linearGradient.setAttribute("y1","0%");
				linearGradient.setAttribute("x2","0%");
				linearGradient.setAttribute("y2","100%");
				linearGradient.setAttribute("id","grad"+(i/4));
			
			
				linearGradient.appendChild(stopCreate("5%",gradients[i+0]));
				linearGradient.appendChild(stopCreate("50%",gradients[i+1]));
				linearGradient.appendChild(stopCreate("95%",gradients[i+0]));
				}

			
			double y= margin_top + (variants.stream().anyMatch(V->V.numCases>0)?variantsHeight:0);
			// main g
			final Element G0 = element("g");
			svgRoot.appendChild(G0);
			
			final Element G1= element("g");			
			G1.setAttribute("transform","translate("+margin_left+","+margin_top+")");
			final Element G_ruler=  element("g");
			G1.appendChild(G_ruler);
			
			G0.appendChild(G1);
			final Element g_feats = element("g");
			G1.appendChild(g_feats);

			/** main title */
			final Element E_title = element("text",accession+" "+entryName+" size:"+StringUtils.niceInt(length)+" aa.");
			E_title.setAttribute("x", "5");
			E_title.setAttribute("y", "20");
			E_title.setAttribute("class", "maintitle");
			G0.appendChild(E_title);

			/* paint sequence **************************************************************************************/
			final Element g_sequence = element("g");
			g_sequence.setAttribute("id", accession);
			g_sequence.appendChild(this.svgDoc.createComment("START Sequence"));
			G1.appendChild(g_sequence);
			
			final Element E_prot_rect = element("rect");
			g_sequence.appendChild(E_prot_rect);
			E_prot_rect.setAttribute("x","0");
			E_prot_rect.setAttribute("y",format(y));
			E_prot_rect.setAttribute("width",format(width));
			E_prot_rect.setAttribute("height",format(featureHeight));
			E_prot_rect.setAttribute("style","fill:url(#grad0);stroke:black;");
			E_prot_rect.appendChild(element("title",accession+" "+entryName+" size:"+length));
			g_sequence.appendChild(this.svgDoc.createComment("END Sequence"));
			final double mid_y_protein = y+featureHeight/2.0;
			y+= featureHeight;
			
			
			/* PAINT VARIANTS =========================================================================== */
			final Element G_var_case = element("g");
			G_var_case.setAttribute("id", "cases");
			G1.appendChild(G_var_case);
			final Element G_var_ctrl = element("g");
			G_var_ctrl.setAttribute("id", "ctrls");
			G1.appendChild(G_var_ctrl);
			
			for(Variant ctx: variants)
				{
				final double x1= pos2pix.applyAsDouble(ctx.pos);
				for(int side=0;side<2;side++)
					{
					final int count=  (side==0?ctx.numCases:ctx.numControls);
					if(count==0) continue;
					
					final String var_color=(ctx.numCases!=0 && ctx.numControls!=0?"green":(side==0?"red":"blue"));
					final Element g_var= element("g");
					g_var.setAttribute("style","stroke-width:1px;stroke:"+var_color+";fill:"+var_color);
					(side==0?G_var_case:G_var_ctrl).appendChild(g_var);
					
					final double y2= mid_y_protein + variantsHeight*(side==0?-1:1);
					
					final Element text= element("text");
					text.appendChild(text((ctx.title)+" ("+count+")"));
					text.setAttribute("x","0");
					text.setAttribute("y","0");
					text.setAttribute("class", "varlabel");
					text.setAttribute("transform","translate("+format(x1)+","+
						(side==0?y2-5:y2+5)+") rotate("+(side==0?-45:45)+")"
						);
					g_var.appendChild(text);
					
					final Element line= element("line");
					line.setAttribute("x1",format(x1));
					line.setAttribute("x2",format(x1));
					line.setAttribute("y1",format(mid_y_protein));
					line.setAttribute("y2",format(y2));
					line.appendChild(element("title",ctx.title));
					g_var.appendChild(line);
					
					final Element circle= element("circle");
					circle.setAttribute("cx",format(x1));
					circle.setAttribute("cy",format(y2));
					circle.setAttribute("r","3");
					g_var.appendChild(circle);
					} 
				}//end loop variants
			y+= (variants.stream().anyMatch(V->V.numControls>0)?variantsHeight*1.5:0);
			
			/** paint features **************************************************************************************/
			g_feats.appendChild(this.svgDoc.createComment("START Features"));
			y+= featureHeight + distanceBetweenFeatures;
			for(final List<Feature> row:rows) {
				for(Feature feat: row)
					{
					final double x1 = pos2pix.applyAsDouble(feat.start);
					final double x2 = pos2pix.applyAsDouble(feat.end+1);
					
					final Element g = element("g");
					
					g_feats.appendChild(g);
					g.appendChild(this.svgDoc.createComment("type:"+feat.type+" desc:"+feat.description));
					
					final Element rect= element("rect");
					Element anchor=null;
					final String url=feat.getURL();
					if(!StringUtils.isBlank(url))
						{
						anchor  = element("a");
						anchor.setAttribute("href",url);
						anchor.setAttribute("target","_blank");
						}
					
					if(anchor==null)
						{
						g.appendChild(rect);
						}
					else
						{
						g.appendChild(anchor);
						anchor.appendChild(rect);
						}
					rect.setAttribute("x",format(x1));
					rect.setAttribute("y",format(y));
					rect.setAttribute("width",format(x2-x1));
					rect.setAttribute("height",format(featureHeight));
					rect.setAttribute("style", "stroke:slategray;fill:"+type2fill.getOrDefault(feat.type,"lavender")+";fill-opacity:0.6;");

	
					if(!StringUtils.isBlank(feat.getTitle()))
						{
						rect.appendChild(element("title",feat.getTitle()));
						}
					
					if(!hidelabels && !StringUtils.isBlank(feat.getLabel()))
						{
						final Element text= element("text",feat.getLabel());
						g.appendChild(text);
						text.setAttribute("y",format(y+ featureHeight + fontSize));
						text.setAttribute("x",format(x1));
						text.setAttribute("class", "featlabel");
						}
					}
				y+= featureHeight + distanceBetweenFeatures + (hidelabels?0:fontSize+1);
				}
			g_feats.appendChild(this.svgDoc.createComment("END Features"));

			/** paint ruler **************************************************************************************/
			if(!this.attMap.getBooleanAttribute("hide.ruler"))
				{
				G_ruler.appendChild(this.svgDoc.createComment("START ruler"));
				for(int i=0;i< length;i+=10)
					{
					final Element rect= element("rect");
					final double x1= pos2pix.applyAsDouble(i+1);
					rect.appendChild(element("title",String.valueOf(i)));
					rect.setAttribute("y",format(0));
					rect.setAttribute("height",format(y));
					rect.setAttribute("x",format(x1));
					rect.setAttribute("width",(i%100==0?"1.5":"0.5"));
					rect.setAttribute("style","fill:white;stroke:lightgray;");
					G_ruler.appendChild(rect);
					}
				G_ruler.appendChild(this.svgDoc.createComment("END ruler"));
				}
			y+=12;
			final Element g_legend = element("text");
			g_legend.setAttribute("class", "legend");
			g_legend.setAttribute("x", "5");
			g_legend.setAttribute("y", format(y+10));
			for(final String tt : type2fill.keySet()) {
				final Element g_span = element("tspan",tt+" ");
				g_legend.appendChild(g_span);
				g_span.setAttribute("fill", type2fill.get(tt));
				}
			G1.appendChild(g_legend);
			y+=12;
			
		
			svgRoot.setAttribute("width",format(margin_left+margin_right+width+1));
			svgRoot.setAttribute("height",format(margin_bottom+y));
			
			final TransformerFactory transformerFactory = TransformerFactory.newInstance();
			final Transformer transformer = transformerFactory.newTransformer();
			
			
			try(Writer w= archive.openWriter(accession+(StringUtils.isBlank(enst)?"":"_"+enst) + suffix)) {
				transformer.transform(new DOMSource(this.svgDoc), new StreamResult(w));
				w.flush();
				}
			}
		catch(final Throwable err) {
			throw new RuntimeException(err);
			}
		}
	
	
	@Override
	public int doWork(final List<String> args) {
		try {
			final List<Path> inputs = IOUtils.unrollPaths(args);
			if(this.pedigreePath!=null) {
				final Pedigree ped = new PedigreeParser().parse(this.pedigreePath);
				this.casesSamples.addAll(ped.getAffectedSamples().stream().map(S->S.getId()).collect(Collectors.toSet()));
				this.ctrlsSamples.addAll(ped.getUnaffectedSamples().stream().map(S->S.getId()).collect(Collectors.toSet()));
				}
			
			final XPathFactory xpathFactory = XPathFactory.newInstance();
			this.xpath = xpathFactory.newXPath();
			this.xpath.setNamespaceContext(new UniprotNsContext());

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringComments(true);
			dbf.setCoalescing(true);
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			
			try(ArchiveFactory archive = ArchiveFactory.open(this.outputFile)) {

				int i=0;
				for(;;) {
					final Document dom;
					if(inputs.isEmpty()) {
						dom = db.parse(stdin());
						}
					else
						{
						IOUtil.assertFileIsReadable(inputs.get(i));
						dom = db.parse(inputs.get(i).toFile());
						}
					final Element root = dom.getDocumentElement();
					if(root==null || !UNIPROT_NS.equals(root.getNamespaceURI()) || !"uniprot".equals(root.getLocalName())) {
						LOG.error("not a u:uniprot root "+root);
						return -1;
						}				
					
					final NodeList nodeList=(NodeList)this.xpath.evaluate("/u:uniprot/u:entry",dom,XPathConstants.NODESET);
					for(int x=0;x< nodeList.getLength();x++) {
						toSVG(archive,Element.class.cast(nodeList.item(x)));
						}
					i++;
					if(inputs.isEmpty() || i>=inputs.size()) break;
					}
				}//end archive
			LOG.info("accepted types: "+ String.join(", ", this.accepted_features));
			return 0;
			}
		catch(final Throwable err ) {
			LOG.error(err);
			return -1;
			}
		}
	
	public static void main(String[] args) {
		new UniprotToSvg().instanceMainWithExit(args);

	}

}
