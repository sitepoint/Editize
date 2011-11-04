package com.editize;

import com.editize.editorkit.BooleanStateAwareTextAction;
import com.editize.editorkit.EditizeEditorKit;

import javax.swing.*;
import javax.swing.FocusManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A Simple WYSIWYG HTML Editor
 * Creation date: (07/07/2001 11:03:18 AM)
 * @author: Kevin Yank
 */
public class Editize extends JPanel implements CaretListener {
	private HTMLBlockTypeActionComboBox pStyleCombo;
	private Hashtable actionHash = new Hashtable();
	private JPopupMenu editPopup;
    private Action cutAction, copyAction;
	private HTMLPane textPane;
	private Keymap map;
	private EditizeEditorKit kit;
    private AbstractButton leftButton;
	private AbstractButton rightButton;
	private AbstractButton centerButton;
	//private AbstractButton justButton;
	private AbstractButton inButton;
	private AbstractButton outButton;
	private AbstractButton bulletButton;
	private AbstractButton numButton;
	private JSeparator pListSep;
	private AbstractButton cutButton;
	private AbstractButton copyButton;
	private AbstractButton pasteButton;
	private AbstractButton undoButton;
	private AbstractButton redoButton;
	private AbstractButton bb;
	private AbstractButton cb;
	private AbstractButton lb;
	private AbstractButton hb;
	private AbstractButton hbb;
	private AbstractButton ub;
	private AbstractButton ib;
	private AbstractButton imb;
	private AbstractButton tbb;
	private JSeparator charStyleSep;
	private KeyStroke boldStroke;
	private Action boldAction;
	private Action italicAction;
	private KeyStroke italicStroke;
	private Action underlineAction;
	private KeyStroke underlineStroke;
    private Action codeAction;
	private KeyStroke codeStroke;
	private Action linkAction;
	private KeyStroke linkStroke;
    private JMenu tableMenu;
	private JSeparator tableSep;
	private JSeparator stdActionSep;
	private JMenuItem aboutMenuItem;
	private JSeparator aboutSeparator;
	private boolean showAbout = true;

	private java.applet.Applet context;
	private JMenuItem imageMenuItem;
	private JMenuItem hyperlinkMenuItem;
	private JSeparator imageSep;
	private JSeparator hyperlinkSep;

	/**
	 * Creates an Editize field.
	 */
	public Editize()
	{
		this(null);
	}

	/**
	 * Creates an Editize field.
	 * @param context If not null, used to display the Editize homepage in a browser window when the 'About Editize' menu item is selected.
	 */
	public Editize(java.applet.Applet context)
	{
		this.context = context;

		setLayout(new BorderLayout());

		// Create JTextPane
		textPane = new HTMLPane();
		//textPane.setContentType("text/html");
		add(new JScrollPane(textPane,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),BorderLayout.CENTER);

		// Create a custom keymap
		map = JTextComponent.addKeymap("HotkeyStylesMap", textPane.getKeymap());
		textPane.setKeymap(map);

        // Unfortunately, a longstanding bug in Java for Mac OS X prevents
        // applets from receiving keyboard events with the Command key modifier.
        // We therefore force shortcuts to use Ctrl when loaded in an applet.
        int menuShortcutMask = context != null ?
            InputEvent.CTRL_MASK :
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		// Assign custom Caret
		Caret caret = new TableAwareCaret();
		caret.setBlinkRate(500);
		textPane.setCaret(caret);

		// Hook up document caret listener
		textPane.addCaretListener(this);

		// Hook up our editor kit
		kit = new EditizeEditorKit();
		textPane.setEditorKit(kit);
		textPane.setDocument(kit.createDefaultDocument());

		// Hash the actions supported by the pane
		hashActions();

		// Add icons and pretty names to kit actions
		decorateActions();

		// Popup menu listener
        MouseAdapter popupMenuListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            public void mouseClicked(MouseEvent e) {
                checkPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(MouseEvent e) {
                if (e.isPopupTrigger()) popup(e.getX(), e.getY());
            }
        };
        getTextPane().addMouseListener(popupMenuListener);

		// Add special text editing actions
		Action dnwAction = getHashedAction("delete-next-word");
		KeyStroke dnwStroke =
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK, false);
		map.addActionForKeyStroke(dnwStroke, dnwAction);
		Action dpwAction = getHashedAction("delete-previous-word");
		KeyStroke dpwStroke =
				KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_MASK, false);
		map.addActionForKeyStroke(dpwStroke, dpwAction);
		Action breakAction = getHashedAction("insert-line-break");
		KeyStroke breakStroke =
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK, false);
		map.addActionForKeyStroke(breakStroke,breakAction);
		Action paragraphbreakAction = getHashedAction(DefaultEditorKit.insertBreakAction);
		KeyStroke paragraphBreakStroke =
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
		map.addActionForKeyStroke(paragraphBreakStroke, paragraphbreakAction);
				Action insertTabAction = getHashedAction(DefaultEditorKit.insertTabAction);
				KeyStroke tabStroke =
								KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false);
				map.addActionForKeyStroke(tabStroke, insertTabAction);

		// Create the toolbar
        EditizeToolBar tb = new EditizeToolBar(textPane);
        tb.setBorderPainted(true);
		tb.setFloatable(false);
		add(tb, BorderLayout.NORTH);

		// Add paragraph style drop-down to toolbar
		pStyleCombo = new HTMLBlockTypeActionComboBox();
		textPane.addCaretListener(pStyleCombo);
		pStyleCombo.setToolTipText("Select paragraph style");
		tb.add(pStyleCombo);
		pStyleCombo.loadStyles(textPane);

		// Add alignment buttons to toolbar
		Action ljAction = getHashedAction("left-justify");
		leftButton = tb.addAction(ljAction);
		leftButton.setText("");
		leftButton.setToolTipText("Left Align");

		Action cjAction = getHashedAction("center-justify");
		centerButton = tb.addAction(cjAction);
		centerButton.setText("");
		centerButton.setToolTipText("Center Align");

		Action rjAction = getHashedAction("right-justify");
		rightButton = tb.addAction(rjAction);
		rightButton.setText("");
		rightButton.setToolTipText("Right Align");

		//Action jjAction = getHashedAction("full-justify");
		//justButton = tb.addAction(jjAction);
		//justButton.setText("");
		//justButton.setToolTipText("Justify");

		Action hbbAction = getHashedAction("highlight-block");
		hbb = tb.addAction(hbbAction);
		hbb.setText("");
		hbb.setToolTipText("Highlight Block");
		hbb.setVisible(false);

		Action outAction = getHashedAction("outdent-block");
		outButton = tb.addAction(outAction);
		outButton.setText("");
		outButton.setToolTipText("Outdent");

		Action inAction = getHashedAction("indent-block");
		inButton = tb.addAction(inAction);
		inButton.setText("");
		inButton.setToolTipText("Indent");

		// Add bullet button to toolbar
		bulletButton = tb.addAction(getHashedAction("set-bulleted"));
		bulletButton.setText("");
		bulletButton.setToolTipText("Bullet List");

		// Add numbered list button to toolbar
		numButton = tb.addAction(getHashedAction("set-numbered"));
		numButton.setText("");
		numButton.setToolTipText("Numbered List");

		pListSep = new HighlightedToolBar.Separator();
		tb.add(pListSep); // Space

		//cLabel = new JLabel("Character ");
		//tb.add(cLabel);

		// Create bold character style and add to toolbar
		boldAction = getHashedAction("font-bold");
		bb = tb.addAction(boldAction);
		bb.setText("");
		boldStroke = KeyStroke.getKeyStroke(KeyEvent.VK_B, menuShortcutMask);
        bb.setToolTipText("Bold (" + KeyEvent.getKeyModifiersText(boldStroke.getModifiers()) + KeyEvent.getKeyText(boldStroke.getKeyCode()) + ")");
		map.addActionForKeyStroke(boldStroke, boldAction);

		// Create italic character style and add to toolbar
		italicAction = getHashedAction("font-italic");
		ib = tb.addAction(italicAction);
		ib.setText("");
		try
		{
			FocusManager.setCurrentManager(new CtrlIFocusManager()); // Allow Ctrl-I
			italicStroke = KeyStroke.getKeyStroke(KeyEvent.VK_I, menuShortcutMask);
			ib.setToolTipText("Italic (" + KeyEvent.getKeyModifiersText(italicStroke.getModifiers()) + KeyEvent.getKeyText(italicStroke.getKeyCode()) + ")");
			map.addActionForKeyStroke(italicStroke, italicAction);
		} catch (java.security.AccessControlException e) {
			ib.setToolTipText("Italic");
		}

		// Create bold character style and add to toolbar
		underlineAction = getHashedAction("font-underline");
		ub = tb.addAction(underlineAction);
		ub.setText("");
		underlineStroke = KeyStroke.getKeyStroke(KeyEvent.VK_U, menuShortcutMask);
		ub.setToolTipText("Underline (" + KeyEvent.getKeyModifiersText(underlineStroke.getModifiers()) + KeyEvent.getKeyText(underlineStroke.getKeyCode()) + ")");
		map.addActionForKeyStroke(underlineStroke, underlineAction);

		// Create inline code character style and add to the toolbar
		codeAction = getHashedAction("font-inlinecode");
		cb = tb.addAction(codeAction);
		cb.setText("");
		codeStroke =
			KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK, false);
		cb.setToolTipText("Inline code (" + KeyEvent.getKeyModifiersText(codeStroke.getModifiers()) + KeyEvent.getKeyText(codeStroke.getKeyCode()) + ")");
		map.addActionForKeyStroke(codeStroke, codeAction);

		// Create highlighted character style and add to toolbar
        Action highlightAction = getHashedAction("font-highlighted");
        hb = tb.addAction(highlightAction);
		hb.setMargin(new Insets(4,2,0,2));
		hb.setText("");
		hb.setToolTipText("Highlight");
		// Hotkey removed -- Ctrl-H seems to act as backspace in most cases, and I
		// can't figure out immediately how to override that.
		//	KeyStroke highlightStroke =
		//		KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK, false);
		//	map.addActionForKeyStroke(highlightStroke, highlightAction);

		charStyleSep = new HighlightedToolBar.Separator();
		tb.add(charStyleSep); // Space

		// Add "standard" buttons to toolbar
        String ctrlOrCmd = KeyEvent.getKeyModifiersText(menuShortcutMask);
		Action cutAction = getHashedAction(DefaultEditorKit.cutAction);
		cutButton = tb.addAction(cutAction);
		cutButton.setText("");
		cutButton.setToolTipText("Cut (" + ctrlOrCmd + "X)");
		Action copyAction = getHashedAction(DefaultEditorKit.copyAction);
		copyButton = tb.addAction(copyAction);
		copyButton.setText("");
		copyButton.setToolTipText("Copy (" + ctrlOrCmd + "C)");
		Action pasteAction = getHashedAction(DefaultEditorKit.pasteAction);
		pasteButton = tb.addAction(pasteAction);
		pasteButton.setText("");
		pasteButton.setToolTipText("Paste (" + ctrlOrCmd + "V)");
		Action undoAction = getHashedAction("undo");
		undoButton = tb.addAction(undoAction);
		undoButton.setText("");
		undoButton.setToolTipText("Undo (" + ctrlOrCmd + "Z)");
		Action redoAction = getHashedAction("redo");
		redoButton = tb.addAction(redoAction);
		redoButton.setText("");
		redoButton.setToolTipText("Redo (" + ctrlOrCmd + "Y)");
		stdActionSep = new HighlightedToolBar.Separator();
		tb.add(stdActionSep); // Space

		// Create hyperlink button
		linkAction = getHashedAction("link");
		lb = tb.addAction(linkAction);
		lb.setText("");
		linkStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, menuShortcutMask);
		lb.setToolTipText("Hyperlink (" + KeyEvent.getKeyModifiersText(linkStroke.getModifiers()) + KeyEvent.getKeyText(linkStroke.getKeyCode()) + ")");
		map.addActionForKeyStroke(linkStroke, linkAction);

				// Create horizontal rule button
//                hrAction = getHashedAction("InsertHR");
//                imb = tb.addAction(hrAction);
//                imb.setText("");
//                imb.setToolTipText("Insert Horizontal Rule");

		// Create image button
        Action imgAction = getHashedAction("insert-image");
        imb = tb.addAction(imgAction);
		imb.setText("");
		imb.setToolTipText("Insert Image");

		// Create table button
        Action tblAction = getHashedAction("InsertTable");
        tbb = tb.addAction(tblAction);
		tbb.setText("");
		tbb.setToolTipText("Insert Table");

		// Workaround for Java < 1.5 where class selectors are not correctly
		// resolved in linked style sheets.
		setHighlightColor(Color.red);
		setBlockHighlightColor(Color.yellow);
	}
	/**
	 * Invoked whenever the caret moves in the text pane.
	 *
	 * @param evt javax.swing.event.CaretEvent
	 */
	public void caretUpdate(CaretEvent evt)
	{
		// Enable/disable clipboard actions
		if (getTextPane().getSelectedText() == null)
		{
			if (cutAction != null)
				cutAction.setEnabled(false);
			if (copyAction != null)
				copyAction.setEnabled(false);
		}
		else
		{
			if (cutAction != null)
				cutAction.setEnabled(true);
			if (copyAction != null)
				copyAction.setEnabled(true);
		}
	}
	/**
	 * Clears the document. Also resets the undo history.
	 */
	public void clear() {
		// Create empty HTML document
		textPane.setText("<p></p>");
		// HACK: Remove extra paragraph created by HTML parser
		try {
            textPane.getDocument().remove(1,1);
        } catch (BadLocationException e) {
            // Don't worry if we can't do it
        }
		kit.clearUndoHistory();

		/*textPane.setDocument(kit.createDefaultDocument());

		applyCustomStyles();*/
	}
	/**
	 * Handles the creation of the Edit pop-up menu.
	 */
	private void installEditMenu()
	{
		if (editPopup == null) {

            // Unfortunately, a longstanding bug in Java for Mac OS X prevents
            // applets from receiving keyboard events with the Command key modifier.
            // We therefore force shortcuts to use Ctrl when loaded in an applet.
            int menuShortcutMask = context != null ?
                InputEvent.CTRL_MASK :
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

            JMenuItem item;

			// Popup menus
			editPopup = new JPopupMenu("Edit");

			// Edit Image menu item
			item = imageMenuItem = editPopup.add(getHashedAction("insert-image"));
			item.setText("Edit Image...");
			editPopup.add(imageSep = new JSeparator());

			// Edit Hyperlink menu item
			item = hyperlinkMenuItem = editPopup.add(getHashedAction("link"));
			item.setText("Edit Hyperlink...");
			editPopup.add(hyperlinkSep = new JSeparator());

			// Table menu
			tableMenu = new JMenu("Table");
			item = tableMenu.add(getHashedAction("table-insert-row"));
			item.setText("Insert Row");
			item = tableMenu.add(getHashedAction("table-insert-column"));
			item.setText("Insert Column");
			tableMenu.addSeparator();
			item = tableMenu.add(getHashedAction("table-delete-row"));
			item.setText("Delete Row");
			item = tableMenu.add(getHashedAction("table-delete-column"));
			item.setText("Delete Column");
//		tableMenu.addSeparator();
//		item = tableMenu.add(getHashedAction("table-increase-colspan"));
//		item.setText("Merge Right");
//		item = tableMenu.add(getHashedAction("table-increase-rowspan"));
//		item.setText("Merge Down");
//		item = tableMenu.add(getHashedAction("table-decrease-colspan"));
//		item.setText("Split Right");
//		item = tableMenu.add(getHashedAction("table-decrease-rowspan"));
//		item.setText("Split Down");
			tableMenu.addSeparator();
			item = tableMenu.add(getHashedAction("edit-cell"));
			item.setText("Cell Properties...");
			item = tableMenu.add(getHashedAction("edit-table"));
			item.setText("Table Properties...");
			item = tableMenu.add(getHashedAction("table-delete"));
			item.setText("Delete Table");
			editPopup.add(tableMenu);
			editPopup.add(tableSep = new JSeparator());

            JMenuItem undoMenuItem = editPopup.add(getHashedAction("undo"));
            undoMenuItem.setText("Undo");
            JMenuItem redoMenuItem = editPopup.add(getHashedAction("redo"));
            redoMenuItem.setText("Redo");
			undoMenuItem.setMnemonic('u');
			redoMenuItem.setMnemonic('r');
			KeyStroke undoStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuShortcutMask);
			undoMenuItem.setAccelerator(undoStroke);
			KeyStroke redoStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutMask);
			redoMenuItem.setAccelerator(redoStroke);

			map.addActionForKeyStroke(undoStroke, getHashedAction("undo"));
			map.addActionForKeyStroke(redoStroke, getHashedAction("redo"));

			editPopup.addSeparator();

			cutAction = getHashedAction(DefaultEditorKit.cutAction);
			//if (cutAction == null) cutAction = getHashedAction("Cut"); // Fixes applet reload bug
			cutAction.setEnabled(false);
			KeyStroke cutStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutMask);
			JMenuItem cutItem = editPopup.add(cutAction);
			cutItem.setText("Cut");
			cutItem.setAccelerator(cutStroke);
			cutItem.setMnemonic('t');
			// Make sure cutStroke is mapped to cutAction in the text area -- Sun's JRE seems to
			// forget this, possibly because we've changed the action's name.
			map.addActionForKeyStroke(cutStroke, cutAction);
			// Add "alternative" keystroke
			cutStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,
                    InputEvent.SHIFT_MASK, false);
			map.addActionForKeyStroke(cutStroke, cutAction);

			copyAction = getHashedAction(DefaultEditorKit.copyAction);
			//if (copyAction == null) copyAction = getHashedAction("Copy"); // Fixes applet reload bug
			copyAction.setEnabled(false);
			KeyStroke copyStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutMask);
			JMenuItem copyItem = editPopup.add(copyAction);
			copyItem.setText("Copy");
			copyItem.setAccelerator(copyStroke);
			copyItem.setMnemonic('o');
			// Make sure copyStroke is mapped to copyAction in the text area -- Sun's JRE seems to
			// forget this, possibly because we've changed the action's name.
			map.addActionForKeyStroke(copyStroke, copyAction);
			// Add "alternative" keystroke
			copyStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, menuShortcutMask);
			map.addActionForKeyStroke(copyStroke, copyAction);

			Action pasteAction = getHashedAction(DefaultEditorKit.pasteAction);
			//if (pasteAction == null) pasteAction = getHashedAction("Paste"); // Fixes applet reload bug
			KeyStroke pasteStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutMask);
			JMenuItem pasteItem = editPopup.add(pasteAction);
			pasteItem.setText("Paste");
			pasteItem.setAccelerator(pasteStroke);
			pasteItem.setMnemonic('p');
			// Make sure pasteStroke is mapped to pasteAction in the text area -- Sun's JRE seems to
			// forget this, possibly because we've changed the action's name.
			map.addActionForKeyStroke(pasteStroke, pasteAction);
			pasteStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_INSERT,
                    InputEvent.SHIFT_MASK, false);
			map.addActionForKeyStroke(pasteStroke, pasteAction);

			editPopup.addSeparator();

			Action selectAllAction = getHashedAction(DefaultEditorKit.
					selectAllAction);
			//if (selectAllAction == null) selectAllAction = getHashedAction("Select All"); // Fixes applet reload bug
			KeyStroke selectAllStroke =
					KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutMask);
			JMenuItem selectAllItem = editPopup.add(selectAllAction);
			selectAllItem.setText("Select All");
			selectAllItem.setAccelerator(selectAllStroke);
			selectAllItem.setMnemonic('A');
			// Make sure selectAllStroke is mapped to selectAllAction in the text area --
			// Sun's JRE seems to forget this, possibly because we've changed the action's name.
			map.addActionForKeyStroke(selectAllStroke, selectAllAction);

			if (context != null) {
				aboutSeparator = new JPopupMenu.Separator();
				editPopup.add(aboutSeparator);
				aboutSeparator.setVisible(showAbout);

				final Action editizeInfoAction = new AbstractAction() {
					public void actionPerformed(ActionEvent evt) {
						try {
							context.getAppletContext().showDocument(new URL(
									"http://www.editize.com/"), "_blank");
						} catch (MalformedURLException ex) {
                            // Not a big deal if we can't launch the URL.
                        }
					}
				};

				aboutMenuItem = editPopup.add(editizeInfoAction);
				aboutMenuItem.setText("About Editize");
				aboutMenuItem.setVisible(showAbout);
			}
		}
	}

	/**
	 * Pulls apart the editPopup, since it is a listener registered with actions
	 * declared in static classes, and will therefore cause a memory leak if it
	 * is not unregistered.
	 */
	private void uninstallEditMenu() {
		editPopup.removeAll();
		editPopup = null;
	}

	/**
	 * Assigns pretty names and icons to actions supported by the HTMLPane's EditorKit.
	 */
	protected void decorateActions()
	{
		Action a;
		InputStream i;
		byte img[];
        int readLength;

        try
		{

			a = getHashedAction(DefaultEditorKit.cutAction);
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Cut16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction(DefaultEditorKit.copyAction);
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Copy16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction(DefaultEditorKit.pasteAction);
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Paste16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("font-bold");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/Bold16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("font-italic");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/Italic16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("font-underline");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/Underline16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("font-inlinecode");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/Code16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("link");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Bookmarks16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("undo");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Undo16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("redo");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Redo16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("left-justify");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/AlignLeft16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("center-justify");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/AlignCenter16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("right-justify");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/AlignRight16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("indent-block");
			i = getClass().getResourceAsStream(
					"toolbarButtonGraphics/text/Indent16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("outdent-block");
			i = getClass().getResourceAsStream(
					"toolbarButtonGraphics/text/Outdent16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("set-bulleted");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/BulletList16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("set-numbered");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/text/NumList16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("InsertHR");
			i = getClass().getResourceAsStream(
					"toolbarButtonGraphics/general/Hr16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("insert-image");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Image16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));

			a = getHashedAction("InsertTable");
			i = getClass().getResourceAsStream("toolbarButtonGraphics/general/Table16.gif");
			img = new byte[i.available()];
			readLength = i.read(img);
			if (readLength > 0) a.putValue(Action.SMALL_ICON, new ImageIcon(img));
		}
		catch (IOException e)
		{
			System.err.println("Error: Could not load required image resource(s).");
		}
		catch (NullPointerException e)
		{
			System.err.println("NullPointerException loading required image resource(s).");
		}
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (10/07/2001 1:15:46 PM)
	 * @return javax.swing.Action
	 * @param name java.lang.String
	 */
	protected Action getHashedAction(String name) {
		return (Action)actionHash.get(name);
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (08/08/2001 1:17:44 PM)
	 * @return javax.swing.JTextPane
	 */
	public JTextPane getTextPane() {
		return textPane;
	}
	/**
	 * Obtains all of the actions supported by the JTextComponent
	 * and places them into a hash. Called by initialization code.
	 * Creation date: (10/07/2001 12:26:12 PM)
	 */
	protected void hashActions()
	{
		Action[] actions = getTextPane().getActions();
		for (int i=0; i<actions.length; i++)
		{
			String name = (String)actions[i].getValue(Action.NAME);
			actionHash.put(name,actions[i]);
		}
	}
	/**
	 * Determines whether the indent/outdent buttons are available or not.
     * @return true if the indent/outdent buttons are available available
	 */
	public boolean isIndentEnabled()
	{
		return inButton.isVisible();
	}
	/**
	 * Determines whether the bold text feature is enabled or not.
     * @return true if the bold text feature is available
	 */
	public boolean isBoldEnabled()
	{
		return bb.isVisible();
	}
	/**
	 * Determines whether the bullet list feature is enabled or not.
     * @return true if the bullet list feature is available
	 */
	public boolean isBulletListEnabled()
	{
		return bulletButton.isVisible();
	}
	/**
	 * Determines whether the Code Block style is available in the drop-down or not.
     * @return true if the Code Block style is available
	 */
	public boolean isCodeblockStyleEnabled()
	{
		return !pStyleCombo.isTagHidden(HTML.Tag.PRE);
	}
	/**
	 * Determines whether the inline code feature is enabled or not.
     * @return true if the inline code feature is available
	 */
	public boolean isCodeEnabled()
	{
		return cb.isVisible();
	}
	/**
	 * Determines whether the Heading style is available in the drop-down or not.
     * @return true if the Heading style is available
	 */
	public boolean isHeadingStyleEnabled()
	{
		return !pStyleCombo.isTagHidden(HTML.Tag.H1);
	}
	/**
	 * Determines whether the highlighted text feature is enabled or not.
     * @return true if the highlighted text feature is available
	 */
	public boolean isHighlightEnabled()
	{
		return hb.isVisible();
	}
	/**
	 * Determines whether the highlighted block feature is enabled or not.
     * @return true if the highlighted block feature is available
	 */
	public boolean isBlockHighlightEnabled()
	{
		return hbb.isVisible();
	}
	/**
	 * Determines whether the hyperlink feature is enabled or not.
     * @return true if the hyperlink feature is available
	 */
	public boolean isHyperlinksEnabled()
	{
		return lb.isVisible();
	}
	/**
	 * Determines whether the image insert feature is enabled or not.
     * @return true if the image insert feature is available
	 */
	public boolean isImagesEnabled()
	{
		return imb.isVisible();
	}
	/**
	 * Determines whether the italic text feature is enabled or not.
     * @return true if the italic text feature is available
	 */
	public boolean isItalicEnabled()
	{
		return ib.isVisible();
	}
	/**
	 * Determines whether the numbered list feature is enabled or not.
     * @return true if the numbered list feature is available
	 */
	public boolean isNumberedListEnabled()
	{
		return numButton.isVisible();
	}
	/**
	 * Determines whether the paragraph alignment buttons are visible or not.
     * @return true if the paragraph alignment buttons are available
	 */
	public boolean isParagraphAlignmentEnabled()
	{
		return leftButton.isVisible() || centerButton.isVisible() || rightButton.isVisible() /*|| justButton.isVisible()*/;
	}
	/**
	 * Determines whether the paragraph styles combo box is visible or not.
     * @return true if the paragraph styles drop-down is available
	 */
	public boolean isParagraphStylesEnabled()
	{
		return pStyleCombo.isVisible();
	}
	/**
	 * Determines whether the Subheading style is available in the drop-down or not.
     * @return true if the Subheading style is available
	 */
	public boolean isSubheadingStyleEnabled()
	{
		return !pStyleCombo.isTagHidden(HTML.Tag.H2);
	}
	/**
	 * Determines whether the underlined text feature is enabled or not.
     * @return true if the underlined text feature is available
	 */
	public boolean isUnderlineEnabled()
	{
		return ub.isVisible();
	}

    /**
     * Determines whether the "About Editize" menu item is displayed or not.
     * @return true if the "About Editize" feature is available
     */
    public boolean isAboutEnabled()
    {
      return showAbout;
    }

	/**
	 * Sets the HTML code to be displayed by the editor. Clears the undo history.
	 * @param html The HTML code to be displayed for editing. At this time, this must be either an empty string or one or more HTML block elements. Editing a fragment of inline HTML content is not yet supported, nor is editing a fully-formed HTML document.
	 */
	public void setHtml(String html)
	{
		setHtml(html,false);
	}

	/**
	 * Sets the HTML code to be displayed by the editor.
	 * @param html The HTML code to be displayed for editing. At this time, this must be either an empty string or one or more HTML block elements. Editing a fragment of inline HTML content is not yet supported, nor is editing a fully-formed HTML document.
	 * @param undoable If true, the user can undo this operation. If false, the undo history is cleared and the operation cannot be undone.
	 */
	public void setHtml(String html, boolean undoable)
	{

		EditizeEditorKit kit = (EditizeEditorKit)textPane.getEditorKit();

		if (!undoable) kit.setUndoManagerActive(false);

		if (html == null)
		{
			clear();
			return;
		}

		textPane.setText(html);

		if (!undoable) kit.setUndoManagerActive(true);

		textPane.setCaretPosition(1);
	}
	/**
	 * Starts the applet when it is run as an application
	 * @param args an array of command-line arguments
	 */
	public static void main(String[] args)
	{

		//Uncomment to use system (e.g. Windows) look-and-feel
		/*try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("WARNING: Error setting system look-and-feel.");
		}*/

		// Uncomment to use Kunstsoff (improved Metal) look-and-feel
		/*try {
			Class kClass = Class.forName("com.incors.plaf.kunststoff.KunststoffLookAndFeel");
			UIManager.setLookAndFeel((LookAndFeel)kClass.newInstance());
		} catch (Exception e) {
			System.err.println("WARNING: Kunststoff Look 'n Feel not loaded.");
		}*/

		Editize main = new Editize();
				main.setBaseFontColor(Color.black);
				main.setBackgroundColor(Color.white);
		JFrame frame = new JFrame("Editize");

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new BasicWindowMonitor());
		frame.getContentPane().add("Center", main);

		frame.setSize(640, 480);
		frame.show();

		main.textPane.requestFocus();
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (17/07/2001 2:44:12 PM)
	 * @param x int
	 * @param y int
	 */
	private void popup(int x, int y)
	{
		if (editPopup == null) return;

		boolean show;
		JTextPane tp = getTextPane();

		// Display Edit Image
		show = ((BooleanStateAwareTextAction)imageMenuItem.getAction()).getState();
		imageMenuItem.setVisible(show);
		imageSep.setVisible(show);

		// Display Edit Hyperlink
		show = ((BooleanStateAwareTextAction)hyperlinkMenuItem.getAction()).getState();
		hyperlinkMenuItem.setVisible(show);
		hyperlinkSep.setVisible(show);

		// Display table menu
		boolean showTable = (EditizeEditorKit.getContainingTableCell(tp.getCaretPosition(),tp.getDocument()) != null);
		tableMenu.setVisible(showTable);
		tableSep.setVisible(showTable);

		editPopup.show(getTextPane(), x, y);
	}

	public void setBaseUrl(String urlStr) throws MalformedURLException
	{
		URL url = new URL(urlStr);

		((HTMLDocument)getTextPane().getDocument()).setBase(url);
	}

	public void setImgListUrl(String urlStr) throws MalformedURLException
	{
		EditizeEditorKit kit = (EditizeEditorKit)getTextPane().getEditorKit();
		kit.setImageListUrl(urlStr);
	}

		public void setBackgroundColor(Color c)
		{
		  HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		  hdoc.getStyleSheet().addRule("body { background-color: " + colorToHex(c) + " }");
		  textPane.setBackground(c);
		}

	/**
	 * Updates the font color used to display normal paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The color of the font.
	 */
	public void setBaseFontColor(Color c)
	{
		//this.baseFontColor = c;
		/*StyledDocument sdoc = textPane.getStyledDocument();
		Style normal = sdoc.getStyle(EditizeEditorKit.NORMAL_STYLE);
		StyleConstants.setForeground(normal,c);*/

		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("body, p { color: " + colorToHex(c) + " }");
	}
	/**
	 * Updates the font family used to display normal paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param family The name of the font family. Logical font names
	 *               may also be used: Dialog, DialogInput, Monospaced,
	 *               Serif, SansSerif, or Symbol.
	 */
	public void setBaseFontFamily(String family)
	{
		//this.baseFontFamily = family;
		/*StyledDocument sdoc = textPane.getStyledDocument();
		Style normal = sdoc.getStyle(EditizeEditorKit.NORMAL_STYLE);
		StyleConstants.setFontFamily(normal,family);*/

		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("body, p { font-family: " + family + " }");
	}
	/**
	 * Updates the font size used to display normal paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param size The size of the font in points.
	 */
	public void setBaseFontSize(int size)
	{
		//this.baseFontSize = size;
		/*StyledDocument sdoc = textPane.getStyledDocument();
		Style normal = sdoc.getStyle(EditizeEditorKit.NORMAL_STYLE);
		StyleConstants.setFontSize(normal,size);*/

		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("body, p { font-size: " + size + "pt }");
	}
	/**
	 * Updates the font color used to display Block Quote paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The color of the font.
	 */
	public void setBlockquoteFontColor(Color c)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("blockquote { color: " + colorToHex(c) + " }");
	}
	/**
	 * Updates the font family used to display Block Quote paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param family The name of the font family. Logical font names
	 *               may also be used: Dialog, DialogInput, Monospaced,
	 *               Serif, SansSerif, or Symbol.
	 */
	public void setBlockquoteFontFamily(String family)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("blockquote { font-family: " + family + " }");
	}
	/**
	 * Updates the font size used to display Block Quote paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param size The size of the font in points
	 */
	public void setBlockquoteFontSize(int size)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("blockquote { font-size: " + size + "pt }");
	}
	/**
	 * Sets whether Indent/Outdent buttons are available or not
     * @param enabled true to show indent/outdent buttons, false to hide them
	 */
	public void setIndentEnabled(boolean enabled)
	{
		  inButton.setVisible(enabled);
		  outButton.setVisible(enabled);
		  pListSep.setVisible(isParagraphStylesEnabled() ||
							  isParagraphAlignmentEnabled() || isIndentEnabled() ||
							  isBulletListEnabled() || isNumberedListEnabled());
	}
	/**
	 * Allows bold text to be enabled or disabled. When disabled,
	 * the bold text button disappears from the toolbar completely,
	 * and the shortcut key does not function.
	 *
	 * The editor can always read, display, and write bold text
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable bold text, false to disable it.
	 */
	public void setBoldEnabled(boolean enabled)
	{
		bb.setVisible(enabled);
		if (enabled)
			map.addActionForKeyStroke(boldStroke,boldAction);
		else
			map.removeKeyStrokeBinding(boldStroke);
		charStyleSep.setVisible(
			isBoldEnabled() ||
			isItalicEnabled() ||
			isUnderlineEnabled() ||
			isHighlightEnabled() ||
			isCodeEnabled());
		//showHideToolbarLabels();
	}
	/**
	 * Allows bullet lists to be enabled or disabled. When disabled,
	 * the bullet list button disappears from the toolbar completely.
	 *
	 * The editor can always read, display, and write bullet lists
	 * correctly, but setting this false prevents creating new ones.
	 *
	 * @param enabled True to enable bullet lists, false to disable them.
	 */
	public void setBulletListEnabled(boolean enabled)
	{
		bulletButton.setVisible(enabled);
				pListSep.setVisible(isParagraphStylesEnabled() ||
									isParagraphAlignmentEnabled() || isIndentEnabled() ||
									isBulletListEnabled() || isNumberedListEnabled());
	}
	/**
	 * Updates the font color used as the background for Code Blocks
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The background color.
	 */
	public void setCodeBlockBackgroundColor(Color c)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("pre { background-color: " + colorToHex(c) + " }");
	}
	/**
	 * Sets whether the Code Block style is available in the drop-down or not.
     * @param enabled true to show the code block style option, false to hide it
	 */
	public void setCodeblockStyleEnabled(boolean enabled)
	{
		if (!enabled) pStyleCombo.hideTag(HTML.Tag.PRE);
		else pStyleCombo.showTag(HTML.Tag.PRE);
	}
	/**
	 * Allows inline code to be enabled or disabled. When disabled,
	 * the inline code button disappears from the toolbar completely,
	 * and the shortcut key does not function.
	 *
	 * The editor can always read, display, and write inline code
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable inline code, false to disable it.
	 */
	public void setCodeEnabled(boolean enabled)
	{
		cb.setVisible(enabled);
		if (enabled)
			map.addActionForKeyStroke(codeStroke,codeAction);
		else
			map.removeKeyStrokeBinding(codeStroke);
		charStyleSep.setVisible(
			isBoldEnabled() ||
			isItalicEnabled() ||
			isUnderlineEnabled() ||
			isHighlightEnabled() ||
			isCodeEnabled());
		//showHideToolbarLabels();
	}
		/**
		 * Allows the "About Editize" menu item to be enabled or disabled.
		 * @param enabled True to display the menu item, false to hide it.
		 */
		public void setAboutEnabled(boolean enabled)
		{
			showAbout = enabled;
			if (aboutSeparator != null && aboutMenuItem != null) {
				aboutSeparator.setVisible(enabled);
				aboutMenuItem.setVisible(enabled);
			}
		}
	/**
	 * Updates the font color used to display Heading paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The color of the font.
	 */
	public void setHeadingFontColor(Color c)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("h1 { color: " + colorToHex(c) + " }");
	}
	/**
	 * Updates the font family used to display Heading paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param family The name of the font family. Logical font names
	 *               may also be used: Dialog, DialogInput, Monospaced,
	 *               Serif, SansSerif, or Symbol.
	 */
	public void setHeadingFontFamily(String family)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("h1 { font-family: " + family + " }");
	}
	/**
	 * Updates the font size used to display Heading paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param size The size of the font in points.
	 */
	public void setHeadingFontSize(int size)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("h1 { font-size: " + size + "pt }");
	}
	/**
	 * Sets whether the Heading style is available in the drop-down or not.
     * @param enabled true to show the Heading style, false to hide it
	 */
	public void setHeadingStyleEnabled(boolean enabled)
	{
		if (!enabled) pStyleCombo.hideTag(HTML.Tag.H1);
		else pStyleCombo.showTag(HTML.Tag.H1);
	}
	/**
	 * Updates the font color used to display highlighted text
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The color of the font.
	 */
	public void setHighlightColor(Color c)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("span.highlighted { color: " + colorToHex(c) + " }");
	}
	/**
	 * Updates the background color used to display highlighted blocks
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The color of the font.
	 */
	public void setBlockHighlightColor(Color c)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("div.highlighted { background-color: " + colorToHex(c) + " }");
	}
	/**
	 * Allows highlighted text to be enabled or disabled. When disabled,
	 * the highlighted text button disappears from the toolbar completely.
	 *
	 * The editor can always read, display, and write highlighted text
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable highlighted text, false to disable it.
	 */
	public void setHighlightEnabled(boolean enabled)
	{
		hb.setVisible(enabled);
		charStyleSep.setVisible(
			isBoldEnabled() ||
			isItalicEnabled() ||
			isUnderlineEnabled() ||
			isHighlightEnabled() ||
			isCodeEnabled());
		//showHideToolbarLabels();
	}

	/**
	 * Allows highlighted blocks to be enabled or disabled. When disabled,
	 * the highlighted block button disappears from the toolbar completely.
	 *
	 * The editor can always read, display, and write highlighted blocks
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable highlighted blocks, false to disable them.
	 */
	public void setBlockHighlightEnabled(boolean enabled)
	{
		hbb.setVisible(enabled);
		pListSep.setVisible(isParagraphStylesEnabled() ||
												isParagraphAlignmentEnabled() ||
												isBlockHighlightEnabled() ||
												isIndentEnabled() ||
												isBulletListEnabled() || isNumberedListEnabled());
	}

	/**
	 * Allows the 'standard' toolbar buttons (cut, copy, paste, undo, redo)
	 * to be shown or hidden from the toolbar. These features are always
	 * available through the pop-up menu and through keyboard shortcuts; this
	 * method only determines whether the buttons are visible on the toolbar.
	 *
	 * @param visible Whether or not to display the buttons on the toolbar.
	 */
	public void setStandardButtonsVisible(boolean visible)
	{
		cutButton.setVisible(visible);
		copyButton.setVisible(visible);
		pasteButton.setVisible(visible);
		undoButton.setVisible(visible);
		redoButton.setVisible(visible);
		stdActionSep.setVisible(visible);
	}

	/**
	 * Determines whether the standard editing buttons are visible or not.
	 * The standard editing buttons are cut, copy, paste, undo, redo.
     * @return true if the standard buttons are available
	 */
	public boolean isStandardButtonsVisible()
	{
		return cutButton.isVisible() || copyButton.isVisible() || pasteButton.isVisible() || undoButton.isVisible() || redoButton.isVisible();
	}

	/**
	 * Allows linked text to be enabled or disabled. When disabled,
	 * the linked text buttons disappear from the toolbar completely.
	 *
	 * The editor can always read, display, and write linked text
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable linked text, false to disable it.
	 */
	public void setHyperlinksEnabled(boolean enabled)
	{
		lb.setVisible(enabled);
		if (enabled)
			map.addActionForKeyStroke(linkStroke,linkAction);
		else
			map.removeKeyStrokeBinding(linkStroke);
		//showHideToolbarLabels();
	}
	/**
	 * Allows image insertion to be enabled or disabled. When disabled,
	 * the image button disappears from the toolbar completely.
	 *
	 * The editor can always read, display, and write images
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable image insert, false to disable it.
	 */
	public void setImagesEnabled(boolean enabled)
	{
		imb.setVisible(enabled);
		//showHideToolbarLabels();
	}

	/**
	 * Allows table insertion to be enabled or disabled. When disabled,
	 * the table button disappears from the toolbar completely.
	 *
	 * The editor can always read, display, edit, and write tables
	 * correctly, but setting this false prevents creating new ones.
	 *
	 * @param enabled True to enable table insert, false to disable it.
	 */
	public void setTablesEnabled(boolean enabled)
	{
		tbb.setVisible(enabled);
		//showHideToolbarLabels();
	}

	/**
	 * Determins if table insertion is enabled or not.
	 *
	 * @return True if enabled, false if not.
	 */
	public boolean isTablesEnabled()
	{
		return tbb.isVisible();
	}

	/**
	 * Allows italic text to be enabled or disabled. When disabled,
	 * the italic text button disappears from the toolbar completely,
	 * and the shortcut key does not function.
	 *
	 * The editor can always read, display, and write italic text
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable italic text, false to disable it.
	 */
	public void setItalicEnabled(boolean enabled)
	{
		ib.setVisible(enabled);
		// If constructor was allowed to override default mapping on Ctrl-I...
		if (italicStroke != null)
		{
			if (enabled)
				map.addActionForKeyStroke(italicStroke,italicAction);
			else
				map.removeKeyStrokeBinding(italicStroke);
		}
		charStyleSep.setVisible(
			isBoldEnabled() ||
			isItalicEnabled() ||
			isUnderlineEnabled() ||
			isHighlightEnabled() ||
			isCodeEnabled());
	}

	/**
	 * Updates the font color used to display linked text
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The color of the font.
	 */
	public void setLinkColor(Color c)
	{
		HTMLDocument hdoc = (HTMLDocument) textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("a { color: " + colorToHex(c) + "; }");
	}

	/**
	 * Updates the suggested links that are available in the hyperlink
	 * dialog box.
	 *
	 * @param urls A Vector of urls.
	 */
	public void setLinkURLs(Vector urls)
	{
		Vector linkUrls = (Vector)urls.clone();
		//this.linkUrls = linkUrls;
		Document doc = textPane.getDocument();
		doc.putProperty("linkurls",linkUrls);
	}

	/**
	 * Returns a Vector of the suggested link URLs that are available in
	 * the hyperlink dialog box.
	 *
	 * @return A Vector of the suggested link URLs.
	 */
	public Vector getLinkURLs()
	{
		return (Vector)textPane.getDocument().getProperty("linkurls");
	}

	/**
	 * Updates the available table classes in the table properties dialog.
	 *
	 * @param v A Vector of CSS class names.
	 */
	public void setTableClasses(Vector v)
	{
		Vector classes = (Vector)v.clone();
		EditizeDocument doc = (EditizeDocument)textPane.getDocument();
		doc.setTableClasses(classes);
	}

	/**
	 * Returns a Vector of the CSS class names that are available in
	 * the table properties dialog box.
	 *
	 * @return A Vector of CSS class names.
	 */
	public Vector getTableClasses()
	{
		return ((EditizeDocument)textPane.getDocument()).getTableClasses();
	}

	/**
	 * Allows numbered lists to be enabled or disabled. When disabled,
	 * the numbered list button disappears from the toolbar completely.
	 *
	 * The editor can always read, display, and write numbered lists
	 * correctly, but setting this false prevents creating new ones.
	 *
	 * @param enabled True to enable numbered lists, false to disable them.
	 */
	public void setNumberedListEnabled(boolean enabled)
	{
		numButton.setVisible(enabled);
				pListSep.setVisible(isParagraphStylesEnabled() ||
									isParagraphAlignmentEnabled() || isIndentEnabled() ||
									isBulletListEnabled() || isNumberedListEnabled());
	}

	/**
	 * Allows paragraph alignment to be enabled or disabled. When
	 * disabled, the three paragraph alignment buttons disappear from
	 * the toolbar completely, and all new paragraphs are left-aligned
	 * by default.
	 *
	 * The editor can always read, display, and write aligned paragraphs
	 * correctly, but setting this false prevents modification of
	 * paragraph alignments.
	 *
	 * @param enabled True to enable paragraph alignment, false to disable it.
	 */
	public void setParagraphAlignmentEnabled(boolean enabled)
	{
		leftButton.setVisible(enabled);
		centerButton.setVisible(enabled);
		rightButton.setVisible(enabled);
		//justButton.setVisible(enabled);
		pListSep.setVisible(isParagraphStylesEnabled() ||
							isParagraphAlignmentEnabled() || isIndentEnabled() ||
							isBulletListEnabled() || isNumberedListEnabled());
	}

		/**
		 * When enabled, Editize produces XHTML Strict compliant code by using
		 * the class names 'leftalign', 'centeralign', 'rightalign', and
		 * 'imgleft', 'imgright', 'imgtop', and 'imgmiddle' to perform alignment
		 * tasks.
		 * @param enabled true to enable XHTML Strict mode
		 */
		public void setXHTMLCompliantAlignment(boolean enabled)
		{
		  EditizeDocument doc = (EditizeDocument) textPane.getStyledDocument();
		  doc.setXHTMLCompliantAlignment(enabled);
		}

    /**
     * When enabled, Editize allows HTML form elements such as 'form', 'input',
     * and 'textarea' to be inserted via the code view, or by pasting from the
     * clipboard.
     * @param enabled true to allow form elements
     */
    public void setFormElementsAllowed(boolean enabled)
    {
      EditizeDocument doc = (EditizeDocument) textPane.getStyledDocument();
      doc.setFormElementsAllowed(enabled);
    }

	/**
	 * Allows paragraph styles to be enabled or disabled. When
	 * disabled, the paragraph style drop-down disappears completely.
	 * The editor can always read, display, and write paragraph styles
	 * correctly, but setting this false prevents modification of
	 * paragraph styles.
	 *
	 * @param enabled True to enable paragraph styles, false to disable them.
	 */
	public void setParagraphStylesEnabled(boolean enabled)
	{
		pStyleCombo.setVisible(enabled);
				pListSep.setVisible(isParagraphStylesEnabled() ||
									isParagraphAlignmentEnabled() || isIndentEnabled() ||
									isBulletListEnabled() || isNumberedListEnabled());
	}
	/**
	 * Updates the font color used to display Heading paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param c The color of the font.
	 */
	public void setSubheadingFontColor(Color c)
	{
		  HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		  hdoc.getStyleSheet().addRule("h2 { color: " + colorToHex(c) + " }");
	}
	/**
	 * Updates the font family used to display Heading paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param family The name of the font family. Logical font names
	 *               may also be used: Dialog, DialogInput, Monospaced,
	 *               Serif, SansSerif, or Symbol.
	 */
	public void setSubheadingFontFamily(String family)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("h2 { font-family: " + family + " }");
	}
	/**
	 * Updates the font size used to display Heading paragraphs
	 * in this component. At present, this does not update the current
	 * display. It is assumed this will be called during the setup
	 * of the component -- before any content is added.
	 *
	 * @param size The size of the font in points
	 */
	public void setSubheadingFontSize(int size)
	{
		HTMLDocument hdoc = (HTMLDocument)textPane.getStyledDocument();
		hdoc.getStyleSheet().addRule("h2 { font-size: " + size+ "pt }");
	}
	/**
	 * Sets whether the Subheading style is available in the drop-down or not.
     * @param enabled true to show the Subheading style, false to hide it
	 */
	public void setSubheadingStyleEnabled(boolean enabled)
	{
		if (!enabled) pStyleCombo.hideTag(HTML.Tag.H2);
		else pStyleCombo.showTag(HTML.Tag.H2);
	}
	/**
	 * Allows underlined text to be enabled or disabled. When disabled,
	 * the underlined text button disappears from the toolbar completely,
	 * and the shortcut key does not function.
	 *
	 * The editor can always read, display, and write underlined text
	 * correctly, but setting this false prevents editing.
	 *
	 * @param enabled True to enable underlined text, false to disable it.
	 */
	public void setUnderlineEnabled(boolean enabled)
	{
		ub.setVisible(enabled);
		if (enabled)
			map.addActionForKeyStroke(underlineStroke,underlineAction);
		else
			map.removeKeyStrokeBinding(underlineStroke);
		charStyleSep.setVisible(
			isBoldEnabled() ||
			isItalicEnabled() ||
			isUnderlineEnabled() ||
			isHighlightEnabled() ||
			isCodeEnabled());
		//showHideToolbarLabels();
	}

	/**
	 * Determines if Editize is set to import HTML from the system clipboard.
	 * @return True if HTML Clipboard importing is enabled, false if not.
	 */
	public boolean isHtmlClipboardEnabled()
	{
		return textPane.isHtmlClipboardEnabled();
	}

	/**
	 * Sets whether Editize should import HTML from the system clipboard.
	 * Clipboard operations within Editize and between Java applications are
	 * unaffected by this setting.
	 *
	 * @param htmlClipboardEnabled Set true to enable importing HTML, false t\
	 *  disable it.
	 */
	public void setHtmlClipboardEnabled(boolean htmlClipboardEnabled)
	{
		textPane.setHtmlClipboardEnabled(htmlClipboardEnabled);
	}

	/**
	 * Updates the visibility of the "Paragraph" and "Character" labels
	 * depending on whether two types of formatting features are enabled
	 * or not.
	 */
	/*protected void showHideToolbarLabels()
	{
		boolean showLabels =
		(
			isParagraphStylesEnabled() ||
			isParagraphAlignmentEnabled() ||
			isBulletListEnabled() ||
			isNumberedListEnabled()
		) && (
			isBoldEnabled() ||
			isItalicEnabled() ||
			isUnderlineEnabled() ||
			isHighlightEnabled() ||
			isCodeEnabled() ||
			isHyperlinksEnabled() ||
			isImagesEnabled()
		);

		pLabel.setVisible(showLabels);
		cLabel.setVisible(showLabels);
	}*/

	/**
	 * Converts a type Color to a hex string in the format "#RRGGBB".
	 * Stolen from javax.swing.text.html.CSS.
     * @param color the Color to convert
     * @return a hex string in the format #RRGGBB
	 */
	static String colorToHex(Color color) {

		String colorstr = "#";

		// Red
		String str = Integer.toHexString(color.getRed());
		if (str.length() > 2)
			colorstr += str.substring(0, 2);
		else if (str.length() < 2)
            colorstr += "0" + str;
		else
            colorstr += str;

		// Green
		str = Integer.toHexString(color.getGreen());
		if (str.length() > 2)
			colorstr += str.substring(0, 2);
		else if (str.length() < 2)
            colorstr += "0" + str;
		else
            colorstr += str;

		// Blue
		str = Integer.toHexString(color.getBlue());
		if (str.length() > 2)
			colorstr = str.substring(0, 2);
		else if (str.length() < 2)
            colorstr += "0" + str;
		else
            colorstr += str;

		return colorstr;
	}

	public String getHtml(boolean bodyOnly) throws IOException
	{
		StringWriter writer = new StringWriter();
		getTextPane().write(writer);
		writer.flush();
		StringBuffer buf = writer.getBuffer();
		String html = buf.toString();

		// Output only the contents of the <body> tag
		if (bodyOnly)
		{
				int pos, start;

			// Find start of <body> tag
			pos = html.indexOf("<body");
			if (pos < 0) return "";

			// Find end of <body> tag
			pos = html.indexOf('>',pos);
			if (html.charAt(pos+1) == '\n') pos++;
			start = pos+1;

			// Find start of </body> tag
			pos = html.indexOf("</body>",pos);

			// Take substring
			html = html.substring(start,pos);
		}

		return html;
	}

	public void addNotify() {
		installEditMenu();
		super.addNotify();
	}

	public void removeNotify() {
		super.removeNotify();
		uninstallEditMenu();
	}
}
