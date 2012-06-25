/* An example of a very simple, multi-threaded HTTP server.
 */
package qat.gui;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Date;
import java.util.Properties;
import java.util.ArrayList;
import java.lang.Thread;
import javax.swing.JOptionPane;

import qat.common.*;
import qat.gui.*;

class HttpQATHandler extends Thread  {
	
	public static final byte[] EOL = {(byte)'\r', (byte)'\n' };

	/** 2XX: generally "OK" */
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_PARTIAL = 206;

    /** 3XX: relocation/redirect */
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_USE_PROXY = 305;

    /** 4XX: client error */
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /** 5XX: server error */
    public static final int HTTP_SERVER_ERROR = 500;
    public static final int HTTP_INTERNAL_ERROR = 501;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION = 505;

    private int port;
	private QAT parent;
	private boolean running;
	
	/* static data stuff */
	/* mapping of file extensions to content-types */
    static java.util.Hashtable map = new java.util.Hashtable();

    static {
        fillMap();
    }
    static void setSuffix(String k, String v) {
        map.put(k, v);
    }

    static void fillMap() {
        setSuffix("", "content/unknown");
        setSuffix(".uu", "application/octet-stream");
        setSuffix(".exe", "application/octet-stream");
        setSuffix(".ps", "application/postscript");
        setSuffix(".zip", "application/zip");
        setSuffix(".sh", "application/x-shar");
        setSuffix(".tar", "application/x-tar");
        setSuffix(".snd", "audio/basic");
        setSuffix(".au", "audio/basic");
        setSuffix(".wav", "audio/x-wav");
        setSuffix(".gif", "image/gif");
        setSuffix(".jpg", "image/jpeg");
        setSuffix(".jpeg", "image/jpeg");
        setSuffix(".htm", "text/html");
        setSuffix(".html", "text/html");
        setSuffix(".text", "text/plain");
        setSuffix(".c", "text/plain");
        setSuffix(".cc", "text/plain");
        setSuffix(".c++", "text/plain");
        setSuffix(".h", "text/plain");
        setSuffix(".pl", "text/plain");
        setSuffix(".txt", "text/plain");
        setSuffix(".java", "text/plain");
    }
	
	/* static class data/methods */

    /* print to stdout */
    protected static void p(String s) {
		if (debug) {
			System.out.println(s);
		}
    }

    /* print to the log file */
    protected static void log(String s) {
		if (debug) {
			synchronized (log) {
				log.println(s);
				log.flush();
			}
		}
    }

    static boolean debug = false;

    static PrintStream log = null;
    /* our server's configuration information is stored
     * in these properties
     */
    protected static Properties props = new Properties();

    /* Where worker threads stand idle */
    static ArrayList threads = new ArrayList();

    /* the web server's virtual root */
    static File root;

    /* timeout on client connections */
    static int timeout = 0;

    /* max # worker threads */
    static int workers = 2;


    /* load www-server.properties from java.home */
    static void loadProps() throws IOException {

        /* if no properties were specified, choose defaults */
		root = new File(System.getProperty("user.dir"));
		
		timeout = 10000;

		log = System.out;
    }

    static void printProps() {
        p("root="+root);
        p("timeout="+timeout);
        p("workers="+workers);
    }
	
	protected HttpQATHandler() {
	}
	
	public HttpQATHandler(QAT parent) {
		this.parent = parent;
	}
	
	public HttpQATHandler(int port, QAT parent) {
		this.port = port;
		this.parent = parent;
	}
	
	public HttpQATHandler(String port, QAT parent) {
		this(Integer.parseInt(port),parent);
	}
	
	public String getPort() {
		return Integer.toString(port);
	}
	
	public void setPort(String port) {
		this.port = Integer.parseInt(port);
	}
	
	public synchronized void stopServer() {
		running = false;
		interrupt();
	}

    public void run() {
		running = true;
		try {
			loadProps();
			printProps();
			/* start worker threads */
			for (int i = 0; i < workers; ++i) {
				HttpWorker w = new HttpWorker(parent);
				(new Thread(w, "worker #"+i)).start();
				threads.add(w);
			}
			
			ServerSocket ss = new ServerSocket(port);
			while (running) {
				
				Socket s = ss.accept();
				
				HttpWorker w = null;
				synchronized (threads) {
					if (threads.isEmpty()) {
						HttpWorker ws = new HttpWorker(parent);
						ws.setSocket(s);
						(new Thread(ws, "additional worker")).start();
					} else {
						w = (HttpWorker) threads.get(0);
						threads.remove(0);
						w.setSocket(s);
					}
				}
			}
		}
		catch (InterruptedIOException e) {
			// this is normal - happens when we kill the http server to start on another port
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(parent, 
										  "Problem starting Http server interface :"+e.getMessage(),
										  Resources.getString("error"), 
										  JOptionPane.ERROR_MESSAGE);		}
		finally {
			threads.clear();
		}
	}
	
	public QAT getParent() {
		return parent;
	}
}
