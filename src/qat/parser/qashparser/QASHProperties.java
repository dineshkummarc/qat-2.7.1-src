package qat.parser.qashparser;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class QASHProperties extends Object {
    private Hashtable propertiesCache;
    private Hashtable functionsHashtable;
    private Hashtable activeAgents;
    private String projectResultsDirectory;
    private boolean autoClean;
    
    public QASHProperties () {       
	propertiesCache = new Hashtable();
	functionsHashtable = new Hashtable();
	activeAgents = new Hashtable();
	autoClean = true;
    }
    
    /**
     * This method is called at the beginning of a parser run
     * on one or more QASH files.
     */
    public void prepare() {
    }
	
    /**
     * This method is called after a parser run on one 
     * or more QASH files.
     */
    public void finish() {
	Properties p;
	for (Enumeration e = propertiesCache.elements() ; e.hasMoreElements() ;) {
	    p = (Properties)e.nextElement();
	    p.clear();
	}
	propertiesCache.clear();
		
	clearFunctions();
    }
	
    /**
     * If we previously loaded this properties file, return the cached version,
     * else load it, cache it and return it.
     */
    public Properties getProperties(String filename) throws IOException {
	Properties newProperties = (Properties)propertiesCache.get(filename);
	if (newProperties==null) {
	    newProperties = new Properties();
	    BufferedInputStream inStream = null;
	    try {
		inStream = new BufferedInputStream(new FileInputStream(filename));
		newProperties.load(inStream);
		inStream.close();
	    }
	    catch (IOException e) {
		e.printStackTrace();
	    }
	    finally {
	    }
	    cacheProperties(filename,newProperties);
	}
	return newProperties;
    }
		
    public void cacheProperties(String filename, Properties p) {
	propertiesCache.put(filename,p);
    }
	
    public void addFunction(String functionName, ArrayList functionBody) {
	functionsHashtable.put(functionName,functionBody);
    }
	
    public ArrayList getFunction(String functionName) {
	return (ArrayList)functionsHashtable.get(functionName);
    }
	
    public void clearFunctions() {
	ArrayList a;
	for (Enumeration e = functionsHashtable.elements() ; e.hasMoreElements() ;) {
	    a = (ArrayList)e.nextElement();
	    a.clear();
	}
	functionsHashtable.clear();
    }	
	
	
    public void clearActiveAgents() {
	activeAgents.clear();
    }
	
    public int getActiveAgentCount() {
	return activeAgents.size();
    }
	
    public void addActiveAgent(Object key, Object value) {
	activeAgents.put(key,value);
    }
	
    public Object getActiveAgent(Object key) {
	return activeAgents.get(key);
    }
	
    public Object removeActiveAgent(Object key) {
	return activeAgents.remove(key);
    }
	
    public Enumeration getActiveAgents() {
	return activeAgents.elements();
    }
	
    public String getProjectResultsDirectory() {
	return projectResultsDirectory;
    }
	
    public void setProjectResultsDirectory(String newProjectResultsDirectory) {
	projectResultsDirectory = newProjectResultsDirectory;
    }
	
    public boolean isAutoClean() {
	return autoClean;
    }
	
    public void setAutoClean(boolean ac) {
	autoClean = ac;
    }
}
