package com.github.lindenb.jvarkit.tools.validatorserver;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;
import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.lang.StringUtils;
import com.github.lindenb.jvarkit.rdf.ns.RDF;
import com.github.lindenb.jvarkit.util.jcommander.Launcher;
import com.github.lindenb.jvarkit.util.jcommander.Program;
import com.github.lindenb.jvarkit.util.log.Logger;
import com.github.lindenb.jvarkit.util.svg.SVG;

import htsjdk.samtools.util.RuntimeIOException;

/**
BEGIN_DOC
## input

input a set of html+svg generated with coverageplotter or one file with the suffix '.list' containing the path to the files.


## example:

```
java -jar dist/jvarkit.jar cnvvalidatorserver path/*.html 
```

or 

```
find /path/to -type f -name "*.html" > review.list
java -jar dist/jvarkit.jar cnvvalidatorserver review.list
```

## Screenshot

https://twitter.com/yokofakun/status/1563132159417757696



END_DOC 

 */

@Program(
		name="cnvvalidatorserver",
		description="Review files generated by coverageplotter",
		keywords={"server","xml","rdf","cnv"},
		creationDate="20220818",
		modificationDate="20220826",
		jvarkit_amalgamion = true,
		generate_doc = true
		)
public class CNVValidatorServer extends Launcher {
	private static final Logger LOG = Logger.build(CNVValidatorServer.class).make();
	private static final String U1087="https://umr1087.univ-nantes.fr/";
	private static final String ACTION_NAVIGATION="navigation";
	private static final String ACTION_GOTO_PAGE ="goto_page";
	@Parameter(names="--port",description="server port.")
	private int serverPort = 8080;

	private static class SimpleNsContext implements NamespaceContext {	 
		private Map<String,String> map = new HashMap<>();
		SimpleNsContext() {
			this.map.put("u", U1087);
			this.map.put("u1087", U1087);
			this.map.put("rdf", RDF.NS);
			this.map.put("svg", SVG.NS);
			}

		
	    public String getNamespaceURI(String prefix) {
	    	return this.map.getOrDefault(prefix, null);
	    }
	 
	    public String getPrefix(final String namespaceURI) {
	        return map.entrySet().stream().filter(KV->KV.getValue().equals(namespaceURI)).map(KV->KV.getKey()).findFirst().orElse(null);
	    }
	 
	    public Iterator<String> getPrefixes(final String namespaceURI) {
	        return map.entrySet().stream().filter(KV->KV.getValue().equals(namespaceURI)).map(KV->KV.getKey()).iterator();
	    	}
		}

	
	
	private static class HtmlFile {
		final File file;
		HtmlFile(File file) {
			this.file = file;
			}
		String getId() {
			return StringUtils.md5(this.file.toString());
			}
		Document load()  {
			return CNVValidatorServer.load(this.file);
			}
		}

	
	@SuppressWarnings("serial")
	private class ValidatorServlet extends HttpServlet {
		private final List<HtmlFile> htmlFiles;
		private Random rnd = new Random();

		ValidatorServlet(List<HtmlFile> list) {
			this.htmlFiles =  Collections.unmodifiableList(list);
		}
		
		
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			doPost(req, resp);
			}
		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			String id = request.getParameter("ID");
			String go = request.getParameter(ACTION_NAVIGATION);
			int n;
			final String gotoPage = request.getParameter(ACTION_GOTO_PAGE);
			if(!StringUtils.isBlank(gotoPage) && StringUtils.isInteger(gotoPage)) {
				n = Integer.parseInt(gotoPage)-1;
				if(n<0) n =0;
				if(n>= this.htmlFiles.size()) n=this.htmlFiles.size()-1;
				}
			else if(!StringUtils.isBlank(id)) {
				for(n=0;n< this.htmlFiles.size();++n) {
					final HtmlFile h = this.htmlFiles.get(n);
					if(h.getId().equals(id)) {
						LOG.info("got page "+h.file);

						if("prev".equalsIgnoreCase(go)) {
							LOG.info("prev");
							n=n-1;
							break;
							}
						if("next".equalsIgnoreCase(go)) {
							LOG.info("next");
							n=n+1;
							break;
							}
						updateHtml(request,h);
						n++;
						break;
						}
					}
				}
			else
				{
				n = rnd.nextInt(this.htmlFiles.size());
				}
			if(n<0) n=this.htmlFiles.size()-1;
			if(n>=this.htmlFiles.size()) n=0;
			
			LOG.info("path info:"+request.getPathInfo()+
					" method:"+request.getMethod() +
					" ctx path:"+request.getContextPath());
			showPage(request,response,n);
			}
		
		private Element xpathElement(Node node,String xpathExpr) {
			try {
				if(node==null) return null;
				final XPath xpath = XPathFactory.newInstance().newXPath();
				xpath.setNamespaceContext(new SimpleNsContext());
				return (Element)xpath.evaluate(xpathExpr, node, XPathConstants.NODE);
				}
			catch(XPathExpressionException err) {
				LOG.error("Cannot eval {"+xpathExpr+"}",err);
				return null;
				}
			}

		
		private Element getVariantNode(Document dom) {
			return xpathElement(dom,"//*[local-name()='Variant']");
			}
		
		private void updateHtml(HttpServletRequest request,final HtmlFile html)  throws IOException {
			try {
				XPath xpath = XPathFactory.newInstance().newXPath();
				xpath.setNamespaceContext(new SimpleNsContext());
				Document dom = html.load();
				Element variant  = getVariantNode(dom);
				if(variant!=null) {
					final String prefix = variant.getPrefix();
					final String ns = variant.getNamespaceURI();
					LOG.info(""+prefix+":"+ns);
					List<Element> to_remove = new ArrayList<>();
					for(Node c=variant.getFirstChild();c!=null;c=c.getNextSibling()) {
						if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
						Element E = Element.class.cast(c);
					
						if(E.getLocalName().equals("filter") ||
							E.getLocalName().equals("comment") ||
							E.getLocalName().equals("reviewer") ||
							E.getLocalName().equals("updated")) {
							to_remove.add(E);
							}
						}
					for(Element e:to_remove) {
						variant.removeChild(e);
					}
					String status = StringUtils.ifBlank(request.getParameter("filter"),".");
					Element filter = dom.createElementNS(ns, prefix+":filter");
					variant.appendChild(filter);
					filter.appendChild(dom.createTextNode(status));
					
					Element updated = dom.createElementNS(ns, prefix+":updated");
					variant.appendChild(updated);
					updated.appendChild(dom.createTextNode(StringUtils.now()));
					
					
					String comment = StringUtils.ifBlank(request.getParameter("comment"),"");
					if(!StringUtils.isBlank(comment)) {
						Element comm = dom.createElementNS(ns, prefix+":comment");
						variant.appendChild(comm);
						comm.appendChild(dom.createTextNode(comment));
						}

					Element reviewer = dom.createElementNS(ns, prefix+":reviewer");
					variant.appendChild(reviewer);
					reviewer.appendChild(dom.createTextNode(System.getProperty("user.name",".")));

					
					TransformerFactory tf = TransformerFactory.newInstance();
					Transformer tr = tf.newTransformer();
					tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					tr.setOutputProperty(OutputKeys.METHOD, "xml");
					final File copy = new File(html.file.getParentFile(),html.file.getName()+".tmp");
					LOG.info("updating "+html.file);
					tr.transform(new DOMSource(dom),new StreamResult(copy));
					copy.renameTo(html.file);
					}
				else
					{
					LOG.info("cannot find rdf:Variant");
					}
				}
			catch(Throwable err) {
				LOG.error(err);
				throw new IOException(err);
				}
			}
		
		private Document convert(HttpServletRequest request,final int page_index) throws XPathExpressionException {
			final HtmlFile html = this.htmlFiles.get(page_index);
			final XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new SimpleNsContext());
			final Document dom = html.load();
			
			Element root = dom.getDocumentElement();
			if(root==null) {
				LOG.error("empty xml in " +html.file );
				return dom;
				}
			// document is a plain svg doc, embed in html
			if("svg".equals(root.getLocalName())) {
				final Element h = dom.createElement("html");
				final Element b = dom.createElement("body");
				h.appendChild(b);
				b.appendChild(root);
				dom.appendChild(h);
				}
			


			Element body=(Element)xpath.evaluate("/html/body", dom, XPathConstants.NODE);
			Element variant  =getVariantNode(dom);
			if(body!=null) {
				Element div = dom.createElement("div");
				
				
				body.insertBefore(div, body.getFirstChild());

				final Element script = dom.createElement("script");
				div.appendChild(script);
				script.appendChild(dom.createTextNode(
					"function keyDownTextField(e) {var k=e.keyCode; var s='xx'; if(k==80 || k==84) {s='bp'} else if(k==65) {s='ba';} else if(k==70) {s='bf';} var E=document.getElementById(s); if(E!=null) E.click();} "+ 
					"document.addEventListener('keydown', keyDownTextField, false);"
					));


				div.setAttribute("style"," background-color:darkgray;border-style:dotted;");
				
				Element form = dom.createElement("form");
				form.setAttribute("method", "GET");
				form.setAttribute("action", request.getPathInfo());
				div.appendChild(form);
				
				Element input = dom.createElement("input");
				input.setAttribute("type", "hidden");
				input.setAttribute("name", "ID");
				input.setAttribute("value",html.getId());
				form.appendChild(input);

				
				
				
				input.appendChild(dom.createTextNode(" "));
				input = dom.createElement("button");
				input.setAttribute("name", ACTION_NAVIGATION);
				input.setAttribute("value", "prev");
				input.appendChild(dom.createTextNode("\u21DA"));
				form.appendChild(input);

				
				String prevFilter = ".";
				Element filter = xpathElement(variant, "*[local-name()='filter']");
				if(filter!=null) {
					prevFilter = filter.getTextContent();
				}

				
				input = dom.createElement("input");
				input.setAttribute("style", "font-size:"+(prevFilter.equals("FAIL")?"200%":"100%")+";background-color:red;");
				input.setAttribute("type", "submit");
				input.setAttribute("name", "filter");
				input.setAttribute("id", "bf");
				input.setAttribute("value", "FAIL");
				form.appendChild(input);

				form.appendChild(dom.createTextNode(" "));
				
				
				input = dom.createElement("input");
				input.setAttribute("style", "font-size:"+(prevFilter.equals("AMBIGOUS")?"200%":"100%")+";background-color:orange;");
				input.setAttribute("type", "submit");
				input.setAttribute("name", "filter");
				input.setAttribute("id", "ba");
				input.setAttribute("value", "AMBIGOUS");
				form.appendChild(input);

				form.appendChild(dom.createTextNode(" "));

				
				input = dom.createElement("input");
				input.setAttribute("style", "font-size:"+(prevFilter.equals("PASS")?"200%":"100%")+";background-color:green;");
				input.setAttribute("type", "submit");
				input.setAttribute("name", "filter");
				input.setAttribute("id", "bp");
				input.setAttribute("value", "PASS");
				form.appendChild(input);
				
				
				input = dom.createElement("label");
				input.setAttribute("for", "form_comment");
				input.appendChild(dom.createTextNode("Comment:"));
				form.appendChild(input);

				
				input = dom.createElement("input");
				input.setAttribute("id", "form_comment");
				input.setAttribute("type", "text");
				input.setAttribute("placeholder", "Any comment about this variant.");
				input.setAttribute("name", "comment");
				input.setAttribute("onkeydown","return event.key != 'Enter';");
				input.setAttribute("value", "");
				form.appendChild(input);

				Element comment = xpathElement(variant, "*[local-name()='comment']");
				if(comment!=null) {
					input.setAttribute("value", comment.getTextContent());
				}
				if(!StringUtils.isBlank(prevFilter)) {
					input.appendChild(dom.createTextNode(" Previous status was: "+prevFilter));
				}	
			
				input.appendChild(dom.createTextNode(" "));
				input = dom.createElement("label");
				input.setAttribute("for", "form_goto");
				input.appendChild(dom.createTextNode("Go To:"));
				form.appendChild(input);

				
				input = dom.createElement("input");
				input.setAttribute("id", "form_goto");
				input.setAttribute("type", "text");
				input.setAttribute("placeholder", "[1-"+htmlFiles.size()+"]");
				input.setAttribute("name", ACTION_GOTO_PAGE);
				input.setAttribute("size", "6");
				input.setAttribute("value", "");
				form.appendChild(input);
				
					
				input.appendChild(dom.createTextNode(" "));
				input = dom.createElement("button");
				input.setAttribute("name", ACTION_NAVIGATION);
				input.setAttribute("value", "next");
				input.appendChild(dom.createTextNode("\u21DB"));
				form.appendChild(input);
				
				form.appendChild(dom.createTextNode((page_index+1)+"/"+this.htmlFiles.size()));

				}
			else
				{
				LOG.warn("cannot find html/body");
				}
			return dom;
			}
		
		private void showPage(HttpServletRequest request, HttpServletResponse response,int page_index) throws ServletException, IOException {
			try {
				
				Document dom = convert(request,page_index);
				response.setContentType("text/html");
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer tr = tf.newTransformer();
				tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				try(PrintWriter pw = response.getWriter()) {
					tr.transform(new DOMSource(dom),new StreamResult(pw));
					pw.flush();
					}
				}
			catch(Throwable err) {
				LOG.error(err);
				throw new IOException();
				}
			}
		}
		
	
	
	private static Document load(File xml) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db=dbf.newDocumentBuilder();
			return db.parse(xml);
			}
		catch(final Throwable err) {
			throw new RuntimeIOException(err);
		}
	}
	
	@Override
	public int doWork(final List<String> args) {

		
		try {
			final List<HtmlFile> htmlFiles = IOUtils.unrollFiles2018(args).stream().map(H->new HtmlFile(H)).collect(Collectors.toList());
			if(htmlFiles.isEmpty()) {
				LOG.info("no file was specified.");
				return -1;
				}
			// test
			for(HtmlFile html: htmlFiles) html.load();
			
			
			final Server server = new Server(this.serverPort);
			
			final ServletContextHandler context = new ServletContextHandler();
			final ValidatorServlet vs = new ValidatorServlet(htmlFiles);
			final ServletHolder sh =new ServletHolder(vs);
	        context.addServlet(sh,"/*");
	        context.setContextPath("/");
	        context.setResourceBase(".");
	        server.setHandler(context);
			
	        final Runnable stop = ()->{
                try {
                    LOG.info("Shutting down ...");

                    sh.stop();
                    server.stop();
                	}
                catch (Exception e) {
                    Thread.currentThread().interrupt();
                    LOG.error(e);
                	}
	        
	        	};
	        
	        Runtime.getRuntime().addShutdownHook(new Thread(stop));
		    
		    LOG.info("Starting server "+getProgramName()+" on port "+this.serverPort);
		    server.start();
		    LOG.info("Server started. Press Ctrl-C to stop. Check your proxy settings ."
		    		+ " Open a web browser at http://localhost:"+this.serverPort+"/validator .");
		    server.join();
		    stop.run();
		    return 0;
			}
		catch (final Throwable err) {
			LOG.error(err);
			return -1;
			}
		
		}	


public static void main(final String[] args) throws Exception{
    new CNVValidatorServer().instanceMainWithExit(args);
	}


}
