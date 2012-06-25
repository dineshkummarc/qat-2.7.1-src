
// @(#)QAT.java 1.16 01/02/28 

import qat.gui.*;
import java.util.*;
import javax.swing.*;
import java.io.*;

import qat.gui.*;
import qat.common.*;

public class QAT extends Object {
	private boolean asCommandLineInterface=false, rescan=false;
	private String projectPropertiesFileName;
	private String resultsOutputDirectory;
	private ArrayList pluskeyword, minuskeyword;
	
	public QAT(String args[]) {
		if (args.length>0) {
			pluskeyword = new ArrayList();
			minuskeyword = new ArrayList();
			parseArguments(args);
		}
		checkJavaVersion();
		if (asCommandLineInterface) {
			launchCommandLineQAT();
		}
		else {
			qat.gui.QAT qat = new qat.gui.QAT();
		}
	}
	
	public void launchCommandLineQAT() {
		try {
			// build the keyword expression
			String keyString="";
			String plusString, minusString;
			if ((pluskeyword.size()==0)&&
				(minuskeyword.size()==0)) {
				keyString = "*";
			}
			else {
				for (int i = 0; i < pluskeyword.size(); i++)
					keyString += "+"+(String)pluskeyword.get(i);
				
				for (int i = 0; i < minuskeyword.size(); i++)
					keyString += "+!"+(String)minuskeyword.get(i);
				
				// remove leading '+' character
				keyString = keyString.substring(1,keyString.length());
			}
			
			// create the Qat instance
			qat.gui.QAT qat = new qat.gui.QAT(false);
			
			// load specified project
			System.out.println("Loading project "+projectPropertiesFileName);
			qat.loadProject(projectPropertiesFileName);
			
			// rescan if neccesary
			if (rescan) {
				System.out.println("Rescanning for new tests");
				qat.parseTests(false);
				System.out.println("Saving the project");
				qat.saveProject(true);
			}
			
			// select keywords
			System.out.println("Selecting tests using keyword expression :"+keyString);
			qat.keywordComponent.applyKeywordExpression(keyString);
			System.out.println("Selected :"+qat.getTestTree().getSelectedTests().size());
			int selectedCount = qat.getTestTree().getSelectedTests().size();		
			// run selected
			if (selectedCount<=0) {
				System.out.println("No tests were selected");
			}
			else {
				System.out.println("Running "+selectedCount+" selected test(s)");
				qat.runSelectedTests(true);		
				System.out.print("Waiting for tests to start");
				while (!qat.isTestRunning()) {
					Thread.sleep(200);
					Thread.yield();
					System.out.print(".");
				}
				System.out.println();
				System.out.println("Tests started - waiting for tests to finish");
				// wait for test run to finish
				while(qat.isTestRunning()) {
					Thread.sleep(1000);
					Thread.yield();
				}
				System.out.println("Finished running "+selectedCount+" selected tests");
			
				// generate report
				if (resultsOutputDirectory!=null) {
					System.out.println("Generating Html report into :"+resultsOutputDirectory);
					qat.generateHtmlReport(resultsOutputDirectory,null);
					System.out.println("Done.");
				}
			}
			// now exit
			System.out.println("Exiting");
			qat.closeQatInstance(true);		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void checkJavaVersion() {
		String version = System.getProperty("java.version");
		String numericVersion = new String();
		char digit;
		for (int i = 0; i < version.length(); i++) {
			digit = version.charAt(i);
			if (Character.isDigit(digit))
				numericVersion+=digit;
		}
		// insert the point
		if (numericVersion.length()==2)
			numericVersion+='0';
		int intVersion = Integer.parseInt(numericVersion);
		if (intVersion<130) {
			System.out.println("*************************************************");
			System.out.println("******              WARNING                ******");
			System.out.println("****** Supported only on Java 1.3 or later ******");
			System.out.println("****** Unpredicatable behaviour may result ******");
			System.out.println("****** if you continue to use this version ******");
			System.out.println("******                                     ******");
			System.out.println("*************************************************");
			System.out.println("(Your java.version="+intVersion+")");
		}
	}
	
	/**
	 * 
	 *  java QAT [-cmdline] [-rescan] -project <filename> [-reportdir <dirname>] [+keyword keyword] [-keyword keyword]
	 */
	private void parseArguments(String[] args) {
		String currArg;
		boolean arg_error = false;
		for (int i = 0; i < args.length; i++) {
			arg_error = true;
			currArg = args[i].toLowerCase().trim();
			if (currArg.indexOf("-fr")==0) {
				Locale.setDefault(Locale.FRENCH);
				arg_error = false;
			}
			if (currArg.indexOf("-af")==0) {
				Locale.setDefault(new Locale("AF","ZA"));
				arg_error = false;
			}
			if (currArg.indexOf("-en")==0) {
				Locale.setDefault(Locale.ENGLISH);
				arg_error = false;
			}
			if (currArg.indexOf("-cmdline")==0) {
				asCommandLineInterface = true;
				arg_error = false;
			}
			if (currArg.indexOf("-project")==0) {
				projectPropertiesFileName = args[i+1];
				i++;
				arg_error = false;
			}
			if (currArg.indexOf("+keyword")==0) {
				pluskeyword.add(args[i+1]);
				i++;
				arg_error = false;
			}
			if (currArg.indexOf("-keyword")==0) {
				minuskeyword.add(args[i+1]);
				i++;
				arg_error = false;
			}
			if (currArg.indexOf("-reportdir")==0) {
				resultsOutputDirectory = args[i+1];
				i++;
				arg_error = false;
			}
			if (currArg.indexOf("-rescan")==0) {
				rescan = true;
				arg_error = false;
			}
			if (arg_error) {
				System.out.println("Bad argument:"+currArg);
				useage();
			}
		}
	}
	
	/*
	 * Prints out the paramter useage for QAT gui and commandline mode
	 */
	public void useage() {
		System.out.println("QAT V"+Common.VERSION);
		System.out.println("Usage: java QAT [-fr / -af  / -en] [-cmdline] [-rescan] -project <filename> [-reportdir <dirname>] [+keyword keyword] [-keyword keyword]");
		System.out.println("");
		System.out.println("[-fr / -af  / -en] - start the QAT harness in French, Afrikaans or English");
		System.out.println("cmdline     : run the QAT gui in commandline mode");
		System.out.println("rescan      : rescan for new tests on disk, and save the project file before");
		System.out.println("              starting the test run");
		System.out.println("project     : the project file (*.prj) to use for running the tests");
		System.out.println("reportdir   : where to generate an html report after the test run. If omitted");
		System.out.println("              no report will be printed");
		System.out.println("+keyword    : any tests containing this keyword will be run");
		System.out.println("-keyword    : any tests containing this keyword will not be run");
		System.out.println("");
		System.out.println("");
		System.exit(1);
	}
	
	public static void main(String[] args) {
		QAT qatWrapper = new QAT(args);	
	}
}
