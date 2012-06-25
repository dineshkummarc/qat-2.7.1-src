package qa.syntax_tests;

public class SyntaxTest extends Object {
	
	public SyntaxTest() {
		System.out.println("SyntaxTest object has been created");
		System.err.println("No errors to report for SyntaxTest");
		System.exit(0);
	}
	
	public static void main(String args[]) {
		SyntaxTest p = new SyntaxTest();
	}
}
