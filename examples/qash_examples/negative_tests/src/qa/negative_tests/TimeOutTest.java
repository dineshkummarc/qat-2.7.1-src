package qa.negative_tests;

import java.util.*;

public class TimeOutTest extends Object {
	
	public TimeOutTest() {
		System.out.println("TimeOutTest object has been created");
		System.out.println("We will not exit for 20 seconds");
		System.err.println("This is the standard error output for demonstration purposes");
		long startTime = new Date().getTime();
		long currTime;
		do {
			currTime = new Date().getTime();
			System.out.println(new Date().toString()+" "+(currTime-startTime));
			try {
				Thread.sleep(250);
			}
			catch (java.lang.InterruptedException ex) {
				ex.printStackTrace();
			}
			Thread.yield();
		} while ((currTime-startTime)<20000);
		System.out.println("Finished - exiting now");
		System.exit(0);
	}
	
	public static void main(String args[]) {
		TimeOutTest p = new TimeOutTest();
	}
}
