package com.editize;

import javax.swing.text.*;

import com.editize.editorkit.EditizeEditorKit;

/**
 * A Caret that is aware of tables in an HTMLDocument, and does not allow selections
 * across table and cell boundaries.
 *
 * @author Kevin Yank
 */

public class TableAwareCaret extends com.editize.DefaultCaret
{
    public TableAwareCaret()
    {
		super();
    }

	protected void moveDot(int dot, Position.Bias dotBias)
	{
		Element e = null;
		Document doc = getComponent().getDocument();
		// If the mark is inside a table cell, constrain the dot to remain
		// inside that cell.
		if ((e = EditizeEditorKit.getContainingTableCell(getMark(),doc)) != null)
		{
			dot = Math.max(e.getStartOffset(),dot);
			dot = Math.min(e.getEndOffset()-1,dot);
		}
		// If the requested dot position is inside a table cell, constrain the
		// dot to remain on the opposite side of the table from the mark.
		else if ((e = EditizeEditorKit.getContainingTableCell(dot,doc)) != null)
		{
			int mark = getMark();
			Element table = e.getParentElement().getParentElement();
			if (mark < dot)
			{
				// If the mark is before the table, put the dot after the table
				dot = table.getEndOffset();
			}
			else if (mark > dot)
			{
				// If the mark is after the table, put the dot before the table
				dot = table.getStartOffset();
			}
		}

		super.moveDot(dot, dotBias);
	}
}
