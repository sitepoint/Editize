package com.editize.editorkit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import com.editize.EditizeDocument;

/*
 * Deletes the character of content that follows the
 * current caret position.
 * @see DefaultEditorKit#deleteNextCharAction
 * @see DefaultEditorKit#getActions
 */
class DeleteNextCharAction extends EditizeTextAction {

	DeleteNextCharAction() {
		super(DefaultEditorKit.deleteNextCharAction);
	}

	public void actionPerformed(ActionEvent e)
	{
		JEditorPane target = getEditor(e);
		if ((target != null) && (target.isEditable()))
		{
			boolean beep = true;
			EditizeEditorKit kit = getEditizeEditorKit(target);
			try
			{
				kit.undoMan.start();

				EditizeDocument doc = getEditizeDocument(target);
				Caret caret = target.getCaret();
				int dot = caret.getDot();
				int mark = caret.getMark();
				if (dot != mark)
				{
					// If the selection ends with a newline, do not delete the newline
					// unless selection covers the block or contains ONLY the newline
					Element lastBlock = doc.getParagraphElement(Math.max(dot, mark) - 1);
					if (doc.getText(Math.max(dot, mark) - 1, 1).equals("\n") &&
							Math.min(dot, mark) > lastBlock.getStartOffset() &&
							Math.abs(dot - mark) > 1)
					{
						if (dot > mark) {
							caret.setDot(--dot);
						} else {
							mark--;
						}
					}

					doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
					beep = false;
				}
				else if (dot < doc.getLength())
				{
					// Fail if we're at the end of a table cell
					Element td = EditizeEditorKit.getContainingTableCell(dot, doc);
					if (td == null || dot != td.getEndOffset() - 1)
					{
                                          // Fail if we're just before the start of a table cell (or an entire table!)
                                          Element td2 = EditizeEditorKit.getContainingTableCell(dot + 1, doc);
                                          if (td2 == null || td2 == td)
                                          {
                                            doc.remove(dot, 1);
                                            beep = false;
                                          }
					}
				}
			}
			catch (BadLocationException bl)
			{
			}
			finally
			{
				if (beep) target.getToolkit().beep();
				kit.undoMan.end();
			}
		}
		else Toolkit.getDefaultToolkit().beep();
	}
}
