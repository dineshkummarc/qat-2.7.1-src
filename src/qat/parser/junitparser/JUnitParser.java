package qat.parser.junitparser;

// JDK imports
import java.io.*;
import java.util.*;
import java.lang.*;
import java.net.*;
import javax.swing.*;

// qat imports
import qat.parser.ParserInterface;
import qat.parser.AgentInstance;
import qat.parser.HtmlPrintStream;
import qat.common.Common;
import qat.common.ProtocolConstants;
import qat.common.Utils;
import qat.agent.ExecProcess;

// junit imports
import junit.framework.*;
import junit.runner.*;

/** 
 * This file loads a single QAT file, and will attempt to resolve all keywords in this qat file
 * file by first including any .INC statements, and their parent statements etc, until all neccesary files
 * have been included.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 *
 */
public class JUnitParser extends BaseTestRunner implements TestListener, ParserInterface {
  
    private HtmlPrintStream printStream;
    private Properties properties;
    private String name, author, description, buginfo;
    private boolean evaluating, interrupted = false;
    private String testPath;
    private int status = ProtocolConstants.PASSED;
    private LineNumberInputStream lineNumberInputStream;
    private String projectRoot, projectResultsDir;
    private long startTime;

    public JUnitParser() {
    }

    /**
     * This method sets the path to root of the current project.
     */
    public void setProjectRoot(String projectRoot) {
	this.projectRoot = projectRoot;
    }

    /**
     * This method lists all the keywords supported by this syntax, for use in
     * the Notepad syntax highlighting. It must be declared static.
     */
    public String[] getSyntaxKeyWords() {
	return new String[0];
    }
	
    /**
     * This method sets the path to file file containing the syntax
     * which will be parsed.
     */
    public void setTestPath(String testPath) {
	this.testPath = testPath;
    }
	
    /**
     * This method sets the PrintStream to use for reporting errors
     * and other types of output from the script.
     */
    public void setPrintStream(PrintStream printStream, boolean useHtml) {
	if (printStream instanceof HtmlPrintStream) {
	    this.printStream = (HtmlPrintStream)printStream;
	}
	else {
	    this.printStream = new HtmlPrintStream(printStream,useHtml);
	}
    }
	
    /**
     * This method sets any default properties which will be required 
     * for parsing this file.
     */
    public void setProperties(java.util.Properties p) {
	this.properties = p;
    }
		
    /**
     * This method returns all the properties obtained by parsing this test file.
     */
    public java.util.Properties getProperties() {
	return properties;
    }

    public String getProperty(String name) {
	return "UNDEFINED";
    }

    public String getProperty(String name, String defaultValue) {
	return defaultValue;
    }

    public String getTestName() {
	return name;
    }

    public String getTestAuthor() {
	return "unknown";
    }

    public String getTestDescription() {
	return "unknown";
    }

    public String getTestBugInfo() {
	return "unknown";
    }

    public String[] getKeyWords() {
	return new String[0];
    }

    /**
     * This should return the list of files other than standard java.util.Properties files which were
     * included to parse this test file.
     */
    public String[] getIncludeList() {
	return new String[0];
    }

    /**
     * This should return the list of standard java.util.Properties files which were
     * included to parse this test file.
     */	
    public String[] getPropertiesIncludeList() {
	return new String[0];
    }

    /**
     * This method should list all available output files produced by 
     * this test when run on the agent, but relative to the harness.
     */
    public String[] getTraceList() {
	return new String[0];
    }

    /**
     * This is called at the beginning of a parser run on
     * one or more tests.
     * @param projectResultsDir - the canonical pathname of
     * the project file, used to decide where to place 
     * the parser trace files.
     */
    public void prepare(String projectResultsDir) {
	//System.out.println("prepare>>>"+projectResultsDir);
    }

    /**
     * If set to true, the parser does not actually make contact with the agents
     * but merely simulates the agent responses to allow standalone parsing.
     */
    public final void setEvaluationMode(boolean mode) {
	evaluating = mode;
    }
	
    public final boolean inEvaluationMode() {
	return evaluating;
    }
	
    /**
     * This method parses the specified file.
     * If not in evaluation mode, it should return the status of the test run :
     * ProtocolConstants.PASSED
     * ProtocolConstants.FAILED
     * ProtocolConstants.NOTRUN
     * ProtocolConstants.UNRESOLVED
     */
    public int parseFile() throws Exception {
	String className = JUnitTestFinder.convertPathToClassName(testPath,projectRoot);
	FileClassLoader classLoader = new FileClassLoader(projectRoot);
	Class testClass = classLoader.loadClass(className);
	TestSuite suite = new TestSuite(testClass);
	name = suite.getName();
	if (name==null) {
	    name = testClass.getName();
	}
	if (inEvaluationMode()) {
	    return ProtocolConstants.PASSED;
	}
	else {
	    junit.framework.TestResult testResult = run(suite);

	    printStream.print("<hr>");
	    if (testResult.wasSuccessful()) {
		printDebug("Run was successfull");
		printStream.print("<hr>");
		return ProtocolConstants.PASSED;
	    }
	    else {
		printError("Run failed");
		printError("Found :"+testResult.errorCount()+" errors");
		for (Enumeration errors = testResult.errors() ; errors.hasMoreElements() ;) {
		    printError(errors.nextElement().toString());
		}
		printError("Found :"+testResult.failureCount()+" failures");
		for (Enumeration errors = testResult.failures() ; errors.hasMoreElements() ;) {
		    printError(errors.nextElement().toString());
		}
		printStream.print("<hr>");
		return ProtocolConstants.FAILED;
	    }
	}
    }

    public void interrupt() {
    }


    /**
     * Returns a handle to the Printstream the parser will use for any output
     * resulting from parsing this test.
     */
    public PrintStream openPrintStream(String fileName) throws java.io.FileNotFoundException {
	return new HtmlPrintStream(new PrintStream(new FileOutputStream(fileName),true),true);	
    }
    
    /*
     * This method centralises all the traces printed by the parser.
     */
    public synchronized void printDebug(String msg) {
	printStream.printBold("[ ");
	printStream.print(HtmlPrintStream.BLUE,"Debug");
	printStream.printBold(" ] ");
	printStream.print(msg);
	printStream.println();
    }
	
    private synchronized void printError(String msg) {
	printStream.printBold("[ ");
	printStream.print(HtmlPrintStream.RED,"Error");
	printStream.printBold(" ] ");
	printStream.print(msg);
	printStream.println();
    }

    private synchronized void printFailure(String msg) {
	printStream.printBold("[ ");
	printStream.print(HtmlPrintStream.PURPLE,"Failure");
	printStream.printBold(" ] ");
	printStream.print(msg);
	printStream.println();
    }

    /**
     * This method indicates we are finished with this parser, and disposes
     * any reserved resources.
     */
    public void finish() {
	printStream.flush();
	printStream.close();
    }
	
    /**
     * This is the handle to to QAT parent GUI to display which commands
     * the parser is processing in real-time.
     */
    public void setStatusLabel(JLabel status) {
    }

    // the basetesrunner implementations
    public void runFailed(java.lang.String msg) {
	printError(msg);
    }

    public void testFailed(int i,junit.framework.Test test, java.lang.Throwable t) {
	printError("index="+i+" name="+test.toString()+" ex="+t.toString());
    }

    public void testEnded(java.lang.String msg) {
	printDebug(msg);
    }

    public void testStarted(java.lang.String msg) {
	printDebug(msg);
    }

    public TestResult run(Test test) {
	return doRun(test);
    }
    
    public TestResult doRun(Test test) {
	return doRun(test, false);
    }
    
    public TestResult doRun(Test test, boolean wait) {
	TestResult result = new TestResult();
	result.addListener(this);
	startTime= System.currentTimeMillis();
	test.run(result);
	return result;
    }


    public synchronized void startTest(Test test) {
	printStream.print("<hr>");
	printDebug("Starting "+test.toString());
    }

    public synchronized void endTest(Test test) {
	long runTime= System.currentTimeMillis()-startTime;
	printDebug("Finished "+test.toString()+" ("+runTime+"ms)");
    }
    
    public synchronized void addError(Test test, Throwable t) {
	printError(test.toString());
	t.printStackTrace(printStream);
    }
    
    public synchronized void addFailure(Test test, AssertionFailedError t) {
	printFailure(test.toString());
	t.printStackTrace(printStream);
    }
    
    public static final void main(String args[]) {
	JUnitParser parser = new JUnitParser();
    }
}
