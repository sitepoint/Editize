package com.editize.editorkit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTMLEditorKit.*;

import com.editize.EditizeDocument;

public class InsertTableRowAction extends EditizeTextAction
{
	public InsertTableRowAction()
	{
		super("table-insert-row");
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditorPane target = getEditor(evt);
		if (target != null) try
		{
			EditizeDocument doc = getEditizeDocument(target);
			EditizeEditorKit kit = getEditizeEditorKit(target);
			int i, j, pos = target.getCaretPosition(), numCols = 0;
			Element table, row, cell;

			// PASS 0: Get definitive column count
			// ** ASSUMES FIRST ROW GIVES DEFINITIVE COLUMN COUNT (Fix this!)
			cell = EditizeEditorKit.getContainingTableCell(pos,doc);
			table = cell.getParentElement().getParentElement();
			row = table.getElement(0); // First row gives definitive column count
			for (i=0; i<row.getElementCount(); i++)
			{
				Element e = row.getElement(i);
				String colspan = (String)e.getAttributes().getAttribute(HTML.Attribute.COLSPAN);
				if (colspan == null || colspan.length() == 0)
				{
					numCols++;
				}
				else
				{
					numCols += Integer.parseInt(colspan);
				}
			}
			// Initialize span array
			final int[] spanArray = new int[numCols];

			// PASS 1: Determine row number to insert at
			row = cell.getParentElement();
			for (i=0; i<table.getElementCount(); i++)
			{
				if (table.getElement(i) == row) break;
			}
			final int rowPos = i;

			kit.undoMan.start();
			// PASS 2.1: Extend rowspanned cells
			for (i=0; i<rowPos; i++)
			{
				// Decrement/set spans
				for (j=0; j<numCols; j++) spanArray[j] = (i==0?0:Math.max(0,spanArray[j]-1));
				row = table.getElement(i);
				int spanCount = 0;
				for (j=0; j<row.getElementCount(); j++)
				{
					cell = row.getElement(j);
					AttributeSet cellAttribs = cell.getAttributes();
					int colspan, rowspan;
					try { colspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.Attribute.COLSPAN)); }
					catch (NumberFormatException ex) { colspan = 1; }
					try { rowspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.Attribute.ROWSPAN)); }
					catch (NumberFormatException ex) { rowspan = 1; }

					if (rowspan > 1 && i+rowspan > rowPos)
					{
						// Increase span of existing cell
						SimpleAttributeSet sas = new SimpleAttributeSet();
						sas.addAttribute(HTML.Attribute.ROWSPAN,(rowspan+1)+"");
						doc.setTagAttributes(cell.getStartOffset(),(HTML.Tag)cellAttribs.getAttribute(StyleConstants.NameAttribute),sas,false);
					}

					for (int k=0;k<colspan;k++) spanArray[spanCount++] = rowspan;
					while (spanCount < numCols && spanArray[spanCount] > 0) spanCount++;
				}
			}
			// PASS 2.2: Insert new row
			int spanCount = 0;
			row = table.getElement(i);
			StringBuffer html = new StringBuffer("<tr>");
			for (j=0; j<row.getElementCount(); j++)
			{
				cell = row.getElement(j);
				AttributeSet cellAttribs = cell.getAttributes();
				int colspan;
				try { colspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.Attribute.COLSPAN)); }
				catch (NumberFormatException ex) { colspan = 1; }
				String tagName = "td", spanString = (colspan > 1 ? " colspan=\""+colspan+"\"" : "");
				if (cellAttribs.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.TH) tagName = "th";
				html.append("<"+tagName+spanString+"></"+tagName+">");
			}
			html.append("</tr>");
			target.setCaretPosition(row.getStartOffset());
			InsertHTMLTextAction proxy =
					new InsertHTMLTextAction((String)this.getValue(Action.NAME), html.toString(), HTML.Tag.TABLE, HTML.Tag.TR);
			proxy.actionPerformed(new ActionEvent(target, evt.getID(), evt.getActionCommand(), evt.getModifiers()));
			kit.undoMan.end();
		}
		catch (IllegalArgumentException ex)
		{
			target.getToolkit().beep();
		}
		else Toolkit.getDefaultToolkit().beep();
	}
}

