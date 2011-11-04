package com.editize.editorkit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import com.editize.EditizeDocument;

/*
 * Deletes the character of content that precedes the
 * current caret position.
 * @see DefaultEditorKit#deletePrevCharAction
 * @see DefaultEditorKit#getActions
 */
class DeletePrevCharAction extends EditizeTextAction
{

	/**
	 * Creates this object with the appropriate identifier.
	 */
	DeletePrevCharAction()
	{
		super(DefaultEditorKit.deletePrevCharAction);
	}

	/**
	 * The operation to perform when this action is triggered.
	 *
	 * @param e the action event
	 */
	public void actionPerformed(ActionEvent e) {
		JEditorPane target = getEditor(e);
		if ((target != null) && (target.isEditable()))
		{
			EditizeEditorKit kit = getEditizeEditorKit(target);
			boolean beep = true;
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
				} else if (dot > 1) { // 1 because 0 is the HTML document header
					// Fail if we're at the start of a table cell
					Element td = EditizeEditorKit.getContainingTableCell(dot,doc);
					if (td == null || dot != td.getStartOffset())
					{
                                          // Fail if we're just after the end of a table cell (or an entire table!)
                                          Element td2 = EditizeEditorKit.getContainingTableCell(dot - 1, doc);
                                          if (td2 == null || td2 == td)
                                          {
                                            doc.remove(dot - 1, 1);
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
		} else Toolkit.getDefaultToolkit().beep();
	}
}
