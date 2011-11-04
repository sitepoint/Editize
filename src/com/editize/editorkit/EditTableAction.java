package com.editize.editorkit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;

import com.editize.EditizeDocument;
import javax.swing.text.AbstractDocument.BranchElement;

/**
 * Displays a dialog that allows the end user to edit a table's properties.
 *
 * @author Kevin Yank
 */

public class EditTableAction extends BooleanStateAwareTextAction
{
	private EditTableDialog dlg;

	public EditTableAction()
	{
		super("edit-table");
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
			Element table = doc.getParagraphElement(target.
					getSelectionStart());

			// Detect table
			while (table != null &&
				   EditizeEditorKit.getElementName(table) != HTML.Tag.TABLE)
			{
				table = table.getParentElement();
			}
			if (table == null) {
				// Not inside a table!
				target.getToolkit().beep();
				return;
			}

			// Display table editing dialog
			if (dlg == null) dlg = new EditTableDialog(target);
			dlg.init(table);
			dlg.show();

			if (dlg.getCloseOption() == dlg.OK) {
				MutableAttributeSet atts = new SimpleAttributeSet();

				// Tag
				atts.addAttribute(StyleConstants.NameAttribute, HTML.Tag.TABLE);

				// Border
				int border;
				try {
					border = Integer.parseInt(dlg.getTableBorder());
				} catch (NumberFormatException ex) {
					border = 0;
				}
				if (border == 0) {
					atts.addAttribute("trueborder", "" + border);
					atts.addAttribute(HTML.Attribute.BORDER, "1");
					atts.addAttributes(doc.getStyleSheet().getDeclaration("border-style: solid; border-color: #cccccc;"));
				} else {
					atts.addAttribute(HTML.Attribute.BORDER, "" + border);
				}

				// Width
				String width = dlg.getTableWidth();
				if (width != null)
					atts.addAttribute(HTML.Attribute.WIDTH, width);

				// Height
				String height = dlg.getTableHeight();
				if (height != null)
					atts.addAttribute(HTML.Attribute.HEIGHT, height);

				// Cellpadding
				String pad = dlg.getTablePadding();
				if (pad != null)
					atts.addAttribute(HTML.Attribute.CELLPADDING, pad);

				// Cellspacing
				String space = dlg.getTableSpacing();
				if (space != null)
					atts.addAttribute(HTML.Attribute.CELLSPACING, space);

				// Align
				String align = dlg.getTableAlign();
				if (align != null)
					atts.addAttribute(HTML.Attribute.ALIGN, align);

				// Summary
				String sum = dlg.getTableSummary();
				if (sum != null)
					atts.addAttribute("summary", sum);

				// Class
				String className = dlg.getTableClass();
				if (className != null)
					atts.addAttribute("class", className);

				doc.replaceBranchElement((BranchElement)table, atts);
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
