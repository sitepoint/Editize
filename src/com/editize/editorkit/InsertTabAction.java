package com.editize.editorkit;

import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTMLEditorKit.*;

/**
 * Places a tab character into the document. If there
 * is a selection, it is removed before the tab is added.
 * If the cursor is within a table, moves the cursor to
 * the next table cell instead. If the cursor is in the
 * last cell of the table, creates a new row instead!
 */
public class InsertTabAction extends EditizeTextAction
{

	/**
	 * Creates this object with the appropriate identifier.
	 */
	public InsertTabAction()
	{
		super(DefaultEditorKit.insertTabAction);
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

			// If we're in a table cell
			Element curCell = EditizeEditorKit.getContainingTableCell(target.
					getCaretPosition(), target.getDocument());
			if (curCell != null)
			{
				// Get the next cell
				Element nextCell = null;
				Element row = curCell.getParentElement();
				boolean gaveUp = false;

				// If we're at the end of the row
				if (row.getEndOffset() <= curCell.getEndOffset())
				{
					// Get the next row
					Element table = row.getParentElement();
					if (table.getEndOffset() <= row.getEndOffset())
						gaveUp = true; // No more rows
					else
						row = table.getElement(table.getElementIndex(row.getEndOffset()));
				}
				if (!gaveUp)
					nextCell = row.getElement(row.getElementIndex(curCell.getEndOffset()));

				if (nextCell == null)
				{
					addNewRow(e,row);
				}
				else
				{
					target.setCaretPosition(nextCell.getStartOffset());
				}
			}
			else target.replaceSelection("\t");
		}
	}

	/**
	 * Adds a new row to the end of the table, using the last row in the
	 * table as a template.
	 *
	 * @param evt The ActionEvent that triggered this process.
	 * @param row The last row of the table.
	 */
	private void addNewRow(ActionEvent evt, Element row)
	{
		///TODO: Make this handle rowspanned cells protruding into the last row!

		JEditorPane target = getEditor(evt);
		EditizeEditorKit kit = getEditizeEditorKit(target);

		kit.undoMan.start();
		StringBuffer html = new StringBuffer("<tr>");
		for (int i=0; i<row.getElementCount(); i++)
		{
			Element cell = row.getElement(i);
			AttributeSet cellAttribs = cell.getAttributes();
			int colspan;
			try
			{
				colspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.
						Attribute.COLSPAN));
			}
			catch (NumberFormatException ex)
			{
				colspan = 1;
			}
			String tagName = "td",
					spanString = (colspan > 1 ? " colspan=\"" + colspan + "\"" : "");
			if (cellAttribs.getAttribute(StyleConstants.NameAttribute) ==
					HTML.Tag.TH)
				tagName = "th";
			html.append("<" + tagName + spanString + "></" + tagName + ">");
		}
		html.append("</tr>");
		target.setCaretPosition(row.getEndOffset());
		InsertHTMLTextAction proxy =
										new InsertHTMLTextAction((String)this.getValue(Action.NAME), html.toString(), HTML.Tag.TABLE, HTML.Tag.TR);
		proxy.actionPerformed(new ActionEvent(target, evt.getID(), evt.getActionCommand(), evt.getModifiers()));
		kit.undoMan.end();
	}
}
