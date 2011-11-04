package com.editize.editorkit;

import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import com.editize.EditizeDocument;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit.InsertHTMLTextAction;
import javax.swing.Action;
import javax.swing.text.SimpleAttributeSet;
import java.awt.Toolkit;
import java.io.StringWriter;
import javax.swing.text.BadLocationException;
import java.io.IOException;
import javax.swing.text.AbstractDocument.BranchElement;

public class MergeTableCellRightAction extends EditizeTextAction
{
	public MergeTableCellRightAction()
	{
		super("table-increase-colspan");
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditorPane target = getEditor(evt);
		if (target != null) //try
		{
			EditizeDocument doc = getEditizeDocument(target);
			EditizeEditorKit kit = getEditizeEditorKit(target);
			int i, j, pos = target.getCaretPosition(), numCols = 0, colPos = 0;
			Element table, row, cell;

			// PASS 0: Get definitive column count
			/**
			 * @todo Assumes first row gives definitive column count -- fix!
			 */
			cell = EditizeEditorKit.getContainingTableCell(pos, doc);
			table = cell.getParentElement().getParentElement();
			i = 0;
			do { // First row gives definitive column count
				row = table.getElement(i++);
			} while (EditizeEditorKit.getElementName(row) != HTML.Tag.TR);
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

			kit.undoMan.start();

			// Determine at which column to merge and then do it!
			boolean complete = false, end = false;
			int rowspan, colspan;
			int colToMergeFrom = -1;
			int rowsToMergeOver = -1;
			int mergeColspan = -1;
			Element mergeTarget = null;
			for (i = 0; i < table.getElementCount(); i++)
			{
				row = table.getElement(i);
				if (EditizeEditorKit.getElementName(row) != HTML.Tag.TR)
					continue;

				// Decrement/set spans
				for (j = 0; j < numCols; j++)
					spanArray[j] = (i == 0 ? 0 : Math.max(0, spanArray[j] - 1));

				colPos = 0;
				for (j = 0; j < row.getElementCount(); j++)
				{
					Element e = row.getElement(j);
					// Get cell details
					AttributeSet cellAttribs = e.getAttributes();
					try {
						colspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.
								Attribute.COLSPAN));
					} catch (NumberFormatException ex) {
						colspan = 1;
					}
					try {
						rowspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.
								Attribute.ROWSPAN));
					} catch (NumberFormatException ex) {
						rowspan = 1;
					}
					// We've found the mergeTarget
					if (e == cell)
					{
						// Record details
						mergeTarget = e;
						colToMergeFrom = colPos + colspan;
						rowsToMergeOver = rowspan;
					}
					// We've found a merge source cell
					if (mergeTarget != null && colPos == colToMergeFrom)
					{
						// Check that this cell is mergable
						if (mergeColspan >= 0 && colspan != mergeColspan ||
								rowspan > rowsToMergeOver)
						{
							// Source cell contains spans incompatible with merging
							end = true;
							break;
						}
						else mergeColspan = colspan;
						mergeInto(mergeTarget, e, target);
						rowsToMergeOver -= rowspan;
						if (rowsToMergeOver <= 0) {
							// Set the new colspan of the merge target
							cellAttribs = mergeTarget.getAttributes();
							try {
								colspan = Integer.parseInt((String)cellAttribs.getAttribute(
										HTML.Attribute.COLSPAN));
							}
							catch (NumberFormatException ex) {
								colspan = 1;
							}
							colspan += mergeColspan;
							SimpleAttributeSet newAttribs = new SimpleAttributeSet();
							newAttribs.addAttribute(HTML.Attribute.COLSPAN,
																			new Integer(colspan).toString());
							doc.setTagAttributes(mergeTarget.getStartOffset(),
																	 (HTML.Tag)EditizeEditorKit.getElementName(mergeTarget),
																	 newAttribs, false);
							complete = end = true;
							break;
						}
					}
					// Increment colpos and set rowspan(s) according to colspan
					for (int k = 0; k < colspan; k++) spanArray[colPos++] = rowspan;
					// Skip columns as prescribed by previous rowspan(s)
					while (colPos < numCols && spanArray[colPos] > 0) colPos++;
				}
				if (end) break;
			}
			kit.undoMan.end();
			if (!complete) {
				// Roll back changes -- process did not complete
				kit.undoMan.undo();
				target.getToolkit().beep();
				return;
			}

//			// Merge cells
//			kit.undoMan.start();
//			for (; i<table.getElementCount(); i++) // Start at caret row
//			{
//				// Find cell with which to merge on this row
//
//
//				// Decrement/set spans
//				for (j=0; j<numCols; j++) spanArray[j] = (i==0?0:Math.max(0,spanArray[j]-1));
//				row = table.getElement(i);
//				int spanCount = 0;
//				for (j=0; j<row.getElementCount(); j++)
//				{
//					cell = row.getElement(j);
//					AttributeSet cellAttribs = cell.getAttributes();
//					try { colspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.Attribute.COLSPAN)); }
//					catch (NumberFormatException ex) { colspan = 1; }
//					try { rowspan = Integer.parseInt((String)cellAttribs.getAttribute(HTML.Attribute.ROWSPAN)); }
//					catch (NumberFormatException ex) { rowspan = 1; }
//					for (int k=0;k<colspan;k++) spanArray[spanCount+k] = rowspan;
//
//					if (spanCount == colPos)
//					{
//						// Insert new cell with same rowspan
//						target.setCaretPosition(cell.getStartOffset());
//						String tagName = "td";
//						if (cellAttribs.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.TH) tagName = "th";
//						String html = (rowspan > 1 ? "<"+tagName+" rowspan=\""+rowspan+"\"><p></p></"+tagName+">" : "<td><p></p></td>");
//						InsertHTMLTextAction proxy =
//								new InsertHTMLTextAction((String)this.getValue(Action.NAME), html, HTML.Tag.TR, HTML.Tag.TD);
//						proxy.actionPerformed(new ActionEvent(target,evt.getID(),evt.getActionCommand(),evt.getModifiers()));
//					}
//					else if (spanCount+colspan > colPos || spanCount+colspan == colPos && colspan > 1)
//					{
//						// Increase span of existing cell
//						SimpleAttributeSet sas = new SimpleAttributeSet();
//						sas.addAttribute(HTML.Attribute.COLSPAN,(colspan+1)+"");
//						doc.setTagAttributes(cell.getStartOffset(),(HTML.Tag)cellAttribs.getAttribute(StyleConstants.NameAttribute),sas,false);
//					}
//					else
//					{
//						// Move to next cell in row
//						spanCount += colspan;
//						while (spanCount < numCols && spanArray[spanCount] > 0) spanCount++;
//						continue;
//					}
//
//					for (j=1; j<rowspan; j++)
//					{
//						for (int k=0;k<numCols;k++) spanArray[k] = Math.max(0,spanArray[k]-1);
//						i++;
//					}
//					break;
//				}
//			}
//			kit.undoMan.end();
		}
//		catch (IllegalArgumentException ex)
//		{
//			target.getToolkit().beep();
//		}
		else Toolkit.getDefaultToolkit().beep();
	}

	private static AttributeSet paragraphAttributes;

	static {
		SimpleAttributeSet a = new SimpleAttributeSet();
		a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.P);
		paragraphAttributes = a;
	}

	/**
	 * Appends the contents of sourcecell to the contents of targetCell, then
	 * removes sourceCell from document. If both contain just one HTML.Tag.IMPLIED
	 * contents are merged inline. Otherwise, HTML.Tag.IMPLIED blocks are
	 * converted to HTML.Tag.P elements.
	 *
	 * @param targetCell Element
	 * @param sourceCell Element
	 * @param editor JEditorPane
	 */
	private static void mergeInto(Element targetCell, Element sourceCell, JEditorPane editor)
	{
		EditizeEditorKit kit = getEditizeEditorKit(editor);
		EditizeDocument doc = getEditizeDocument(editor);
		com.editize.HTMLPane pane = (com.editize.HTMLPane)editor;

		boolean impliedOnly = containsOnlyImplied(targetCell) &&
				containsOnlyImplied(sourceCell);
		int start = sourceCell.getStartOffset();
		int len = sourceCell.getEndOffset() - start;
		StringWriter sw = new StringWriter();

		if (!impliedOnly) {
			// Make all IMPLIED blocks into paragraphs
			int numBlocks = targetCell.getElementCount();
			int i;
			for (i = 0; i < numBlocks; i++) { // In target
				BranchElement e = (BranchElement)targetCell.getElement(i);
				if (EditizeEditorKit.getElementName(e) == HTML.Tag.IMPLIED) {
					doc.replaceBranchElement(e, paragraphAttributes);
				}
			}
			numBlocks = sourceCell.getElementCount();
			for (i = 0; i < numBlocks; i++) { // In source
				BranchElement e = (BranchElement)sourceCell.getElement(i);
				if (EditizeEditorKit.getElementName(e) == HTML.Tag.IMPLIED) {
					doc.replaceBranchElement(e, paragraphAttributes);
				}
			}
		}

		// Move cell contents
		try {
			kit.write(sw, doc, start, len, true); // Write cell to delimited HTML
			doc.remove(start, len); // Remove cell from document
			pane.setCaretPosition(targetCell.getEndOffset() - 1);
			pane.readDelimitedHtml(sw.toString()); // Insert into previous cell
		}
		catch (BadLocationException ex) {}
		catch (IOException ex) {}
	}

	private static boolean containsOnlyImplied(Element e)
	{
		for (int i = 0; i < e.getElementCount(); i++) {
			if (EditizeEditorKit.getElementName(e.getElement(i)) != HTML.Tag.IMPLIED) {
				return false;
			}
		}
		return true;
	}
}
