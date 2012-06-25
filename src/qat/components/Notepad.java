package qat.components;
/*
 * @(#)Notepad.java	1.13 98/08/28
 *
 */

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.lang.Thread;
import java.lang.InterruptedException;
import javax.swing.text.*;
import javax.swing.undo.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.*;

import qat.parser.ParserInterface;
import qat.common.Resources;

/**
 * Modified to fit needs of the QAT tool by webhiker. Added syntax highlighting, etc....
 *
 * @author  Timothy Prinzing
 * @modified  webhiker
 * @version 1.13 08/28/98
 */
public class Notepad extends JPanel {

    private static ResourceBundle resources;
    private boolean syntaxHighlighting;
    private ParserInterface parser;
    public File file;
	
    static {
	try {
	    resources = ResourceBundle.getBundle("resources.Notepad");
	} 
	catch (Exception mre) {
	    System.err.println(mre.toString());
	}
    }
	
    public Notepad(String fileName, ParserInterface parser, boolean syntaxHighlighting) {
	this(parser,syntaxHighlighting);
	File f = new File(fileName);
	if (f.exists()) {
	    Document oldDoc = getEditor().getDocument();
	    if(oldDoc != null)
		oldDoc.removeUndoableEditListener(undoHandler);
	    getEditor().setDocument(createDocument());
	    getFrame().setTitle(fileName);
	    Thread loader = new FileLoader(f, (Document)editor.getDocument());
	    loader.start();
	}
    }
	
    public Notepad(ParserInterface parser, boolean syntaxHighlighting) {
	super(true);
	this.parser = parser;
	this.syntaxHighlighting = syntaxHighlighting;
		
	setBorder(BorderFactory.createEtchedBorder());
	setLayout(new BorderLayout());

	// create the embedded JTextPane
	editor = createEditor();
		
	// Add this as a listener for undoable edits.
	editor.getDocument().addUndoableEditListener(undoHandler);

	// install the command table
	commands = new Hashtable();
	Action[] actions = getActions();
	for (int i = 0; i < actions.length; i++) {
	    Action a = actions[i];
	    //commands.put(a.getText(Action.NAME), a);
	    commands.put(a.getValue(Action.NAME), a);
	}
	
	JScrollPane scroller = new JScrollPane();
	// this can be removed when Java 1.3 is standard because it will beimplemented internally by default
	scroller.getViewport().putClientProperty("EnableWindowBlit", Boolean.TRUE);
	// end remove
	JViewport port = scroller.getViewport();
	port.add(editor);

	menuItems = new Hashtable();
	menubar = createMenubar();
	add("North", menubar);
	JPanel panel = new JPanel();
	panel.setLayout(new BorderLayout());	
	panel.add("North",createToolbar());
	panel.add("Center", scroller);
	add("Center", panel);
	add("South", createStatusbar());
		
	// now create the frame object
	try {
	    JFrame frame = new JFrame();
	    frame.setTitle(getResourceString("Title"));
	    frame.setBackground(Color.lightGray);
	    frame.setIconImage(new ImageIcon(Resources.getResource(Resources.getString("sunLogo"))).getImage());
	    frame.getContentPane().setLayout(new BorderLayout());
	    frame.getContentPane().add("Center", this);
	    frame.addWindowListener(new AppCloser(this));
	    frame.pack();
	    frame.setSize(500, 600);
	    frame.show();
        }
	catch (Throwable t) {
            System.out.println("uncaught exception: " + t);
            t.printStackTrace();
        }
    }
	
    private Document createDocument() {
	if (syntaxHighlighting) {
	    try {
		return new ParserDocument(parser.getSyntaxKeyWords());
	    }
	    catch (Throwable e) {
		return new DefaultStyledDocument();
	    }
	}
	else {
	    return new DefaultStyledDocument();
	}
    }

    /**
     * Fetch the list of actions supported by this
     * editor.  It is implemented to return the list
     * of actions supported by the embedded JTextPane
     * augmented with the actions defined locally.
     */
    public Action[] getActions() {
	return TextAction.augmentList(editor.getActions(), defaultActions);
    }

    /**
     * Create an editor to represent the given document.  
     */
    protected JTextPane createEditor() {
	JTextPane t = new JTextPane();
	t.addCaretListener(createStatusbar());
	return t;
    }

    /** 
     * Fetch the editor contained in this panel
     */
    protected JTextPane getEditor() {
	return editor;
    }

    /**
     * To shutdown when run as an application.  This is a
     * fairly lame implementation.   A more self-respecting
     * implementation would at least check to see if a save
     * was needed.
     */
    protected static final class AppCloser extends WindowAdapter {
	Notepad parent;
		
	public AppCloser(Notepad parent) {
	    this.parent = parent;
	}
		
        public void windowClosing(WindowEvent e) {
	    parent.closeWindow();
	}
    }

    /**
     * Find the hosting frame, for the file-chooser dialog.
     */
    protected Frame getFrame() {
	for (Container p = getParent(); p != null; p = p.getParent()) {
	    if (p instanceof Frame) {
		return (Frame) p;
	    }
	}
	return null;
    }

    /**
     * This is the hook through which all menu items are
     * created.  It registers the result with the menuitem
     * hashtable so that it can be fetched with getMenuItem().
     * @see #getMenuItem
     */
    protected JMenuItem createMenuItem(String cmd) {
	JMenuItem mi = new JMenuItem(getResourceString(cmd + labelSuffix));
		
	String imageStr = getResourceString(cmd + imageSuffix);
        URL url = getResource(imageStr);
	if (url != null) {
	    mi.setHorizontalTextPosition(JButton.RIGHT);
	    mi.setIcon(new ImageIcon(url));
	}
	String astr = getResourceString(cmd + actionSuffix);
	if (astr == null) {
	    astr = cmd;
	}
	mi.setActionCommand(astr);
	Action a = getAction(astr);
	if (a != null) {
	    mi.addActionListener(a);
	    a.addPropertyChangeListener(createActionChangeListener(mi));
	    mi.setEnabled(a.isEnabled());
	} else {
	    mi.setEnabled(false);
	}
	menuItems.put(cmd, mi);
	return mi;
    }

    /**
     * Fetch the menu item that was created for the given
     * command.
     * @param cmd  Name of the action.
     * @returns item created for the given command or null
     *  if one wasn't created.
     */
    protected JMenuItem getMenuItem(String cmd) {
	return (JMenuItem) menuItems.get(cmd);
    }

    protected Action getAction(String cmd) {
	return (Action) commands.get(cmd);
    }

    protected String getResourceString(String nm) {
	if (nm==null)
	    return null;
	String s;
	try {
	    s = resources.getString(nm);
	}
	catch (Exception e) {
	    s = null;
	}
	//System.out.println("1:Got :"+s+" for "+nm);
	return s;		
    }

    protected URL getResource(String resourceStr) {
	if (resourceStr==null)
	    return null;
	URL u = Resources.getResource(resourceStr);
	//System.out.println("2:Got :"+u+" for "+resourceStr);
	return u;
    }

    protected Container getToolbar() {
	return toolbar;
    }

    protected JMenuBar getMenubar() {
	return menubar;
    }

    /**
     * Create a status bar
     */
    protected StatusBar createStatusbar() {
	// need to do something reasonable here
	if (status==null)
	    status = new StatusBar();
	return status;
    }

    /**
     * Create the toolbar.  By default this reads the 
     * resource file for the definition of the toolbar.
     */
    private Component createToolbar() {
	toolbar = new JToolBar();
	String[] toolKeys = tokenize(getResourceString("toolbar"));
	for (int i = 0; i < toolKeys.length; i++) {
	    if (toolKeys[i].equals("-")) {
		toolbar.add(Box.createHorizontalStrut(5));
	    } else {
		toolbar.add(createTool(toolKeys[i]));
	    }
	}
	toolbar.add(Box.createHorizontalGlue());
	return toolbar;
    }

    /**
     * Hook through which every toolbar item is created.
     */
    protected Component createTool(String key) {
	return createToolbarButton(key);
    }

    /**
     * Create a button to go inside of the toolbar.  By default this
     * will load an image resource.  The image filename is relative to
     * the classpath (including the '.' directory if its a part of the
     * classpath), and may either be in a JAR file or a separate file.
     * 
     * @param key The key in the resource file to serve as the basis
     *  of lookups.
     */
    protected JButton createToolbarButton(String key) {
	URL url = getResource(getResourceString(key + imageSuffix));
        JButton b = new JButton(new ImageIcon(url)) {
		public float getAlignmentY() { return 0.5f; }
	    };
        b.setRequestFocusEnabled(false);
        b.setMargin(new Insets(1,1,1,1));

	String astr = getResourceString(key + actionSuffix);
	if (astr == null) {
	    astr = key;
	}
	Action a = getAction(astr);
	if (a != null) {
	    b.setActionCommand(astr);
	    b.addActionListener(a);
	} else {
	    b.setEnabled(false);
	}

	String tip = getResourceString(key + tipSuffix);
	if (tip != null) {
	    b.setToolTipText(tip);
	}
 
        return b;
    }

    /**
     * Take the given string and chop it up into a series
     * of strings on whitespace boundries.  This is useful
     * for trying to get an array of strings out of the
     * resource file.
     */
    protected String[] tokenize(String input) {
	ArrayList v = new ArrayList();
	StringTokenizer t = new StringTokenizer(input);
	String cmd[];

	while (t.hasMoreTokens())
	    v.add(t.nextToken());
	cmd = new String[v.size()];
	for (int i = 0; i < cmd.length; i++)
	    cmd[i] = (String) v.get(i);

	return cmd;
    }

    /**
     * Create the menubar for the app.  By default this pulls the
     * definition of the menu from the associated resource file. 
     */
    protected JMenuBar createMenubar() {
	JMenuItem mi;
	JMenuBar mb = new JMenuBar();

	String[] menuKeys = tokenize(getResourceString("menubar"));
	for (int i = 0; i < menuKeys.length; i++) {
	    JMenu m = createMenu(menuKeys[i]);
	    if (m != null) {
		mb.add(m);
	    }
	}
	return mb;
    }

    /**
     * Create a menu for the app.  By default this pulls the
     * definition of the menu from the associated resource file.
     */
    protected JMenu createMenu(String key) {
	String[] itemKeys = tokenize(getResourceString(key));
	JMenu menu = new JMenu(getResourceString(key + "Label"));
	for (int i = 0; i < itemKeys.length; i++) {
	    if (itemKeys[i].equals("-")) {
		menu.addSeparator();
	    } else {
		JMenuItem mi = createMenuItem(itemKeys[i]);
		menu.add(mi);
	    }
	}
	return menu;
    }

    // Yarked from JMenu, ideally this would be public.
    protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
	return new ActionChangedListener(b);
    }

    // Yarked from JMenu, ideally this would be public.
    private class ActionChangedListener implements PropertyChangeListener {
        JMenuItem menuItem;
        
        ActionChangedListener(JMenuItem mi) {
            super();
            this.menuItem = mi;
        }
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (e.getPropertyName().equals(Action.NAME)) {
                String text = (String) e.getNewValue();
                menuItem.setText(text);
            } else if (propertyName.equals("enabled")) {
                Boolean enabledState = (Boolean) e.getNewValue();
                menuItem.setEnabled(enabledState.booleanValue());
            }
        }
    }
	
    public synchronized void closeWindow() {
	// first make sure no save/load threads are running
	try {
	    while (threadRunning) {
		Thread.yield();
		Thread.sleep(500);
	    }
	}
	catch (InterruptedException e) {
	}
	finally {
	    Frame f = getFrame();
	    f.setVisible(false);
	    f.remove(this);
	    f.dispose();
	}
    }
	
    private boolean threadRunning=false;
    private JTextPane editor;
    private Hashtable commands;
    private Hashtable menuItems;
    private JMenuBar menubar;
    private JToolBar toolbar;
    private StatusBar status;

    protected FileDialog fileDialog;

    /**
     * Listener for the edits on the current document.
     */
    protected UndoableEditListener undoHandler = new UndoHandler();

    /** UndoManager that we add edits to. */
    protected UndoManager undo = new UndoManager();

    /**
     * Suffix applied to the key used in resource file
     * lookups for an image.
     */
    public static final String imageSuffix = "Image";

    /**
     * Suffix applied to the key used in resource file
     * lookups for a label.
     */
    public static final String labelSuffix = "Label";

    /**
     * Suffix applied to the key used in resource file
     * lookups for an action.
     */
    public static final String actionSuffix = "Action";

    /**
     * Suffix applied to the key used in resource file
     * lookups for tooltip text.
     */
    public static final String tipSuffix = "Tooltip";

    public static final String openAction   = "open";
    public static final String newAction    = "new";
    public static final String saveAction   = "save";
    public static final String saveAsAction = "saveas";
    public static final String exitAction   = "exit";

    class UndoHandler implements UndoableEditListener {

	/**
	 * Messaged when the Document has created an edit, the edit is
	 * added to <code>undo</code>, an instance of UndoManager.
	 */
        public void undoableEditHappened(UndoableEditEvent e) {
	    undo.addEdit(e.getEdit());
	    undoAction.update();
	    redoAction.update();
	}
    }

    class StatusBar extends JComponent implements CaretListener {
	private JProgressBar progress;
	private JLabel cursorLabel;
        public StatusBar() {
	    super();
	    setLayout(new BorderLayout());
	    // initialize the statusbar
	    progress = new JProgressBar();
	    add(progress,BorderLayout.CENTER);
	    JPanel p = new JPanel(new GridLayout(1,2));
	    p.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	    p.add(new JLabel("Position ",JLabel.CENTER));
	    p.add(cursorLabel = new JLabel("",JLabel.CENTER));
	    add(p,BorderLayout.EAST);
	}
		
	public void setProgress(int value) {
	    progress.setValue(value);
	}
		
	public int getProgress() {
	    return progress.getValue();
	}
		
	public void setProgressBounds(int min, int max) {
	    progress.setMinimum(min);
	    progress.setMaximum(max);
	}
		
	public void caretUpdate(CaretEvent e) {
	    try {
		//cursorLabel.setText(new Integer(((JTextArea)e.getSource()).getLineOfOffset(e.getDot())+1).toString());
		cursorLabel.setText(Integer.toString(e.getDot()));
	    }
	    catch (Throwable t) {
		t.printStackTrace();
	    }
	}
    }

    // --- action implementations -----------------------------------

    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();

    /**
     * Actions defined by the Notepad class
     */
    private Action[] defaultActions = {
	new NewAction(),
	new OpenAction(),
	new SaveAction(this),
	new SaveAsAction(),
	new ExitAction(),
        undoAction,
        redoAction
    };

    class UndoAction extends AbstractAction {
	public UndoAction() {
	    super("Undo");
	    super.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
	    try {
		undo.undo();
	    } catch (CannotUndoException ex) {
		//System.out.println("Unable to undo: " + ex);
		//ex.printStackTrace();
	    }
	    update();
	    redoAction.update();
	}

	protected void update() {
	    if(undo.canUndo()) {
		super.setEnabled(true);
		putValue(Action.NAME, undo.getUndoPresentationName());
	    }
	    else {
		super.setEnabled(false);
		putValue(Action.NAME, "Undo");
	    }
	}
    }

    class RedoAction extends AbstractAction {
	public RedoAction() {
	    super("Redo");
	    super.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
	    try {
		undo.redo();
	    } catch (CannotRedoException ex) {
		//System.out.println("Unable to redo: " + ex);
		//ex.printStackTrace();
	    }
	    update();
	    undoAction.update();
	}

	protected void update() {
	    if(undo.canRedo()) {
		super.setEnabled(true);
		putValue(Action.NAME, undo.getRedoPresentationName());
	    }
	    else {
		super.setEnabled(false);
		putValue(Action.NAME, "Redo");
	    }
	}
    }

    class OpenAction extends NewAction {

	OpenAction() {
	    super(openAction);
	}

        public void actionPerformed(ActionEvent e) {
	    Frame frame = getFrame();
	    if (fileDialog == null) {
		fileDialog = new FileDialog(frame);
	    }
	    fileDialog.setMode(FileDialog.LOAD);
	    fileDialog.show();

	    String file = fileDialog.getFile();
	    if (file == null) {
		return;
	    }
	    String directory = fileDialog.getDirectory();
	    File f = new File(directory, file);
	    if (f.exists()) {
		Document oldDoc = getEditor().getDocument();
		if(oldDoc != null)
		    oldDoc.removeUndoableEditListener(undoHandler);
		getEditor().setDocument(createDocument());
		frame.setTitle(file);
		Thread loader = new FileLoader(f, (Document)editor.getDocument());
		loader.start();
	    }
	}
    }
    
    class NewAction extends AbstractAction {

	NewAction() {
	    super(newAction);
	}

	NewAction(String nm) {
	    super(nm);
	}

        public void actionPerformed(ActionEvent e) {
	    Document oldDoc = getEditor().getDocument();
	    if(oldDoc != null)
		oldDoc.removeUndoableEditListener(undoHandler);
	    getEditor().setDocument(createDocument());
	    getEditor().getDocument().addUndoableEditListener(undoHandler);
	    revalidate();
	}
    }
	
    class SaveAction extends AbstractAction {
	private Component parent;
		
	SaveAction(Component parent) {
	    super(saveAction);
	    this.parent = parent;
	}
		
	SaveAction(String nm) {
	    super(nm);
	}
		
        public void actionPerformed(ActionEvent e) {
	    Document oldDoc = getEditor().getDocument();
	    if(oldDoc != null) {
		getFrame().setTitle(file.getName());
		Thread saver = new FileSaver(parent,file, editor.getDocument());
		saver.start();				
	    }
	}
    }
	
    class SaveAsAction extends SaveAction {
		
	SaveAsAction() {
	    super(saveAsAction);
	}
		
	SaveAsAction(String nm) {
	    super(nm);
	}
		
        public void actionPerformed(ActionEvent e) {
	    Frame frame = getFrame();
	    if (fileDialog == null) {
		fileDialog = new FileDialog(frame);
	    }
	    //System.out.println("Name:"+file.getName()+" Dir:"+file.getParent());
	    fileDialog.setMode(FileDialog.SAVE);
	    fileDialog.setFile(file.getName());
	    fileDialog.setDirectory(file.getParent());
	    fileDialog.show();
			
	    String fileName = fileDialog.getFile();
	    if (fileName == null) {
		return;
	    }
	    String directory = fileDialog.getDirectory();
	    file = new File(fileDialog.getDirectory()+File.separator+fileDialog.getFile());
	    super.actionPerformed(e);
	}
    }
	
    /**
     * Really lame implementation of an exit command
     */
    class ExitAction extends AbstractAction {
	private Frame parent=null;
		
	ExitAction() {
	    super(exitAction);
	}
		
        public void actionPerformed(ActionEvent e) {
	    closeWindow();
	}
    }

    
    /**
     * Thread to load a file into the text storage model
     */
    class FileLoader extends Thread {

	FileLoader(File f, Document doc) {
	    setPriority(4);
	    this.f = f;
	    file = f;
	    this.doc = doc;
	}

        public void run() {

	    try {
		// initialize the statusbar
		status.setProgressBounds(0,(int) f.length());
				
		// indicate we can't exit cos this thread is running
		threadRunning = true;
				
		// try to start reading
		Reader in = new FileReader(f);
		char[] buff = new char[4096];
		int nch;
		while ((nch = in.read(buff, 0, buff.length)) != -1) {
		    doc.insertString(doc.getLength(), new String(buff, 0, nch), null);
		    status.setProgress(status.getProgress() + nch);
		}

		// we are done... get rid of progressbar
		doc.addUndoableEditListener(undoHandler);
		file = this.f; // store handle to name in parent
	    }
	    catch (IOException e) {
		System.err.println(e.toString());
	    }
	    catch (BadLocationException e) {
		System.err.println(e.getMessage());
	    }
	    finally {
		status.setProgress(0);
		// indicate we can exit cos this thread is finished
		threadRunning = false;
	    }
	}

	Document doc;
	File f;
    }
	
    /**
     * Thread to save a file from the text storage model
     */
    class FileSaver extends Thread {
	private Document doc;
	private File f;
	private Component parent;
		
	public FileSaver(Component parent, File f, Document doc) {
	    setPriority(4);
	    this.parent = parent;
	    this.f = f;
	    this.doc = doc;
	}

        public void run() {
	    try {
		// indicate we can't exit cos this thread is running
		threadRunning = true;
				
		Writer out = new FileWriter(f);
		// initialize the statusbar
		status.setProgressBounds(0, doc.getLength());
                // try to start writing
		int blockSize=4096;

		int count = 0;
		String s;
		while(count < doc.getLength()) {
		    if ((doc.getLength()-count) < blockSize)
			blockSize = doc.getLength()-count;
		    s = doc.getText(count,blockSize);
		    out.write(s);
		    count += s.length();
		    status.setProgress(count);
		}

		// we are done... get rid of progressbar
		doc.addUndoableEditListener(undoHandler);
		out.close();
	    }
	    catch (Exception e) {
		JOptionPane.showMessageDialog(parent, 
					      e.getMessage(),
					      Resources.getString("error"), 
					      JOptionPane.ERROR_MESSAGE);
	    }
	    finally {
		status.setProgress(0);
		// indicate we can exit cos this thread is finished
		threadRunning = false;
	    }
	}
    }
    }
