package qat.gui;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 *
 */
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Date;
import java.util.Properties;
import java.util.ArrayList;
import java.lang.Thread;

import qat.common.Common;
import qat.common.ProtocolConstants;
import qat.gui.TestSpecification;
import qat.common.Resources;

public class HttpWorker extends HttpQATHandler {
	final static int BUF_SIZE = 2048;
	final static String pageBgCol               = "#CCCCCC";
	final static String fontCol                 = "#FFFFFF";
 	final static String commandTableCol         = "#EEFFE6"; // Run All, Run Passed etc....
	final static String agentTableCol           = "#ADEEDD"; // table with defined agents
	final static String agentTableTitleCol      = "#FFCCCC"; // title of table for defined agents
	final static String loadProjectTableCol     = "#FFE6FF";
	final static String testOutputTableTitleCol = "#FFCCCC"; // the titles for the table containg test traces
	final static String testOutputTableCol      = "#EDFFCC"; // the table containg test traces
	final static String parserOutputTableCol    = "#EDBFCC"; // the table containing the parser output for each test
	final static String parserOutputTableTitleCol = "#FFCCCC"; // the table titles containing the parser output
	final static String includesTableCol          = "#EDFFAC";
	final static String includesTableTitleCol     = "#FFCCCC";
	final static String passedCol                 = "#00FF00";
	final static String pendingCol                = "#DFE43E";
	final static String failedCol                 = "#FF0000";
	final static String notrunCol                 = "#CC33CC";
	final static String unresolvedCol             = "#0000FF";
	final static String otherCol                  = "#ABCDEF";
	final static String testStatusCol             = "#EDFFCC";	
	final static String testStatusTitleCol        = "#FFCCCC";	
	final static String resultSummaryCol          = "#FFFFFF";	
	final static String resultSummaryTitleCol     = "#AFEF11"; 
	final static String testOverviewTitleCol      = "#AFEF11"; 
	final static String testOverviewCol           = "#FFFFFF"; 
	public final static String FILEDETAIL         = "FD@"; 
	public final static String TESTDETAIL         = "TD@"; 
	/* buffer to use for requests */
	byte[] buf;
	/* Socket to client we're handling */
	private Socket socket;
	
	HttpWorker(QAT parent) {
		super(parent);
		buf = new byte[BUF_SIZE];
		socket = null;
	}

	synchronized void setSocket(Socket s) {
		this.socket = s;
		notify();
	}

	public synchronized void run() {
		while(true) {
			if (socket == null) {
				/* nothing to do */
				try {
					wait();
				} catch (InterruptedException e) {
					/* should not happen */
					continue;
				}
			}
			try {
				handleClient();
			} 
			catch (Exception e) {
				//e.printStackTrace();
			}
			/* go back in wait queue if there's fewer
			 * than numHandler connections.
			 */
			socket = null;
			ArrayList pool = HttpQATHandler.threads;
			synchronized (pool) {
				if (pool.size() >= HttpQATHandler.workers) {
					/* too many threads, exit this one */
					return;
				} 
				else {
					pool.add(this);
				}
			}
		}
	}

	private void handleClient() throws Exception {
		InputStream is = new BufferedInputStream(socket.getInputStream());
		PrintStream ps = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
		/* we will only block in read for this many milliseconds
		 * before we fail with java.io.InterruptedIOException,
		 * at which point we will abandon the connection.
		 */
		socket.setSoTimeout(HttpQATHandler.timeout);
		socket.setTcpNoDelay(true);
		/* zero out the buffer from last time */
		for (int i = 0; i < BUF_SIZE; i++) {
			buf[i] = 0;
		}
		try {
			/* We only support HTTP GET/HEAD, and don't
			 * support any fancy HTTP options,
			 * so we're only interested really in
			 * the first line.
			 */
			int nread = 0, r = 0;

		outerloop:
			while (nread < BUF_SIZE) {
				r = is.read(buf, nread, BUF_SIZE - nread);
				if (r == -1) {
					/* EOF */
					return;
				}
				int i = nread;
				nread += r;
				for (; i < nread; i++) {
					if (buf[i] == (byte)'\n' || buf[i] == (byte)'\r') {
						/* read one line */
						break outerloop;
					}
				}
			}

			/* are we doing a GET or just a HEAD */
			boolean doingGet;
			/* beginning of file name */
			int index;
			if (buf[0] == (byte)'G' &&
				buf[1] == (byte)'E' &&
				buf[2] == (byte)'T' &&
				buf[3] == (byte)' ') {
				doingGet = true;
				index = 4;
			} 
			else if (buf[0] == (byte)'H' &&
					 buf[1] == (byte)'E' &&
					 buf[2] == (byte)'A' &&
					 buf[3] == (byte)'D' &&
					 buf[4] == (byte)' ') {
				doingGet = false;
				index = 5;
			} 
			else {
				/* we don't support this method */
				ps.print("HTTP/1.0 " + HTTP_BAD_METHOD +
						 " unsupported method type: ");
				ps.write(buf, 0, 5);
				ps.write(EOL);
				ps.flush();
				return;
			}

			int i = 0;
			/* find the file name, from:
			 * GET /foo/bar.html HTTP/1.0
			 * extract "/foo/bar.html"
			 */
			for (i = index; i < nread; i++) {
				if (buf[i] == (byte)' ') {
					break;
				}
			}
			String fname = (new String(buf, index,i-index)).replace('/', File.separatorChar);
			if (fname.startsWith(File.separator)) {
				fname = fname.substring(1);
			}
	  
			printHeaders(ps);
			
			if (doingGet) {
				sendRequestedPage(fname,ps);
			}
			else {
				send404(fname,ps);
			}
		} 
		finally {
			ps.flush();
			socket.close();
		}
	}

	private void printHeaders(PrintStream ps) throws IOException {
		writeLn(ps,"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");
		writeLn(ps,"<HTML>");
		writeLn(ps,"<HEAD>");
		writeLn(ps,"<TITLE>QAT Browser Interface</TITLE>");
		writeLn(ps,"<BODY BGCOLOR=\""+pageBgCol+"\">");
		writeLn(ps,"<P>");
	}
  
	private void printTailers(PrintStream ps) throws IOException {
		writeLn(ps,"<br>");
		writeHr(ps);
		writeLn(ps,"<A HREF=\"/\">QAT Browser Interface Home</A><BR>");
		writeLn(ps,"</BODY>");	
		writeLn(ps,"</HTML>");	
	}

	void send404(String targ, PrintStream ps) throws IOException {
		ps.write(EOL);
		ps.write(EOL);
		writeH1(ps,"Not Found");
		writeLn(ps,EOL);
		writeLn(ps,"The requested resource was not found :"+targ);
	}
  
	private void sendRequestedPage(String tag, PrintStream ps) throws IOException {
		if (tag.equals("")) {
			sendRoot(ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("RUNTHIS@")>=0) {
			sendRunThis(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf(FILEDETAIL)>=0) {
			sendFileDetail(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("SAVEPROJECT")>=0) {
			sendSaveProject(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("LOADPROJECTFILE")>=0) {
			sendLoadProjectFile(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("LOADPROJECT")>=0) {
			sendLoadProject(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("PROJECTSUMMARY")>=0) {
			sendProjectSummary(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("RUNALL")>=0) {
			sendRunAll(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("RUNFAILED")>=0) {
			sendRunFailed(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("RUNPASSED")>=0) {
			sendRunPassed(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("RUNUNRESOLVED")>=0) {
			sendRunUnresolved(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("RUNNOTRUN")>=0) {
			sendRunNotRun(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf("RUNSTOP")>=0) {
			sendStopRun(tag,ps);
			printTailers(ps);
			return;
		}
		if (tag.indexOf(TESTDETAIL)>=0) {
			sendTestDetail(tag,ps);
			printTailers(ps);
			return;
		}

		send404(tag,ps);
		printTailers(ps);
	}
  
	private void sendRoot(PrintStream ps) throws IOException {
		String summary = "This is the summary table listing all the test contained in the project";
		writeLn(ps,"<table COLS=1 WIDTH=\"100%\" BGCOLOR=\"8000FF\" summary=\""+summary+"\">");
		writeLn(ps,"<tr><td><center><h1>");
		writeLn(ps,"<font color=\""+fontCol+"\">");
		writeH1(ps,Resources.getString("qatHome"));
		writeLn(ps,"</font></h1></center></tr></table>");
		writeHr(ps);
		ArrayList menu = new ArrayList();
		menu.add("<A HREF=\"RUNALL\">"+Resources.getString("runAll")+"</A>");
		menu.add("<A HREF=\"RUNPASSED\">"+Resources.getString("runPassed")+"</A>");
		menu.add("<A HREF=\"RUNFAILED\">"+Resources.getString("runFailed")+"</A>");
		menu.add("<A HREF=\"RUNUNRESOLVED\">"+Resources.getString("runUnresolved")+"</A>");
		menu.add("<A HREF=\"RUNNOTRUN\">"+Resources.getString("runNotRun")+"</A>");
		menu.add("<A HREF=\"RUNSTOP\">"+Resources.getString("stopRun")+"</A>");		
		menu.add("   ");
		menu.add("<A HREF=\"PROJECTSUMMARY\">"+Resources.getString("projectSummary")+"</A>");
		menu.add("<A HREF=\"SAVEPROJECT\">"+Resources.getString("saveProject")+"</A>");
		menu.add("<A HREF=\"LOADPROJECT\">"+Resources.getString("loadProject")+"</A>");
		simpleTable(menu,commandTableCol,menu.size(),ps);
		
		writeHr(ps);
		// write title of this project
		writeH2(ps,getParent().getProjectResultsDirectory());
	
		writeHr(ps);
		// now display the agent information
		sendRunStatus(ps);
		writeHr(ps);
					 
		// send the test table
		sendTestOverview(ps);
		writeLn(ps);
		sendAgentTable(ps);
	}
	
	private void sendAgentTable(PrintStream ps) throws IOException {
		ArrayList agents = AgentInfo.getAgents(getParent().getProperties());
		ArrayList tableForm = new ArrayList(agents.size()*5);
		ArrayList t;
		for (int i = 0 ; i < agents.size(); i++) {
			for (int j = 0; j < 5; j++) {
				t = (ArrayList)agents.get(i);
				tableForm.add(t.get(j));
			}
		}
		ArrayList titles = new ArrayList(4);
		titles.add(Resources.getString("number"));
		titles.add(Resources.getString("name"));
		titles.add(Resources.getString("number"));
		titles.add(Resources.getString("os"));
		titles.add(Resources.getString("architecture"));
		tabulate(null,titles,tableForm,agentTableTitleCol,agentTableCol,ps);
	}
  
	private void sendFileDetail(String tag, PrintStream ps) throws IOException {
		try {
			tag = tag.substring(tag.indexOf(FILEDETAIL)+FILEDETAIL.length(),tag.length());
			tag = URLDecoder.decode(tag);
			sendTextFile(tag,ps);
		}
		catch (Exception e) {
			writeH1(ps,Resources.getString("fileNotFound"));
			writeLn(ps,e.getMessage());
		}
	}
	
	private void sendLoadProject(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			writeH1(ps,Resources.getString("loadProject"));
			File projectDir = new File(Common.getHarnessBaseDirectory());
			String projectFiles[] = projectDir.list();
			ArrayList menu = new ArrayList();
			String projectFileName;
			for (int i = 0; i < projectFiles.length; i++) {
				if (projectFiles[i].toLowerCase().endsWith("."+Common.PROJECT_FILE_SUFFIX)) {
					projectFileName = URLEncoder.encode(projectDir.getCanonicalPath()+File.separator+projectFiles[i]);
					menu.add("<A HREF=\"LOADPROJECTFILE@"+projectFileName+"\">"+projectFiles[i]+"</A>");
				}
			}
			simpleTable(menu,loadProjectTableCol,1,ps);
			writeHr(ps);
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void okResponse(PrintStream ps) throws IOException {
		writeLn(ps,Resources.getString("ok"));
	}
	
	private void errorResponse(PrintStream ps, Throwable ex) throws IOException {
		writeLn(ps,"Error");
		writeLn(ps,ex.toString());
	}
	
	private void sendLoadProjectFile(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			String pft = "LOADPROJECTFILE@";
			String projectFile = tag.substring(tag.indexOf(pft)+pft.length(),tag.length());
			projectFile = URLDecoder.decode(projectFile);
			writeH1(ps,Resources.getString("loadProject")+":"+projectFile);
			try {
				getParent().loadProject(projectFile);
				okResponse(ps);
			}
			catch (Exception e) {
				errorResponse(ps,e);
			}
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void sendSaveProject(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			writeH1(ps,Resources.getString("saveProject"));
			getParent().saveProject(true);
			okResponse(ps);
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void sendProjectSummary(String tag, PrintStream ps) throws IOException {
		writeH1(ps,Resources.getString("projectSummary")+":");				
		writeH2(ps,getParent().getProjectResultsDirectory());				
		writeHr(ps);
		sendRunStatus(ps);
	}
	
	private void sendRunAll(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			writeH1(ps,Resources.getString("running"));
			getParent().selectAll();
			getParent().runSelectedTests();
			okResponse(ps);
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void sendRunPassed(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			writeH1(ps,Resources.getString("running"));
			if (getParent().runPassedTests())
				okResponse(ps);
			else
				writeLn(ps,Resources.getString("noTestsSelected"));
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void sendRunFailed(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			writeH1(ps,Resources.getString("running"));
			if (getParent().runFailedTests())
				okResponse(ps);
			else
				writeLn(ps,Resources.getString("noTestsSelected"));
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void sendRunUnresolved(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			writeH1(ps,Resources.getString("running"));
			if (getParent().runUnresolvedTests())
				okResponse(ps);
			else
				writeLn(ps,Resources.getString("noTestsSelected"));
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void sendRunNotRun(String tag, PrintStream ps) throws IOException {
		if (!getParent().isTestRunning()) {
			writeH1(ps,Resources.getString("running"));
			if (getParent().runNotRunTests())
				okResponse(ps);
			else
				writeLn(ps,Resources.getString("noTestsSelected"));
		}
		else {
			writeH1(ps,Resources.getString("error"));
			writeLn(ps,Resources.getString("runInProgress"));
		}
	}
	
	private void sendRunThis(String tag, PrintStream ps) throws IOException {
		tag = tag.substring(tag.indexOf("RUNTHIS@")+8,tag.length());
		tag = URLDecoder.decode(tag);
		TestSpecification test = getParent().getTestTree().getTestSpecification(tag);
		if (test!=null) {
			if (!getParent().isTestRunning()) {
				getParent().runSingleTest(test);
				writeH1(ps,Resources.getString("running"));
				okResponse(ps);
			}
			else {
				writeH1(ps,Resources.getString("error"));
				writeLn(ps,Resources.getString("runInProgress"));
			}
		}
		else {
			send404(tag,ps);
		}
	}
	
	private void sendStopRun(String tag, PrintStream ps) throws IOException {
		writeH1(ps,Resources.getString("stopCurrentRun"));
		if (getParent().isTestRunning()) {
			getParent().stopTestRun();
			writeLn(ps,Resources.getString("interrupted"));
		}
		else {
			writeLn(ps,Resources.getString("noRunInProgress"));
		}
	}
	
	private void sendTestDetail(String tag, PrintStream ps) throws IOException {
		ArrayList data, titles;
		tag = tag.substring(tag.indexOf("@")+1,tag.length());
		tag = URLDecoder.decode(tag);
		TestSpecification test = getParent().getTestTree().getTestSpecification(tag);
		String fileName;
		if (test!=null) {		
			writeH1(ps, Resources.getString("testName")+":"+test.getTestName());
			// test name, author & detail table
			titles = new ArrayList();
			data = new ArrayList();
			titles.add(Resources.getString("testDescription"));
			titles.add(Resources.getString("testAuthor"));
			titles.add(Resources.getString("testBugInfo"));
			data.add(test.getTestDescription());
			data.add(test.getTestAuthor());
			data.add(test.getTestBugInfo());
			tabulate("",titles,data,testOutputTableTitleCol,testOutputTableCol,ps);
						
			writeHr(ps);
			
			// "run this" command table
			data.clear();
			fileName = URLEncoder.encode(test.getTestSpecPath());
			data.add("<A HREF=\"RUNTHIS@"+fileName+"\">"+Resources.getString("runNode")+"</A>");
			simpleTable(data,commandTableCol,data.size(),ps);
			writeHr(ps);
			
			// output command table
			String commandList[] = test.getViewOutputList();
			titles.clear();
			titles.add("Command");
			titles.add(Resources.getString("environment"));
			titles.add(Resources.getString("standardOutput"));
			titles.add(Resources.getString("standardError"));
			data.clear();
			String tempFileName;
			for (int i = 0; i < commandList.length; i++) {
				data.add(commandList[i]);				
				tempFileName = test.getEnvironmentTraceFileName(getParent().getProjectResultsDirectory(),commandList[i]);
				tempFileName = URLEncoder.encode(tempFileName);
				data.add("<A HREF=\""+FILEDETAIL+tempFileName+"\">"+
							  commandList[i]+" "+Resources.getString("environment")+"</A>");
				tempFileName = test.getStdOutTraceFileName(getParent().getProjectResultsDirectory(),commandList[i]);
				tempFileName = URLEncoder.encode(tempFileName);
				data.add("<A HREF=\""+FILEDETAIL+tempFileName+"\">"+
							  commandList[i]+" "+Resources.getString("standardOutput")+"</A>");
				tempFileName = test.getStdErrTraceFileName(getParent().getProjectResultsDirectory(),commandList[i]);
				tempFileName = URLEncoder.encode(tempFileName);
				data.add("<A HREF=\""+FILEDETAIL+tempFileName+"\">"+
							  commandList[i]+" "+Resources.getString("standardError")+"</A>");
			}
			tabulate("",titles,data,testOutputTableTitleCol,testOutputTableCol,ps);
			writeLn(ps);
	  
			// parser output table
			titles.clear();
			titles.add(Resources.getString("parserOutput"));
			data.clear();
			data.add(getFileContents(test.getParserTraceFileName(getParent().getProjectResultsDirectory())));
			tabulate("",titles,data,parserOutputTableTitleCol,parserOutputTableCol,ps);
			writeLn(ps);
	  
			// properties include table
			titles.clear();
			titles.add(Resources.getString("propIncludes"));
			data.clear();
			String dataA[] = test.getIncludePropList();
			for (int i = 0; i < dataA.length; i++) {
				fileName = URLEncoder.encode(dataA[i]);
				data.add("<A HREF=\""+FILEDETAIL+fileName+"\">"+dataA[i]+"</A>");
			}
			tabulate("",titles,data,includesTableTitleCol,includesTableCol,ps);
			writeLn(ps);
	  
			// misc include table
			titles.clear();
			titles.add(Resources.getString("miscIncludes"));
			data.clear();
			dataA = test.getIncludeMiscList();
			for (int i = 0; i < dataA.length; i++) {
				fileName = URLEncoder.encode(dataA[i]);
				data.add("<A HREF=\""+FILEDETAIL+fileName+"\">"+dataA[i]+"</A>");
			}
			tabulate("",titles,data,includesTableTitleCol,includesTableCol,ps);
			writeLn(ps);
	  	
	  
			// keywords table
			titles.clear();
			titles.add(Resources.getString("keyword"));
			dataA = test.getKeyWords();
			tabulate("",titles,arrayToArrayList(test.getKeyWords()),includesTableTitleCol,includesTableCol,ps);	  
			writeLn(ps);
			
			// write the test status
			titles.clear();
			data.clear();
			titles.add(Resources.getString("testStatus"));
			data.add(test.getStatusString());
// 			switch (test.getStatus()) {
// 			case ProtocolConstants.PASSED :
// 				simpleTable(data,passedCol,2,ps);
// 				break;
// 			case ProtocolConstants.FAILED :
// 				simpleTable(data,failedCol,2,ps);
// 				break;
// 			case ProtocolConstants.UNRESOLVED :
// 				simpleTable(data,unresolvedCol,2,ps);
// 				break;
// 			case ProtocolConstants.NOTRUN :
// 				simpleTable(data,notrunCol,2,ps);
// 				break;
// 			default :
// 				simpleTable(data,otherCol,2,ps);
// 			}
			tabulate("",titles,data,testStatusTitleCol,testStatusCol,ps);
			writeLn(ps);
		}
		else {
			send404(tag,ps);
		}
	}
  
	private void sendTestOverview(PrintStream ps) throws IOException {
		ArrayList tests = getParent().getTestTree().getAllTestsByParent();
		ArrayList testList;
		TestSpecification test;
		String testFileName, nodeTitle, color;
		int projectRootIndex = getParent().getProperty("qat.project.path").length()+1;
		
		for (int j = 0; j < tests.size(); j++) {
			testList = (ArrayList)tests.get(j);
			// set up the node title
			nodeTitle = (String)testList.remove(0);
			nodeTitle = nodeTitle.substring(projectRootIndex,nodeTitle.length());
 			
			// add the description field to the ArrayList
 			for (int i = testList.size()-1; i >=0; i--) {
				test = ((TestSpecification)testList.get(i));
 				switch (test.getStatus()) {
				case ProtocolConstants.PASSED     : 
					color = passedCol;
					break;
				case ProtocolConstants.FAILED     : 
					color = failedCol;
					break;
				case ProtocolConstants.UNRESOLVED :
					color = unresolvedCol;
					break;
				case ProtocolConstants.NOTRUN     : 
					color = notrunCol;
					break;
				case ProtocolConstants.PENDING     : 
					color = pendingCol;
					break;
				default :
					color = otherCol;
				}
				if (test.getStatus()==ProtocolConstants.RUNNING) {
					testList.add(i+1,"<b><blink><font color =\""+color+"\">"+test.getStatusString()+"</font></blink></b>");
				}
				else {
					testList.add(i+1,"<b><font color =\""+color+"\">"+test.getStatusString()+"</font></b>");
				}
 				testList.add(i+1,test.getTestDescription());
 			}

 			// replace the TestSpecification by a link to the test detail.
 			for (int i = 0; i < testList.size(); i+=3) {
 				test = (TestSpecification)testList.get(i); 
 				testFileName = URLEncoder.encode(test.getTestSpecPath());
 				testList.set(i,"<A HREF=\""+TESTDETAIL+testFileName+"\">"+test.getTestName()+"</A>");
 			}
 			// create the table titles
 			ArrayList titles = new ArrayList();
 			titles.add(nodeTitle);
 			titles.add(Resources.getString("testDescription"));
 			titles.add(Resources.getString("testStatus"));
 			writeLn(ps,EOL);
 			tabulate("",titles,testList,testOverviewTitleCol,testOverviewCol,ps);
		}
	}
	
	void sendRunStatus(PrintStream ps) throws IOException {
		Object objArray[] = getParent().calculateStatus();
		String titles[] = {Resources.getString("passed"),
						   Resources.getString("failed"),
						   Resources.getString("unresolved"),
						   Resources.getString("notrun"),
						   Resources.getString("pending")};
		tabulate("Result summary :",arrayToArrayList(titles),arrayToArrayList(objArray),resultSummaryTitleCol,resultSummaryCol,ps);
	}
	
	private void tabulate(String caption, 
						  ArrayList titles, 
						  ArrayList data,
						  String tableColor,
						  PrintStream ps) throws IOException {
		tabulate(caption,titles,data,tableColor,tableColor,ps);
	}
	
	private void tabulate(String caption, 
						  ArrayList titles, 
						  ArrayList data,
						  String titleColor,
						  String tableColor,
						  PrintStream ps) throws IOException {
		String summary = caption;
		writeLn(ps,"<table BORDER BGCOLOR=\""+tableColor+"\" summary=\""+summary+"\">");
		if (caption!=null)
			writeLn(ps,"<caption>"+caption+"</caption>");		
		writeLn(ps,"<tr BGCOLOR=\""+titleColor+"\">");
		for (int i = 0; i < titles.size(); i++) {
			writeLn(ps,"<td><b>"+titles.get(i)+"</b></td>");
		}
		writeLn(ps,"</tr>");
		
		for (int j = 0; (titles.size()*j) < data.size(); j++) {
			writeLn(ps,"<tr>");
			for (int i = 0; i < titles.size(); i++) {
				writeLn(ps,"<td>"+data.get(i+(j*titles.size()))+"</td>");
			}
			writeLn(ps,"</tr>");
		}
		writeLn(ps,"</table>");
	}
  
	private void simpleTable(ArrayList data, 
							 String color,
							 int cols,
							 PrintStream ps) throws IOException {
		String summary = "No summary available";
		writeLn(ps,"<table BORDER BGCOLOR=\""+color+"\" summary=\""+summary+"\">");
		writeLn(ps,"<tr>");
		for (int j = 0; (cols*j) < data.size(); j++) {
			writeLn(ps,"<tr>");
			for (int i = 0; i < cols; i++) {
				writeLn(ps,"<td>"+data.get(i+(j*cols))+"</td>");
			}
			writeLn(ps,"</tr>");
		}
		writeLn(ps,"</table>");
	}
  
	/**
	 * This method will send a text file to the browser. If the <html> tag
	 * is detected in the first line of the input file, it will be sent with no modifications,
	 * else each line will be forced with a <br> tag.
	 */
	private void sendTextFile(String filePath, PrintStream ps) throws IOException {
        BufferedReader in = null ;
		try {
			in = new BufferedReader(new FileReader(filePath));       
			StringBuffer strBuff = new StringBuffer();
			String line;
			boolean htmlDetected = false;
			boolean checked = false;
			while ((line = in.readLine()) != null) {
				if (!checked) {
					if (line.toLowerCase().indexOf("<html>")>=0) {
						htmlDetected = true;
					}
					checked = true;
				}
				if (htmlDetected)
					writeLn(ps,line);
				else
					writeLn(ps,line+"<br>");
			}
		}
		catch (Exception e) {
			writeLn(ps,e.getMessage());
			writeLn(ps,"<br>");
			writeLn(ps,"TextFile:"+filePath);			
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
  
	private String getFileContents(String filePath) throws IOException {
		StringBuffer strBuff = new StringBuffer();
		String line;
		try {
			BufferedReader in = new BufferedReader(new FileReader(filePath));       
			int n;
			while ((line = in.readLine()) != null) {
				strBuff.append(line+"<br>");
			}
			in.close();
		}
		catch (IOException e) {
			strBuff.append(e.getMessage());
		}
		return strBuff.toString();
	}
  
	private void writeH1(PrintStream ps, String s) throws IOException {
		writeLn(ps,"<H1>"+s+"</H1>");
	}
	
	private void writeH2(PrintStream ps, String s) throws IOException {
		writeLn(ps,"<H2>"+s+"</H2>");
	}
	
	private void writeH3(PrintStream ps, String s) throws IOException {
		writeLn(ps,"<H3>"+s+"</H3>");
	}	
  
	private void writeLn(PrintStream ps, byte[] b) throws IOException {
		ps.write(b);
	}
  
	private void writeLn(PrintStream ps) throws IOException {
		writeLn(ps,"<br>");
	}
	
	private void writeHr(PrintStream ps) throws IOException {
		writeLn(ps,"<hr>");
	}
	
	
	private void writeLn(PrintStream ps, String s) throws IOException {
		ps.print(s);
		ps.write(EOL);
	}
	
	private ArrayList arrayToArrayList(Object obj[]) {
		ArrayList res = new ArrayList(obj.length);
		for (int i = 0; i < obj.length; i++)
			res.add(obj[i]);
		return res;
	}
}
