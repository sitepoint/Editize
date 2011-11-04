package com.editize.editorkit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.text.AbstractDocument.*;
import javax.swing.text.html.HTML;

import com.editize.EditizeDocument;

/**
 * Displays a dialog that allows the end user to edit a table cell's properties.
 *
 * @author Kevin Yank
 */

public class EditCellAction extends BooleanStateAwareTextAction
{
	private EditCellDialog dlg;

	public EditCellAction()
	{
		super("edit-cell");
	}

	/**
	 * Performs the action.
	 * @param e ActionEvent
	 */
	public void actionPerformed(ActionEvent e)
	{
		final JEditorPane target = getAssignedEditor(e);
		if (target != null)
		{
			if ((! target.isEditable()) || (! target.isEnabled()))
			{
				target.getToolkit().beep();
				return;
			}

			EditizeDocument doc = getEditizeDocument(target);
			Element cell = EditizeEditorKit.getContainingTableCell(
					target.getSelectionStart(), doc);
			if (cell == null) {
				// Not inside a table cell!
				target.getToolkit().beep();
				return;
			}

			// Display image selection dialog
			if (dlg == null) dlg = new EditCellDialog(target);
			dlg.init(cell);
			dlg.show();

			if (dlg.getCloseOption() == dlg.OK) {
				MutableAttributeSet atts = new SimpleAttributeSet();

				// Tag
				boolean header = dlg.isCellHeader();
				if (header)
					atts.addAttribute(StyleConstants.NameAttribute, HTML.Tag.TH);
				else
					atts.addAttribute(StyleConstants.NameAttribute, HTML.Tag.TD);

				// Horizontal Align
				String hAlign = dlg.getHAlign();
				if (hAlign != null)
					atts.addAttribute(HTML.Attribute.ALIGN, hAlign);

				// Vertical Align
				String vAlign = dlg.getVAlign();
				if (vAlign != null)
					atts.addAttribute(HTML.Attribute.VALIGN, vAlign);

//				doc.setTagAttributes(cell.getStartOffset(),
//									 (HTML.Tag)EditizeEditorKit.getElementName(cell),
//									 atts, true);
				doc.replaceBranchElement((BranchElement)cell, atts);
			}

			target.requestFocus();
		}
		else
		{
			Toolkit.getDefaultToolkit().beep();
		}
	}
	public boolean getStateFromCaretLocation(DocumentEvent e)
	{
		/**@todo Implement this com.editize.editorkit.BooleanStateAwareTextAction abstract method*/
		return false;
	}
	public boolean getStateFromCaretLocation(CaretEvent e)
	{
		/**@todo Implement this com.editize.editorkit.BooleanStateAwareTextAction abstract method*/
		return false;
	}
}
