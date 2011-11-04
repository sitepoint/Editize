package com.editize.editorkit;

import java.beans.*;
import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import com.editize.*;

/**
 * Action responsible for highlighted blocks.
 */
public class BSABlockHighlightAction
		extends BooleanStateAwareTextAction
		implements ChangeListener
{
	public BSABlockHighlightAction()
	{
		super("highlight-block");

		try
		{
			InputStream i = getClass().getResourceAsStream("toolbarButtonGraphics/blockcolor.gif");
			byte[] img = new byte[i.available()];
			i.read(img);
			ImageIcon icon = new ImageIcon(img);
			putValue(Action.SMALL_ICON, icon);
		}
		catch (IOException e)
		{ /* Could not load Action graphic */ }
	}

	/**
	 * Adjusts icon to reflect the highlighted block color of the assigned editor's document.
	 * @param e The assigned JEditorPane.
	 */
	public void setAssignedEditor(JEditorPane e)
	{
		super.setAssignedEditor(e);

		Document doc = e.getDocument();
		if (doc instanceof EditizeDocument)
		{
			EditizeStyleSheet ss = (EditizeStyleSheet)((HTMLDocument)doc).getStyleSheet();
			Style s = ss.lookupStyle("div.highlighted");
			Color c = StyleConstants.getBackground(s);

			if (c != null) updateIcon((Color)c);

			// Listen for changes to highlightcolor
			ss.addChangeListener(this);
		}
		// Listen for style changes
		doc.addDocumentListener(this);
		// Listen for new documents
		e.addPropertyChangeListener(this);
	}

	public void changedUpdate(DocumentEvent evt) {
		super.changedUpdate(evt);
		Document doc = getAssignedEditor().getDocument();
		updateIconFromDocument(doc);
	}

	/**
	 * Responds to changes in the assigned editor's document property.
	 * @param evt
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		Object newValue = evt.getNewValue();
		if (evt.getPropertyName().equals("document") && newValue != null) // New document loaded
		{
			// Register to listen for highlight color changes from the new document
			if (newValue instanceof EditizeDocument) {
				EditizeDocument doc = (EditizeDocument) newValue;
				doc.getStyleSheet().addChangeListener(this);
				doc.addDocumentListener(this);
				updateIconFromDocument(doc);
			}
		}
	}

	/**
	 * Responds to changes in the stylesheet.
	 * @param evt
	 */
	public void stateChanged(ChangeEvent evt)
	{
		Document doc = getAssignedEditor().getDocument();
		updateIconFromDocument(doc);
	}

	protected void updateIconFromDocument(Document doc) {
		if (doc instanceof HTMLDocument) {
			EditizeStyleSheet ss = (EditizeStyleSheet) ( (HTMLDocument) doc).
					getStyleSheet();
			Style s = ss.lookupStyle("div.highlighted");
			Color c = StyleConstants.getBackground(s);
			if (c != null) updateIcon( (Color) c);
		}
	}

	/**
	 * Adjusts icon to reflect the highlighted text color of the specified editor's document.
	 * @param e The JEditorPane from which to obtain the color.
	 */
	protected void updateIcon(Color c)
	{
		// Modify the image to display the highlight color assigned to the document
		if (c != null && c instanceof java.awt.Color)
		{
			Color color = (Color)c;
			Image icon = ((ImageIcon)getValue(Action.SMALL_ICON)).getImage();

			BufferedImage img = new BufferedImage(16,16,BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = img.getGraphics();
			g.drawImage(icon,0,0,getAssignedEditor());
			g.setColor(color);
			g.fillRect(1,11,14,4);
			putValue(Action.SMALL_ICON, new ImageIcon(img));
		}
	}

	/**
	 * Toggles the highlighted state of selected block(s).
	 *
	 * @param e The ActionEvent
	 */
	public void actionPerformed(ActionEvent evt)
	{
		JEditorPane editor = getAssignedEditor(evt);
		if (editor != null)
		{
			StyledDocument doc = getStyledDocument(editor);
			if (!(doc instanceof EditizeDocument)) return;
			EditizeDocument eDoc = (EditizeDocument)doc;
			int startPos = editor.getSelectionStart();
			int endPos = editor.getSelectionEnd();
			Element root = eDoc.getDefaultRootElement();
			boolean removeHighlight = false;

			Element e;
			DefaultStyledDocument.BranchElement hBlock;
			Vector hBlocks;
			int pos, depth, curDepth;
			Object name, className;

			/*

			1) Make sure selection is inside a single highlighted block:

				 - identify existing highlighted block,
				 - expand existing highlighted block,
				 - combine two or more peer highlighted blocks, or
				 - create a new highlighted block

			*/

			// Find the shallowest highlighted block(s) that intersect
			// the selection (if any).
			pos = startPos;
			depth = Integer.MAX_VALUE;
			hBlocks = new Vector();
			while (pos <= endPos)
			{
				e = eDoc.getParagraphElement(pos);
				pos = e.getEndOffset();
				curDepth = 0;
				do
				{
					e = e.getParentElement();
					curDepth++;
					name = e.getAttributes().getAttribute(StyleConstants.NameAttribute);
					className = e.getAttributes().getAttribute(HTML.Attribute.CLASS);
				} while (name != HTML.Tag.DIV && !"highlighted".equals(className) && e != root && curDepth < depth);
				if (name == HTML.Tag.DIV && "highlighted".equals(className))
				{
					if (curDepth < depth)
					{
						depth = curDepth;
						hBlocks = new Vector();
						hBlocks.addElement(e);
					}
					else if (curDepth == depth && !hBlocks.contains(e))
					{
						hBlocks.addElement(e);
					}
				}
			}

			EditizeEditorKit kit = getEditizeEditorKit(editor);

			try
			{
				kit.undoMan.start();

				if (hBlocks.size() == 0)
				{
					/* No highlighted blocks found. */

					// Are we next to a highlighted block?
					e = eDoc.getParagraphElement(startPos).getParentElement();
					int pIndex = e.getElementIndex(startPos);
					if (pIndex != 0 &&
							kit.getElementName(e.getElement(pIndex-1)) == HTML.Tag.DIV &&
							"highlighted".equals(e.getElement(pIndex-1).getAttributes().getAttribute(HTML.Attribute.CLASS)))
					{
						// Extend highlighted block to include this block
						hBlock = (AbstractDocument.BranchElement)e.getElement(pIndex-1);
						AttributeSet attrs = hBlock.getAttributes();
						int start = hBlock.getStartOffset();
						int end = hBlock.getEndOffset();
						eDoc.removeBranchElement(hBlock);
						hBlock = eDoc.insertSubBranchElement((AbstractDocument.BranchElement)hBlock.getParent(),start,end,attrs);
					}
					else if (pIndex < e.getElementCount() - 1 &&
									 kit.getElementName(e.getElement(pIndex + 1)) == HTML.Tag.DIV &&
									 "highlighted".equals(e.getElement(pIndex+1).getAttributes().getAttribute(HTML.Attribute.CLASS)))
					{
						// Extend highlighted block to include this block
						hBlock = (AbstractDocument.BranchElement)e.getElement(pIndex+1);
						AttributeSet attrs = hBlock.getAttributes();
						int start = hBlock.getStartOffset();
						int end = hBlock.getEndOffset();
						eDoc.removeBranchElement(hBlock);
						hBlock = eDoc.insertSubBranchElement((AbstractDocument.BranchElement)hBlock.getParent(),start-1,end-1,attrs);
					}
					else
					{
						// Create a new highlightedblock

						// Find shallowest parent block covering the selection
						e = eDoc.getParagraphElement(startPos);
						do
						{
							e = e.getParentElement();
						} while (e.getEndOffset() < endPos);

						// Enclose selected children in a highlighted block
						SimpleAttributeSet hTagAttrs = new SimpleAttributeSet();
						hTagAttrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.DIV);
						hTagAttrs.addAttribute(HTML.Attribute.CLASS, "highlighted");
						e = eDoc.insertSubBranchElement(
							(DefaultStyledDocument.BranchElement)e,
							editor.getSelectionStart(),
							editor.getSelectionEnd(),
							hTagAttrs
						);

						hBlock = (DefaultStyledDocument.BranchElement)e;
					}
				}
				else if (hBlocks.size() == 1)
				{
					/* One highlighted block found. */
					hBlock = (DefaultStyledDocument.BranchElement)hBlocks.get(0);

					/* If the pre-existing block covers the selection, set a flag to
					 tell stage 2) to un-highlight the selection */
					if (hBlock.getStartOffset() <= startPos &&
							hBlock.getEndOffset() >= endPos) {
						removeHighlight = true;
					}
				}
				else
				{
					/* Multiple highlighted blocks found. Group all child blocks under one div. */

					// Find first common parent of blocks. If blocks are peers,
					// this will be the immediate parent of both lists. For now,
					// we make sure this simple case works, and in more complex
					// cases we just need a valid document structure to result.
					java.util.Iterator it = hBlocks.iterator();
					hBlock = (AbstractDocument.BranchElement)it.next();
					int hStartOffset = hBlock.getStartOffset();
					int hEndOffset = hStartOffset; // dummy value
					for (e = hBlock;
						it.hasNext();
						e = (Element)it.next())
					{
						hEndOffset = e.getEndOffset();
						while (hBlock.getEndOffset() < e.getEndOffset())
						{
							hBlock = (AbstractDocument.BranchElement)hBlock.getParent();
						}

						// Remove the highlighted block from the document tree
						eDoc.removeBranchElement((DefaultStyledDocument.BranchElement)e);
					}

					// Enclose relevant children in a single highlighted block
					SimpleAttributeSet hTagAttrs = new SimpleAttributeSet();
					hTagAttrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.DIV);
					hTagAttrs.addAttribute(HTML.Attribute.CLASS, "highlighted");
					hBlock = eDoc.insertSubBranchElement(
						(DefaultStyledDocument.BranchElement)hBlock,
						hStartOffset,
						hEndOffset-1,
						hTagAttrs
					);
				}

				// Make sure hBlock covers the selection
				while (hBlock.getStartOffset() > startPos)
				{
					/* Expand list to cover start of selection */

					// Absorb previous sibling, or parent if none exists
					AbstractDocument.BranchElement parent;
					int hIndex;

					parent = (AbstractDocument.BranchElement)hBlock.getParent();
					hIndex = parent.getElementIndex(hBlock.getStartOffset());

					SimpleAttributeSet hTagAttrs = new SimpleAttributeSet();
					hTagAttrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.DIV);
					hTagAttrs.addAttribute(HTML.Attribute.CLASS, "highlighted");
					if (hIndex == 0)
					{
						// Absorb parent
						eDoc.removeBranchElement(hBlock);
						hBlock = eDoc.insertSubBranchElement(
							(AbstractDocument.BranchElement)parent.getParent(),
							parent.getStartOffset(),
							parent.getEndOffset()-1,
							hTagAttrs);
					}
					else
					{
						// Absorb previous sibling
						int startOffset = parent.getElement(hIndex-1).getStartOffset();
						int endOffset = hBlock.getEndOffset();
						eDoc.removeBranchElement(hBlock);
						hBlock = eDoc.insertSubBranchElement(parent,startOffset,endOffset,hTagAttrs);
					}
				}
				while (hBlock.getEndOffset() < endPos)
				{
					/* Expand list to cover end of selection */

					// Absorb next sibling, or parent if none exists
					AbstractDocument.BranchElement parent;
					int hIndex;

					parent = (AbstractDocument.BranchElement)hBlock.getParent();
					hIndex = parent.getElementIndex(hBlock.getStartOffset());

					SimpleAttributeSet hTagAttrs = new SimpleAttributeSet();
					hTagAttrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.DIV);
					hTagAttrs.addAttribute(HTML.Attribute.CLASS, "highlighted");
					if (hIndex == parent.getChildCount()-1)
					{
						// Absorb parent

						eDoc.removeBranchElement(hBlock);
						hBlock = eDoc.insertSubBranchElement(
							(AbstractDocument.BranchElement)parent.getParent(),
							parent.getStartOffset(),
							parent.getEndOffset()-1,
							hTagAttrs);
					}
					else
					{
						// Absorb next sibling
						int startOffset = hBlock.getStartOffset();
						int endOffset = parent.getElement(hIndex+1).getEndOffset()-1;
						eDoc.removeBranchElement(hBlock);
						hBlock = eDoc.insertSubBranchElement(parent,startOffset,endOffset,hTagAttrs);
					}
				}

				/*

				2) If pre-existing highlighted block covers selection:

					 - Split, shrink, or remove list as appropriate.

				*/

				// First highlighted element index
				int startIndex = hBlock.getElementIndex(startPos);
				// Last highlighted element index
				int endIndex = hBlock.getElementIndex(endPos);

				if (removeHighlight)
				{
					/* Trim list as a appropriate */
					startIndex = hBlock.getElementIndex(startPos);
					if (startIndex > 0)
						eDoc.splitBranchElement(hBlock,startIndex);
					endIndex = hBlock.getElementIndex(endPos);
					if (endIndex < hBlock.getElementCount()-1)
						eDoc.splitBranchElement(hBlock,endIndex-hBlock.getElementCount()+1);

					// We've removed all the items from the list. Delete it.
					eDoc.removeBranchElement(hBlock);

				}
			}
			finally
			{
				kit.undoMan.end();
				editor.requestFocus();
			}
		}
	}

	public boolean getStateFromCaretLocation(CaretEvent e)
	{
		return isInHighlightedBlock();
	}

	public boolean getStateFromCaretLocation(DocumentEvent e)
	{
		return isInHighlightedBlock();
	}

	public boolean isInHighlightedBlock()
	{
		// Find highlighted block that intersects the selection
		JEditorPane editor = getAssignedEditor();
		EditizeDocument eDoc = (EditizeDocument)editor.getDocument();
		Element root = eDoc.getDefaultRootElement();
		int startPos = editor.getSelectionStart();
		int endPos = editor.getSelectionEnd();
		int pos, depth;
		Element e, hBlock;
		Object name, className;

		pos = startPos;
		hBlock = null;
		while (pos <= endPos)
		{
			e = eDoc.getParagraphElement(pos);
			pos = e.getEndOffset();
			do
			{
				e = e.getParentElement();
				name = EditizeEditorKit.getElementName(e);
				className = e.getAttributes().getAttribute(HTML.Attribute.CLASS);
			} while (name != HTML.Tag.DIV &&
							 "highlighted".equals(className) && e != root);
			if (name == HTML.Tag.DIV && "highlighted".equals(className)) {
				return true;
			}
		}
		return false;
	}
}
