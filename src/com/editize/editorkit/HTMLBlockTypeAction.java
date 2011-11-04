package com.editize.editorkit;

import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.AbstractDocument.*;
import javax.swing.text.html.HTML;

import com.editize.EditizeDocument;

/**
 * An Action that assigns a particular block tag to the current paragraph of
 * an EditizeDocument in a JEditorPane. The HTML.Tag is passed to the constructor.
 *
 * @author: Kevin Yank
 */
public class HTMLBlockTypeAction extends EditizeTextAction
{
	HTML.Tag tag;
	AttributeSet tagAttributes, impliedAttributes, paragraphAttributes;

	/**
	 * If true, block requires a HTML.Tag.IMPLIED block inside it to be
	 * properly displayed.
	 */
	boolean isPre = false;

	/**
	 * The constructor that allows for a Tag to be specified.
	 *
	 * @param tag The Tag to be applied by this action. tag.isBlock() must be true.
	 */
	public HTMLBlockTypeAction(HTML.Tag tag)
	{
		this(tag,tag.toString());
	}

	public HTMLBlockTypeAction(HTML.Tag tag, String displayName)
	{
		super(displayName);
		if (!tag.isBlock())
			throw new IllegalArgumentException(tag.toString() +
																				 " is not a block tag.");
		this.tag = tag;

		// Prepare tag attribute sets
		MutableAttributeSet attrs = new SimpleAttributeSet();
		attrs.addAttribute(StyleConstants.NameAttribute, tag);
		tagAttributes = attrs;
		attrs = new SimpleAttributeSet();
		attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.IMPLIED);
		impliedAttributes = attrs;
		attrs = new SimpleAttributeSet();
		attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.P);
		paragraphAttributes = attrs;

		// PRE is a special case requiring IMPLIED elements inside it
		// to be correctly handled
		if (tag == HTML.Tag.PRE)
			isPre = true;
}

	/**
	 * Applies the tag to the currently selected paragraph(s) of the EditizeDocument
	 * in the JEditorPane responsible for an ActionEvent. If no selection is present,
	 * the paragraph containing the cursor gets the Tag.
	 *
	 * <p>Note: If multiple paragraphs are selected, each tag change will count as one
	 * UndoableEdit and DocumentEvent. To combine them all into a single edit would
	 * require an new version of the replaceBranchElement function provided by the
	 * EditizeDocument class and used by this method to perform the updates. If this
	 * behavior becomes a serious issue, it might be worth implementing a version of
	 * replaceBranchElement that takes a range of the document and automatically
	 * combines all the updates into a single edit.
	 *
	 * @param evt An ActionEvent that points to the JEditorPane to which the action
	 *            should be applied.
	 */
	public void actionPerformed(ActionEvent evt)
	{
		JEditorPane editor = getEditor(evt);
		if (editor != null)
			try {
				EditizeDocument doc = getEditizeDocument(editor);
				EditizeEditorKit kit = getEditizeEditorKit(editor);

				kit.undoMan.start();

				EditizeDocument eDoc = (EditizeDocument)doc;

				int p0 = editor.getSelectionStart();
				int p1 = editor.getSelectionEnd();

				BranchElement paragraph;
				do {
					paragraph = (BranchElement)doc.getParagraphElement(p0);

					// If we hit a table cell, create an IMPLIED sub-block to receive tag
					if (EditizeEditorKit.getElementName(paragraph) == HTML.Tag.TD ||
							EditizeEditorKit.getElementName(paragraph) == HTML.Tag.TH) {
						paragraph = eDoc.insertSubBranchElement(paragraph,
								paragraph.getStartOffset(), paragraph.getEndOffset(),
								impliedAttributes);
					}

					// If we hit an implied paragraph with a PRE as a parent,
					// we need to ditch the PRE and make its children into
					// paragraphs.
					if (EditizeEditorKit.getElementName(paragraph) == HTML.Tag.IMPLIED &&
							EditizeEditorKit.getElementName(paragraph.getParentElement()) == HTML.Tag.PRE) {
						// Remove parent block and replace implied paragraph children with actual paragraphs
						BranchElement parent = (BranchElement)paragraph.getParent();
						java.util.Enumeration children = parent.children();
						while (children.hasMoreElements()) {
							paragraph = (BranchElement)children.nextElement();
							if (paragraph.getAttributes().getAttribute(StyleConstants.
									NameAttribute) == HTML.Tag.IMPLIED) {
								eDoc.replaceBranchElement(paragraph, paragraphAttributes);
								/**
								 * @todo Normalize spaces when converting a block from PRE.
								 */
							}
						}
						eDoc.removeBranchElement(parent);
						continue;
					}
					paragraph = eDoc.replaceBranchElement(paragraph, tagAttributes);
					if (isPre) {
						eDoc.insertSubBranchElement(paragraph,
								paragraph.getStartOffset(),
								paragraph.getEndOffset(),
								impliedAttributes);
					}
					p0 = paragraph.getEndOffset() + 1;
				}
				while (p0 <= p1);

				kit.undoMan.end();

				editor.requestFocus();
			}
			catch (IllegalArgumentException ex) {
				editor.getToolkit().beep();
			}
	}

	public HTML.Tag getTag()
	{
		return tag;
	}

	/**
	 * Obtains the name of the Style to be applied.
	 *
	 * @return The name of the Style to be applied.
	 */
	public String toString() {
		return (String)getValue(Action.NAME);
	}

	public boolean equals(Object tag)
	{
		if (tag instanceof HTML.Tag)
			return this.tag == tag;
		else return super.equals(tag);
	}
}
