package com.editize.editorkit;

import java.util.*;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.AbstractDocument.*;
import javax.swing.text.html.*;

import com.editize.*;

/**
 * Action responsible for HTML ordered and unordered lists.
 */
public class BSASimpleListAction extends BooleanStateAwareTextAction
{
	private HTML.Tag listTag;

	/**
	 * Constructor specifies list type and action name.
	 *
	 * @param listTag The list type. Should be HTML.Tag.UL or HTML.Tag.OL.
	 * @param name The action name.
	 */
	public BSASimpleListAction(HTML.Tag listTag, String name)
	{
		super(name);
		this.listTag = listTag;
	}

	/**
	 * Toggles the bulleted attribute.
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

			Element e;
			DefaultStyledDocument.BranchElement listBlock;
			Vector listBlocks;
			int pos, depth, curDepth;
			Object name;

			/*

			1) Make sure selection is inside a single list:

				 - identify existing list block,
				 - expand existing list block,
				 - combine two or more peer list blocks, or
				 - create a new list block

			*/

			// Find the shallowest list block(s) that intersect
			// the selection (if any).
			pos = startPos;
			depth = Integer.MAX_VALUE;
			listBlocks = new Vector();
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
				} while (name != HTML.Tag.UL && name != HTML.Tag.OL && e != root && curDepth < depth);
				if (name == HTML.Tag.UL || name == HTML.Tag.OL)
				{
					if (curDepth < depth)
					{
						depth = curDepth;
						listBlocks = new Vector();
						listBlocks.addElement(e);
					}
					else if (curDepth == depth && !listBlocks.contains(e))
					{
						listBlocks.addElement(e);
					}
				}
			}

			EditizeEditorKit kit = getEditizeEditorKit(editor);

			try
			{
				kit.undoMan.start();

				if (listBlocks.size() == 0)
				{
					/* No list blocks found. */

					// Are we next to a list block?
					e = eDoc.getParagraphElement(startPos).getParentElement();
					int pIndex = e.getElementIndex(startPos);
					if (pIndex != 0 && e.getElement(pIndex-1).getAttributes().getAttribute(StyleConstants.NameAttribute) == listTag)
					{
						// Extend listblock to include this block
						listBlock = (BranchElement)e.getElement(pIndex-1);
						AttributeSet attrs = listBlock.getAttributes();
						int start = listBlock.getStartOffset();
						int end = listBlock.getEndOffset();
						eDoc.removeBranchElement(listBlock);
						listBlock = eDoc.insertSubBranchElement((BranchElement)listBlock.getParent(),start,end,attrs);
					}
					else if (pIndex != e.getElementCount()-1 && e.getElement(pIndex+1).getAttributes().getAttribute(StyleConstants.NameAttribute) == listTag)
					{
						// Extend listblock to include this block
						listBlock = (BranchElement)e.getElement(pIndex+1);
						AttributeSet attrs = listBlock.getAttributes();
						int start = listBlock.getStartOffset();
						int end = listBlock.getEndOffset();
						eDoc.removeBranchElement(listBlock);
						listBlock = eDoc.insertSubBranchElement((BranchElement)listBlock.getParent(),start-1,end-1,attrs);
					}
					else
					{
						// Create a new listblock

						// Find shallowest parent block covering the selection
						e = eDoc.getParagraphElement(startPos);
						do
						{
							e = e.getParentElement();
						} while (e.getEndOffset() < endPos);

						// Enclose selected children in a list block
						SimpleAttributeSet listTagAttrs = new SimpleAttributeSet();
						listTagAttrs.addAttribute(StyleConstants.NameAttribute, listTag);
						e = eDoc.insertSubBranchElement(
							(DefaultStyledDocument.BranchElement)e,
							editor.getSelectionStart(),
							editor.getSelectionEnd(),
							listTagAttrs
						);

						listBlock = (DefaultStyledDocument.BranchElement)e;
					}
				}
				else if (listBlocks.size() == 1)
				{
					/* One list block found. Use it. */

					listBlock = (DefaultStyledDocument.BranchElement)listBlocks.get(0);

					// Make sure list is of correct type
					if (listBlock.getAttribute(StyleConstants.NameAttribute) != listTag)
					{
						int start = listBlock.getStartOffset();
						int end = listBlock.getEndOffset();
						SimpleAttributeSet sas = new SimpleAttributeSet();
						sas.addAttribute(StyleConstants.NameAttribute,listTag);
						eDoc.removeBranchElement(listBlock);
						listBlock = eDoc.insertSubBranchElement((BranchElement)listBlock.getParent(),start,end-1,sas);
						return;
					}

				}
				else
				{
					/* Multiple list blocks found. Group all child blocks under one list. */

					// Find first common parent of lists. If lists are peers,
					// this will be the immediate parent of both lists. For now,
					// we make sure this simple case works, and in more complex
					// cases we just need a valid document structure to result.
					java.util.Iterator it = listBlocks.iterator();
					listBlock = (BranchElement)it.next();
					int listStartOffset = listBlock.getStartOffset();
					int listEndOffset = listStartOffset; // dummy value
					for (e = listBlock;
						it.hasNext();
						e = (Element)it.next())
					{
						listEndOffset = e.getEndOffset();
						while (listBlock.getEndOffset() < e.getEndOffset())
						{
							listBlock = (BranchElement)listBlock.getParent();
						}

						// Remove the list from the document tree
						eDoc.removeBranchElement((DefaultStyledDocument.BranchElement)e);
					}

					// Enclose relevant children in a single list block
					SimpleAttributeSet listTagAttrs = new SimpleAttributeSet();
					listTagAttrs.addAttribute(StyleConstants.NameAttribute, listTag);
					listBlock = eDoc.insertSubBranchElement(
						(DefaultStyledDocument.BranchElement)listBlock,
						listStartOffset,
						listEndOffset-1,
						listTagAttrs
					);
				}

				// Make sure listBlock covers the selection
				while (listBlock.getStartOffset() > startPos)
				{
					/* Expand list to cover start of selection */

					// Absorb previous sibling, or parent if none exists
					BranchElement parent;
					int listIndex;

					parent = (BranchElement)listBlock.getParent();
					listIndex = parent.getElementIndex(listBlock.getStartOffset());

					SimpleAttributeSet listTagAttrs = new SimpleAttributeSet();
					listTagAttrs.addAttribute(StyleConstants.NameAttribute, listTag);
					if (listIndex == 0)
					{
						// Absorb parent
						eDoc.removeBranchElement(listBlock);
						listBlock = eDoc.insertSubBranchElement(
							(BranchElement)parent.getParent(),
							parent.getStartOffset(),
							parent.getEndOffset()-1,
							listTagAttrs);
					}
					else
					{
						// Absorb previous sibling
						int startOffset = parent.getElement(listIndex-1).getStartOffset();
						int endOffset = listBlock.getEndOffset();
						eDoc.removeBranchElement(listBlock);
						listBlock = eDoc.insertSubBranchElement(parent,startOffset,endOffset,listTagAttrs);
					}
				}
				while (listBlock.getEndOffset() < endPos)
				{
					/* Expand list to cover end of selection */

					// Absorb next sibling, or parent if none exists
					BranchElement parent;
					int listIndex;

					parent = (BranchElement)listBlock.getParent();
					listIndex = parent.getElementIndex(listBlock.getStartOffset());

					SimpleAttributeSet listTagAttrs = new SimpleAttributeSet();
					listTagAttrs.addAttribute(StyleConstants.NameAttribute, listTag);
					if (listIndex == parent.getChildCount()-1)
					{
						// Absorb parent

						eDoc.removeBranchElement(listBlock);
						listBlock = eDoc.insertSubBranchElement(
							(BranchElement)parent.getParent(),
							parent.getStartOffset(),
							parent.getEndOffset()-1,
							listTagAttrs);
					}
					else
					{
						// Absorb next sibling
						int startOffset = listBlock.getStartOffset();
						int endOffset = parent.getElement(listIndex+1).getEndOffset()-1;
						eDoc.removeBranchElement(listBlock);
						listBlock = eDoc.insertSubBranchElement(parent,startOffset,endOffset,listTagAttrs);
					}
				}

				/*

				2) If selection is covered by items in the list(s):

					 - Remove selected items from the list,
					 - Split, shrink, or remove list as appropriate.

					 Else:

					 - Make sure all direct child blocks of list
						 are list items.

				*/

				// First list element index
				int startIndex = listBlock.getElementIndex(startPos);
				// Last list element index
				int endIndex = listBlock.getElementIndex(endPos);

				if (isCoveredByListItems(listBlock,startPos,endPos))
				{
					/* Remove selected items from the list */
					Vector liTags = new Vector(endIndex - startIndex + 1);
					for (int i=startIndex; i<=endIndex; i++)
					{
						liTags.addElement(listBlock.getElement(i));
					}
					Iterator liIterator = liTags.iterator();
					Element li;
					while (liIterator.hasNext())
					{
						li = (Element)liIterator.next();

						// If list item contains content,
						// replace the list item tag with
						// a paragraph.
						if (li.getElement(0) instanceof LeafElement)
						{
							SimpleAttributeSet pAttrs = new SimpleAttributeSet();
							pAttrs.addAttribute(
								StyleConstants.NameAttribute,
								HTML.Tag.P
							);
							eDoc.replaceBranchElement((BranchElement)li,pAttrs);
						}
						// If list item contains blocks,
						// drop the list item tag.
						else
						{
							// If the list item contains p-implieds,
							// replace them with P's.
							Element liContent;
							SimpleAttributeSet pAttrs = new SimpleAttributeSet();
							pAttrs.addAttribute(StyleConstants.NameAttribute,HTML.Tag.P);
							for (int i=0; i<li.getElementCount(); i++)
							{
								liContent = li.getElement(i);
								if (liContent.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMPLIED)
								{
									eDoc.replaceBranchElement((BranchElement)liContent,pAttrs);
								}
							}
							eDoc.removeBranchElement((BranchElement)li);
						}
					}

					/* Trim list as a appropriate */
					startIndex = listBlock.getElementIndex(startPos);
					if (startIndex > 0)
						eDoc.splitBranchElement(listBlock,startIndex);
					endIndex = listBlock.getElementIndex(endPos);
					if (endIndex < listBlock.getElementCount()-1)
						eDoc.splitBranchElement(listBlock,endIndex-listBlock.getElementCount()+1);

					// We've removed all the items from the list. Delete it.
					eDoc.removeBranchElement(listBlock);

				}
				else
				{
					/* Make elegible blocks into list items. */

					// Attributes for creating list item tags.
					SimpleAttributeSet liTagAttributes = new SimpleAttributeSet();
					liTagAttributes.addAttribute(StyleConstants.NameAttribute, HTML.Tag.LI);
					// Attributes for creating implied paragraphs.
					SimpleAttributeSet impliedPTagAttributes = new SimpleAttributeSet();
					impliedPTagAttributes.addAttribute(StyleConstants.NameAttribute, HTML.Tag.IMPLIED);
					DefaultStyledDocument.BranchElement licontents;
					HTML.Tag blockTag;
					for (int i=startIndex; i<=endIndex; i++)
					{
						licontents = (DefaultStyledDocument.BranchElement)listBlock.getElement(i);
						blockTag = (HTML.Tag)licontents.getAttributes().getAttribute(StyleConstants.NameAttribute);
						if (blockTag == HTML.Tag.LI) continue; // Already a list item
						// Paragraphs, Implied Paragraphs, and Blockquotes get replaced by the list item, other blocks are encapsulated.
						if (blockTag == HTML.Tag.P || blockTag == HTML.Tag.IMPLIED || blockTag == HTML.Tag.BLOCKQUOTE)
							eDoc.replaceBranchElement(licontents,impliedPTagAttributes);
						eDoc.insertSubBranchElement(listBlock,licontents.getStartOffset(),licontents.getEndOffset()-1,liTagAttributes);
					}
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
		return isSelectionCoveredByList();
	}

	public boolean getStateFromCaretLocation(DocumentEvent e)
	{
		return isSelectionCoveredByList();
	}

	public boolean isSelectionCoveredByList()
	{
		// Find the shallowest list block that intersects
		// the selection (if any).
		JEditorPane editor = getAssignedEditor();
		EditizeDocument eDoc = (EditizeDocument)editor.getDocument();
		Element root = eDoc.getDefaultRootElement();
		int startPos = editor.getSelectionStart();
		int endPos = editor.getSelectionEnd();
		int pos, depth, curDepth;
		Element e, listBlock;
		Object name;

		pos = startPos;
		depth = Integer.MAX_VALUE;
		listBlock = null;
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
			} while (name != listTag && e != root && curDepth < depth);
			if (curDepth < depth && name == listTag)
			{
				depth = curDepth;
				listBlock = e;
			}
		}

		if (listBlock == null) return false;

		return isCoveredByListItems((BranchElement)listBlock,startPos,endPos);
	}

	public boolean isListItem(Element block)
	{
		Object name;
		name = block.getAttributes().getAttribute(StyleConstants.NameAttribute);
		return name == HTML.Tag.LI;

	}

	public boolean isCoveredByListItems(
		BranchElement listBlock, int startPos, int endPos)
	{
		int curIndex = listBlock.getElementIndex(startPos);
		int endIndex = listBlock.getElementIndex(endPos-1);
		boolean isCovered = true;
		while (curIndex <= endIndex)
		{
			Element e = listBlock.getElement(curIndex++);
			if (!isListItem(e))
			{
				isCovered = false;
				break;
			}
		}
		return isCovered;
	}
}
