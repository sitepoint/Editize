package com.editize.editorkit;

import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;

import com.editize.EditizeDocument;

/**
 * Places a line/paragraph break into the document.
 * If there is a selection, it is removed before
 * the break is added.
 *
 * If the cursor is sitting in a list item that has no content,
 * this action should instead remove the list styling from this block.
 *
 * @see DefaultEditorKit#insertBreakAction
 * @see DefaultEditorKit#getActions
 */
public class InsertBreakAction
		extends EditizeTextAction
{

	/**
	 * Creates this object with the appropriate identifier.
	 */
	public InsertBreakAction()
	{
		super(DefaultEditorKit.insertBreakAction);
	}

	/**
	 * The operation to perform when this action is triggered.
	 *
	 * @param e the action event
	 */
	public void actionPerformed(ActionEvent e)
	{
		JEditorPane target = getEditor(e);
		if (target != null)
		{
			if ( (!target.isEditable()) || (!target.isEnabled()))
			{
				target.getToolkit().beep();
				return;
			}

			// If we're in a list item
			EditizeDocument doc = (EditizeDocument)target.getDocument();
			Element li = doc.findElementMatchingTag(target.getCaretPosition(), HTML.Tag.LI);
			if (li != null)
			{
				// If the list item contains only one character (a carriage return)
				if (li.getEndOffset() - li.getStartOffset() < 2)
				{
					// Use a surrogate action to remove the list item
					Element list = li.getParentElement();
					Action surrogate = new BSASimpleListAction((HTML.Tag)EditizeEditorKit.
							getElementName(list), "surrogate");
					surrogate.actionPerformed(e);
					return;
				}
			}

			// If we're in an unmarked block or table cell
			Element p = doc.getParagraphElement(target.getCaretPosition());
			if (EditizeEditorKit.getElementName(p) == HTML.Tag.IMPLIED ||
					EditizeEditorKit.getElementName(p) == HTML.Tag.TD ||
					EditizeEditorKit.getElementName(p) == HTML.Tag.TH)
			{
				p = p.getParentElement();
				if (EditizeEditorKit.getElementName(p) == HTML.Tag.BODY ||
						EditizeEditorKit.getElementName(p) == HTML.Tag.TR ||
						EditizeEditorKit.getElementName(p) == HTML.Tag.TD ||
						EditizeEditorKit.getElementName(p) == HTML.Tag.TH) {
					// Insert a <br> tag instead
					Action surrogate = new InsertLineBreakAction();
					surrogate.actionPerformed(e);
					return;
				}
			}

			EditizeEditorKit kit = getEditizeEditorKit(target);
			kit.undoMan.start();
			target.replaceSelection("\n");
			kit.undoMan.end();
		}
	}
}
