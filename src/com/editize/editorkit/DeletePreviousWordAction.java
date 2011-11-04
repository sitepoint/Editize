package com.editize.editorkit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import com.editize.EditizeDocument;

class DeletePreviousWordAction extends EditizeTextAction
{
	DeletePreviousWordAction()
	{
		super("delete-previous-word");
	}

	public void actionPerformed(ActionEvent e)
	{
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
				if (dot != mark) {
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

					// If a selection exists, just remove it.
					doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
					beep = false;
				} else if (dot > 0) {
					// Fail if we're at the start of a table cell
					Element td = EditizeEditorKit.getContainingTableCell(dot,doc);
					if (td == null || dot != td.getStartOffset())
					{
                                          // Fail if we're just after the end of a table cell (or an entire table!)
                                          Element td2 = EditizeEditorKit.getContainingTableCell(dot - 1, doc);
                                          if (td2 == null || td2 == td)
                                          {
                                            // If no selection, and we're not at the start of the document,
                                            // delete whitespace then a word, or a word then spaces.
                                            int pos = dot;
                                            boolean whitespacefound = false;
                                            // First, delete any whitespace characters preceding the caret
                                            while (pos > 0 && isWhiteSpaceChar(doc.getText(pos - 1, 1)) &&
                                                   (td == null || pos != td.getStartOffset()))
                                            {
                                              whitespacefound = true;
                                              pos--;
                                            }
                                            // Then delete any non-whitespace characters that follow.
                                            while (pos > 0 && !isWhiteSpaceChar(doc.getText(pos - 1, 1)) &&
                                                   (td == null || pos != td.getStartOffset())) pos--;
                                            // strip any following *spaces* if no whitespace already found.
                                            if (!whitespacefound)while (pos > 0 &&
                                                                        doc.getText(pos - 1, 1).equals(" ") &&
                                                                        (td == null ||
                                                                         pos != td.getStartOffset())) pos--;
                                            doc.remove(pos, dot - pos);
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

	private boolean isWhiteSpaceChar(String str)
	{
		return ( str.equals(" ") || str.equals("\t") || str.equals("\n") || str.equals("\r") );
	}
}
