package com.editize.editorkit;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;



public class TableAction extends BooleanStateAwareTextAction
{
	public TableAction()
	{
		super("InsertTable");
	}

	public void actionPerformed(ActionEvent evt)
	{
		final JEditorPane target = getEditor(evt);

		if (target != null)
		{
			TableDialog dlg = new TableDialog(target);

			Dimension screenSize =
					Toolkit.getDefaultToolkit().getScreenSize();
			dlg.setLocation(
					(screenSize.width - dlg.getWidth()) / 2,
					(screenSize.height - dlg.getHeight()) / 2
					);
			dlg.show();
			if (dlg.getCloseOption() == dlg.OK)
			{
				String html = "<table";

				String className = dlg.getTableClass();
				if (className != null)
					html += " class=\"" + className + "\"";

				html += dlg.getTableBorder().equals("0") ?
						" border=\"1\" trueborder=\"0\" style=\"border-style: solid; border-color: #cccccc;\"" :
						" border=\"" + dlg.getTableBorder() + "\"";

				String align = dlg.getTableAlign();
				if (align != null)
					html += " align=\"" + align + "\"";

				String width = dlg.getTableWidth();
				if (width != null)
					html += " width=\"" + width + "\"";

				String height = dlg.getTableHeight();
				if (height != null)
					html += " height=\"" + height + "\"";

				html += ">";

				for (int row = Integer.parseInt(dlg.getRows()); row > 0; row--)
				{
					html += "<tr>";
					for (int col = Integer.parseInt(dlg.getCols()); col > 0; col--)
					{
						html += "<td></td>";
					}
					html += "</tr>";
				}
				html += "</table>";

//				HTML.Tag container = HTML.Tag.BODY;
//				Element cell = null;
//				if ((cell = EditizeEditorKit.getContainingTableCell(
//					target.getCaretPosition(),
//					target.getDocument())) != null)
//				{
//					container = (HTML.Tag) cell.getAttributes().getAttribute(
//							StyleConstants.NameAttribute);
//				}

				getEditizeEditorKit(target).insertBlockHtml(getEditizeDocument(target),
						html, target.getCaretPosition());
			}

			// This doesn't work when the dialog is cancelled... (fixed?)
			target.requestFocus();
			// ...so we use a delayed call instead.
//			SwingUtilities.invokeLater(new Runnable()
//			{
//				public void run()
//				{
//					target.requestFocus();
//				}
//			});
		}
	}

	public boolean getStateFromCaretLocation(CaretEvent evt)
	{
		return false;
	}

	public boolean getStateFromCaretLocation(DocumentEvent evt)
	{
		return false;
	}
}
