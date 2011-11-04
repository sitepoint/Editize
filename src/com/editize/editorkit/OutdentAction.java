package com.editize.editorkit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import javax.swing.text.*;
import javax.swing.text.html.HTML;

import com.editize.EditizeDocument;

public class OutdentAction extends EditizeTextAction
{
	public OutdentAction()
	{
		super("outdent-block");
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
				// Get deepest blockquote element that covers the selection
				EditizeDocument eDoc = (EditizeDocument) doc;
				// Find deepest parent block covering the selection
				Element e = eDoc.getParagraphElement(startPos);
				do
				{
					e = e.getParentElement();
					if (e == null) return;
				}
				while (e.getEndOffset() < endPos ||
							 e.getAttributes().getAttribute(StyleConstants.NameAttribute) !=
							 HTML.Tag.BLOCKQUOTE);
				eDoc.removeBranchElement((EditizeDocument.BranchElement)e);
			}
		}
		target.requestFocus();
	}
}
