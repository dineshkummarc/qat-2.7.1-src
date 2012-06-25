package qa.client_server;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

public class ClientServer extends Object {
	
	private String port;
	private String server_host, server_port;
	private String testName;
	
	public ClientServer(String args[]) {
		parseArgs(args);
		System.out.println("<html>");
		System.out.println("<head>");
		System.out.println("</head>");
		System.out.println("<body>");
		println("We shall attempt to connect client(s) to the server on host "+server_host+" (port "+server_port+")");		
		runTest();
		System.out.println("</body>");
		System.out.println("</html>");
	}
	
	public void runTest() {
        // Here we use reflection to find which testcase method to call
        // according to the testCaseID value.
        try {
            println("Running the test : " + testName);
            String methodName =  testName;
            Method testCase = null ;
            try {
                testCase = this.getClass().getMethod(methodName, null) ;
            } 
			catch (NoSuchMethodException no) {
                fail("Could not find test: " +  methodName) ;
            }
            testCase.invoke(this, null);
        }
		catch (Throwable t) {
            fail("Exception in " + testName+" "+t.toString());
        }
    }
	
	public void start_client() {
		println("<h1>Client</h1>");
		try {
			println("Connecting to server "+server_host+" on port "+port);
			DataInputStream  inStream;
			DataOutputStream outStream;
			Socket socket = new Socket(server_host, new Integer(port).intValue());
			inStream= new DataInputStream(socket.getInputStream());
			outStream= new DataOutputStream(socket.getOutputStream());
			socket.close();
			println("Connection succesfull");
			pass("Test worked OK");
		}
		catch (Exception e) {
			fail(e.toString());
		}
	}
	
	public void start_server() {
		println("<h1>Server</h1>");
		try {
			println("Starting server on port "+server_port);
			Socket socket;
			ServerSocket server = new ServerSocket(Integer.parseInt(server_port));
			DataInputStream  inStream;
			DataOutputStream outStream;
						
			while (true) {
				try {
					socket = server.accept();
					println("Connection established with " + socket.getInetAddress().getHostName());
					socket.close();				
				}
				catch (Exception e) {
					println("Error establishing socket connection:"+e.getMessage());
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			println("Error starting server:"+e.getMessage());
		}
	}
	
	private void fail(String message) {
		println("<font color=#FF0000> Failed :"+message+"</font>");
		System.exit(-1);
	}
	
	private void pass(String message) {
		println("<font color=#00FF00> Passed :"+message+"</font>");
		System.exit(0);
	}
	
	private void println(String s) {
		System.out.println(s+"<br>");
	}
	
	private void parseArgs(String args[]) {
		String param = "" ;
        for (int i=0; i < args.length; i++) {
            param = args[i].trim() ;
            if (param.indexOf("-client_port")==0) {
                port = args[i+1].trim();
            }
			if (param.indexOf("-server_host")==0) {
                server_host = args[i+1].trim();
            }
			if (param.indexOf("-server_port")==0) {
                server_port = args[i+1].trim();
            }
			if (param.indexOf("-test_name")==0) {
                testName = args[i+1].trim();
            }
			
		}
	}
	
	public static void main(String args[]) {
		ClientServer p = new ClientServer(args);
	}
}
