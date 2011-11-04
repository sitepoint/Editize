package com.editize.editorkit;

import com.editize.EditizeDocument;
import com.editize.EditizeStyleSheet;
import com.editize.ParserDelegator;
import com.editize.SelfClosingFilterReader;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.lang.reflect.Method;


/**
 * Custom EditorKit to support editing of SitePoint articles. This class
 * extends the functionality of StyledEditorKit to encompass some of the
 * more advanced text styles (e.g. hyperlinks) and paragraph styles
 * (e.g. bullet lists) that are required by such articles.
 * Creation date: (09/07/2001 7:07:56 PM)
 * @author: Kevin Yank
 */
public class EditizeEditorKit extends HTMLEditorKit {
	NotifyingUndoManager undoMan = new NotifyingUndoManager();
	private UndoAction undoAction = new UndoAction(UndoAction.UNDO,undoMan);
	private UndoAction redoAction = new UndoAction(UndoAction.REDO,undoMan);
	private ImageAction imageAction = new ImageAction();
	private TableAction tableAction = new TableAction();

	private Action[] defaultActions = {
		undoAction,
		redoAction,
		new com.editize.editorkit.DeletePrevCharAction(),
		new com.editize.editorkit.DeleteNextCharAction(),
		new com.editize.editorkit.DeleteNextWordAction(),
		new com.editize.editorkit.DeletePreviousWordAction(),
		new com.editize.editorkit.InsertBreakAction(),
		new com.editize.editorkit.InsertLineBreakAction(),
		new BSASimpleListAction(HTML.Tag.UL,"set-bulleted"),
		new BSASimpleListAction(HTML.Tag.OL,"set-numbered"),
		new BSAHighlightAction(),
		new BSABlockHighlightAction(),
		new HyperlinkAction(),
		new BSACharTagAction(HTML.Tag.STRONG,"font-bold"),
		new BSACharTagAction(HTML.Tag.EM,"font-italic"),
		new BSACharTagAction(HTML.Tag.U,"font-underline"),
		new BSACharTagAction(HTML.Tag.CODE,"font-inlinecode"),
		new BSAAlignmentAction("left-justify", StyleConstants.ALIGN_LEFT),
		new BSAAlignmentAction("center-justify", StyleConstants.ALIGN_CENTER),
		new BSAAlignmentAction("right-justify", StyleConstants.ALIGN_RIGHT),
		new BSAAlignmentAction("full-justify", StyleConstants.ALIGN_JUSTIFIED),
		imageAction,
		tableAction,
		new com.editize.editorkit.InsertTabAction(),
		new InsertTableRowAction(),
		new InsertTableColumnAction(),
		new DeleteTableColumnAction(),
		new DeleteTableRowAction(),
		new DeleteTableAction(),
		new MergeTableCellRightAction(),
		new BeginAction(beginAction,false),
		new BeginAction(selectionBeginAction,true),
		new BeginLineAction(beginLineAction,false),
		new BeginLineAction(selectionBeginLineAction,true),
		new SelectAllAction(),
		new IndentAction(),
		new OutdentAction(),
		new EditTableAction(),
		new EditCellAction()
	};

        public EditizeEditorKit()
        {
          super();
          setDefaultCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }

	public Document createDefaultDocument() {

		StyleSheet styles = getStyleSheet();
		StyleSheet ss = new EditizeStyleSheet();

		ss.addStyleSheet(styles);

		EditizeDocument doc = new EditizeDocument(ss);
		doc.setParser(getParser());
		doc.setAsynchronousLoadPriority(4);
		doc.setTokenThreshold(100);
		doc.setPreservesUnknownTags(false);

		// Reset undo list after setting up initial styles
		doc.addUndoableEditListener(undoMan);

		final String initHtml = "<html><head></head><body><p></p></body>";
		try
		{
			read(new StringReader(initHtml),doc,0);
		}
		catch (IOException ex)
		{
			throw new RuntimeException("IOError initializing document.");
		}
		catch (BadLocationException ex)
		{
			throw new RuntimeException("BadLocationException initializing document.");
		}

		clearUndoHistory();

		return doc;
	}

	public void setStyleSheet(StyleSheet ss)
	{
		defaultStyles = ss;
	}

	public StyleSheet getStyleSheet()
	{
		if (defaultStyles == null) {
			defaultStyles = new EditizeStyleSheet();
			defaultStyles.addStyleSheet(super.getStyleSheet());

			// Set default CSS properties
			defaultStyles.addRule("body { font-family:\"Times New Roman\"; font-size: 16pt; background: white; }");
			defaultStyles.addRule("p { font-family:\"Times New Roman\"; font-size: 16pt; margin-top: 11pt; margin-bottom: 7pt; }");
			defaultStyles.addRule("h1 { font-weight:bold; font-size: 32pt; margin-top: 10pt; margin-bottom: 6pt; }");
			defaultStyles.addRule("h2 { font-weight:bold; font-size: 24pt; margin-top: 10pt; margin-bottom: 6pt; }");
			defaultStyles.addRule("pre { font-family: monospace }");
			defaultStyles.addRule("blockquote { margin-top: 0; margin-bottom: 0; }");
			defaultStyles.addRule("span.highlighted { color: #ff0000; }");
			defaultStyles.addRule("div.highlighted { background-color: #ffff00; }");

			// Classes for XHTML Strict compliant left/right alignment
			defaultStyles.addRule(".leftalign { text-align: left; }");
			defaultStyles.addRule(".centeralign { text-align: center; }");
			defaultStyles.addRule(".rightalign { text-align: right; }");
			defaultStyles.addRule(".imgleft { float: left; }");
			defaultStyles.addRule(".imgright { float: right; }");
		}
		return defaultStyles;
	}

	 private static StyleSheet defaultStyles;
        /**
         * Installs our new, improved input attribute tracking system
         * @param c The JEditorPane in which to install this kit
         */
        public void install(JEditorPane c)
        {
          c.addCaretListener(inputAttributeUpdater);
          c.addPropertyChangeListener(inputAttributeUpdater);
          Caret caret = c.getCaret();
          if (caret != null)
          {
            inputAttributeUpdater.updateInputAttributes
                (caret.getDot(), caret.getMark(), c);
          }
        }

	/**
	 * Insert the method's description here.
	 * Creation date: (10/07/2001 12:45:42 PM)
	 * @return javax.swing.Action[]
	 */
	public Action[] getActions() {
		return TextAction.augmentList(super.getActions(),defaultActions);
	}

	/**
	* Fetch a factory that is suitable for producing
	* views of any models that are produced by this
	* kit.
	*
	* @return the factory
	*/
	public ViewFactory getViewFactory() {
		return defaultFactory;
	}

	/** Shared factory for creating HTML Views. */
	private static final ViewFactory defaultFactory = new EditizeFactory();

	public static class EditizeFactory extends HTMLEditorKit.HTMLFactory
	{
		/**
		 * Creates a view from an element.
		 *
		 * @param elem the element
		 * @return the view
		 */
		public View create(Element elem)
		{
			Object o = getElementName(elem);
			if (o instanceof HTML.Tag)
			{
				HTML.Tag kind = (HTML.Tag) o;
				if (kind == HTML.Tag.IMPLIED) {
				        String ws = (String) elem.getAttributes().getAttribute(CSS.Attribute.WHITE_SPACE);
				        if ((ws == null) || !ws.equals("pre"))
					{
					        return new com.editize.editorkit.ParagraphView(elem);
					}
				}
				else if ((kind == HTML.Tag.P) ||
					(kind == HTML.Tag.H1) ||
					(kind == HTML.Tag.H2) ||
					(kind == HTML.Tag.H3) ||
					(kind == HTML.Tag.H4) ||
					(kind == HTML.Tag.H5) ||
					(kind == HTML.Tag.H6) ||
					(kind == HTML.Tag.DT))
				{
					// paragraph
					return new com.editize.editorkit.ParagraphView(elem);
				}
				else if (kind==HTML.Tag.IMG)
				{
					return new ImageView(elem);
				}
                                else if (kind==HTML.Tag.BODY)
                                {
                                  return new BodyView(elem,View.Y_AXIS);
                                }
			}
			return super.create(elem);
		}
	}

	/**
	 * Reads a simple HTML stream into a Document.
	 *
	 * @param in  The stream to read from
	 * @param doc The destination for the insertion.
	 * @param pos The location in the document to place the
	 *   content >= 0.
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid
	 *   location within the document.
	 */
	public void read(Reader in, Document doc, int pos)
		throws IOException, BadLocationException
	{
          // Filter out self-closing tags, which Java's HTML parser cannot
          // handle.
          in = new SelfClosingFilterReader(in);

		undoMan.start();

		if (doc instanceof EditizeDocument)
		{
			EditizeDocument adoc = (EditizeDocument)doc;

			//adoc.allowMultiSpaces = true;

			HTMLEditorKit.Parser p = getParser();
			if (p == null) throw new IOException("Can't load parser");
			if (pos > doc.getLength()) throw new BadLocationException("Invalid location", pos);

			HTMLEditorKit.ParserCallback receiver = adoc.getReader(pos);
			//Boolean ignoreCharset = (Boolean)doc.getProperty("IgnoreCharsetDirective");
			p.parse(in, receiver, true);//(ignoreCharset == null) ? false : ignoreCharset.booleanValue());
			receiver.flush();

			adoc.removeRedundantSpans();

			//adoc.allowMultiSpaces = false;
		}
		else
		{
			super.read(in, doc, pos);
		}

		undoMan.end();
	}

        private Parser defaultParser;

        /**
         * @param doc The HTMLDocument to insert into
         * @param offset The character position at which to insert
         * @param html The HTML fragment to insert
         * @param popDepth the number of ElementSpec.EndTagTypes to generate before inserting
         * @param pushDepth the number of ElementSpec.StartTagTypes with a direction of ElementSpec.JoinNextDirection that should be generated before inserting, but after the end tags have been generated
         * @param insertTag the first tag to start inserting into document
         * @throws BadLocationException
         * @throws IOException
         */
        public void insertHTML(HTMLDocument doc, int offset, String html,
                               int popDepth, int pushDepth, HTML.Tag insertTag)
            throws BadLocationException, IOException
        {
          // Filter out self-closing HTML tags, which Java's parser can't handle
          BufferedReader htmlReader = new BufferedReader(new SelfClosingFilterReader(new StringReader(html)));
          StringBuffer buf = new StringBuffer();
          String line = htmlReader.readLine();
          while (line != null)
          {
            buf.append(line).append("\n");
            line = htmlReader.readLine();
          }
          html = buf.toString();

          undoMan.start();
          super.insertHTML(doc, offset, html, popDepth, pushDepth, insertTag);
          if (doc instanceof EditizeDocument) ((EditizeDocument)doc).removeRedundantSpans();

          undoMan.end();
        }

	/**
	 * Modify Java's default parser to correctly handle SPAN tags.
	 * @return An HTML parser
	 */
	protected Parser getParser() {
		if (defaultParser == null) {
			defaultParser = new ParserDelegator();
		}
		return defaultParser;
	}

	/**
	 * Inserts inline HTML into the middle of the document. The HTML
	 * code provided should not contain any block elements (undefined
	 * results will occur).
	 *
	 * @param doc The document to insert into
	 * @param html The string of inline HTML to insert.
	 * @param offs The offset at which to insert the HTML.
	 */
	public void insertInlineHtml(EditizeDocument doc, String html, int offs)
	{
		try
		{
			Element run = doc.getCharacterElement(offs);

			undoMan.start();

			// Insert plain text first
			int htmlStart = html.indexOf('<');
			if (htmlStart < 0)
			{
				// We can trust the default parser with non-tag HTML
				Element newRun = doc.splitLeaf(run,offs);
				doc.insertBeforeStart(newRun,html);
			}
			else
			{
				// First insert our tagged HTML
				insertIntoTag(doc,html.substring(htmlStart),offs,
							  (HTML.Tag)run.getParentElement().getAttributes().getAttribute(StyleConstants.NameAttribute),
							  getFirstTagInHTMLString(html));

				// We can trust the default parser with our non-tag HTML
				if (htmlStart > 0) doc.insertBeforeStart(doc.getCharacterElement(offs),html.substring(0,htmlStart));
			}

			undoMan.end();
		}
		catch (BadLocationException ex)
		{
			throw new RuntimeException(ex.toString());
		}
		catch (java.io.IOException ex)
		{
			throw new RuntimeException(ex.toString());
		}
	}

	/**
	 * Inserts block HTML into the middle of the document. The HTML
	 * code provided should contain complete block elements (undefined
	 * results will occur otherwise).
	 *
	 * @param doc The document to insert into
	 * @param html The string of block HTML to insert.
	 * @param offs The offset at which to insert the HTML.
	 */
	public void insertBlockHtml(EditizeDocument doc, String html, int offs)
	{
		HTML.Tag addTag, parentTag;

		addTag = getFirstTagInHTMLString(html);

		// Get the parent tag in which to insert in the document
		parentTag = HTML.Tag.BODY;
		int parentHeight = doc.heightToElementWithName(HTML.Tag.BODY,offs);
		int height = doc.heightToElementWithName(HTML.Tag.TD,offs);
		if (height > 0 && height < parentHeight)
		{
			parentTag = HTML.Tag.TD;
			parentHeight = height;
		}
		height = doc.heightToElementWithName(HTML.Tag.TH,offs);
		if (height > 0 && height < parentHeight)
		{
			parentTag = HTML.Tag.TH;
			//parentHeight = height;
		}

		try
		{
			insertIntoTag(doc, html, offs, parentTag, addTag);
		}
		catch (BadLocationException ex)
		{
			throw new RuntimeException(ex.toString());
		}
		catch (java.io.IOException ex)
		{
			throw new RuntimeException(ex.toString());
		}
	}

	protected HTML.Tag getFirstTagInHTMLString(String html)
	{
		int len = html.length();
		boolean foundTagStart = false;
		boolean foundTagNameStart = false;
		StringBuffer firstTagName = new StringBuffer(Math.max(0,html.indexOf('>')));

		// Get the name of the first HTML tag in the string
		for (int pos=0; pos < len; pos++)
		{
			if (foundTagStart)
			{
				char theChar = html.charAt(pos);
				if (!foundTagNameStart)
				{
					if (theChar >= 'A' && theChar <= 'Z' ||
						theChar >= 'a' && theChar <= 'z')
					{
						firstTagName.append(theChar);
						foundTagNameStart = true;
					}
				}
				else if (theChar >= 'A' && theChar <= 'Z' ||
						 theChar >= 'a' && theChar <= 'z')
				{
					firstTagName.append(theChar);
				}
				else break;
			}
			else
			{
				if (html.charAt(pos) == '<')
				{
					foundTagStart = true;
				}
			}
		}
		return firstTagName.length() > 0 ? HTML.getTag(firstTagName.toString().toLowerCase()) : null;// HTML.Tag.CONTENT;
	}

	/**
	 * This is invoked when inserting at a boundary. It determines
	 * the number of pops, and then the number of pushes that need
	 * to be performed, and then invokes insertHTML.
     * @param doc The HTML document to insert into
     * @param offset The character offset at which to insert
     * @param insertElement The element to insert into (may be null)
     * @param html The HTML fragment to insert
     * @param addTag The tag to insert
     * @throws java.io.IOException -
     * @throws javax.swing.text.BadLocationException -
     */
	protected void insertAtBoundary(HTMLDocument doc, int offset, Element insertElement,
					   String html, HTML.Tag addTag)
			throws BadLocationException, IOException
	{
		// Find the common parent.
		Element e;
		Element commonParent;
		boolean isFirst = (offset == 0);

		if (offset > 0 || insertElement == null) {
		e = doc.getDefaultRootElement();
		while (e != null && e.getStartOffset() != offset &&
			   !e.isLeaf()) {
			e = e.getElement(e.getElementIndex(offset));
		}
		commonParent = (e != null) ? e.getParentElement() : null;
		}
		else {
		// If inserting at the origin, the common parent is the
		// insertElement.
		commonParent = insertElement;
		}
		if (commonParent != null) {
		// Determine how many pops to do.
		int pops = 0;
		int pushes = 0;
		if (isFirst && insertElement != null) {
			e = commonParent;
			while (e != null && !e.isLeaf()) {
			e = e.getElement(e.getElementIndex(offset));
			pops++;
			}
		}
		else {
			e = commonParent;
			offset--;
			while (e != null && !e.isLeaf()) {
			e = e.getElement(e.getElementIndex(offset));
			pops++;
			}

			// And how many pushes
			e = commonParent;
			offset++;
			while (e != null && e != insertElement) {
			e = e.getElement(e.getElementIndex(offset));
			pushes++;
			}
		}
		pops = Math.max(0, pops - 1);

		// And insert!
		insertHTML(doc, offset, html, pops, pushes, addTag);
		}
	}

	/**
	 * If there is an Element with name <code>tag</code> at
	 * <code>offset</code>, this will invoke either insertAtBoundary
	 * or <code>insertHTML</code>.
     * @param doc The EditizeDocument to insert into
     * @param html The HTML fragment to insert into
     * @param offset The character position to insert at
     * @param tag The tag to insert into
     * @param addTag The tag to insert
     * @return true if there is a match, and one of the inserts is invoked
     * @throws BadLocationException -
     * @throws IOException -
	 */
	protected boolean insertIntoTag(EditizeDocument doc, String html, int offset, HTML.Tag tag, HTML.Tag addTag)
			throws BadLocationException, IOException
	{
		Element e = doc.findElementMatchingTag(offset, tag);
		if (e != null && e.getStartOffset() == offset)
		{
			insertAtBoundary(doc, offset, e, html, addTag);
			return true;
		}
		else if (offset > 0)
		{
			int depth = doc.heightToElementWithName(tag,offset - 1);
			if (depth != -1)
			{
				insertHTML(doc, offset, html, depth, 0, addTag);
				return true;
			}
		}
		return false;
	}

        /**
         * Strips out rendundant spans from input attributes. Also prevents
         * links from absorbing newly typed characters at their ends.
         *
         * @param element The element to strip
         * @param set The element's attribute set
         * @param endOfLink If true, indicates that we're at the end of a
         *                 hyperlink.
         */
        protected void createInputAttributes(Element element,
                                             MutableAttributeSet set,
                                             boolean endOfLink)
        {
          super.createInputAttributes(element, set);

          if (getElementName(element) == HTML.Tag.CONTENT &&
              set.isDefined(HTML.Tag.SPAN))
          {
            AttributeSet a = (AttributeSet)set.getAttribute(HTML.Tag.SPAN);
            if (! (a.isDefined(HTML.Attribute.CLASS) ||
                   a.isDefined(HTML.Attribute.STYLE)))
            {
              set.removeAttribute(HTML.Tag.SPAN);
            }
          }

          if (endOfLink && getElementName(element) == HTML.Tag.CONTENT &&
              set.isDefined(HTML.Tag.A))
          {
            set.removeAttribute(HTML.Tag.A);
          }
        }

	/**
	 * Clears the Undo history for the document this kit is responsible for.
	 */
	public void clearUndoHistory()
	{
		undoMan.discardAllEdits();
	}

	/**
	 * Sets whether this kit should monitor undo events in the document or not.
	 * When the UndoManager is deactivated, undo history is lost.
     * @param active true to activate the undo manager
	 */
	public void setUndoManagerActive(boolean active)
	{
		undoMan.setActive(active);
	}

	/**
	 * Determines whether or not this kit is monitoring undo events in the document.
	 * @return True if undoable edits are being monitored.
	 */
	public boolean isUndoManagerActive()
	{
		return undoMan.isActive();
	}


	/**
	 * Determines if a given document position is contained within a table cell.
	 * If it is, the Element for that cell is returned.
	 * @param pos The position to check for a containing table cell.
     * @param doc The document
	 * @return The containing table cell, or null if none exists.
	 */
	public static Element getContainingTableCell(int pos, Document doc)
	{
		if (doc instanceof HTMLDocument)
		{
			HTMLDocument hdoc = (HTMLDocument)doc;

			Element e = hdoc.getParagraphElement(pos);
			Element root = hdoc.getDefaultRootElement();
			Object name;
			do
			{
				name = e.getAttributes().getAttribute(
						StyleConstants.NameAttribute);
				if (name == HTML.Tag.TD || name == HTML.Tag.TH)
				{
					return e;
				}
				e = e.getParentElement();
			} while (e != null && e != root);
		}
		return null;
	}

	public static class NotifyingUndoManager extends CompoundingUndoManager {
		private PropertyChangeSupport pcs;
		private boolean active = true;
	/**
	 * NotifyingUndoManager constructor comment.
	 */
	public NotifyingUndoManager() {
		super();
		pcs = new PropertyChangeSupport(this);
	}
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (28/08/2001 11:43:19 PM)
	 */
	public void discardAllEdits() {
		boolean oldCanUndo = canUndo();
		boolean oldCanRedo = canRedo();
		String oldUndoPresentationName = getUndoPresentationName();
		String oldRedoPresentationName = getRedoPresentationName();
		super.discardAllEdits();
		pcs.firePropertyChange("canUndo",oldCanUndo,canUndo());
		pcs.firePropertyChange("canRedo",oldCanRedo,canRedo());
		pcs.firePropertyChange("undoPresentationName",oldUndoPresentationName,getUndoPresentationName());
		pcs.firePropertyChange("redoPresentationName",oldRedoPresentationName,getRedoPresentationName());
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (28/08/2001 11:43:19 PM)
	 */
	public void redo() throws CannotUndoException {
		if (!active) return;
		boolean oldCanUndo = canUndo();
		boolean oldCanRedo = canRedo();
		String oldUndoPresentationName = getUndoPresentationName();
		String oldRedoPresentationName = getRedoPresentationName();
		super.redo();
		pcs.firePropertyChange("canUndo",oldCanUndo,canUndo());
		pcs.firePropertyChange("canRedo",oldCanRedo,canRedo());
		pcs.firePropertyChange("undoPresentationName",oldUndoPresentationName,getUndoPresentationName());
		pcs.firePropertyChange("redoPresentationName",oldRedoPresentationName,getRedoPresentationName());
	}
	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}
	public void undo() throws CannotUndoException {
		if (!active) return;
		boolean oldCanUndo = canUndo();
		boolean oldCanRedo = canRedo();
		String oldUndoPresentationName = getUndoPresentationName();
		String oldRedoPresentationName = getRedoPresentationName();
		super.undo();
		pcs.firePropertyChange("canUndo",oldCanUndo,canUndo());
		pcs.firePropertyChange("canRedo",oldCanRedo,canRedo());
		pcs.firePropertyChange("undoPresentationName",oldUndoPresentationName,getUndoPresentationName());
		pcs.firePropertyChange("redoPresentationName",oldRedoPresentationName,getRedoPresentationName());
	}

	public boolean addEdit(UndoableEdit e) {
		if (!active) return false;
		boolean ret;
		boolean oldCanUndo = canUndo();
		boolean oldCanRedo = canRedo();
		String oldUndoPresentationName = getUndoPresentationName();
		String oldRedoPresentationName = getRedoPresentationName();
		ret = super.addEdit(e);
		pcs.firePropertyChange("canUndo",oldCanUndo,canUndo());
		pcs.firePropertyChange("canRedo",oldCanRedo,canRedo());
		pcs.firePropertyChange("undoPresentationName",oldUndoPresentationName,getUndoPresentationName());
		pcs.firePropertyChange("redoPresentationName",oldRedoPresentationName,getRedoPresentationName());
		return ret;
	}

	public void setActive(boolean active)
	{
		this.active = active;
		if (!active) discardAllEdits();
	}
	public boolean isActive()
	{
		return active;
	}
	}

	/**
	 * An UndoManager that is capable of creating CompoundEdits on the fly.
	 * <p>
	 * To begin collecting edits, call <tt>start()</tt>. From that point, any
	 * calls to addEdit put those edits into a CompoundEdit. Call <tt>end()</tt>
	 * to close off the CompoundEdit and add it to the undo history as a single
	 * UndoableEdit. Calls to <tt>undo()</tt> and <tt>redo()</tt> also have the
	 * effect of automatically calling <tt>end()</tt>.
	 *
	 * @author Kevin Yank
	 */
	public static class CompoundingUndoManager extends UndoManager
	{
		int collecting = 0;
		CompoundEdit ce;

		/**
		 * Begins collecting edits into a <tt>CompoundEdit</tt>.
		 */
		public void start()
		{
			if (collecting++ == 0) ce = null;
		}

		/**
		 * Closes off a <tt>CompoundEdit</tt>.
		 */
		public void end()
		{
			if (collecting > 0) collecting--;
			if (collecting == 0)
			{
				if (ce != null)
				{
					ce.end();
					addEdit(ce);
				}
				ce = null;
			}
		}

		/**
		 * @return True if collecting edits into a single CompoundEdit.
		 */
		public boolean isCollecting()
		{
			return collecting > 0;
		}

		public void undo()
		{
			collecting = 0;
			end();
			super.undo();
		}

		/**
		 * Adds an edit to the undo history.
		 * @param anEdit the edit
		 * @return True if successful.
		 */
		public boolean addEdit(UndoableEdit anEdit)
		{
			if (collecting > 0)
			{
				if (ce == null) ce = new CompoundEdit();
				ce.addEdit(anEdit);
				return true;
			}
			else return super.addEdit(anEdit);
		}
		/**
		* Overridden to preserve usual semantics: returns true if an undo
		* operation would be successful now, false otherwise
		*/
		public synchronized boolean canUndo() {
			if (isInProgress()) {
				UndoableEdit edit = editToBeUndone();
				return edit != null && edit.canUndo();
			} else {
				return super.canUndo();
			}
		}
	}

	/**
	 * Gets a HTMLBlockTypeAction for this EditorKit.
	 * @param tag the HTML block tag for this action to insert
     * @return a new HTMLBlockTypeAction for the requested tag
	 */
	public static HTMLBlockTypeAction getHTMLBlockTypeAction(HTML.Tag tag)
	{
		return new HTMLBlockTypeAction(tag);
	}

	public static HTMLBlockTypeAction getHTMLBlockTypeAction(HTML.Tag tag, String displayName)
	{
		return new HTMLBlockTypeAction(tag,displayName);
	}


	public void setImageListUrl(String listUrl)
	{
		imageAction.listUrl = listUrl;
	}



	/**
	 * Fetch a resource relative to the EditizeEditorKit classfile.
	 * If this is called on 1.2 the loading will occur under the
	 * protection of a doPrivileged call to allow the EditizeEditorKit
	 * to function when used in an applet.
	 *
	 * @param name the name of the resource, relative to the
	 *  EditizeEditorKit class
	 * @return a stream representing the resource
	 */
	public static InputStream getResourceAsStream(String name)
	{
		try
		{
			Class klass;
			ClassLoader loader = HTMLEditorKit.class.getClassLoader();
			if (loader != null) {
			        klass = loader.loadClass("javax.swing.text.html.ResourceLoader");
			}
			else
			{
			        klass = Class.forName("javax.swing.text.html.ResourceLoader");
			}
			Class[] parameterTypes = { String.class };
			Method loadMethod = klass.getMethod("getResourceAsStream", parameterTypes);
			String[] args = { name };
			return (InputStream) loadMethod.invoke(null, args);
		} catch (Throwable e) {
			// If the class doesn't exist or we have some other
			// problem we just try to call getResourceAsStream directly.
			return EditizeEditorKit.class.getResourceAsStream(name);
		}
	}

	/**
	 * Write content from a document to the given stream
	 * in a format appropriate for this kind of content handler.
	 *
	 * @param out  the stream to write to
	 * @param doc  the source for the write
	 * @param pos  the location in the document to fetch the
	 *   content
	 * @param len  the amount to write out
	 * @param delimit if true, writes start/end delimiters in document
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid
	 *   location within the document
	 */
	public void write(Writer out, Document doc, int pos, int len, boolean delimit)
	        throws IOException, BadLocationException
	{
		if (doc instanceof HTMLDocument)
		{
			com.editize.DelimitedHTMLWriter w = new com.editize.DelimitedHTMLWriter(out, (HTMLDocument)doc, pos, len, delimit);
			w.write();
		}
		else super.write(out, doc, pos, len);
	}

	public void write(Writer out, Document doc, int pos, int len)
	        throws IOException, BadLocationException
	{
          write(out, doc, pos, len, false);
	}

	/**
	 * Select the word around the caret
	 * Reimplemented for the use of DefaultCaret.
	 */
	public static class SelectWordAction extends TextAction {

		/**
		 * Create this action with the appropriate identifier.
		 */
		public SelectWordAction() {
			super(selectWordAction);
			start = new BeginWordAction("pigdog", false);
			end = new EndWordAction("pigdog", true);
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			start.actionPerformed(e);
			end.actionPerformed(e);
		}

		private Action start;
		private Action end;
	}

	/*
	 * Select the entire document
	 * @see DefaultEditorKit#endAction
	 * @see DefaultEditorKit#getActions
	 */
	static class SelectAllAction extends TextAction {

		/**
		 * Create this action with the appropriate identifier.
		 */
		SelectAllAction() {
			super(selectAllAction);
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			if (target != null) {
				Document doc = target.getDocument();
				target.setCaretPosition(1);
				target.moveCaretPosition(doc.getLength());
			}
		}

	}

	/**
	 * Select the line around the caret
	 * Reimplemented for the use of DefaultCaret.
	 */
	public static class SelectLineAction extends TextAction {

		/**
		 * Create this action with the appropriate identifier.
		 */
		public SelectLineAction() {
			super(selectLineAction);
			start = new BeginLineAction("pigdog", false);
			end = new EndLineAction("pigdog", true);
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			start.actionPerformed(e);
			end.actionPerformed(e);
		}

		private Action start;
		private Action end;
	}

	/*
	 * Move the caret to the begining of the document.
	 * Reimplemented to set position to '1' instead of '0'.
	 */
	static class BeginAction extends TextAction {

		/* Create this object with the appropriate identifier. */
		BeginAction(String nm, boolean select) {
			super(nm);
			this.select = select;
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			if (target != null) {
				if (select) {
					target.moveCaretPosition(1);
				} else {
					target.setCaretPosition(1);
				}
			}
		}

		private boolean select;
	}

	/**
	 * Position the caret to the beginning of the line.
	 * Reimplemented for the use of DefaultCaret, and to
	 * call our version of getRowStart.
	 */
	static class BeginLineAction extends TextAction {

		/**
		 * Create this action with the appropriate identifier.
		 * @param nm  the name of the action, Action.NAME.
		 * @param select whether to extend the selection when
		 *  changing the caret position.
		 */
		BeginLineAction(String nm, boolean select) {
			super(nm);
			this.select = select;
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			if (target != null) {
				try {
					int offs = target.getCaretPosition();
					int begOffs = getRowStart(target, offs);
					if (select) {
						target.moveCaretPosition(begOffs);
					} else {
						target.setCaretPosition(begOffs);
					}
				} catch (BadLocationException bl) {
					target.getToolkit().beep();
				}
			}
		}

		private boolean select;
	}

	/**
	 * Reimplementation of Utilities.getRowStart() that will stop looking
	 * at position 1 instead of position 0, to allow for HTMLDocument's
	 * habit of using positon 0 for the document title.
	 *
	 * @param c the JTextComponent
	 * @param offs the character offset to search from
	 * @return the start of the row
	 * @throws BadLocationException If the offset specified is outside the document
	 */
	public static int getRowStart(JTextComponent c, int offs) throws BadLocationException {
		Rectangle r = c.modelToView(offs);
		if (r == null) {
			return -1;
		}
		int lastOffs = offs;
		int y = r.y;
		while ((r != null) && (y == r.y)) {
			offs = lastOffs;
			lastOffs -= 1;
			r = (lastOffs > 0) ? c.modelToView(lastOffs) : null;
		}
		return offs;
    }

	/**
	 * Position the caret to the end of the line.
	 * Reimplemented for the use of DefaultCaret.
	 */
	static class EndLineAction extends TextAction {

		/**
		 * Create this action with the appropriate identifier.
		 * @param nm  the name of the action, Action.NAME.
		 * @param select whether to extend the selection when
		 *  changing the caret position.
		 */
		EndLineAction(String nm, boolean select) {
			super(nm);
			this.select = select;
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			if (target != null) {
				try {
					int offs = target.getCaretPosition();
					int endOffs = Utilities.getRowEnd(target, offs);
					if (select) {
						target.moveCaretPosition(endOffs);
					} else {
						target.setCaretPosition(endOffs);
					}
				} catch (BadLocationException bl) {
					target.getToolkit().beep();
				}
			}
		}

		private boolean select;
    }

	/**
	 * Position the caret to the beginning of the word.
	 * Reimplemented for the use of DefaultCaret.
	 */
	static class BeginWordAction extends TextAction {

		/**
		 * Create this action with the appropriate identifier.
		 * @param nm  the name of the action, Action.NAME.
		 * @param select whether to extend the selection when
		 *  changing the caret position.
		 */
		BeginWordAction(String nm, boolean select) {
			super(nm);
			this.select = select;
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			if (target != null) {
				try {
					int offs = target.getCaretPosition();
					int begOffs = Utilities.getWordStart(target, offs);
					if (select) {
						target.moveCaretPosition(begOffs);
					} else {
						target.setCaretPosition(begOffs);
					}
				} catch (BadLocationException bl) {
					target.getToolkit().beep();
				}
			}
		}

		private boolean select;
	}

	/**
	 * Position the caret to the end of the word.
	 * Reimplemented for the use of DefaultCaret.
	 */
	static class EndWordAction extends TextAction {

		/**
		 * Create this action with the appropriate identifier.
		 * @param nm  the name of the action, Action.NAME.
		 * @param select whether to extend the selection when
		 *  changing the caret position.
		 */
		EndWordAction(String nm, boolean select) {
			super(nm);
			this.select = select;
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			if (target != null) {
				try {
					int offs = target.getCaretPosition();
					int endOffs = Utilities.getWordEnd(target, offs);
					if (select) {
						target.moveCaretPosition(endOffs);
					} else {
						target.setCaretPosition(endOffs);
					}
				} catch (BadLocationException bl) {
					target.getToolkit().beep();
				}
			}
		}

		private boolean select;
    }

    /**
     * Utility function for getting the name of an element.
     * @param e The element.
     * @return The element's name.
     */
    public static Object getElementName(Element e)
    {
      return e.getAttributes().getAttribute(StyleConstants.NameAttribute);
    }

    Element currentRun;
    Element currentParagraph;

    /**
     * This is the set of attributes used to store the
     * input attributes.
     */
    MutableAttributeSet inputAttributes = new SimpleAttributeSet() {
        public AttributeSet getResolveParent() {
            return (currentParagraph != null) ? currentParagraph.getAttributes() : null;
        }

        public Object clone() {
            return new SimpleAttributeSet(this);
        }
    };

    boolean endOfLink = false;

    /**
     * This listener will be attached to the caret of
     * the text component that the EditorKit gets installed
     * into.  This should keep the input attributes updated
     * for use by the styled actions.
     */
    private AttributeTracker inputAttributeUpdater = new AttributeTracker();

    /**
     * Tracks caret movement and keeps the input attributes set
     * to reflect the current set of attribute definitions at the
     * caret position.
     * <p>This implements PropertyChangeListener to update the
     * input attributes when the Document changes, as if the Document
     * changes the attributes will almost certainly change.
     */
    class AttributeTracker implements CaretListener, PropertyChangeListener, Serializable {

        /**
         * Updates the attributes. <code>dot</code> and <code>mark</code>
         * mark give the positions of the selection in <code>c</code>.
         * @param dot The dot of the current text selection
         * @param mark The mark of the current text selection
         * @param c The component to get the input attributes from
         */
        void updateInputAttributes(int dot, int mark, JTextComponent c) {
            // EditorKit might not have installed the StyledDocument yet.
            Document aDoc = c.getDocument();
            if (!(aDoc instanceof StyledDocument)) {
                return ;
            }
            int start = Math.min(dot, mark);
            // record current character attributes.
            StyledDocument doc = (StyledDocument)aDoc;
            // If nothing is selected, get the attributes from the character
            // before the start of the selection, otherwise get the attributes
            // from the character element at the start of the selection.
            Element run;
            currentParagraph = doc.getParagraphElement(start);
            if (currentParagraph.getStartOffset() == start || dot != mark) {
                // Get the attributes from the character at the selection
                // if in a different paragrah!
                run = doc.getCharacterElement(start);
            }
            else {
                run = doc.getCharacterElement(Math.max(start-1, 0));
            }

            // Determine if we're now at the end of a link
            boolean newEndOfLink = false;
            AttributeSet a1, a2;
            a1 = run.getAttributes();
            if (a1.isDefined(HTML.Tag.A))
            {
              a1 = (AttributeSet)a1.getAttribute(HTML.Tag.A);
              if (a1.isDefined(HTML.Attribute.HREF))
              {
                // We're in a link. Check if it ends here.
                Element nextRun = doc.getCharacterElement(Math.max(start, 0));
                a2 = nextRun.getAttributes();
                if (a2.isDefined(HTML.Tag.A))
                {
                  a2 = (AttributeSet)a2.getAttribute(HTML.Tag.A);
                  if (!a2.equals(a1))
                    newEndOfLink = true;
                }
                else newEndOfLink = true;
              }
            }

            // Update input attributes if cursor placed in a new run
            if (run != currentRun || endOfLink != newEndOfLink) {
              currentRun = run;

              /**
               * @todo Currently this flag will stay true when moving from the
               * end of one link to the end of another, which allows the user
               * to continue the newly-selected link. This should be fixed.
               */
              if (endOfLink != newEndOfLink) {
                endOfLink = newEndOfLink;
                // Passes the endOfLink condition only when this condition
                // has been newly acquired by the change in cursor position
                createInputAttributes(currentRun, getInputAttributes(), endOfLink);
              } else {
                createInputAttributes(currentRun, getInputAttributes(), false);
              }
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            Object newValue = evt.getNewValue();
            Object source = evt.getSource();

            if ((source instanceof JTextComponent) &&
                (newValue instanceof Document)) {
                // New document will have changed selection to 0,0.
                updateInputAttributes(0, 0, (JTextComponent)source);
            }
        }

        public void caretUpdate(CaretEvent e) {
            updateInputAttributes(e.getDot(), e.getMark(),
                                  (JTextComponent)e.getSource());
        }
    }

    /**
     * Gets the input attributes for the pane.  When
     * the caret moves and there is no selection, the
     * input attributes are automatically mutated to
     * reflect the character attributes of the current
     * caret location.  The styled editing actions
     * use the input attributes to carry out their
     * actions.
     *
     * @return the attribute set
     */
    public MutableAttributeSet getInputAttributes() {
        return inputAttributes;
    }

    /**
     * Fetches the element representing the current
     * run of character attributes for the caret.
     *
     * @return the element
     */
    public Element getCharacterAttributeRun() {
        return currentRun;
    }

	/**
	 * Begins grouping edits for the document(s) created by this kit for undo
	 * purposes.
	 */
	public void startCompoundEdit()
	{
		undoMan.start();
	}

	/**
	 * Stops grouping edits for the document(s) created by this kit.
	 */
	public void endCompoundEdit()
	{
		undoMan.end();
	}
}

