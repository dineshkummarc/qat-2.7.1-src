package qat.common;
/**
 * This class is used for retrieving strings from the resource.properties file for use
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import java.util.*;
import java.net.*;

public class Resources {
	private static ResourceBundle resources;
	
    static {
		resources = loadBundle("resources.QAT");
    }
	
	public static ResourceBundle loadBundle(String bundle) {
       try {
           return ResourceBundle.getBundle(bundle, Locale.getDefault());
	   } 
	   catch (MissingResourceException mre) {
		   mre.printStackTrace();
		   System.err.println(mre.toString());
		   //System.exit(1);
	   }
	   return null;
	}
	
	public static String getString(String s) {
		try {
			return resources.getString(s);
		}
		catch (MissingResourceException e) {
			System.out.println("Error: No resources defined for "+s);
			return null;
		}
	}
	
	public static URL getResource(Class c, String name) {
		try {
			return c.getResource(name);
		}
		catch (MissingResourceException e) {
			//System.out.println("Error: No resources defined for "+name);
			return null;
		}
	}
	
	public static URL getResource(String name) {		
		try {
			return getResource(Class.forName("QAT"),name);
		}
		catch (ClassNotFoundException e) {
			//System.out.println("Class not defined !!!");
			return null;
		}
		catch (MissingResourceException e) {
			//System.out.println("Error: No resources defined for "+name);
			return null;
		}
	}
	
	/**
	 * Returns the URL for an image to be loaded out
	 * of the jar file.
	 */
	public static URL getImageResource(String key) {
		try {
			String name = Resources.getString(key);
			if (name != null) {
				return Resources.getResource(name);
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			System.out.println(e.toString());
			return null;
		}
	}
}
