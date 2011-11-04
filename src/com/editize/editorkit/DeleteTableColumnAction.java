package com.editize.editorkit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import javax.swing.text.*;
import javax.swing.text.html.HTML;

import com.editize.EditizeDocument;

public class DeleteTableColumnAction extends EditizeTextAction
{
	public DeleteTableColumnAction()
	{
		super("table-delete-column");
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

			// PASS 1: Determine at which column to delete
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

			// PASS 2: Delete cells
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

					if (spanCount == colPos && colspan == 1)
					{
						// Delete the cell
						int start = cell.getStartOffset();
						int rowStart = row.getStartOffset();
						// HACK: Set a special attribute on the row just in case
						// Java feels tempted to join it with the next row
						SimpleAttributeSet sas = new SimpleAttributeSet();
						sas.addAttribute("DO-NOT-JOIN",Boolean.TRUE);
						doc.setTagAttributes(rowStart,HTML.Tag.TR,sas,false);
						doc.remove(start,cell.getEndOffset()-start);
						doc.clearTagAttributes(rowStart,HTML.Tag.TR,new Object[] {"DO-NOT-JOIN"});
					}
					else if (spanCount+colspan > colPos || spanCount+colspan == colPos && colspan > 1)
					{
						// Decrease span of existing cell
						SimpleAttributeSet sas = new SimpleAttributeSet();
						sas.addAttribute(HTML.Attribute.COLSPAN,(colspan-1)+"");
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
		catch (BadLocationException ex)
		{
			target.getToolkit().beep();
		}
		else Toolkit.getDefaultToolkit().beep();
	}
}
