package qat.agent;
/**
 * This class used used to read the standard output and standard error streams of started processes.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import java.io.*;
import java.lang.InterruptedException;
import java.lang.Thread;
import java.util.StringTokenizer;

import qat.common.ConsoleServer;
import qat.common.Utils;

/** 
 * This class is used to allow reading of streams without the risk of blocking thread
 * execution.
 */
public class ReaderThread extends Thread {
	private BufferedReader stream;
	private PrintWriter trace;
	private boolean showOutput;
	private boolean thread_running;
	
	/** 
	 * Constructs a stream reader, and gets ready to read it.
	 * @param s - the stream we want to read
	 */
    public  ReaderThread(String name, InputStream s, boolean mode, File traceFileName) throws IOException {   
		stream = new BufferedReader(new InputStreamReader(s));
		trace = new PrintWriter(new FileOutputStream(traceFileName));
		showOutput = mode;
		thread_running = true;
    }
	/**
	 * This starts reading the actual thread. Do not call this method directly, but use ObjectName.start()
	 * to start this thread correctly.
	 */
	public void run() {
		String s;
		try {
			do {
				s = stream.readLine();
				if (s!=null) {
					if (showOutput) {
						ConsoleServer.debugMsg(s);
					}
					trace.println(s);
					trace.flush();
				}
			} while ((s != null)&&(thread_running));
		}
		catch (IOException ex) {
			ConsoleServer.debugMsg("Caught exception :"+ex.toString(),2);
			ConsoleServer.debugStackTrace(ex);
		}
	}
	
	/** 
	 * This method causes this object instance to stop excuting, and frees all the resources associated with
	 * this stream reader.
	 */
	public void interrupt() {
		thread_running = false; // stop this thread
		trace.flush();
		trace.close();
		try {
			stream.close();
		}
		catch (IOException ex) {
			ConsoleServer.debugStackTrace(ex);
		}
		finally {
			stream = null;
		}
	}
}
