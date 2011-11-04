package com.editize.editorkit;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.StyleConstants;
import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import java.awt.Toolkit;
import javax.swing.text.Document;
import com.editize.EditizeDocument;
import javax.swing.text.Element;
import javax.swing.text.AbstractDocument.BranchElement;

public class IndentAction extends EditizeTextAction
{
	private AttributeSet bq;

	public IndentAction()
	{
		super("indent-block");

		bq = new SimpleAttributeSet();
		((MutableAttributeSet)bq).addAttribute(StyleConstants.NameAttribute,
																					 HTML.Tag.BLOCKQUOTE);
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditorPane target = getEditor(evt);
		if (target == null)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		else if (!target.isEditable())
			target.getToolkit().beep();
		else
		{
			int startPos = target.getSelectionStart();
			int endPos = target.getSelectionEnd();
			Document doc = target.getDocument();
			if (doc instanceof EditizeDocument)
			{
				EditizeDocument eDoc = (EditizeDocument) doc;
				// Find deepest parent block covering the selection
				Element e = eDoc.getParagraphElement(startPos);
				do
				{
					e = e.getParentElement();
				}
				while (e.getEndOffset() < endPos);

				// Enclose selected children in a blockquote
				e = eDoc.insertSubBranchElement((BranchElement)e, startPos, endPos, bq);
			}
		}
		target.requestFocus();
	}
}
