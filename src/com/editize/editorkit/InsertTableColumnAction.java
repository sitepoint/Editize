package com.editize.editorkit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTMLEditorKit.*;

import com.editize.EditizeDocument;

public class InsertTableColumnAction extends EditizeTextAction
{
	public InsertTableColumnAction()
	{
		super("table-insert-column");
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditorPane target = getEditor(evt);
		if (target != null) try
		{
			EditizeDocument doc = getEditizeDocument(target);
			EditizeEditorKit kit = getEditizeEditorKit(target);
			int i, j, pos = target.getCaretPosition(), numCols = 0, colPos = 0;
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

			// PASS 1: Determine at which column to insert
			boolean found = false;
			for (i=0; i<table.getElementCount(); i++)
			{
				// Decrement/set spans
				for (j=0; j<numCols; j++) spanArray[j] = (i==0?0:Math.max(0,spanArray[j]-1));
				row = table.getElement(i);
				colPos = 0;
				for (j=0; j<row.getElementCount(); j++)
				{
					Element e = row.getElement(j);
					// We've found the cell -- colPos is correct
					if (e == cell)
					{
						found = true;
						break;
					}
					// Get cell details
					AttributeSet cellAttribs = e.getAttributes();
					int colspan, rowspan;
					try { colspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.Attribute.COLSPAN)); }
					catch (NumberFormatException ex) { colspan = 1; }
					try { rowspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.Attribute.ROWSPAN)); }
					catch (NumberFormatException ex) { rowspan = 1; }
					for (int k=0;k<colspan;k++) spanArray[colPos++] = rowspan;
					while (colPos < numCols && spanArray[colPos] > 0) colPos++;
				}
				if (found) break;
			}

			// PASS 2: Insert cells
			kit.undoMan.start();
			for (i=0; i<table.getElementCount(); i++)
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
					for (int k=0;k<colspan;k++) spanArray[spanCount+k] = rowspan;

					if (spanCount == colPos)
					{
						// Insert new cell with same rowspan
						target.setCaretPosition(cell.getStartOffset());
						String tagName = "td";
						if (cellAttribs.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.TH) tagName = "th";
						String html = (rowspan > 1 ? "<"+tagName+" rowspan=\""+rowspan+"\"></"+tagName+">" : "<td></td>");
						InsertHTMLTextAction proxy =
								new InsertHTMLTextAction((String)this.getValue(Action.NAME), html, HTML.Tag.TR, HTML.Tag.TD);
						proxy.actionPerformed(new ActionEvent(target,evt.getID(),evt.getActionCommand(),evt.getModifiers()));
					}
					else if (spanCount+colspan > colPos || spanCount+colspan == colPos && colspan > 1)
					{
						// Increase span of existing cell
						SimpleAttributeSet sas = new SimpleAttributeSet();
						sas.addAttribute(HTML.Attribute.COLSPAN,(colspan+1)+"");
						doc.setTagAttributes(cell.getStartOffset(),(HTML.Tag)cellAttribs.getAttribute(StyleConstants.NameAttribute),sas,false);
					}
					else
					{
						// Move to next cell in row
						spanCount += colspan;
						while (spanCount < numCols && spanArray[spanCount] > 0) spanCount++;
						continue;
					}

					for (j=1; j<rowspan; j++)
					{
						for (int k=0;k<numCols;k++) spanArray[k] = Math.max(0,spanArray[k]-1);
						i++;
					}
					break;
				}
			}
			kit.undoMan.end();
		}
		catch (IllegalArgumentException ex)
		{
			target.getToolkit().beep();
		}
		else Toolkit.getDefaultToolkit().beep();
	}
}
