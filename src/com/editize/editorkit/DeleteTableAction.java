package com.editize.editorkit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import com.editize.*;

public class DeleteTableAction extends EditizeTextAction
{
	public DeleteTableAction()
	{
		super("table-delete");
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditorPane target = getEditor(evt);
		if (target != null) try
		{
			EditizeDocument doc = getEditizeDocument(target);
			EditizeEditorKit kit = getEditizeEditorKit(target);
			Element cell = kit.getContainingTableCell(target.getCaretPosition(),doc);
			if (cell != null)
			{
				Element table = cell.getParentElement().getParentElement();
				int start = table.getStartOffset();
				int end = table.getEndOffset();

				kit.startCompoundEdit();
				doc.remove(start,end-start);
				kit.endCompoundEdit();
			}
		}
		catch (IllegalArgumentException ex)
		{
			target.getToolkit().beep();
		}
		catch (BadLocationException ex)
		{
			target.getToolkit().beep();
		}
		else Toolkit.getDefaultToolkit().beep();
	}
}
