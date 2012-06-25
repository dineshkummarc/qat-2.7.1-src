package qat.gui;

// @(#)HttpReport.java 1.16 01/06/12 

// java import
import java.net.*;
import java.io.*;
import java.util.*;
//

// qat import
import qat.gui.*;
import qat.common.*;
import qat.components.*;
//

public class HttpReport extends Object {
	
	private ArrayList processedLinks;
	private StatusWindow status;
	private QAT parent;
	
    public HttpReport(QAT parent, String urlString, String baseDir, StatusWindow status) {
		// check if the basedir exists
		if (!(new File(baseDir).exists())) {
			String message = "Error - cannot generate report, directory does not exist:"+baseDir;
			if (status == null) {
				System.out.println(message);
			}
			else {
				status.setMessage(message);
			}
			return;
		}
		this.parent = parent;
		this.status = status;
		processedLinks = new ArrayList();
		try {
			processURL(new URL(urlString),baseDir,status);
			writeDeadLinks(baseDir);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			processedLinks.clear();
		}
    }
	
	private void writeDeadLinks(String dirBase) throws IOException {
		dirBase += File.separator;
		writeDeadFile(dirBase+"SAVEPROJECT.html");
		writeDeadFile(dirBase+"LOADPROJECT.html");
		writeDeadFile(dirBase+"RUNALL.html");
		writeDeadFile(dirBase+"RUNPASSED.html");
		writeDeadFile(dirBase+"RUNFAILED.html");
		writeDeadFile(dirBase+"RUNUNRESOLVED.html");
		writeDeadFile(dirBase+"RUNNOTRUN.html");
		writeDeadFile(dirBase+"RUNSTOP.html");
		writeDeadFile(dirBase+"RUNTHIS.html");
	}
	
	private void writeDeadFile(String fileName) {
		try {
			PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileName)));
			out.println("<TITLE>Error</TITLE>");
			out.println("<BODY BGCOLOR=\"white\">");
			out.println("<h1>Operation not allowed</h1>");
			out.println("<hr>");
			out.println("<P>This operation only works for live Qat instances");
			out.println("<hr>");
			out.println("<A HREF=\"./index.html\">Report Index</A><BR>");
			out.println("</BODY>");	
			out.println("</HTML>");	
			out.flush();
			out.close();
		}
		catch (IOException e) {
			System.out.println("Error writing page:"+fileName+" "+e.toString());
		}
	}
	
	private boolean processURL(URL url, String baseDir, StatusWindow status) throws IOException {		
		if (processedLinks.contains(url)) {
			return false;
		}
		else {
			processedLinks.add(url);
		}
		URLConnection connection = url.openConnection();
		InputStream in = new BufferedInputStream(connection.getInputStream());
        ArrayList list = processPage(in, baseDir, url) ;
		if ((status!=null)&&
			(list.size()>0)) {
			status.setMaximum(list.size());
		}
		for (int i = 0; i < list.size(); i++) {
			if (status!=null) {
				status.setMessage(Utils.trimFileName(list.get(i).toString(),40),i);
			}
			if ((!((String)list.get(i)).startsWith("RUN"))&&
				(!((String)list.get(i)).startsWith("SAVE"))&&
				(!((String)list.get(i)).startsWith("LOAD"))) {
				processURL(new URL(url.getProtocol(),
								   url.getHost(),
								   url.getPort(),
								   (String)list.get(i)),
						   baseDir,
						   status);
			}
		}
		in.close();
		return true;
	}
	
	/**
	 * This method takes a string, and replaces all file separators and space etc
	 * by normal characters.
	 */
	private String niceifyLink(String link) {
		link = URLDecoder.decode(link);
		if (link.equals("/"))
			return "index.html";
		
		int i1, i2;
		// trim off the project path if it exists 
		String testDir = parent.getProperty("qat.project.path"); //"/local/workspaces/QAT/examples"
		i1 = link.indexOf(testDir);
		if (i1>=0) {
			link = link.substring(0,i1)+link.substring(i1+testDir.length()+1,link.length());
		}
		
		// trim off the qat basedir path if it exists
		String projectResultsDir = parent.getProjectResultsDirectory(); //"/home/webhiker/.qat/harness/results/examples"
		i1 = link.indexOf(projectResultsDir);
		if (i1>=0) {
			link = link.substring(0,i1)+link.substring(i1+projectResultsDir.length()+1,link.length());
		}
		
		link = link.replace('\\','_').replace('/','_').replace(' ','_').replace(':','_')+".html";	
		return link;
	}
		
	private ArrayList processPage(InputStream in, String baseDir, URL url) throws IOException {
		BufferedReader buffIn = new BufferedReader(new InputStreamReader(in));
		String line;
		ArrayList urlList = new ArrayList();
		String fileName;
		if (url.getFile().equals("/"))
			fileName = "index";
		else {
			fileName = url.getFile();
		}
		fileName = baseDir+File.separator+niceifyLink(fileName);
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileName)));

		while ((line=buffIn.readLine()) != null) {
			out.print(checkForLinks(line, urlList));			
		}

		if (out != null) {
			out.flush();
			out.close();
		}
        return urlList ;
    }
    
	/** 
	 * Scans the html string and checks for <A HREF= > tags, 
	 * then adds them to a ArrayList and returns the modified html string.
	 */
	private String checkForLinks(String htmlStr, ArrayList urlList) {
		String STARTTAG="<A HREF=\"";
		String ENDTAG="\">";
		String urlString;
		int startTag = 0;
		int endTag = 0;
		while ((startTag!=-1)&&(endTag!=-1)) {
			startTag = htmlStr.toUpperCase().indexOf(STARTTAG,endTag);
			endTag = htmlStr.indexOf(ENDTAG,startTag);
			if ((startTag!=-1)&&(endTag!=-1)) {
				urlString = htmlStr.substring(startTag+STARTTAG.length(),endTag);
				urlList.add(urlString);
				// now nicify this link
                StringBuffer htmlStrBuffer = new StringBuffer(htmlStr) ;
				htmlStrBuffer.replace(startTag+STARTTAG.length(), endTag, niceifyLink(urlString));
                htmlStr = htmlStrBuffer.toString() ;
				endTag = htmlStr.indexOf(ENDTAG,startTag)+ENDTAG.length();
			}
		}
		return htmlStr;
	}
}
