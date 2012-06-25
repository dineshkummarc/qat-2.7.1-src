package qat.gui;
/** 
 *  PrintManager.java
 *  -----------------
 *  This class simplifies printing simple text in Java, and also allows direct printing onto the
 *  Graphic print object by calling the getGraphics() method.
 *  Any instantiation of this class should be used as follows:
 *  
 *  
 *    PrintManager p = new PrintManager("Print job title",this);
 *    if (p.startPrint()) {
 *      p.setFontStyle(Font.BOLD);
 *      p.println("This is the BOLD style");
 *      p.endPrint();
 *    }
 *    
 *
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
import java.lang.Object;
import java.lang.String;
import java.awt.Graphics;
import java.awt.PrintJob;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Color;
import java.util.Properties;
import javax.swing.JFrame;

public class PrintManager extends Object {
  private Graphics page;
  private PrintJob printJob;
  private Font printFont;
  private FontMetrics fm;
  private Color printColor;
  private int printXPos, printYPos;
  private int pageNumber=0;
  private int pageWidth, pageHeight;
  private int fontHeight, fontDescent;
  private static final int INSET = 20;
  private boolean printBorder;
  private boolean wrapText;
  private String header[];

  public PrintManager(String printJobTitle, JFrame f) {    
    printJob = f.getToolkit().getPrintJob(f, printJobTitle, new Properties());
    if (printJob != null) {
		printFont = getMonospacedFont();
		setFont(printFont);
		printColor = Color.black;
		printBorder = false;
		wrapText = true;
		newPage();
    }
  }
	
	private Font getMonospacedFont() {
		Font f = new Font("monospaced",Font.PLAIN,8);
		if (f==null) {
			System.out.println("Warning - no monospaced fonts found on this system");
			f = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[0];
		}
		return f;
	}
	
  /** This writes out the string s at the current position, and then positions output
    to start on the next line.
    If there is not enough room on the current page, a new page is started.
    No text wrapping occurs, so text too long to be printed on one line will be lost!
   */
  public void println(String s) {
    print(s);
    newLine();
  }
  
  /** This function prints text, continuing from position of the last text printed.
    If wrap is set to true, text longer than the page size will be wrapped to a new line.
   */
  public void print(String s) {
    // check if we need to scroll to the next page
    if ((printYPos+fontHeight) > pageHeight-((INSET*2)+2)) {
      newPage();
      print(s);
    }
    else {
      if ((wrapText)&&(fm.stringWidth(s)>(printXPos+pageWidth-((INSET*2)+2)))) {
	// check if s will fit on the current line
	int i = s.length();
	while (fm.stringWidth(s.substring(0,i))>(printXPos+pageWidth-((INSET*2)+2))) {
	  i--;
	}
	print(s.substring(0,i));
	// if we scrolled, write the rest of the text on this new line
	if (i != s.length()) {
	  newLine();
	  print(s.substring(i,s.length()));
	}
      }
      else {
	page.drawString(s,printXPos+INSET,printYPos-fontDescent+INSET);
	printXPos += fm.stringWidth(s);
      }
    }
  }
  
  /** This function prints the string s at the co-ord (x,y). It has no effect
    on where the next call to println produces it's output.
   */
  public void print(int x, int y, String s) {
    page.drawString(s,x,y);
  }
  
  /** This function prints a border around the current page
   */
  public void setBorder(boolean val) {
    printBorder = val;
  }
  
  public boolean getBorder() {
    return printBorder;
  }
  
  /** This function sets whether text too long for the current line is wrapped or not
   */
  public void setWrap(boolean wrap) {
    wrapText = wrap;
  }
  
  public boolean getWrap() {
    return wrapText;
  }
  
  /** This prints a blank line
   */
  public void newLine() {
    printXPos = 0;
    printYPos += fontHeight;
  }
  
  /** This function generates a new page, and sends the previous one to the printer.
   */
  public void newPage() {
    // send the current page to the printer, unless it's null
    if (page != null)
      page.dispose();
    // create a new page
    page = printJob.getGraphics();
    pageNumber++;
    setFont(getFont());
    setColor(getColor());
    printXPos = 0;
    printYPos = 0;
    pageHeight = printJob.getPageDimension().height;
    pageWidth = printJob.getPageDimension().width;
    if (printBorder)
      page.drawRect(INSET ,INSET  ,pageWidth-((INSET*2)+2),pageHeight-((INSET*2)+2));
    newLine();
    printHeader();
  }
  
  public void printHeader() {
    if (header==null)
      return;
    int style = getFontStyle();
	setFontStyle(Font.BOLD);
    // insert the page number at hte end of the last line of the header
    String pageStr = " Page "+pageNumber;
	// chop off or pad the last line
	for (int i = 0; i < (pageWidth / fm.stringWidth("X")); i++)
		header[0] = header[0] + " ";
	
	header[0] = header[0].substring(0,(pageWidth / fm.stringWidth("X"))-pageStr.length()-10)+pageStr;
    for (int i = 0; i < header.length; i++)
		println(header[i]);
	printLine('-');
	newLine();
    setFontStyle(style);    
  }
	
	public void printLine(char c) {
		println(fill(c,(pageWidth / fm.stringWidth("X"))-10));
	}
	
  public void setHeader(String h[]) {
    header = h;
  }
	
	public void setHeader(String h) {
		header = new String[1];
		header[0] = h;
	}
	
	public static String fill(char c, int len) {
		StringBuffer sb = new StringBuffer(len);
		for (int i = 0; i < len; i++)
			sb  = sb.append(c);
		return sb.toString();
	}
	
	public static String padRight(String s, char c, int len) {
		try {
			return (s+fill(c,len-s.length()));
		}
		catch (Exception e) {
			return s;
		}
	}
	
	public static String padLeft(String s, char c, int len) {
		try {
			return (fill(c,len-s.length())+s);
		}
		catch (Exception e) {
			return s;
		}			
	}
	
	
  /** Returns true if the print is Okayed by the user, else if user selected cancel,
    this function returns false.
    */
  public boolean startPrint() {
    return (printJob!=null);
  }
  
  /** This function must be called to end the print job correctly.
   */
  public void endPrint() {
    // send the current page to the printer
    page.dispose();
    // clean up the print job
    printJob.end();
  }
  
  /** This functions sets the color of the text to print
   */
  public void setColor(Color c) {
    printColor = c;
    page.setColor(printColor);
  }
  /** This function returns the color text is currently being printed in
   */
  public Color getColor() {
    return printColor;
  }
  
  /** This method sets the font used to print the text.
   */
	public void setFont(Font f) {
		if (f==null) {
			System.out.println("Warning - trying to set a non-existent font");
			f =  java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[0];
			System.out.println("Using font "+f.toString());
		}
		printFont = f;
		if (page!=null) {
			page.setFont(f);
			fm = page.getFontMetrics(printFont);
			fontHeight = fm.getHeight();
			fontDescent = fm.getDescent();
		}
  }
  
  public Font getFont() {
    return printFont;
  }
  
  public void setFontStyle(int style) {
    setFont(new Font(getFont().getName(), style, getFont().getSize()));
  }
	
	public int getFontStyle() {
		return getFont().getStyle();
	}
	
	public void setFontSize(int size) {
		setFont(new Font(getFont().getName(), getFontStyle(), size));
	}
	
	public int getFontSize() {
		return getFont().getSize();
	}
  
  public int getPageNumber() {
    return pageNumber;
  }
  
  public int getPageHeight() {
    return pageHeight;
  }
  
  public int getPageWidth() {
    return pageWidth;
  }
  
  /** This function returns the Graphics object of the current page, to allow direct manipulation
    or printing onto the printer canvas
    */
  public Graphics getGraphics() {
    return page;
  }
}

