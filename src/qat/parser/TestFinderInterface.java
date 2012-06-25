package qat.parser;
/**
 * This method allows you to define what type of tests are loaded into the test tree.
 * By default, the QASHTestFinder implementation of this interface is used.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
import java.io.File;

public interface TestFinderInterface {
	
    /**
     * This method is called to determine whether the corresponding file is a test file or not.
     * If it is, it will be added into the test tree as a test node, and the 
     * ParserInterface class will be called on it to determine keywords, name etc.
     */
    public boolean isTestFile(File file);
	
    /**
     * This method should be called by the class instanciating this TestFinder,
     * to indicate the root path to the current file offset, as set in the project setings.
     */
    public void setProjectRoot(String rootDirectory);

    /**
     * Return a parser for the specified file type, or null
     * if it's not one we recognise.
     */
    public ParserInterface getParser(File file);

}
