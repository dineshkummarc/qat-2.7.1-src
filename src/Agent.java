
// @(#)Agent.java 1.3 99/10/14 

public class Agent extends Object {
	/** This constructs an agent which will listen to harness requests on the specified port.
	 * @param args the port number on which to listen for harness messages..
	 */	
    public static void main(String[] args) {
        qat.agent.Agent agent = new qat.agent.Agent(args);
    }
}
