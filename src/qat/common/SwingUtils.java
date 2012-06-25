package qat.common;
/**
 * This files contains common utility methods useful when developing the QA Tester harness and agent. 
 * The methods are all static, and no object need be instantiated for their useage.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 *
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.*;
import javax.swing.*;
import qat.common.*;
import qat.components.*;

public abstract class SwingUtils {
	
    /**
     * This method copies sourceName to destName.
     * If sourceName is a directory, a recursive copy of all sub-directories
     * is done.
     * @param sourceName the name of the source file/directory to be copied.
     * @param destName the name of the destination file/directory.
     * @param status gui component to poll status.
     * @exception IOException thrown if any of the files cannot be read or written.
     */
    public static void copy(String sourceName, String destName, StatusWindow status) throws IOException {
	File src = new File(sourceName);
	File dest = new File(destName);
	BufferedInputStream source = null;
	BufferedOutputStream destination = null;
	byte[] buffer;
	int bytes_read;
	long byteCount=0;
	// Make sure the specified source exists and is readable.
	if (!src.exists())
	    throw new IOException("Source not found: " + src);
	if (!src.canRead())
	    throw new IOException("Source is unreadable: " + src);
		
	if (src.isFile()) {
	    if (!dest.exists()) {
		File parentdir = Utils.parent(dest);
		if (!parentdir.exists())
		    parentdir.mkdir();
	    }
	    else if (dest.isDirectory()) {
		if (src.isDirectory())
		    dest = new File(dest + File.separator + src);
		else
		    dest = new File(dest + File.separator + src.getName());
	    }
	}
	else if (src.isDirectory()) {
	    if (dest.isFile())
		throw new IOException("Cannot copy directory " + src + " to file " + dest);
			
	    if (!dest.exists())
		dest.mkdir();
	}
		
		
	if ((!dest.canWrite())&&(dest.exists()))
	    throw new IOException("Destination is unwriteable: " + dest);
		
	// If we've gotten this far everything is OK and we can copy.
	if (src.isFile()) {
	    try {
		if (status != null) {
		    status.setMaximum(100);
		    status.setMessage(Utils.trimFileName(src.toString(),40),50);
		}
		source = new BufferedInputStream(new FileInputStream(src));
		destination = new BufferedOutputStream(new FileOutputStream(dest));
		buffer = new byte[4096];
		byteCount=0;
		while(true) {
		    bytes_read = source.read(buffer);
		    if (bytes_read == -1) break;
		    destination.write(buffer, 0, bytes_read);
		    byteCount = byteCount+bytes_read;
		}
	    }
	    finally {
		if (status != null) {
		    status.setMessage(Utils.trimFileName(src.toString(),40),100);
		}
		if (source != null)
		    source.close();
		if (destination != null)
		    destination.close();
	    }
	}
	else if (src.isDirectory()) {
	    String targetfile, target, targetdest;
	    String[] files = src.list();
	    if (status != null) {
		status.setMaximum(files.length);
	    }
	    for (int i = 0; i < files.length; i++) {
		if (status != null) {
		    status.setMessage(Utils.trimFileName(src.toString(),40),i);
		}
		targetfile = files[i];
		target = src + File.separator + targetfile;
		targetdest = dest + File.separator + targetfile;
				
		if ((new File(target)).isDirectory()) {
		    copy(new File(target).getCanonicalPath(), new File(targetdest).getCanonicalPath(), status);
		}
		else {
					
		    try {
			byteCount = 0;
			source = new BufferedInputStream(new FileInputStream(target));
			destination = new BufferedOutputStream(new FileOutputStream(targetdest));
			buffer = new byte[4096];
						
			while(true) {
			    bytes_read = source.read(buffer);
			    if (bytes_read == -1) break;
			    destination.write(buffer, 0, bytes_read);
			    byteCount=byteCount+bytes_read;
			}
		    }
		    finally {
			if (source != null)
			    source.close();
			if (destination != null)
			    destination.close();
		    }
		}
	    }
	}
    }
	
    /**
     * This method deletes the file specified by fileName.
     * If it is a directory, all subdirectories are also deleted.
     * @param fileName the name of the file or directory to be deleted.
     * @exception IOException thrown if the file or directory could not be deleted.
     */
    public static void delete(String fileName, StatusWindow status) throws IOException {
	delete(new File(fileName), status);
    }
	
    /**
     * This method deletes the file specified by fileName.
     * If it is a directory, all subdirectories are also deleted.
     * @param node the File identifier of the file or directory to be deleted.
     * @exception IOException thrown if the file or directory could not be deleted.
     */
    public static void delete(File node, StatusWindow status) throws IOException {
		
	if (node.isDirectory()) {
	    String[] files = node.list();
	    if (status!=null) status.setMaximum(files.length);
	    for (int i = 0; i < files.length; i++) {
		if (status!=null) status.setMessage(Utils.trimFileName(files[i],40),i);
		delete(new File(node.getCanonicalPath()+File.separator+files[i]),status);
	    }
	    // now delete this directory cos it's empty, or it has write-protected files inside
	    if (!node.delete()){
		throw new IOException("Could not delete the directory :"+node+"- write protected or not empty");
	    }
	}
	else {
	    if (status!=null) status.setMessage(Utils.trimFileName(node.toString(),40));
	    if (!node.delete()){
		throw new IOException("Could not delete the file :"+node+" - may be write protected");
	    }
	}
    }

    public static void setLocationRelativeTo(Window child, Window parent) {
	Dimension d = parent.getSize();
	Dimension u = child.getSize();
	child.setLocation((int)((d.getWidth()/2)-(u.getWidth()/2)),
			  (int)((d.getHeight()/2)-(u.getHeight()/2)));
    }
}
