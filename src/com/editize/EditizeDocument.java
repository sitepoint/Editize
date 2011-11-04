package com.editize;

import com.editize.editorkit.EditizeEditorKit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

/**
 * Insert the type's description here.
 *
 * Creation date: (17/07/2001 11:44:28 PM)
 * @author:
 */
public class EditizeDocument extends HTMLDocument {
	public static final String XHTMLALIGNMENT = "XHTMLALIGNMENT";
	public static final String TABLECLASSES = "TABLECLASSES";
    public static final String FORMELEMENTSALLOWED = "FORMELEMENTSALLOWED";
    private static char[] NEWLINE = new char[] {'\n'};

//	PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	// This flag is set true during article load so that
	// multiple spaces in Code Block paragraphs are not
	// lost before styles are applied to the document.
	private boolean allowMultiSpaces = false;
	/**
	 * ArticleDocument constructor comment.
	 */
	public EditizeDocument() {
		super();
	}
	/**
	 * ArticleDocument constructor comment.
	 * @param c javax.swing.text.AbstractDocument.Content
	 * @param styles javax.swing.text.StyleContext
	 */
	public EditizeDocument(AbstractDocument.Content c, StyleSheet styles) {
		super(c, styles);
	}
	/**
	 * ArticleDocument constructor comment.
	 * @param styles javax.swing.text.StyleContext
	 */
	public EditizeDocument(StyleSheet styles) {
		super(styles);
	}

		public void setMultiSpacesAllowed(boolean multiSpacesAllowed) {
		  allowMultiSpaces = multiSpacesAllowed;
		}

		public boolean isMultiSpacesAllowed() {
		  return allowMultiSpaces;
		}

	/**
     * @param name Element name to search for
     * @param offset Position in the document to begin the search
	 * @return number of parents of the leaf at <code>offset</code>
	 *         until a parent with name, <code>name</code> has been
	 *         found. -1 indicates no matching parent with
	 *         <code>name</code>.
	 */
	public int heightToElementWithName(Object name, int offset) {
		Element       e = getCharacterElement(offset).getParentElement();
		int           count = 0;

		while (e != null && e.getAttributes().getAttribute
		   (StyleConstants.NameAttribute) != name) {
		count++;
		e = e.getParentElement();
		}
		return (e == null) ? -1 : count;
	}

	/**
	 * Splits a leaf element into two leaves at the given offset.
	 *
	 * @param run The leaf to split
	 * @param offs The position within the document at which to split
	 * @return The new leaf following the split. A new leaft is also created preceding the split, but it is not returned.
     * @throws javax.swing.text.BadLocationException if <code>offs</code> is not within <code>run</code>
	 */
	public Element splitLeaf(Element run, int offs) throws BadLocationException
	{
		int runStart = run.getStartOffset();
		int runEnd = run.getEndOffset();

		Element leftRun, rightRun=null;

		try
		{
			writeLock();

			// Split the run into two runs
			DefaultDocumentEvent changes =
					new DefaultDocumentEvent(runStart, runEnd - runStart, DocumentEvent.EventType.CHANGE);

			AttributeSet attrs = run.getAttributes();
			BranchElement block = (BranchElement)run.getParentElement();
			int runIndex = block.getElementIndex(offs);

			leftRun = createLeafElement(block,attrs,runStart,offs);
			rightRun = createLeafElement(block,attrs,offs,runEnd);

			changes.addEdit(new ElementEdit(block,runIndex,new Element[] { run },new Element[] { leftRun, rightRun }));
			block.replace(runIndex,1,new Element[] { leftRun, rightRun });

			changes.end();
			fireChangedUpdate(changes);
			fireUndoableEditUpdate(new UndoableEditEvent(this,changes));
		}
		finally
		{
			writeUnlock();
		}

		return rightRun;
	}

//	public void addPropertyChangeListener(PropertyChangeListener l)
//	{
//		pcs.addPropertyChangeListener(l);
//	}
//	public void removePropertyChangeListener(PropertyChangeListener l)
//	{
//		pcs.removePropertyChangeListener(l);
//	}

	public void insertString(int offs, String str, AttributeSet a)
		throws BadLocationException
	{

		// Improves performance in Java 1.4, but not compatible
		// with Java 1.3.
		//try
		//{
			//      writeLock();

			int i;
			StringBuffer strbuff = new StringBuffer(str);

			// Replace tabs with 4 spaces
			while ((i = strbuff.toString().indexOf('\t')) >= 0)
			{
				strbuff.replace(i,i+1,"    ");
			}

			// Replace "smart quotes"
			while ((i = strbuff.toString().indexOf(8220)) >= 0 ||
				   (i = strbuff.toString().indexOf(8221)) >= 0)
			{
				strbuff.replace(i,i+1,"\"");
			}

			// Replace emdashes
			while ((i = strbuff.toString().indexOf(8211)) >= 0)
			{
				strbuff.replace(i,i+1,"--");
			}

			// Replace 'smart quotes'
			while ((i = strbuff.toString().indexOf(8216)) >= 0 ||
				   (i = strbuff.toString().indexOf(8217)) >= 0)
			{
				strbuff.replace(i,i+1,"'");
			}

			// Replace elipses (...)
			while ((i = strbuff.toString().indexOf(8230)) >= 0)
			{
				strbuff.replace(i,i+1,"...");
			}

			Element paragraph = getParagraphElement(offs);
			BranchElement parent = (BranchElement)paragraph.getParentElement();
			Object name = EditizeEditorKit.getElementName(parent);

			// Detect if insert occurred in an implied paragraph (but not in a PRE or an LI)
			if (EditizeEditorKit.getElementName(paragraph) == HTML.Tag.IMPLIED &&
				name != HTML.Tag.PRE && name != HTML.Tag.LI) {
				// Replace line breaks in inserted string with spaces
				// to prevent line breaks in non-block content
				strbuff = new StringBuffer(strbuff.toString().replace('\n', ' '));
			}

			// Remove multiple spaces except within code block paragraphs
			if (!allowMultiSpaces && name != HTML.Tag.PRE)
			{
				// Remove multiple spaces in a row in the insert string
				if (strbuff.length() > 1)
				{
					char lastChar = strbuff.charAt(0);
					for (i=1;i<strbuff.length();i++)
					{
						if (lastChar == ' ' && strbuff.charAt(i) == lastChar)
							strbuff.deleteCharAt(i);
						lastChar = strbuff.charAt(i);
					}
				}

				// Remove space at the end of the string if inserting before a space
				if (strbuff.length() > 0 && offs < getLength() &&
					getText(offs, 1).equals(" ") &&
					EditizeEditorKit.getElementName(getCharacterElement(offs)) != HTML.Tag.IMG &&
					strbuff.charAt(strbuff.length() - 1) == ' ')
				{
					remove(offs,1);
				}

				// Remove space before the insert point if insert string starts with a space
				else if (strbuff.length() > 0 && offs > 0 &&
						 getText(offs - 1, 1).equals(" ") &&
						 EditizeEditorKit.getElementName(getCharacterElement(offs - 1)) != HTML.Tag.IMG &&
						 strbuff.charAt(0) == ' ')
				{
					remove(offs-1,1);
					offs--;
				}

			}

			String insertString = strbuff.toString();

			super.insertString(offs, insertString, a);
			insertedLength = insertString.length();

			paragraph = getParagraphElement(offs);
			parent = (BranchElement)paragraph.getParentElement();
			name = EditizeEditorKit.getElementName(parent);

			// Detect if insert occurred in a list item
			if (name == HTML.Tag.LI)
			{
				// Count line breaks in the inserted string
				int breakCount = 0;
				int pos = 0;
				for (pos = insertString.indexOf('\n',pos);
					pos >= 0;
					pos = insertString.indexOf('\n',pos+1))
				{
					breakCount++;
				}

				// Split new children off into their own list items
				for (i=0; i<breakCount; i++)
				{
					splitBranchElement(parent,1);
				}
			}

		// Improves performance in Java 1.4, but not compatible
		// with Java 1.3.
		//}
		//finally
		//{
		//writeUnlock();
		//}
	}

	/**
	 * Removes specified character range from the document, ensuring that the
	 * HTML element structure that results remains valid.
	 *
	 * @param offs int
	 * @param len int
	 * @throws BadLocationException
	 */
	public void remove(int offs, int len) throws BadLocationException
	{
		Element root = getDefaultRootElement();
		Segment s = new Segment();
		getText(offs, len, s);
		char txt[] = s.array;
		int n = s.count;
		boolean firstNewLine = true;
		int firstBlockDepth = 0;
		int i = 0;

		// If removal fully contains the first block, skip to first block
		// following the shallowest block starting here that ends within the
		// selection. (This special case speeds up table deletion.)
		while (getParagraphElement(offs + i).getStartOffset() == offs + i &&
				getParagraphElement(offs + i).getEndOffset() <= offs + len) {
			Element e = getParagraphElement(offs + i);
			while (e.getParentElement().getStartOffset() == offs + i &&
				   e.getParentElement().getEndOffset() <= offs + len) {
				e = e.getParentElement();
			}
			i = e.getEndOffset() - offs; // Skip to following block
		}

		for (; i < n; i++)
		{
			// If area of deletion contains a newline
			if (txt[i + s.offset] == '\n')
			{
				BranchElement e, parent;

				// Do not join to next block when newline occurs at the end of the
				// deletion area and start of deletion area is start of a block
				// That is, when deleting an entire block, the following block should
				// not be changed to match the deleted block's type.
				if (i + 1 == n && getParagraphElement(offs).getStartOffset() == offs)
					break;

				// Find the shallowest block that starts after the newline and that
				// is fully contained by the selection, or the deepest block
				// starting at the newline if all parents extend beyond the
				// end of the selection.
				e = (BranchElement)getParagraphElement(offs + i + 1);
				if (e == null) continue; // Ignore end of document
				if (e.getStartOffset() < offs + i + 1) continue; // Ignore <br/>

				while ((parent = (BranchElement)e.getParent()).getStartOffset() >=
					   e.getStartOffset() && parent.getEndOffset() < offs + len) {
					e = parent;
				}

				// Extract this block from any parent blocks that also begin here
				int blockStart = e.getStartOffset();
				while ((parent = (BranchElement)e.getParentElement()).getStartOffset() >= blockStart)
				{
					// If parent extends beyond the end of the selection, and
					// contains blocks that come after the selection, split it
					// so they have their own parent element outside the
					// selection.
					if (parent.getEndOffset() >= offs + len &&
						parent.getElementIndex(offs + len) < parent.getElementCount() - 1) {
						parent = splitBranchElement(parent, parent.getElementIndex(offs + len) + 1);
					}
					// Remove the parent, which should now only contain blocks
					// within (or partially within) the selection.
					removeBranchElement(parent);
					// Find new incarnation of e
					e = (BranchElement)parent.getParentElement();
					e = (BranchElement)e.getElement(e.getElementIndex(offs + len));
				}

				// Record depth of the deepest first block in the selection
				if (firstNewLine)
				{
					e = (BranchElement)getParagraphElement(offs + i);
					firstBlockDepth = elementDepth(e);
					firstNewLine = false;
				}

				// Get shallowest block ending at the newline
				e = (BranchElement)getParagraphElement(offs + i);
				int blockEnd = e.getEndOffset();
				if (blockEnd > offs + i + 1) continue; // Ignore <br/> (sanity check)
				parent = e;
				do
				{
					e = parent;
					parent = (BranchElement)e.getParentElement();
				} while (parent != root && parent.getEndOffset() <= blockEnd);
				// Extend non-"leaf" blocks with depth < firstBlockDepth
				// ending at the newline to contain next block
				while (e != getParagraphElement(offs + i) &&
					   elementDepth(e) < firstBlockDepth) {
					int start = e.getStartOffset();
					int end = e.getEndOffset();
					AttributeSet attrs = e.getAttributes();
					removeBranchElement(e);
					// Note: end instead of end - 1 makes the difference
					e = insertSubBranchElement((BranchElement)e.getParent(),
											   start, end, attrs);
					// Navigate to next deeper block
					e = (BranchElement)e.getElement(e.getElementIndex(offs + i));
				}

				// Get the shallowest block ending at the newline
				Element paragraph1 = getParagraphElement(offs + i);
				while (paragraph1.getParentElement().getEndOffset() <=
					   paragraph1.getEndOffset())
					paragraph1 = paragraph1.getParentElement();
				// Get the shallowest block starting after the newline
				Element paragraph2 = getParagraphElement(offs + i + 1);
				while (paragraph2.getParentElement().getStartOffset() >=
					   paragraph2.getStartOffset())
					paragraph2 = paragraph2.getParentElement();
				// If the two paragraphs are in the same parent container (sanity check!)
				if (paragraph1.getParentElement() == paragraph2.getParentElement())
				{
					// Make sure the paragraph beginning at the linebreak
					// is of the same type as the paragraph ending the linebreak
					// so they are correctly joined by super.remove().
					paragraph2 = replaceBranchElement((BranchElement)paragraph2, paragraph1.getAttributes());

					// Skip to the end of paragraph2
					i = paragraph2.getEndOffset() - offs - 2; // -2 because i will be incremented next
				}
			}
		}

		super.remove(offs, len);
	}

	private int elementDepth(Element e) {
		Element root = getDefaultRootElement();
		int depth = 0;
		while ((e = e.getParentElement()) != root)
			depth++;
		return depth;
	}

	/**
	 * Stores the number of characters inserted by the last insertString call.
	 * Used in HTMLReader to update the document offset.
	 */
	private int insertedLength = 0;

	/**
	 * Sets attributes for paragraphs.
	 * <p>
	 * This method is thread safe, although most Swing methods
	 * are not. Please see
	 * <A HREF="http://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
	 * and Swing</A> for more information.
	 *
	 * @param offset the offset into the paragraph (must be at least 0)
	 * @param length the number of characters affected (must be at least 0)
	 * @param s the attributes
	 * @param replace whether to replace existing attributes, or merge them
	 */
	public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace)
	{
		try
		{
			writeLock();
			// Make sure we send out a change that doesn't go beyond the end of the doc.
			int end = Math.min(offset + length, getLength());
			Element e = getParagraphElement(offset);
			offset = e.getStartOffset();
			e = getParagraphElement(end);
			length = Math.max(0, e.getEndOffset() - offset);
			DefaultDocumentEvent changes = new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);
			AttributeSet sCopy = s.copyAttributes();
			int lastEnd = Integer.MAX_VALUE;
			for (int pos = offset; pos <= end; pos = lastEnd)
			{
				Element paragraph = getParagraphElement(pos);

				// Ignore implied paragraphs (blockquotes, list items, preformatted blocks)
				if (paragraph.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMPLIED)
				{
					lastEnd = paragraph.getEndOffset();
					continue;
				}

				if (lastEnd == paragraph.getEndOffset())
				{
					lastEnd++;
				}
				else
				{
						lastEnd = paragraph.getEndOffset();
				}
				MutableAttributeSet attr = (MutableAttributeSet) paragraph.getAttributes();
				changes.addEdit(new AttributeUndoableEdit(paragraph, sCopy, replace));
				if (replace)
				{
						attr.removeAttributes(attr);
				}
				attr.addAttributes(s);
			}
			changes.end();
			fireChangedUpdate(changes);
			fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
		}
		finally
		{
			writeUnlock();
		}
	}

	/**
	 * Sets attributes for the deepest tag at offset.
	 * <p>
	 * This method is thread safe, although most Swing methods
	 * are not. Please see
	 * <A HREF="http://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
	 * and Swing</A> for more information.
	 *
	 * @param offset the offset into the paragraph (must be at least 0)
	 * @param tag the tag to which the attributes will be assigned
	 * @param s the attributes
	 * @param replace whether to replace existing attributes, or merge them
	 */
	public void setTagAttributes(int offset, HTML.Tag tag, AttributeSet s, boolean replace)
	{
		try
		{
			writeLock();

			// Make sure we send out a change that doesn't go beyond the end of the doc.
			Element e = findElementMatchingTag(offset,tag);
			offset = e.getStartOffset();
			int end = e.getEndOffset();
			int length = Math.max(0, end - offset);
			DefaultDocumentEvent changes = new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);
			AttributeSet sCopy = s.copyAttributes();

			MutableAttributeSet attr = (MutableAttributeSet) e.getAttributes();
			changes.addEdit(new AttributeUndoableEdit(e, sCopy, replace));
			if (replace)
			{
					attr.removeAttributes(attr);
			}
			attr.addAttributes(s);

			changes.end();
			fireChangedUpdate(changes);
			fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
		}
		finally
		{
			writeUnlock();
		}
	}

	public void clearTagAttributes(int offset, HTML.Tag tag, Object[] attributes)
	{
		try
		{
			writeLock();

			// Make sure we send out a change that doesn't go beyond the end of the doc.
			Element e = findElementMatchingTag(offset,tag);
			offset = e.getStartOffset();
			int end = e.getEndOffset();
			int length = Math.max(0, end - offset);
			DefaultDocumentEvent changes = new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

			MutableAttributeSet attr = (MutableAttributeSet) e.getAttributes();
			MutableAttributeSet newAttr = new SimpleAttributeSet(attr);
			for (int j=0;j<attributes.length;j++) newAttr.removeAttribute(attributes[j]);
			changes.addEdit(new AttributeUndoableEdit(e, newAttr, true));
			attr.removeAttributes(attr);
			attr.addAttributes(newAttr);

			changes.end();
			fireChangedUpdate(changes);
			fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
		}
		finally
		{
			writeUnlock();
		}
	}


	/**
	 * Returns the deepest element at <code>offset</code> matching
	 * <code>tag</code>.
     * @param offset The document position at which to search
     * @param tag The HTML tag name to search for
     * @return The element found, or null.
     */
	public Element findElementMatchingTag(int offset, HTML.Tag tag)
	{
		Element e = getDefaultRootElement();
		Element lastMatch = null;
		while (e != null) {
			if (e.getAttributes().getAttribute
				(StyleConstants.NameAttribute) == tag) {
				lastMatch = e;
			}
			e = e.getElement(e.getElementIndex(offset));
		}
		return lastMatch;
	}

	/**
	 * Clears an attribute from one or more paragraphs. Does not handle bi-directional
	 * text.
	 * <p>
	 * This method is thread safe, although most Swing methods
	 * are not. Please see
	 * <A HREF="http://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
	 * and Swing</A> for more information.
	 *
	 * @param offset the offset into the paragraph >= 0
	 * @param length the number of characters affected >= 0
	 * @param attributes Array of attributes to clear
	 */
	public void clearParagraphAttributes(int offset, int length, Object[] attributes)
	{
		try
		{
			writeLock();
			DefaultDocumentEvent changes = new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

			Element currentElement, prevElement = null;
			Element lastElement = getParagraphElement(offset + ((length > 0) ? length - 1 : 0));
			int i = offset;
			do
			{
				currentElement = getParagraphElement(i++);
				if (currentElement == null) break;
				if (prevElement == null || currentElement != prevElement)
				{
					// The following cast may be presumtuous, but again the Sun JRE does it, so
					// it should be safe for us to do it too...
					MutableAttributeSet attr = (MutableAttributeSet) currentElement.getAttributes();
					MutableAttributeSet newAttr = new SimpleAttributeSet(attr);
					for (int j=0;j<attributes.length;j++) newAttr.removeAttribute(attributes[j]);
					changes.addEdit(new AttributeUndoableEdit(currentElement, newAttr, true));
					attr.removeAttributes(attr);
					attr.addAttributes(newAttr);
				}
				prevElement = currentElement;
			} while (currentElement != lastElement);

			changes.end();
			fireChangedUpdate(changes);
			fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
		}
		finally
		{
			writeUnlock();
		}
	}

	/**
	 * Clears attributes for some part of the document.
	 * A write lock is held by this operation while changes
	 * are being made, and a DocumentEvent is sent to the listeners
	 * after the change has been successfully completed.
	 * <p>
	 * This method is thread safe, although most Swing methods
	 * are not. Please see
	 * <A HREF="http://java.sun.com/products/jfc/swingdoc-archive/threads.html">Threads
	 * and Swing</A> for more information.
	 *
	 * @param offset the offset in the document >= 0
	 * @param length the length >= 0
	 * @param attributes the attributes to clear
	 */
	public void clearCharacterAttributes(int offset, int length, Object[] attributes)
	{
		try
		{
			writeLock();
						doClearCharacterAttributes(offset, length, attributes);
		}
		finally
		{
			writeUnlock();
		}

	}

		/**
		 * Executive method for clearCharacterAttributes. Requires a write lock to be called.
         * @param offset the offset in the document >= 0
         * @param length the length >= 0
         * @param attributes the attributes to clear
		 */
		protected void doClearCharacterAttributes(int offset, int length, Object[] attributes)
		{
		  DefaultDocumentEvent changes =
			  new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

		  // split elements that need it
		  buffer.change(offset, length, changes);

		  // PENDING(prinz) - this isn't a very efficient way to iterate
		  int lastEnd; // = Integer.MAX_VALUE;
		  for (int pos = offset; pos < (offset + length); pos = lastEnd)
		  {
			Element run = getCharacterElement(pos);
			lastEnd = run.getEndOffset();
			if (pos == lastEnd)
			{
			  // offset + length beyond length of document, bail.
			  break;
			}
			MutableAttributeSet attr = (MutableAttributeSet) run.getAttributes();
			MutableAttributeSet sas = new SimpleAttributeSet(attr);
			for (int i = 0; i < attributes.length; i++)
			  sas.removeAttribute(attributes[i]);
			changes.addEdit(new AttributeUndoableEdit(run, sas, true));
			for (int i = 0; i < attributes.length; i++)
			  attr.removeAttribute(attributes[i]);
			attr.removeAttributes(attr);
			attr.addAttributes(sas);
		  }
		  changes.end();
		  fireChangedUpdate(changes);
		  fireUndoableEditUpdate(new UndoableEditEvent(this, changes));

		}

	/**
	 * Groups a subset of a BranchElement's children under a new branch element. Used to insert
	 * an HTML tag below other HTML tags (potentially block tags) in the document structure.
	 *
	 * @param parent The parent BranchElement.
	 * @param startOffset A document offset marking the first child branch.
	 * @param endOffset A document offset marking the last child branch.
	 * @param attrs The AttributeSet to apply to the new BranchElement.
	 * @return The new element.
	 */
	public BranchElement insertSubBranchElement(BranchElement parent, int startOffset, int endOffset, AttributeSet attrs)
	{
		try
		{
			writeLock();

			int numChildren = parent.getElementCount();
			if (numChildren > 0) // Unneccessary?
			{
				BranchElement to = (BranchElement)createBranchElement(parent,attrs);
				int leftChildIndex = parent.getElementIndex(startOffset);
				int rightChildIndex = parent.getElementIndex(endOffset);
				int startPos = parent.getElement(leftChildIndex).getStartOffset();
				int endPos = parent.getElement(rightChildIndex).getEndOffset();

				DefaultDocumentEvent changes = new DefaultDocumentEvent(startPos, endPos - startPos, DocumentEvent.EventType.CHANGE);

				// Assign children to appropriate branches
				int childIndex = 0, parentChildCounter = 0, branchChildCounter = 0;
				boolean insertedBranch = false;
				Element[] parentChildren = new Element[numChildren - rightChildIndex + leftChildIndex];
				Element[] branchChildren = new Element[rightChildIndex - leftChildIndex + 1];
				Element[] removedChildren = new Element[rightChildIndex - leftChildIndex + 1];
				for (Enumeration e = parent.children();e.hasMoreElements();childIndex++)
				{
					if (childIndex < leftChildIndex)
						parentChildren[parentChildCounter++] = (Element)e.nextElement();
						else if (childIndex <= rightChildIndex)
					{
						removedChildren[branchChildCounter] = (Element)e.nextElement();
						branchChildren[branchChildCounter] = buffer.clone(to,removedChildren[branchChildCounter]);
						branchChildCounter++;
					}
					else
					{
						if (!insertedBranch)
						{
							parentChildren[parentChildCounter++] = to;
							insertedBranch = true;
						}
							parentChildren[parentChildCounter++] = (Element)e.nextElement();
					}
				}
				if (!insertedBranch)
				{
					parentChildren[parentChildCounter] = to;
				}

				changes.addEdit(new ElementEdit(parent,leftChildIndex,removedChildren,new Element[] { to }));

				parent.replace(0,parent.getElementCount(),parentChildren);
				to.replace(0,0,branchChildren);

				changes.end();
				fireChangedUpdate(changes);
				fireUndoableEditUpdate(new UndoableEditEvent(this,changes));

				return to;
			}
			return null;
		}
		finally
		{
			writeUnlock();
		}
	}

	/**
	 * Replaces a BranchElement with another, copying the existing branch's children into the
	 * new branch. Used to replace one block HTML tag with another.
	 *
	 * @param toReplace The BranchElement to replace.
	 * @param attrs The AttributeSet to apply to the new BranchElement.
	 * @return The new element.
	 */
	public BranchElement replaceBranchElement(BranchElement toReplace, AttributeSet attrs)
	{
		try
		{
			writeLock();

			int numChildren = toReplace.getElementCount();
			BranchElement parent = (BranchElement)toReplace.getParentElement();
			BranchElement replacement = (BranchElement)createBranchElement(parent,attrs);
			int startPos = toReplace.getStartOffset();
			int endPos = toReplace.getEndOffset();
			int elementOffset = parent.getElementIndex(toReplace.getStartOffset());

			DefaultDocumentEvent changes = new DefaultDocumentEvent(startPos, endPos - startPos, DocumentEvent.EventType.CHANGE);

			// Assign children to appropriate branches
			int childIndex = 0;
			Element[] newChildren = new Element[numChildren];
			for (Enumeration e = toReplace.children();e.hasMoreElements();childIndex++)
			{
				newChildren[childIndex] = buffer.clone(replacement,(Element)e.nextElement());
			}

			changes.addEdit(new ElementEdit(parent,elementOffset,new Element[] { toReplace },new Element[] { replacement }));
			parent.replace(elementOffset,1,new Element[] { replacement });
			replacement.replace(0,0,newChildren);

			changes.end();
			fireChangedUpdate(changes);
			fireUndoableEditUpdate(new UndoableEditEvent(this,changes));

			return replacement;
		}
		finally
		{
			writeUnlock();
		}
	}

	/**
	 * Removes a block element from the document hierarchy, adding its children to the
	 * parent's children. Used to remove an HTML block that is no longer needed.
	 *
	 * @param toRemove The BranchElement to remove.
	 */
	public void removeBranchElement(BranchElement toRemove)
	{
		try
		{
			writeLock();
						doRemoveBranchElement(toRemove);
		}
		finally
		{
			writeUnlock();
		}
	}

		/**
		 * Executive method for removeBranchElement. Requires write lock.
		 * @param toRemove The BranchElement to remove.
		 */
		protected void doRemoveBranchElement(BranchElement toRemove)
		{
		  int numChildren = toRemove.getElementCount();
		  BranchElement parent = (BranchElement)toRemove.getParentElement();
		  int startPos = toRemove.getStartOffset();
		  int endPos = toRemove.getEndOffset();
		  int elementOffset = parent.getElementIndex(startPos);

		  DefaultDocumentEvent changes = new DefaultDocumentEvent(startPos, endPos - startPos, DocumentEvent.EventType.CHANGE);

		  // Assign children to appropriate branches
		  int childIndex = 0;
		  Element[] newChildren = new Element[numChildren];
		  for (Enumeration e = toRemove.children();e.hasMoreElements();childIndex++)
		  {
				  newChildren[childIndex] = buffer.clone(parent,(Element)e.nextElement());
		  }

		  changes.addEdit(new ElementEdit(parent,elementOffset,new Element[] { toRemove },newChildren));
		  parent.replace(elementOffset,1,newChildren);

		  changes.end();
		  fireChangedUpdate(changes);
		  fireUndoableEditUpdate(new UndoableEditEvent(this,changes));
		}

		/**
		 * Scans for and removes any &lt;span&gt; tags that have no attributes, which
		 * may be left over from filtering Microsoft garbage during insertion.
		 *
		 * Note: Each tag removed will normally cause an UndoableEdit, so be
		 * sure to manage undo history when calling this method.
		 */
		public void removeRedundantSpans()
		{
		  try
		  {
			writeLock();
			ElementIterator i = new ElementIterator(this);
			Element el;
			Stack removeList = new Stack();
			while ((el = i.next()) != null)
			{
			  AttributeSet a = el.getAttributes();
			  if (EditizeEditorKit.getElementName(el) == HTML.Tag.CONTENT &&
				  a.isDefined(HTML.Tag.SPAN))
			  {
				a = (AttributeSet)a.getAttribute(HTML.Tag.SPAN);
				if (! (a.isDefined(HTML.Attribute.CLASS) ||
					   a.isDefined(HTML.Attribute.STYLE)))
				{
				  removeList.push(el);
				}
			  }
			}

			// Remove the branches in the opposite order they were found,
			// ensuring deepest branches are removed first.
			while (removeList.size() > 0)
			{
			  el = (Element)removeList.pop();
			  doClearCharacterAttributes(el.getStartOffset(),
										 el.getEndOffset() - el.getStartOffset(),
										 new Object[] {HTML.Tag.SPAN});
			}
		  }
		  finally
		  {
			writeUnlock();
		  }
		}

	/**
	 * Splits children 0 to n or n to last into a new, identical branch
	 * before or after the specified branch, respectively.
	 *
	 * @param toSplit The BranchElement from which to split the children.
	 * @param childrenToSplit The number of children to move into a new
	 *        branch. If positive, takes the children from the start. If
	 *        negative, takes the children from the end.
	 * @return The new branch containing the split children.
	 */
	public BranchElement splitBranchElement(BranchElement toSplit, int childrenToSplit)
	{
		if (childrenToSplit == 0) return null;
		try
		{
			writeLock();

				int numChildren = toSplit.getChildCount();
			if (Math.abs(childrenToSplit) > numChildren -1)
				throw new IllegalArgumentException("Can't split all or more than the number of children in the branch!");

				BranchElement parent = (BranchElement)toSplit.getParent();
			int toSplitIndex = parent.getElementIndex(toSplit.getStartOffset());
				int startPos = toSplit.getStartOffset();
			int endPos = toSplit.getEndOffset();

			DefaultDocumentEvent changes = new DefaultDocumentEvent(startPos, endPos - startPos, DocumentEvent.EventType.CHANGE);

			// New branch
			BranchElement newBranch = (BranchElement)createBranchElement(parent,toSplit.getAttributes());
			int newBranchIndex = childrenToSplit > 0 ? toSplitIndex : toSplitIndex + 1;

			// Add new branch to parent
				parent.replace(newBranchIndex,0,new Element[] {newBranch});
			changes.addEdit(new ElementEdit(parent,newBranchIndex,new Element[] {}, new Element[] {newBranch}));

				// Clone children to be moved
			Element[] newChildren = new Element[Math.abs(childrenToSplit)];
			Element[] oldChildren = new Element[Math.abs(childrenToSplit)];
			int j=0;
			for (int i=(childrenToSplit > 0 ? 0 : numChildren + childrenToSplit);
				i < (childrenToSplit > 0 ? childrenToSplit : numChildren);
				i++)
			{
				oldChildren[j] = toSplit.getElement(i);
				newChildren[j] = buffer.clone(newBranch,oldChildren[j]);
				j++;
			}

			// Remove children from old branch
			toSplit.replace(childrenToSplit > 0 ? 0 : numChildren + childrenToSplit,Math.abs(childrenToSplit),new Element[] {});
			changes.addEdit(new ElementEdit(toSplit,childrenToSplit > 0 ? 0 : numChildren + childrenToSplit,oldChildren,new Element[] {}));

			// Add children to new branch
				newBranch.replace(0,0,newChildren);
			changes.addEdit(new ElementEdit(newBranch,0,new Element[] {},newChildren));

			changes.end();
			fireChangedUpdate(changes);
			fireUndoableEditUpdate(new UndoableEditEvent(this,changes));

			return newBranch;
		}
		finally
		{
			writeUnlock();
		}
	}

	/**
	 * Adds support for the span tag, and filters out form elements
	 */
	public class HTMLReader extends HTMLDocument.HTMLReader
	{
		CharacterAction spanAction = new CharacterAction();
		boolean emptyTableCell = false;

        public HTMLReader(int offset)
		{
			super(offset);

		}

		public HTMLReader(int offset, int popDepth, int pushDepth, HTML.Tag insertTag)
		{
			super(offset, popDepth, pushDepth, insertTag);
		}

		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos)
		{
			emptyTableCell = false;

            // Strip out form elements
            if (!isFormElementsAllowed() && isFormTag(t)) return;

            a = scrubMicrosoftAttributes(t,a);

			if (t == HTML.Tag.TABLE) {
				// Modify tables with border="0" or no border so
				// they'll be displayed with a visible border
				String border = (String)a.getAttribute(HTML.Attribute.BORDER);
				if (border == null || border.equals("0")) {
					a.addAttribute(HTML.Attribute.BORDER, "1");
					a.addAttribute("trueborder", border == null ? "" : border);
					a.addAttribute(HTML.Attribute.STYLE,
							"border-style: solid; border-color: #cccccc;");
				}
			}

			super.handleStartTag(t, a, pos);

			if (t == HTML.Tag.SPAN) {
				spanAction.start(t, a);
			}

			// Spot table cells and set a flag
			if (t == HTML.Tag.TD || t == HTML.Tag.TH) {
				emptyTableCell = true;
			}
		}

		public void handleEndTag(HTML.Tag t, int pos)
		{
            // Strip out form elements
            if (!isFormElementsAllowed() && isFormTag(t)) return;

			// Need to insert an implied paragraph inside each empty table cell
			// so that they are rendered properly if text is added. Java 1.4
			// already does this, but this way of doing it forces Java 1.3 to do
			// it without causing Java 1.4 to do it twice. Handy!
			if (emptyTableCell)
			{
				addContent(NEWLINE, 0, 1, true);
			}

			super.handleEndTag(t, pos);

			if (t == HTML.Tag.SPAN)
			{
				spanAction.end(t);
			}

			emptyTableCell = false;
		}

		public void handleText(char[] data, int pos)
		{
			if (data.length != 0) emptyTableCell = false;
			super.handleText(data, pos);
		}

		public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos)
		{
            // Strip out form elements
            if (!isFormElementsAllowed() && isFormTag(t)) return;

			emptyTableCell = false;
			super.handleSimpleTag(t, a, pos);
		}

        protected boolean isFormTag(HTML.Tag t)
        {
            return t == HTML.Tag.FORM || t == HTML.Tag.INPUT || t == HTML.Tag.TEXTAREA || t == HTML.Tag.SELECT || t == HTML.Tag.OPTION;
        }
    }

	/**
	 * Scrubs AttributeSets of Microsoft garbage.
	 *
     * @param t The tag being scrubbed.
	 * @param a The AttributeSet to scrub.
	 * @return A new AttributeSet, with Microsoft garbage attributes removed.
	 */
	private static MutableAttributeSet scrubMicrosoftAttributes(HTML.Tag t, MutableAttributeSet a)
	{
		Object attr;
		MutableAttributeSet a2 = new SimpleAttributeSet(a);

		// Strip class=Mso*
		attr = a2.getAttribute(HTML.Attribute.CLASS);
		if (attr != null &&
			attr instanceof String &&
			((String)attr).startsWith("Mso"))
		{
			a2.removeAttribute(HTML.Attribute.CLASS);
		}

		// Strip style=* off all tags except <table>
        if (t != HTML.Tag.TABLE)
            a2.removeAttribute(HTML.Attribute.STYLE);

		return a2;
	}

	/**
	 * Fetches the reader for the parser to use to load the document
	 * with HTML.  This is implemented to return an instance of
	 * HTMLDocument.HTMLReader.  Subclasses can reimplement this
	 * method to change how the document get structured if desired
	 * (e.g. to handle custom tags, structurally represent character
	 * style elements, etc.).
	 */
	public HTMLEditorKit.ParserCallback getReader(int pos)
	{
		Object desc = getProperty(Document.StreamDescriptionProperty);
		if (desc instanceof java.net.URL)
		{
			setBase((java.net.URL)desc);
		}

        return new HTMLReader(pos);
	}

	/**
	 * Fetches the reader for the parser to use to load the document
	 * with HTML.  This is implemented to return an instance of
	 * HTMLDocument.HTMLReader.  Subclasses can reimplement this
	 * method to change how the document get structured if desired
	 * (e.g. to handle custom tags, structurally represent character
	 * style elements, etc.).
	 *
	 * @param popDepth   the number of ElementSpec.EndTagTypes to generate before
	 *        inserting
	 * @param pushDepth  the number of ElementSpec.StartTagTypes with a direction
	 *        of ElementSpec.JoinNextDirection that should be generated
	 *        before inserting, but after the end tags have been generated
	 * @param insertTag  the first tag to start inserting into document
	 */
	public HTMLEditorKit.ParserCallback getReader(int pos, int popDepth,
						  int pushDepth,
						  HTML.Tag insertTag)
	{
		Object desc = getProperty(Document.StreamDescriptionProperty);
		if (desc instanceof java.net.URL)
		{
			setBase((java.net.URL)desc);
		}
        return new HTMLReader(pos, popDepth, pushDepth, insertTag);
	}

	public void setXHTMLCompliantAlignment(boolean enabled)
	{
		putProperty(XHTMLALIGNMENT, enabled ? Boolean.TRUE : Boolean.FALSE);
	}

	public boolean isXHTMLCompliantAlignment()
	{
		return Boolean.TRUE.equals(getProperty(XHTMLALIGNMENT));
	}

    public void setFormElementsAllowed(boolean enabled)
    {
        putProperty(FORMELEMENTSALLOWED, enabled ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isFormElementsAllowed()
    {
        return Boolean.TRUE.equals(getProperty(FORMELEMENTSALLOWED));
    }

    public void setTableClasses(Vector tableClasses)
	{
		putProperty(TABLECLASSES, tableClasses);
	}

	public Vector getTableClasses()
	{
		return (Vector)getProperty(TABLECLASSES);
	}

	/**
	 * Fetches the list of styles defined in style sheet chain.
	 *
	 * @return all the style names.
	 */
	public Enumeration getStyleNames() {
        Vector names = new Vector();
		StyleSheet ss = getStyleSheet();
		readNamesFromSheet(ss, names);
		return names.elements();
	}

	/**
	 * Adds style names from StyleSheet and all linked StyleSheets to start of
	 * supplied Vector.
	 *
	 * @param ss StyleSheet
	 * @param names Vector
	 */
	private static void readNamesFromSheet(StyleSheet ss, Vector names) {
		StyleSheet[] sheets = ss.getStyleSheets();
		if (sheets != null)
			for (int i = 0; i < sheets.length; i++)
				readNamesFromSheet(sheets[i], names);
		Enumeration e = ss.getStyleNames();
		while (e.hasMoreElements()) {
			names.insertElementAt(e.nextElement(), 0);
		}
	}

	/**
	 * An attempt to avoid deadlocks by overriding this method to obtain a
	 * readLock instead of a writeLock on the document.
	 */
	/**
	 * Called when any of this document's styles have changed.
	 * Subclasses may wish to be intelligent about what gets damaged.
	 *
	 * @param style The Style that has changed.
	 */
	protected void styleChanged(Style style) {
		// Only propagate change updated if have content
		if (getLength() != 0) {
			// lazily create a ChangeUpdateRunnable
			if (updateRunnable == null) {
				updateRunnable = new ChangeUpdateRunnable();
			}

			// We may get a whole batch of these at once, so only
			// queue the runnable if it is not already pending
			synchronized(updateRunnable) {
				if (!updateRunnable.isPending) {
					SwingUtilities.invokeLater(updateRunnable);
					updateRunnable.isPending = true;
				}
			}
		}
	}

	/** Run to create a change event for the document */
	private transient ChangeUpdateRunnable updateRunnable;

	/**
	 * When run this creates a change event for the complete document
	 * and fires it.
	 */
	class ChangeUpdateRunnable implements Runnable
	{
		boolean isPending = false;

		public void run()
		{
			synchronized (this) {
				isPending = false;
			}

			try {
				readLock();
				DefaultDocumentEvent dde = new DefaultDocumentEvent(0,
						getLength(),
						DocumentEvent.EventType.CHANGE);
				dde.end();
				fireChangedUpdate(dde);
			} finally {
				readUnlock();
			}
		}
	}

}
