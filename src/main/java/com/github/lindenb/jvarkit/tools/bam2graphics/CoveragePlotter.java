/*
The MIT License (MIT)

Copyright (c) 2020 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/
package com.github.lindenb.jvarkit.tools.bam2graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleFunction;

import javax.imageio.ImageIO;


import com.beust.jcommander.Parameter;
import com.github.lindenb.jvarkit.io.ArchiveFactory;
import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.io.NullOuputStream;
import com.github.lindenb.jvarkit.jcommander.converter.FractionConverter;
import com.github.lindenb.jvarkit.lang.CharSplitter;
import com.github.lindenb.jvarkit.lang.StringUtils;
import com.github.lindenb.jvarkit.math.DiscreteMedian;
import com.github.lindenb.jvarkit.samtools.SAMRecordDefaultFilter;
import com.github.lindenb.jvarkit.samtools.util.IntervalListProvider;
import com.github.lindenb.jvarkit.samtools.util.SimpleInterval;
import com.github.lindenb.jvarkit.util.bio.SequenceDictionaryUtils;
import com.github.lindenb.jvarkit.util.bio.bed.BedLine;
import com.github.lindenb.jvarkit.util.bio.bed.BedLineCodec;
import com.github.lindenb.jvarkit.util.bio.fasta.ContigNameConverter;
import com.github.lindenb.jvarkit.util.bio.gtf.GTFCodec;
import com.github.lindenb.jvarkit.util.bio.gtf.GTFLine;
import com.github.lindenb.jvarkit.util.iterator.LineIterator;
import com.github.lindenb.jvarkit.util.jcommander.Launcher;
import com.github.lindenb.jvarkit.util.jcommander.NoSplitter;
import com.github.lindenb.jvarkit.util.jcommander.Program;
import com.github.lindenb.jvarkit.util.log.Logger;
import com.github.lindenb.jvarkit.variant.vcf.VCFReaderFactory;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.AbstractIterator;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.FileExtensions;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.Locatable;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.samtools.util.StringUtil;
import htsjdk.tribble.readers.TabixReader;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFileReader;
/**
BEGIN_DOC
input is an interval of a file source of interval (bed, vcf, gtf, interval_list , ,etc...)


```
java -jar dist/coverageplotter.jar -R src/test/resources/rotavirus_rf.fa -B src/test/resources/S1.bam -B src/test/resources/S2.bam "RF01:1-4000" -w 50 | less -r
```


END_DOC 
 */
@Program(
	name="coverageplotter",
	description="Find anomaly of depth in intervals+bams",
	keywords={"cnv","bam","depth","coverage"},
	creationDate="20200605",
	modificationDate="20200605",
	generate_doc=false
	)
public class CoveragePlotter extends Launcher {
	private static final Logger LOG = Logger.build( CoveragePlotter.class).make();
	@Parameter(names={"-o","--output"},description=ArchiveFactory.OPT_DESC)
	private Path outputFile = null;
	@Parameter(names={"-R","--reference"},description=INDEXED_FASTA_REFERENCE_DESCRIPTION,required=true)
	private Path refPath = null;
	@Parameter(names={"-B","--bams"},description = "list of bams. one file with a '.list' suffix is interpretted a a list of path to the bams",required=true)
	private List<String> bamsPath= new ArrayList<>();
	@Parameter(names={"--mapq"},description = "min mapping quality")
	private int min_mapq=1;
	@Parameter(names={"--max-depth"},description = "ignore position if depth > 'x'")
	private int max_depth=500;
	@Parameter(names={"--dimension"},description = "Image Dimension. " + com.github.lindenb.jvarkit.jcommander.converter.DimensionConverter.OPT_DESC, converter=com.github.lindenb.jvarkit.jcommander.converter.DimensionConverter.StringConverter.class,splitter=NoSplitter.class)
	private Dimension dimension = new Dimension(1000,300);
	@Parameter(names={"--extend","-x"},description = "extend original interval by this fraction")
	private double extend=1.0;
	@Parameter(names= {"--gtf"},description="Optional Tabix indexed GTF file. Will be used to retrieve an interval by gene name, or to display gene names in a region.")
	private Path gtfFile = null;
	@Parameter(names= {"--known"},description="Optional Tabix indexed Bed or VCF file containing known CNV. Both types must be indexed.")
	private Path knownCnvFile = null;
	@Parameter(names= {"--prefix"},description="Image File Prefix.")
	private String prefix="";
	@Parameter(names= {"--alpha"},description="line opacity. "+ FractionConverter.OPT_DESC,converter=FractionConverter.class,splitter=NoSplitter.class)
	private double alpha=1.0;
	@Parameter(names= {"--manifest"},description="Optional. Manifest file")
	private Path manifestPath =null;

	
	private void drawKnownCnv(final Graphics2D g,final Rectangle rectangle,final Locatable region,final Locatable R) {
		final IntToDoubleFunction position2pixel = X->((X-region.getStart())/(double)region.getLengthOnReference())*rectangle.getWidth();
		final double y= rectangle.getHeight()-8.0;
		final double x1 = position2pixel.applyAsDouble(R.getStart());
		final double x2 = position2pixel.applyAsDouble(R.getEnd());
		g.draw(new Rectangle2D.Double(x1, y-1, Math.max(1.0,x2-x1), 3));
		}


	private void drawKnownCnv(final Graphics2D g,final Rectangle rectangle,final Locatable region) {
		if(this.knownCnvFile==null) return;
		final String fname=this.knownCnvFile.getFileName().toString();
		final Composite oldComposite = g.getComposite();
		final Stroke oldStroke = g.getStroke();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
		g.setColor(Color.MAGENTA);

		if(fname.endsWith(".bed.gz")) {
			try(TabixReader tbr = new TabixReader(this.knownCnvFile.toString())) {
				final ContigNameConverter cvt = ContigNameConverter.fromContigSet(tbr.getChromosomes());
				final String ctg = cvt.apply(region.getContig());
					if(!StringUtils.isBlank(ctg)) {
						final BedLineCodec codec = new BedLineCodec();
						final TabixReader.Iterator iter = tbr.query(ctg,region.getStart(), region.getEnd());
						for(;;) {
							final String line = iter.next();
							if(line==null) break;
							final BedLine bed = codec.decode(line);
							if(bed==null) continue;
							final Interval rgn = new Interval(region.getContig(),bed.getStart(),bed.getEnd(),false,bed.getOrDefault(3, ""));
							drawKnownCnv(g,rectangle,region,rgn);
							}
					}
				}
			catch(final Throwable err) {
				LOG.error(err);
				}
			}
		else if(FileExtensions.VCF_LIST.stream().anyMatch(X->fname.endsWith(X))) {
			try(VCFFileReader vcfFileReader= VCFReaderFactory.makeDefault().open(this.knownCnvFile,true)) {
				final ContigNameConverter cvt = ContigNameConverter.fromOneDictionary(SequenceDictionaryUtils.extractRequired(vcfFileReader.getFileHeader()));
				final String ctg = cvt.apply(region.getContig());
				if(!StringUtils.isBlank(ctg)) {
					vcfFileReader.query(ctg, region.getStart(), region.getEnd()).
							stream().
							filter(VC->!VC.isSNP()).
							forEach(VC->{
								final List<String> list = new ArrayList<>();
								if(VC.hasID()) list.add(VC.getID());
								if(VC.hasAttribute(VCFConstants.SVTYPE))  list.add(VC.getAttributeAsString(VCFConstants.SVTYPE,"."));
								final Interval rgn= new Interval(region.getContig(),VC.getStart(),VC.getEnd(),false,String.join(";",list));
								drawKnownCnv(g,rectangle,region,rgn);
								});
					}
				}
			catch(final Throwable err) {
				LOG.error(err);
				}
			}
		else
			{
			LOG.warn("not a vcf of bed.gz file "+this.knownCnvFile);
			}
		g.setComposite(oldComposite);
		g.setStroke(oldStroke);
		}

	private void drawGenes(final Graphics2D g,final Rectangle rect,final Locatable region) {
		if(this.gtfFile==null) return;
		final IntToDoubleFunction position2pixel = X->((X-region.getStart())/(double)region.getLengthOnReference())*rect.getWidth();
		final Composite oldComposite = g.getComposite();
		final Stroke oldStroke = g.getStroke();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
		g.setColor(Color.ORANGE);
		final double y= rect.getMaxY()-4.0;

		try (TabixReader tbr = new TabixReader(this.gtfFile.toString())) {
			final ContigNameConverter cvt = ContigNameConverter.fromContigSet(tbr.getChromosomes());
			final String ctg = cvt.apply(region.getContig());
			if(StringUtils.isBlank(ctg)) {
				}
			final GTFCodec codec = new GTFCodec();
			final TabixReader.Iterator iter=tbr.query(ctg,region.getStart(),region.getEnd());
			for(;;) {
				final String line = iter.next();
				if(line==null) break;
				if(StringUtils.isBlank(line) ||  line.startsWith("#")) continue;
				final String tokens[]= CharSplitter.TAB.split(line);
				if(tokens.length<9 ) continue;
				tokens[0]=region.getContig();
				final GTFLine gtfline = codec.decode(line);
				if(gtfline==null) continue;

				final double x1 = position2pixel.applyAsDouble(gtfline.getStart());
				final double x2 = position2pixel.applyAsDouble(gtfline.getEnd());

				if(gtfline.getType().equals("gene") ) {
					g.drawString(gtfline.getAttribute("gene_name"),(int)Math.max(x1,1),(int)(y+3));
					}
				else if(gtfline.getType().equals("exon") ) {
					g.draw(new Rectangle2D.Double(x1, y-1, (x2-x1), 3));
					}
				else if(gtfline.getType().equals("transcript") ) {
					g.draw(new Line2D.Double(x1, y, x2, y));
					}
				}
			}
		catch(Throwable err) {
			}
		finally {
			g.setComposite(oldComposite);
			g.setStroke(oldStroke);
			}
		}
	
	
	
	

@Override
public int doWork(final List<String> args) {
	ArchiveFactory archive = null;
	PrintWriter manifest = null;
	try
		{
		if(extend<1.0) {
			LOG.error("extend is lower than 1 :"+this.extend);
			return -1;
			}
		final SAMSequenceDictionary dict = SequenceDictionaryUtils.extractRequired(this.refPath);
		final SamReaderFactory samReaderFactory = SamReaderFactory.
					makeDefault().
					referenceSequence(CoveragePlotter.this.refPath).
					validationStringency(ValidationStringency.LENIENT)
					;
		
		 final List<Path> inputBams =  IOUtils.unrollPaths(this.bamsPath);
		
		if(inputBams.isEmpty()) {
			LOG.error("input bam file missing.");
			return -1;
			}
		
		 Iterator<? extends Locatable> iter;
		 final String input = oneFileOrNull(args); 
		 if(input==null) {
			 final BedLineCodec codec = new BedLineCodec();
			 final LineIterator liter = new LineIterator(stdin());
			 iter = new AbstractIterator<Locatable>() {
			 	@Override
			 	protected Locatable advance() {
			 		while(liter.hasNext()) {
			 			final String line = liter.next();
			 			final BedLine bed = codec.decode(line);
			 			if(bed==null) {
			 				continue;
			 				}
			 			return bed;
			 			}
			 		liter.close();
			 		return null;
			 		}
			 	};
		 	}
		 else
		 	{
			iter = IntervalListProvider.from(input).dictionary(dict).stream().iterator();
		 	}
		final BufferedImage image = new BufferedImage(this.dimension.width,this.dimension.height,BufferedImage.TYPE_INT_RGB);
		final BufferedImage offscreen = new BufferedImage(this.dimension.width,this.dimension.height,BufferedImage.TYPE_INT_ARGB);
		final double y_mid = this.dimension.getHeight()/2.0;
		final ToDoubleFunction<Double> normToPixelY = NORM->  this.dimension.getHeight() - NORM*y_mid;
		final DiscreteMedian<Integer> discreteMedian = new DiscreteMedian<>();
		
		manifest = (this.manifestPath==null?new PrintWriter(new NullOuputStream()):IOUtils.openPathForPrintWriter(this.manifestPath));
		archive = ArchiveFactory.open(this.outputFile);
		while(iter.hasNext()) {
			final Locatable the_locatable = iter.next();
			String label="";
			if(the_locatable instanceof BedLine) {
				final BedLine bedline = BedLine.class.cast(the_locatable);
				label= bedline.getOrDefault(3,label);
				}
			
			final SimpleInterval rawRegion = new SimpleInterval(the_locatable);
			final SimpleInterval extendedRegion;
			/* extend interval */
			if(this.extend>1) {
				final int L1 = rawRegion.getLengthOnReference();
				final  int L2 = (int)Math.ceil(L1*this.extend);
				final int mid = rawRegion.getCenter().getPosition();
				int x0 = mid - L2/2;
				if(x0<0) x0=1;
				int x1= mid + L2/2;
				final SAMSequenceRecord ssr = dict.getSequence(rawRegion.getContig());
				if(ssr!=null) x1=Math.min(ssr.getSequenceLength(), x1);
				if(x0>x1) continue;
				extendedRegion = new SimpleInterval(rawRegion.getContig(),x0,x1);
				}
			else
				{
				extendedRegion=rawRegion;
				}
			final ToDoubleFunction<Integer> pos2pixel = POS-> (POS - extendedRegion.getStart())/(double)extendedRegion.getLengthOnReference() * this.dimension.getWidth();

			
			final Graphics2D g2= offscreen.createGraphics();
			g2.setColor(Color.BLACK);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
			g2.fillRect(0, 0, this.dimension.width,this.dimension.height);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			
			final Graphics2D g= image.createGraphics();
			g.setColor(Color.WHITE);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.fillRect(0, 0, this.dimension.width,this.dimension.height);
			int y =(int)(this.dimension.height/2.0);
			g.setColor(Color.BLUE);
			g.drawLine(0, y, image.getWidth(),y );
			y=(int)(this.dimension.height/4.0);
			g.setColor(Color.CYAN);
			g.drawLine(0, y,image.getWidth(),y );
			y=(int)(3.0*this.dimension.height/4.0);
			g.drawLine(0, y,image.getWidth(),y );
			g.setColor(Color.DARK_GRAY);
			g.drawRect(0, 0, this.dimension.width-1,this.dimension.height-1);
			drawGenes(g, new Rectangle(0,0,image.getWidth(),image.getHeight()), extendedRegion);
			drawKnownCnv(g, new Rectangle(0,0,image.getWidth(),image.getHeight()), extendedRegion);
			if(this.extend>1) {
				g.setColor(Color.GREEN);
				int x =(int) pos2pixel.applyAsDouble(rawRegion.getStart());
				g.drawLine(x, 0, x, image.getHeight() );
				x = (int) pos2pixel.applyAsDouble(rawRegion.getEnd());
				g.drawLine(x, 0, x, image.getHeight() );
				}
			
			final int depth[]= new int[extendedRegion.getLengthOnReference()];
			final int copy[]= new int[depth.length];
			final Map<String,Point2D> sample2maxPoint = new HashMap<>(inputBams.size());
			boolean drawAbove = false;
			for(final Path path: inputBams) {
				try(SamReader sr = samReaderFactory.open(path)) {
					final SAMFileHeader header= sr.getFileHeader();
					
					final String sample = header.getReadGroups().stream().
							map(RG->RG.getSample()).
							filter(S->!StringUtil.isBlank(S)).
							findFirst().
							orElse(IOUtils.getFilenameWithoutCommonSuffixes(path));
					SequenceUtil.assertSequenceDictionariesEqual(dict,header.getSequenceDictionary());
					Arrays.fill(depth, 0);
					try(CloseableIterator<SAMRecord> siter = sr.queryOverlapping(extendedRegion.getContig(), extendedRegion.getStart(), extendedRegion.getEnd())) {
						while(siter.hasNext()) {
							final SAMRecord rec= siter.next();
							if(rec.getReadUnmappedFlag()) continue;
							if(!SAMRecordDefaultFilter.accept(rec, this.min_mapq)) continue;
							int ref=rec.getStart();
							final Cigar cigar = rec.getCigar();
							if(cigar==null) continue;
							
							if(rec.getReadPairedFlag() && 
								!rec.getMateUnmappedFlag() && 
								!rec.getProperPairFlag() && 
								rec.getReferenceIndex().equals(rec.getMateReferenceIndex()))
								{
								final double xstart = pos2pixel.applyAsDouble(rec.getAlignmentStart());
								final double xend = pos2pixel.applyAsDouble(rec.getMateAlignmentStart());
								final double len = (xend - xstart);
								
								if(Math.abs(len)>10) {
									final double y2 = y_mid + (drawAbove?-1:1)*Math.min(y_mid,Math.abs(len/2.0));
									final Composite oldComposite = g2.getComposite();
									g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
									g2.setColor(Color.ORANGE);
									final GeneralPath curve = new GeneralPath();
									curve.moveTo(xstart,y_mid);
									curve.quadTo(xstart + len/2.0, y2, xend, y_mid);
									g2.draw(curve);
									g2.setComposite(oldComposite);
									drawAbove = !drawAbove;
									}
								}
							
							for(CigarElement ce:cigar) {
								final CigarOperator op = ce.getOperator();
								final int len = ce.getLength();
								if(op.consumesReferenceBases()) {
									if(op.consumesReadBases()) {
										for(int i=0;i< len;i++) {
											final int pos = ref+i;
											if(pos < extendedRegion.getStart()) continue;
											if(pos > extendedRegion.getEnd()) break;
											depth[pos-extendedRegion.getStart()]++;
										}
									}
									ref+=len;
								}
							}
						}// loop cigar
					}// end samItere
	
				
				if(extendedRegion.getLengthOnReference()>image.getWidth()) {
					//smooth
					final int bases_per_pixel = (int)Math.ceil(extendedRegion.getLengthOnReference()/100.0);
					System.arraycopy(depth, 0, copy, 0, depth.length);
					for(int i=0;i< depth.length && bases_per_pixel>1;i++) {
						double t=0;
						int count=0;
						for(int j=i-bases_per_pixel;j<=i+bases_per_pixel && j< depth.length;j++) {
							if(j<0) continue;
							t+=copy[j];
							count++;
							}
						if(count==0) continue;
						depth[i]=(int)(t/count);
						}
					}
				
				discreteMedian.clear();
				for(int i=0;i< depth.length;i++) {
					if(depth[i]>this.max_depth) continue;
					discreteMedian.add(depth[i]);
				}

				final double median = discreteMedian.getMedian().orElse(0);
				if(median<=0) {
					LOG.warning("Skipping "+sample +" "+extendedRegion+" because median is 0");
					continue;
				}
				
				Point2D max_position=null;
				double max_distance_to_1=0.0;
				final GeneralPath line = new GeneralPath();
				
				for(int x=0;x< image.getWidth();x++) {
					discreteMedian.clear();
					int pos1= (int)Math.floor(((x+0)/(double)image.getWidth())*depth.length);
					final int pos2= (int)Math.ceil(((x+0)/(double)image.getWidth())*depth.length);
					while(pos1 <= pos2 && pos1 < depth.length) {
						discreteMedian.add(depth[pos1]);
						pos1++;
						} 
					final double average = discreteMedian.getMedian().orElse(0);
					final double normDepth = (average/median);
					
					final double y2 = normToPixelY.applyAsDouble(normDepth);
					double distance_to_1 = Math.abs(normDepth-1.0);
					if(distance_to_1 > 0.3 && (max_position==null || distance_to_1 > max_distance_to_1)) {
						max_distance_to_1 = distance_to_1;
						max_position = new Point2D.Double(x,y2);
						}
					if(x==0) {
						line.moveTo(x, y2);
						}
					else
						{
						line.lineTo(x, y2);
						}
					}
				
				g.setColor(max_distance_to_1<0.1?Color.lightGray:Color.GRAY);
				final Stroke oldStroke = g.getStroke();
				final Composite oldComposite = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)this.alpha));
				g.setStroke(new BasicStroke(0.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));
				g.draw(line);
				g.setStroke(oldStroke);
				g.setComposite(oldComposite);
				
				if(max_position!=null) sample2maxPoint.put(sample,max_position);
				}
			}
		
			

		g2.dispose();
		g.drawImage(offscreen,0,0,null);
				
			
			
		g.setColor(Color.BLACK);
		g.drawString(extendedRegion.toNiceString()+" Length:"+StringUtils.niceInt(extendedRegion.getLengthOnReference())+
				" Sample(s):"+StringUtils.niceInt(inputBams.size())+" "+label, 10, 10);
		
		if(!sample2maxPoint.isEmpty())
			{
			/** draw sample names */
			g.setColor(Color.BLUE);
			final int sampleFontSize = 7;
			final Font oldFont = g.getFont();
			g.setFont(new Font(oldFont.getName(), Font.PLAIN, sampleFontSize));
			for(final String sample:sample2maxPoint.keySet()) {
				final Point2D pt = sample2maxPoint.get(sample);
				double sny = pt.getY();
				if(sny>y_mid) sny+=sampleFontSize;
				g.drawString(sample, (int)pt.getX(),
						(int)Math.min(this.dimension.height-sampleFontSize,Math.max(sampleFontSize,sny))
						);
				}
			g.setFont(oldFont);
			}
		
		g.dispose();
		final String fname=prefix + extendedRegion.getContig()+"_"+extendedRegion.getStart()+"_"+extendedRegion.getEnd()+
				(StringUtils.isBlank(label)?"":"."+label.replaceAll("[^A-Za-z\\-\\.0-9]+", "_"))+".png";
		try(OutputStream out=archive.openOuputStream(fname)){
			ImageIO.write(image, "PNG", out);
			out.flush();
			}
		manifest.print(rawRegion.getContig());
		manifest.print("\t");
		manifest.print(rawRegion.getStart()-1);
		manifest.print("\t");
		manifest.print(rawRegion.getEnd());
		manifest.print("\t");
		manifest.print(fname);
		manifest.println();
		
		}// end while iter
		archive.close();
		archive=null;
		manifest.flush();
		manifest.close();
		manifest=null;
		return 0;
		}
	catch(final Throwable err)
		{
		LOG.error(err);
		return -1;
		}
	finally
		{
		CloserUtil.close(manifest);
		CloserUtil.close(archive);
		}
	}

public static void main(final String[] args) {
	new CoveragePlotter().instanceMainWithExit(args);
	}

}