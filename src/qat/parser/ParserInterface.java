package qat.parser;
/**
 * This interface is provided to allow users to define their own shell
 * syntax, while keeping the GUI interface.
 * All that is needed is to set a project property to indicate which
 * Parser class to use, and to ensure that class is visible in the 
 * classpath, and implements this interface.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
import java.util.Properties;
import java.io.PrintStream;
import javax.swing.*;

public interface ParserInterface {
	
    /**
     * This method lists all the keywords supported by this syntax, for use in
     * the Notepad syntax highlighting. It must be declared static.
     */
    public String[] getSyntaxKeyWords();
	
    /**
     * This method sets the path to file file containing the syntax
     * which will be parsed.
     */
    public void setTestPath(String testFile);

    /**
     * This method sets the path to root of the current project.
     */
    public void setProjectRoot(String projectRoot);
	
    /**
     * This method sets the PrintStream to use for reporting errors
     * and other types of output from the script.
     */
    public void setPrintStream(PrintStream printStream, boolean useHtml);
	
    /**
     * This method sets any default properties which will be required 
     * for parsing this file.
     */
    public void setProperties(java.util.Properties p);
		
    /**
     * This method returns all the properties obtained by parsing this test file.
     */
    public java.util.Properties getProperties();
    /**
     * If set to true, the parser does not actually make contact with the agents
     * but merely simulates the agent responses to allow standalone parsing.
     */
    public void setEvaluationMode(boolean evalMode);
	
    /**
     * This method parses the specified file.
     * If not in evaluation mode, it should return the status of the test run :
     * ProtocolConstants.PASSED
     * ProtocolConstants.FAILED
     * ProtocolConstants.NOTRUN
     * ProtocolConstants.UNRESOLVED
     */
    public int parseFile() throws Exception;
	
    /**
     * This method is responsible for killing any processes already started on the agents,
     * and immediately halt parsing any files.
     */
    public void interrupt();
	
    /**
     * This method retrieves the specified property from the results of parsing this file.
     */
    public String getProperty(String key);
	
    /**
     * This method retrieves the specified property from the results of parsing this file.
     * If the value is not found, the defaultValue is returned.
     */
    public String getProperty(String key, String defaultValue);
	
    /**
     * This method should return a test name which will be used to display the test in
     * the test tree.
     */
    public String getTestName();
	
    /**
     * This method should return a test Author which will be used to display the test in
     * the test tree.
     */
    public String getTestAuthor();
	
    /**
     * This method should return a test Description which will be used to display the test in
     * the test tree.
     */
    public String getTestDescription();
	
    /**
     * This method should return a test BugInfo which will be used for displaying the test in
     * the test tree.
     */
    public String getTestBugInfo();
	
    /**
     * This method should return all keywords associated with this test. These will be used
     * in using the keywords to select/deselect tests in the harness.
     */
    public String[] getKeyWords();
	
    /**
     * This should return the list of files other than standard java.util.Properties files which were
     * included to parse this test file.
     */
    public String[] getIncludeList();
	
    /**
     * This should return the list of standard java.util.Properties files which were
     * included to parse this test file.
     */	
    public String[] getPropertiesIncludeList();
	
    /**
     * This method should list all available output files produced by this test when run on the agent,
     * but relative to the harness.
     */
    public String[] getTraceList();
	
    /**
     * This is called at the beginning of a parser run on
     * one or more tests.
     * @param projectResultsDir - the canonical pathname of
     * the project file, used to decide where to place 
     * the parser trace files.
     */
    public void prepare(String projectFileName);
	
    /**
     * Returns a handle to the Printstream the parser will use for any output
     * resulting from parsing this test.
     */
    public PrintStream openPrintStream(String projectResultsDirectory) throws java.io.FileNotFoundException;
	
    /*
     * This method centralises all the traces printed by the parser.
     */
    public void printDebug(String msg);
	
    /**
     * This method indicates we are finished with this parser, and disposes
     * any reserved resources.
     */
    public void finish();
	
    /**
     * This is the handle to to QAT parent GUI to display which commands
     * the parser is processing in real-time.
     */
    public void setStatusLabel(JLabel status);
	
}
