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

import qat.common.*;

public abstract class Utils {

    private static final char  dot     = '.';
    private static int         counter = 0;  // used to generate unique numbers per VM instance of Utils
	
    /**
     * This method will create a new zip file zipFileName, or overwrite an existing one, and place in it
     * all the files specified in fileList. Their offset within the zip file will be exactly the same
     * as the offset given in the fileList argument for each item.
     * The fileList may contain directories, but they will not be explicitly followed. Each zip file must contain
     * at least one non-directory entry.
     * @param zipFileName the name of the zip file we want to create.
     * @param fileList an array containing the absolute path names of the files or directories to store in this zip file.
     * @exception IOException thrown if the file could not be created, or we cannot read some of the input files.
     */
    public static void zipFiles(String zipFileName, String fileList[]) throws IOException {
	zipFiles(zipFileName, fileList, fileList);
    }
	
    /**
     * This method will create a new zip file zipFileName, or overwrite an existing one, and place in it
     * all the files specified in fileList. The name and offset of the file entries in the zip file 
     * will not match that of the original source file, but will match the name and offset given
     * in the zipFileName.
     * The fileList may contain directories, but they will not be explicitly followed. Each zip file must contain
     * at least one non-directory entry.
     * @param zipFileName the name of the zip file we want to create.
     * @param fileList an array containing the absolute path names of the files or directories to store in this zip file.
     * @param fileNameInZip the exact name to call the entry in the zip file, if it is different to the name in fileList.
     * @exception IOException thrown if the file could not be created, or we cannot read some of the input files.
     */
    public static void zipFiles(String zipFileName, String fileList[], String fileNameInZip[]) throws IOException {
	File file;
	// if the file already exists, delete it
	if ((file = new File(zipFileName)).exists()) {
	    delete(file);
	}
	ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(zipFileName));
		
	for (int i = 0; i < fileList.length; i++) {
	    file = new File(fileList[i]);
	    if (file.isDirectory()) {
		fileNameInZip[i] = fileNameInZip[i]+'/'; // this indicates it's a directory, according to jdk javadoc
		ZipEntry zipEntry = new ZipEntry(fileNameInZip[i]);
		zipEntry.setSize(0);
		zipFile.putNextEntry(zipEntry);
	    }
	    else {
		ZipEntry zipEntry = new ZipEntry(fileNameInZip[i]);
		zipEntry.setSize(file.length());
		zipFile.putNextEntry(zipEntry);
		writeZipBytes(zipFile,file);
	    }
	}
	zipFile.flush();
	zipFile.close();
    }
	
    /**
     * This methods will unzip all the files contained in zipFileName, placing them in a position
     * offset with directory outputDirectory.
     * @param zipFileName the name of the zip file we want to extract.
     * @param outputDirectory the name of the directory in which to extract the files.
     * @exception IOException thrown if the file could not be unzipped, or we cannot write in the output directory.
     */
    public static synchronized void unzipFiles(String zipFileName, String outputDirectory) throws IOException {
	ZipInputStream zipFile = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFileName)));
	ZipEntry entry;
	while ((entry = zipFile.getNextEntry())!=null) {
	    try {
		createFile(entry,zipFile,outputDirectory);
		zipFile.closeEntry();
	    }
	    catch (Throwable t) {
		t.printStackTrace();
	    }
	}	
	zipFile.close();
    }
	
    /**
     * This method deletes all the files and subdirectories as created when the zipFileName was unzipped
     * in the outputDirectory.
     */
    public static void cleanUnzippedFiles(String zipFileName, String outputDirectory) throws IOException {
	FileInputStream inStream;
	ZipInputStream zipFile = new ZipInputStream(inStream = new FileInputStream(zipFileName));
	ZipEntry entry;
	File f;
	while ((entry = zipFile.getNextEntry())!=null) {
	    f = new File(outputDirectory+File.separator+entry.getName());
	    if (f.exists()) {
		Utils.delete(f);
	    }
	    zipFile.closeEntry();
	}	
	zipFile.close();
	inStream.close();
    }
	
    /**
     * This method creates a file specified by fileName,
     * if it doesn't already exist.
     * If the parent directories don't exist, they will be created automatically.
     * @param fileName the name of the file to create.
     * @exception IOException thrown if any IOException occur due to permission problems etc.
     */
    public static void touch(String fileName) throws IOException {
	checkSubDirsExist(fileName);
	RandomAccessFile f = new RandomAccessFile(new File(fileName),"rw");
	f.close();
    }
	
    /**
     * This method creates a dir specified by dirName,
     * if it doesn't already exist.
     * If the parent directories don't exist, they will be created automatically.
     * @param dirName the name of the dir to create.
     * @exception IOException thrown if any IOException occur due to permission problems etc.
     */
    public static void touchDir(String dirName) throws IOException {
	new File(dirName).mkdirs();
    }
	
    /**
     * This writes the file specified by fileName to the stdout.
     * @param fileName the name of the file to be written to stdout.
     * @exception IOException thrown if the file does not exist, or cannot be read.
     */
    public static void cat(String fileName) throws IOException {
	BufferedReader f = new BufferedReader(new FileReader(fileName));
	String line = "";
	//read all the lines in
	while (true)
	    {
		line = f.readLine();
		if (line == null)
		    break;
		else
		    System.out.println(line);
	    }
	f.close();
    }
	
    /**
     * This method copies sourceName to destName.
     * If sourceName is a directory, a recursive copy of all sub-directories
     * is done.
     * @param sourceName the name of the source file/directory to be copied.
     * @param destName the name of the destination file/directory.
     * @exception IOException thrown if any of the files cannot be read or written.
     */
/**
     * This method copies sourceName to destName.
     * If sourceName is a directory, a recursive copy of all sub-directories
     * is done.
     * @param sourceName the name of the source file/directory to be copied.
     * @param destName the name of the destination file/directory.
     * @param status gui component to poll status.
     * @exception IOException thrown if any of the files cannot be read or written.
     */
    public static void copy(String sourceName, String destName) throws IOException {
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
		File parentdir = parent(dest);
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
		if (source != null)
		    source.close();
		if (destination != null)
		    destination.close();
	    }
	}
	else if (src.isDirectory()) {
	    String targetfile, target, targetdest;
	    String[] files = src.list();
	    for (int i = 0; i < files.length; i++) {
		targetfile = files[i];
		target = src + File.separator + targetfile;
		targetdest = dest + File.separator + targetfile;
				
		if ((new File(target)).isDirectory()) {
		    copy(new File(target).getCanonicalPath(), new File(targetdest).getCanonicalPath());
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
    public static void delete(String fileName) throws IOException {
	delete(new File(fileName));
    }
	
    /**
     * This method deletes the file specified by fileName.
     * If it is a directory, all subdirectories are also deleted.
     * @param node the File identifier of the file or directory to be deleted.
     * @exception IOException thrown if the file or directory could not be deleted.
     */
    public static void delete(File node) throws IOException {
		
	if (node.isDirectory()) {
	    String[] files = node.list();
	    for (int i = 0; i < files.length; i++) {
		delete(new File(node.getCanonicalPath()+File.separator+files[i]));
	    }
	    // now delete this directory cos it's empty, or it has write-protected files inside
	    if (!node.delete()){
		throw new IOException("Could not delete the directory :"+node+"- write protected or not empty");
	    }
	}
	else {
	    if (!node.delete()){
		throw new IOException("Could not delete the file :"+node+" - may be write protected");
	    }
	}
    }

    /**
     * This breaks up a list of strings
     * into a string array.
     * eg "s1" + "s2" + "s3" would give {"s1,"s2,"s3"}
     */
    public static String[] toStringArray(String s) {
	if (s==null)
	    return new String[0];
	StringTokenizer token = new StringTokenizer(s,"\"+ ");
	String list[] = new String[token.countTokens()];
	for (int i = 0; i < list.length; i++) {
	    list[i] = token.nextToken();
	}
	return list;
    }
	
    /**
     * This method will block until the specified number of seconds elapsed, as measured
     * by the system clock.
     * May have differing behaviour on various JVM implementations (blocking etc)
     * @param seconds the number of seconds to sleep for.
     */
    public synchronized static void safeSleep(long seconds) {
	try {
	    Date finTime = new Date(new Date().getTime()+(seconds*1000));
	    while (new Date().before(finTime)) {
		// sleep for half the requested time
		Thread.sleep(seconds*500);
	    }
	}
	catch (Exception ex) {
	    ConsoleServer.debugStackTrace(ex);
	}
    }
	
    /**
     * This method will block until the specified number of seconds elapsed, as measured
     * by the system clock.
     * @param seconds the number of seconds to sleep for.
     */
    // 	public synchronized static void saferSleep(int seconds) {
    // 		int milli_seconds;
    // 		TimeWaster timeWaster = new TimeWaster();
    // 		timeWaster.setDelay(milli_seconds=seconds*1000);
    // 		timeWaster.start();
    // 		try {
    // 			timeWaster.join(milli_seconds);
    // 		}
    // 		catch (InterruptedException ex) {
    // 			ConsoleServer.debugStackTrace(ex);
    // 		}
    // 	}
	
    public static void microSleep(int milliseconds) {
	try {
	    Thread.sleep(milliseconds);
	    Thread.yield();
	}
	catch (InterruptedException ex) {
	    ConsoleServer.debugStackTrace(ex);
	}
    }
	
    /**
     * This method will pad the string to the specified length with spaces.
     * If the string is longer than the length specified, no changes are effected
     * @param str the String to pad.
     * @param length the desired length of the string.
     */
    public static String pad(String  str, int length) {
	StringBuffer buff = new StringBuffer(str);
	while (buff.length()<length)
	    buff = buff.append(' ');
	return buff.toString();
    }
    /**
     * This method returns the absolute path of the parent of the
     * file specified by f.
     * For use internally by the copy method.
     * @param f the name of the file whose parent we seek.
     */
    public static File parent(File f) {
	String dirname = f.getParent();
	if (dirname == null) {
	    if (f.isAbsolute())
		return new File(File.separator);
	    else
		return new File(System.getProperty("user.dir"));
	}
	return new File(dirname);
    }
	
    private static void writeZipBytes(ZipOutputStream zipOut, File file) throws IOException {
	if (file.isDirectory()) {
	}
	else {
	    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
	    BufferedOutputStream out = new BufferedOutputStream(zipOut);
	    byte buff[] = new byte[4096];
	    int c=0;
	    do {
		c = in.read(buff,0,buff.length);
		out.write(buff,0,c);
	    } while (c !=-1);
	}
    }
	
    /**
     * Creates all neccesary parent directories
     * contained in this path.
     * Not to be confused with  checkSubDirsExist(String rootPath, String path) which
     * is used exclusively by the unzip routines, since java zip files always
     * store directories separated by the '/' instead of File.separator char.
     */
    public static synchronized void checkSubDirsExist(String path) {
	StringTokenizer token = new StringTokenizer(path,File.separator);
	String dirComponents[] = new String[token.countTokens()];
	String root = (path.charAt(0)!=File.separator.charAt(0) ? token.nextToken() : File.separator + token.nextToken());
	File f;
	for (int i = 1; i < dirComponents.length-1; i++) {
	    root = root+File.separator+token.nextToken();
	    f = new File(root);
	    if (!f.exists())
		if (!f.mkdir())
		    System.out.println("Error creating directory:"+root);
	}
    }
	
    private static synchronized void checkSubDirsExist(String rootPath, String path) {
	StringTokenizer token = new StringTokenizer(path,"/");
	String dirComponents[] = new String[token.countTokens()];
	String root = rootPath;
	File f;
	for (int i = 0; i < dirComponents.length-1; i++) {
	    root = root+File.separator+token.nextToken();
	    f = new File(root);
	    if (!f.exists())
		if (!f.mkdir())
		    System.out.println("Error creating directory:"+root);
	}
    }
	
    private static synchronized void createFile(ZipEntry file, ZipInputStream source, String outputDir) throws IOException {
	if ((file.isDirectory())||
	    (file.getName().charAt(file.getName().length()-1)=='/')) {
	    // create any parent nodes needed before this node
	    checkSubDirsExist(outputDir,file.getName());
	    // create this node
	    (new File(outputDir+File.separator+file.getName())).mkdir();
	}
	else {
	    // create any parent nodes needed before this node
	    checkSubDirsExist(outputDir,file.getName());

	    // create and write this node
	    int bytesRead;
	    byte buff[] = new byte[4096];
	    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputDir+File.separator+file.getName()));
	    while ((bytesRead = source.read(buff,0,buff.length))>=0) {
	    	output.write(buff,0,bytesRead);
	    }			
	    output.close();
	}
    }
	
    /**
     * This method extracts the fileName portion from the specified path.
     */
    public static String extractFileName(String path) {
	int p = path.lastIndexOf(File.separator);
	if (p<0)
	    return path;
	else
	    return path.substring(p+1,path.length());
    }
	
    /**
     * This method ensures the path is never longer than max characters.
     */
    public static String trimFileName(String path, int max) {
	String dotstring = "...";
	try {
	    if (path.length()>(max-dotstring.length()))
		return dotstring+path.substring((path.length()-max)+dotstring.length(),path.length());
	    else
		return path;
	}
	catch (java.lang.StringIndexOutOfBoundsException ex) {
	    System.out.println("Error trying to trim :"+path+ "with length "+path.length()+" to length "+max);
	    return path;
	}
    }
	
    /**
     * This method extracts the path portion from the given parameter.
     */
    public static String extractPath(String path) {
	int p = path.lastIndexOf(File.separator);
	if (p<0) {
	    return path;
	}
	else {
	    return path.substring(0,p);
	}
    }
	
    /**
     * This method appends the specified suffix to the fileName if
     * it doesn't already have it. If the original suffix is different, it is removed.
     * It works whether suffix includes the "." or not.
     */
    public static String ensureSuffix(String fileName, String suffix) {
	if (suffix.charAt(0)!=dot)
	    suffix = dot+suffix;
	return removeSuffix(fileName)+suffix;
    }
	
    /**
     * This method removes the file suffix, if it exists, from the parameter fileName.
     * The preceding path information is not changed.
     */
    public static String removeSuffix(String fileName) {
	try {
	    int index = fileName.lastIndexOf(dot);
	    // only delete the suffix if it is the fourth last character in the filename
	    if (index == fileName.length()-4)
		return fileName.substring(0,index);
	}
	catch (Exception e) {
	}
	return fileName;
    }
	
    /**
     * This method is used to allocate unique identifiers.
     */
    public static int getUniqueID() {
	return counter++;
    }
	
    /**
     * Merge the properties of propertiesSource with those of propertiesDest.
     */
    public static Properties mergeProperties(Properties propertiesDest, Properties propertiesSource) {
	propertiesDest.putAll(propertiesSource);
	return propertiesDest;
    }
	
    public static File createTempFile(String prefix,String suffix) {
	File result=null;
	try {
	    result = File.createTempFile(prefix,suffix);
	    result.deleteOnExit();
	}
	catch (IOException ex) {
	    ConsoleServer.debugStackTrace(ex);
	}
	catch (java.lang.NoSuchMethodError e) {
	    // might be using jdk1.1.8
	    String something = String.valueOf(new Random().nextInt());
	    result = new File(Common.getBaseDirectory()+
			      File.separator+
			      Common.AGENT_DIR_NAME+
			      File.separator+
			      prefix+
			      something+
			      suffix);
	    checkSubDirsExist(result.getPath());
	}
	return result;
    }
	
    // 	public static void search(JEditorPane editor, String searchString) {
    // 		try {
    // 			int index  = editor.getDocument().getText(0,editor.getDocument().getLength()).indexOf(searchString,
    // 																								  editor.getCaretPosition());
    // 			if (index>=0) {
    // 				editor.setCaretPosition(index);
    // 				editor.moveCaretPosition(index+searchString.length());
    // 				editor.getCaret().setSelectionVisible(true);
    // 				editor.getCaret().setVisible(true);
    // 			}
    // 			else {
    // 				editor.setCaretPosition(0);
    // 				editor.getCaret().setVisible(false);
    // 				JOptionPane.showMessageDialog(editor.getParent(), 
    // 											  Resources.getString("searchFailed")+" ("+searchString+")",
    // 											  Resources.getString("notFound"), 
    // 											  JOptionPane.INFORMATION_MESSAGE);
    // 			}
    // 		}
    // 		catch (javax.swing.text.BadLocationException ex) {
    // 			ex.printStackTrace();
    // 		}
    // 	}
	
	
    }
// 	class TimeWaster extends Thread {
// 		private Date finTime;
// 		public synchronized void setDelay(int milli_seconds) {
// 			finTime = new Date(new Date().getTime()+(milli_seconds));
// 		}
		
// 		public void run() {
// 			try {
// 				while (new Date().before(finTime)) {
// 					Thread.sleep(250);
// 				}
// 			}
// 			catch (InterruptedException ex) {
// 			ConsoleServer.debugStackTrace(ex);
// 			}
// 		}
		
// 	}
