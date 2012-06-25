package qat.components;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import javax.swing.text.*; 
import javax.swing.text.html.*;
import javax.swing.*; 
import java.util.StringTokenizer; 
import java.util.ArrayList; 
import java.awt.Color; 

public class ParserDocument extends DefaultStyledDocument{ 
    //private String word = ""; 
    private SimpleAttributeSet keyword = new SimpleAttributeSet(); 
    private SimpleAttributeSet string = new SimpleAttributeSet(); 
    private SimpleAttributeSet normal = new SimpleAttributeSet(); 
    private SimpleAttributeSet number = new SimpleAttributeSet(); 
    private SimpleAttributeSet comments = new SimpleAttributeSet(); 
    private SimpleAttributeSet separator = new SimpleAttributeSet(); 
    private int currentPos = 0; 
    private ArrayList keywordSet;
    private ArrayList separatorSet;
    private String separators;
    private int lineStart;
    private int attribList[];
    private static final char COMMENT_CHAR = '#';
    private static final char QUOTE_CHAR = '"';
  

    public ParserDocument(String[] keywords) {
	//set the keyword attribute 
	StyleConstants.setBold(keyword, true); 
	StyleConstants.setForeground(keyword, Color.blue); 
	//set the string attribute 
	StyleConstants.setBold(keyword, true); 
	StyleConstants.setItalic(comments, true);
	StyleConstants.setForeground(string, Color.gray); 
	//set the number attribute 
	StyleConstants.setForeground(number, Color.magenta); 
	//set the comments attribute 
	StyleConstants.setForeground(comments, Color.cyan); 
	StyleConstants.setItalic(comments, true);
	//set the separators attribute 
	StyleConstants.setForeground(separator, Color.red); 
		
	keywordSet = setUpSet(keywords,new ArrayList(keywords.length));
	separators = "0123456789() \n\t\r";
    } 
	
    private ArrayList setUpSet(String[] items, ArrayList set) {
	set = new ArrayList(items.length);
	for (int i = 0; i < items.length; i++)
	    set.add(items[i]);
	return set;
    }
  
    public void insertString(int offs, 
			     String str, 
			     AttributeSet a) throws BadLocationException{
	super.insertString(offs, str, normal);

	// if it's only one character, retrieve the entire line for processing
	if (str.length()==1) {
	    Element element = this.getParagraphElement(offs); 
	    try{ 
		//this gets our chuck of current text for the element we're on 
		str = this.getText(element.getStartOffset(), 
				   element.getEndOffset() - 
				   element.getStartOffset()); 
		// clear attributes
		offs = element.getStartOffset();
	    } 
	    catch(Exception ex){ 
		//whoops! 
		System.out.println("no text"); 
	    }
	}
	// clear attributes
	setParagraphAttributes(offs, str.length(),normal, true);
		
	// process this string now		
	processParagraph(str,offs);
    } 
  

    public ArrayList getKeywords(){ 
	return this.keywordSet; 
    } 
  

    public void setKeywords(ArrayList aKeywordList) { 
	if (aKeywordList != null){ 
	    this.keywordSet = aKeywordList;
	}
    } 
	
    private void processParagraph(String paragraph, int offs) {
	String line;
	StringTokenizer token = new StringTokenizer(paragraph,"\n\r");
	lineStart=0;
	while(token.hasMoreTokens()) {
	    line = token.nextToken();
	    attribList = new int[line.length()];
	    lineStart = offs+paragraph.indexOf(line,lineStart-offs);
	    processLine(line);
	}
    }
	
    private void processLine(String line) {
	processComments(line);
	processStrings(line);
	//processNumbers(line);
	processKeywordsAndNumbers(line);
    }
	
    private void processComments(String line) {
	int start, end;
	for (start = line.indexOf(COMMENT_CHAR); ((start>=0)&&(start<line.length()));) {
	    if (!posInsideQuotes(start,line)) {
		setCharacterAttributes(lineStart+start, line.length()-start,comments, true);		
		setAttribute(start, line.length()-start);		
		return;
	    }
	    else {
		start = line.indexOf(COMMENT_CHAR,start+1);
	    }
	}
    }
	
    private void processStrings(String line) {
	int start=0, end = 0;
	while((start=line.indexOf(QUOTE_CHAR,start))>=0) {
	    if (!isAttribute(start)) {
		end = getMatchingQuote(start,line);
		if (end>0) {
		    setCharacterAttributes(lineStart+start, 1+end-start,string, true);
		    setAttribute(start, 1+end-start);
		    start = end+1;
		}
		else {
		    return;
		}
	    }
	    else {
		start++;
	    }
	}
    }
	
    /**
     * Process keywords and numbers since they share separators, so we save parsing
     * twice.
     */
    private void processKeywordsAndNumbers(String line) {
	StringTokenizer tokens = new StringTokenizer(line,separators,true);
	String currToken;
	int pos=-1;
	while (tokens.hasMoreTokens()) {
	    currToken = tokens.nextToken();
			
	    if (!isAttribute(pos=line.indexOf(currToken,pos+1))) {
		// check if it's a keyword
		if (keywordSet.contains(currToken.trim())) {
		    setCharacterAttributes(lineStart+pos, currToken.length(),keyword, true);
		    setAttribute(pos, currToken.length());
		}
		else {
		    // check if it's a separator
		    //					if (separatorSet.contains(currToken.trim())) {
		    //						setCharacterAttributes(lineStart+pos, currToken.length(),separator, true);
		    //						setAttribute(pos, currToken.length());
		    //					}
		    //					else {
		    // check if it's a number
		    try {
			Integer.parseInt(currToken);
			setCharacterAttributes(lineStart+pos, currToken.length(),number, true);
			setAttribute(pos, currToken.length());
		    }
		    catch (NumberFormatException e) {
		    }
		    //					}
		}
	    }
	}
    }
	
    private int getMatchingQuote(int start, String line) {
	return line.indexOf(QUOTE_CHAR,start+1);
    }
	
    private boolean isAttribute(int pos) {
	return (attribList[pos]==1);
    }
	
    private void setAttribute(int pos, int length) {
	for (int i = 0; i < length; i++)
	    attribList[pos+i]=1;
    }
	
    private boolean posInsideQuotes(int pos, String line) {
	int currPos;
	if ((currPos=line.indexOf(QUOTE_CHAR))>=0) {
	    boolean inQuote = false;
	    while (currPos<=pos) {
		if (line.charAt(currPos)==QUOTE_CHAR) {
		    inQuote = !inQuote;
		}
		currPos++;
	    }
	    return inQuote;
	}
	else {
	    return false;
	}
    }
}
