package qat.gui;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.lang.*;

import qat.common.*;

public class PageLoader implements Runnable {
    private String text;
    private URL url;
    private JEditorPane editorPane;
	
    public PageLoader(JEditorPane editorPane, String text) {
	this.editorPane = editorPane;
	this.text = text;
	this.url = null;
    }

    public PageLoader(JEditorPane editorPane, URL url) {
	this.editorPane = editorPane;
	this.text = null;
	this.url = url;
    }

    public static void setText(JEditorPane editorPane, String text) {
	PageLoader pl = new PageLoader(editorPane, text);
	java.awt.EventQueue.invokeLater(pl);
    }
	
    public static void setPage(JEditorPane editorPane, URL url) {
	PageLoader pl = new PageLoader(editorPane, url);
	java.awt.EventQueue.invokeLater(pl);
    }

    public void run() {
	if (url != null) {
	    try {			
		editorPane.setDocument(editorPane.getEditorKit().createDefaultDocument());
		//workAround4412125(editorPane, url);
		editorPane.setPage(url);
	    } 
	    catch (FileNotFoundException e) {
		setText(editorPane,Resources.getString("fileNotFound")+url.toString());
	    }
	    catch (java.lang.Throwable e) {
		setText(editorPane,"Caught internal error :85432"+e.toString());
	    }
	}
	if (text != null) {
	    try {			
		Document doc = editorPane.getDocument();
		editorPane.setDocument(editorPane.getEditorKit().createDefaultDocument());
		if (doc instanceof HTMLDocument) {
		    editorPane.setText("<html><body>"+text+"</body></html>");
		}
		else {
		    editorPane.setText(text);
		}
	    }
	    catch (Throwable e) {
		e.printStackTrace();
	    }
	}
    }
	
    //     private static synchronized void workAround4412125(JEditorPane editorPane, URL url) {
    // 	try {
    // 	    editorPane.setPage(new URL(url.getProtocol(),url.getHost(),url.getPort(),""));
    // 	}
    // 	catch (Throwable t) {
    // 	}
    //     }
}
