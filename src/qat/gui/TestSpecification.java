package qat.gui;
/**
 * This object encapsulates the harness's view of a test, hiding the way in which
 * keywords are used etc.
 *
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.lang.Runtime;
import java.lang.reflect.Constructor;
import javax.swing.JComponent;

import qat.common.Utils;
import qat.common.ProtocolConstants;
import qat.common.Common;
import qat.common.Resources;
import qat.parser.ParserInterface;

public class TestSpecification extends Object {
    private static final short testName        = 0;
    private static final short testAuthor      = 1;
    private static final short testDescription = 2;
	
    private static final short keyWordList     = 3;  // list of keywords, as they are resolved by the parser
    private static final short includeMiscList = 4;
    private static final short includePropList = 5;
    private static final short traceList       = 6;
    private static final short testSpec        = 7;
    private static final short status          = 8;
    private static final short testBugInfo     = 9;
	
    private static final short propertyCount   = 10;
    private Object[] propertyValue;
	
    private static ParserInterface currentParser;
	
    public TestSpecification(String path) {
	propertyValue = new Object[propertyCount];
	propertyValue[testSpec] = path;
	setStatus(ProtocolConstants.NOTRUN);
	setParser(null);
    }
	
    public TestSpecification(java.io.ObjectInputStream in, String version) throws IOException, ClassNotFoundException {
	readData(in,version);
    }
	
    public TestSpecification(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	readData(in,Common.SERIALIZED_NODE_HEADERV10);
    }
	
    public boolean equals(Object o) {
	if (o instanceof TestSpecification) {
	    return getTestSpecPath().equals(((TestSpecification)o).getTestSpecPath());
	}
	else {
	    return false;
	}
    }
	
    public String getTestSpecPath() {
	return (String)propertyValue[testSpec];
    }
	
    private ParserInterface getParser() {
	return currentParser;
    }
	
    private void setParser(ParserInterface p) {
	currentParser = p;
    }
	
    /**
     * Global static method for all interested classes to get a handle on 
     * the parser.
     */
//     public static ParserInterface getParser(Properties defaultProperties) throws Exception {
// 	String className = defaultProperties.getProperty(Common.PARSER_CLASSNAME,
// 							 Common.DEFAULT_PARSER_CLASSNAME);
		
// 	Class c = (Class.forName(className));
// 	Constructor list[] = c.getConstructors();
// 	for (int i = 0; i < list.length; i++) {
// 	    // check for default constructor
// 	    if (list[i].getParameterTypes().length==0) {
// 		return (ParserInterface)list[i].newInstance(null);
// 	    }
// 	}
// 	return null; // no constructor found....
//     }
	
    private void closePrintStream(PrintStream printStream) {
	if ((printStream != null)&&
	    (printStream != System.out)) {
	    printStream.close();
	}
    }

    private PrintStream openPrintStream(ParserInterface parserInstance, String fileName) {
	PrintStream printStream;
	try {
	    Utils.touch(fileName);
	    printStream = parserInstance.openPrintStream(fileName);
	}
	catch (Exception e) {
	    System.out.println("Error opening parser trace stream - using stdout instead :"+e.toString());
	    printStream = System.out;
	}
	return printStream;
    }

    private void parseTest(String projectResultsDirectory,
			   Properties defaultProperties, 
			   boolean evaluationMode, 
			   ParserInterface parserInstance) {
	setParser(parserInstance);
	defaultProperties = new Properties(defaultProperties);
	int result = ProtocolConstants.UNRESOLVED;
	PrintStream printStream = openPrintStream(parserInstance, getParserTraceFileName(projectResultsDirectory));

	try {			
	    parserInstance.setTestPath(getTestSpecPath());
	    parserInstance.setPrintStream(printStream,true);
	    parserInstance.setProperties(Utils.mergeProperties(defaultProperties,System.getProperties()));
	    parserInstance.setEvaluationMode(evaluationMode);
	    Date date1, date2;
	    parserInstance.printDebug(Resources.getString("startParse")+(date1 = new Date()).toString());
	    result = parserInstance.parseFile();
	    parserInstance.printDebug(Resources.getString("endParse")+(date2 = new Date()).toString()+" ( "+(date2.getTime()-date1.getTime())+"ms )");
	    // if no errors occurred in evaluationMode, 
	    // treat the test as NOTRUN instead of PASSED
	    if (evaluationMode) {
		if (result==ProtocolConstants.PASSED)
		    result = ProtocolConstants.NOTRUN;
		else {
		    result = ProtocolConstants.UNRESOLVED;					
		}
	    }
	    updateProperties(parserInstance);
	}
	catch (Throwable e) {
	    System.out.println("Caught error while running the parser on :"+getTestSpecPath()+" ("+e.toString()+")");
	    e.printStackTrace();
	}
	finally {
	    setStatus(result);
	    closePrintStream(printStream);
	    printStream = null;
	    // clear the properties
	    defaultProperties.clear();
	    setParser(null);
	}
    }
	
	
    public void parseTest(String projectResultsDirectory, 
			  Properties defaultProperties, 
			  ParserInterface parser) {
	parseTest(projectResultsDirectory, defaultProperties, true, parser);
    }
	
    public void runTest(String projectResultsDirectory,
			Properties defaultProperties,
			ParserInterface parser) {
	parseTest(projectResultsDirectory, defaultProperties, false, parser);
    }
	
    /**
     * This is the name of the test as it will appear in the harness test tree.
     */
    public String getTestName() {
	return (String)propertyValue[testName];
    }
	
    /**
     * This is the Description of the test as it will appear in the harness test tree tooltip.
     */
    public String getTestDescription() {
	return (String)propertyValue[testDescription];
    }
	
    /**
     * This is the Author of the test as it will appear in the harness test tree.
     */
    public String getTestAuthor() {
	return (String)propertyValue[testAuthor];
    }
	
    /**
     * This is the BugInfo of the test as it will appear in the harness test tree.
     */
    public String getTestBugInfo() {
	return (String)propertyValue[testBugInfo];
    }
	
    public String[] getKeyWords() {
	return (String[])propertyValue[keyWordList];
    }
	
    /**
     * This method returns a list of all properties files that were used to resolve this file.
     */
    public String[] getIncludePropList() {
	return (String[])propertyValue[includePropList];
    }
    /**
     * This method returns a list of all other files that were used to resolve this file.
     */
    public String[] getIncludeMiscList() {
	return (String[])propertyValue[includeMiscList];
    }
	
    /**
     * Returns a list of all the GETTRACE commands used in this qash file.
     */
    public String[] getViewOutputList() {
	return (String[])propertyValue[traceList];
    }
	
    /**
     * This method gets the status of this test.
     * Returns PASSED, FAILED, UNRESOLVED, RUNNING or NOTRUN.
     */
    public int getStatus() {
	return ((Integer)propertyValue[status]).intValue();
    }
	
    public static String getStatusString(int s) {
	switch (s) {
	case ProtocolConstants.PASSED : return Resources.getString("passed");
	case ProtocolConstants.FAILED : return Resources.getString("failed");
	case ProtocolConstants.UNRESOLVED : return Resources.getString("unresolved");
	case ProtocolConstants.NOTRUN : return Resources.getString("notrun");
	case ProtocolConstants.RUNNING : return Resources.getString("running");
	case ProtocolConstants.PENDING : return Resources.getString("pending");
	default : return Resources.getString("unknown");
	}
    }
	
    public String getStatusString() {
	return getStatusString(getStatus());
    }
	
    /**
     * This method sets the status of this test.
     * s should be one of PASSED, FAILED, UNRESOLVED or NOTRUN.
     */
    public void setStatus(int s) {
	propertyValue[status] = new Integer(s);
    }
	
    /** 
     * This is the label painted on the test tree.
     * I remove the quotation marks and spaces to make it prettier.
     */		
    public String toString() {
	return getTestName();
    }
	
    private void updateProperties(ParserInterface parser) {
	propertyValue[testName] = parser.getTestName();
	propertyValue[testDescription] = parser.getTestDescription();
	propertyValue[testAuthor]      = parser.getTestAuthor();
	propertyValue[testBugInfo]     = parser.getTestBugInfo();
	propertyValue[keyWordList] = parser.getKeyWords();
	propertyValue[includeMiscList] = parser.getIncludeList();
	propertyValue[includePropList] = parser.getPropertiesIncludeList();
	propertyValue[traceList] = parser.getTraceList();
    }
	
    public void interrupt() {
	if (getParser()!=null) {
	    getParser().interrupt();
	    setParser(null);	
	}
	setStatus(ProtocolConstants.UNRESOLVED);
    }
	
    public void clearTraceFiles(String projectResultsDirectory) {
	String traceList[] = getViewOutputList();
	if (traceList==null)
	    return;
	for (int i = 0; i < traceList.length; i++) {
	    try {
		Utils.delete(getEnvironmentTraceFileName(projectResultsDirectory,traceList[i]));
	    }
	    catch (Exception e) {
	    }
	    try {
		Utils.delete(getStdOutTraceFileName(projectResultsDirectory,traceList[i]));
	    }
	    catch (Exception e) {
	    }
	    try {
		Utils.delete(getStdErrTraceFileName(projectResultsDirectory,traceList[i]));
	    }
	    catch (Exception e) {
	    }
	}
	try {
	    Utils.delete(getParserTraceFileName(projectResultsDirectory));
	}
	catch (Exception e) {
	}
    }
	
    public String getEnvironmentTraceFileName(String projectResultsDirectory,
					      String commandID) {
	return getTraceFileBase(projectResultsDirectory,commandID)+Common.ENV_TRACE_SUFFIX;
    }
	
    public String getStdOutTraceFileName(String projectResultsDirectory,
					 String commandID) {
	return getTraceFileBase(projectResultsDirectory,commandID)+Common.STDOUT_TRACE_SUFFIX;
    }
	
    public String getStdErrTraceFileName(String projectResultsDirectory,
					 String commandID) {
	return getTraceFileBase(projectResultsDirectory,commandID)+Common.STDERR_TRACE_SUFFIX;
    }
	
    public String getParserTraceFileName(String projectResultsDirectory) {
	return projectResultsDirectory+File.separator+Common.getUniqueTestIdentifier(getTestSpecPath())+Common.PARSER_TRACE_SUFFIX;
    }
	
    private String getTraceFileBase(String projectResultsDirectory,
				    String commandID) {
	return projectResultsDirectory+File.separator+Common.getUniqueTestIdentifier(getTestSpecPath())+"_"+commandID;
    }
	
    private void writeArray(java.io.ObjectOutputStream out, Object objArray[]) throws IOException, ClassNotFoundException {
	out.writeObject(new Integer(objArray.length));
	for (int i = 0; i < objArray.length; i++)
	    out.writeObject(objArray[i]);
    }
	
    private String[] readArray(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	String objArray[] = new String[((Integer)in.readObject()).intValue()];
	for (int i = 0; i < objArray.length; i++)
	    objArray[i] = (String)in.readObject();
	return objArray;
    }
	
    public void readData(ObjectInputStream in, String version) throws IOException, ClassNotFoundException {		
	if (version.equals(Common.SERIALIZED_NODE_HEADERV11)) {
	    propertyValue = new Object[propertyCount];
	    propertyValue[testSpec]        = (String)in.readObject();
	    propertyValue[status]          = (Integer)in.readObject();
	    propertyValue[testName]        =(String)in.readObject();
	    propertyValue[keyWordList]     =readArray(in);
	    propertyValue[includeMiscList] =readArray(in);
	    propertyValue[includePropList] =readArray(in);
	    propertyValue[traceList]       =readArray(in);
	    propertyValue[testAuthor]      = in.readObject();
	    propertyValue[testDescription] = in.readObject();
	    propertyValue[testBugInfo]     = "";
	}
	if (version.equals(Common.SERIALIZED_NODE_HEADERV12)) {
	    propertyValue = new Object[propertyCount];
	    propertyValue[testSpec]        = (String)in.readObject();
	    propertyValue[status]          = (Integer)in.readObject();
	    propertyValue[testName]        =(String)in.readObject();
	    propertyValue[keyWordList]     =readArray(in);
	    propertyValue[includeMiscList] =readArray(in);
	    propertyValue[includePropList] =readArray(in);
	    propertyValue[traceList]       =readArray(in);
	    propertyValue[testAuthor]      = (String)in.readObject();
	    propertyValue[testDescription] = (String)in.readObject();
	    propertyValue[testBugInfo]     = (String)in.readObject();
	}
    }
	
    public void writeData(ObjectOutputStream out) throws IOException, ClassNotFoundException {
	out.writeObject(Common.SERIALIZED_NODE_HEADERV12);
	out.writeObject(propertyValue[testSpec]);
	out.writeObject(propertyValue[status]);
	out.writeObject(propertyValue[testName]);
	writeArray(out,(String[])propertyValue[keyWordList]);
	writeArray(out,(String[])propertyValue[includeMiscList]);
	writeArray(out,(String[])propertyValue[includePropList]);
	writeArray(out,(String[])propertyValue[traceList]);
	out.writeObject(propertyValue[testAuthor]);
	out.writeObject(propertyValue[testDescription]);
	out.writeObject(propertyValue[testBugInfo]);
    }
}
