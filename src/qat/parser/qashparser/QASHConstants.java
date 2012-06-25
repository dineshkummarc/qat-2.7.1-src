package qat.parser.qashparser;

// @(#)QASHConstants.java 1.8 01/03/14 

// java import
import java.util.Enumeration;
import java.util.Hashtable;


public abstract class QASHConstants {
	
    private static final String[] KEYWORD_LIST = {"",
						  "LOOP_OP", "INCLUDE", "IF", "THEN", "ELSE", "WHILE", 
						  "FOR", "TO", "BY", "DO", "PRINT", "END",
						  "SETAGENT", "DELAGENT", 
						  "ZIPSEND", "ZIPCLEAN", 
						  "SETPROP","DELPROP", 
						  "CMDSTART", "AUTOCLEAN_ON", "CMDSTOP", "CMDSTATUS", 
						  "CMDGETTRACE", "CMDCLEAN", 
						  "SLEEP", 
						  "REPORTSTATUS", 
						  "RANDOM",
						  "GETFILE", "SENDFILE", 
						  "CHECKFILE", "CHECKFILE_LOCAL", 
						  "CHECKAGENT", 
						  "DELFILE_LOCAL", "DELFILE", 
						  "MKDIR", "MKDIR_LOCAL",
						  "SETSTATIC", "DELSTATIC", "GETSTATIC",
						  "PRINTENV",
						  "KILLAGENT",
						  "ENDFUNCTION",
						  "FUNCTION",
						  "CALLFUNCTION",
						  "AUTOCLEAN_OFF",
						  "ENVTRACECONTAINS", "STDOUTCONTAINS", "STDERRCONTAINS",
						  "GETTRACEPATHS",
						  "DAEMONSTART"};
	
    private static final Hashtable tokenHashtable;
	
    static {
	tokenHashtable = new Hashtable(KEYWORD_LIST.length);
	for (int i = 0; i < KEYWORD_LIST.length; i++) {
	    tokenHashtable.put(KEYWORD_LIST[i],new Integer(i*1000));
	}	
    }
	
    public static String getTokenValue(int i) {
	return KEYWORD_LIST[i/1000];
    }
	
    public static String[] getSyntaxKeywords() {
	String keyWordList[] = new String[tokenHashtable.size()];
	int i = 0;
	for (Enumeration e = tokenHashtable.keys() ; e.hasMoreElements() ;) {
	    keyWordList[i++] = (String)(e.nextElement());
	}
	return keyWordList;
    }
	
    public static int getTokenID(String token) {
	Object index = tokenHashtable.get(token);
	if (index!= null) {
	    return ((Integer)index).intValue();
	}
	else {
	    return -1;
	}
    }
}
