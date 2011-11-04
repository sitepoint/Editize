package com.editize.editorkit;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public abstract class BSABlockAction extends BooleanStateAwareTextAction
{
	public BSABlockAction(String name)
	{
		super(name);
	}

	public BSABlockAction(String name, JEditorPane editor)
	{
		super(name,editor);
	}

				public boolean getStateFromCaretLocation(DocumentEvent evt)
	{
		return getStateFromBlockElement(getBlockElement(evt));
	}

				public boolean getStateFromCaretLocation(CaretEvent evt)
	{
		return getStateFromBlockElement(getBlockElement(evt));
	}

	/**
	 * Determines the state based on an AttributeSet. Concrete subclasses
	 * should implement this, which will allow the state at the Caret to
	 * be tracked automatically by this class.
	 * @param a The AttributeSet for which the state must be determined.
	 * @return The state for the given AttributeSet.
	 */
	public abstract boolean getStateFromBlockElement(Element block);

	/**
	 * Convenience function that fetches the block Element at the new caret
	 * position indicated by a CaretEvent.
	 * @param e A CaretEvent.
	 * @return The block Element at the new caret position.
	 */
	protected Element getBlockElement(CaretEvent e)
	{
		StyledDocument sdoc = getStyledDocument(getAssignedEditor());
		if (sdoc instanceof HTMLDocument)
		{
			return getSelectionBlock((HTMLDocument)sdoc,e.getDot(),e.getMark());
		}
		else return null;
	}

	/**
	 * Convenience function that fetches the block Element at the current
	 * caret position in the document indicated by a DocumentEvent.
	 * @param e A DocumentEvent
	 * @return The block Element at the current caret position.
	 */
	protected Element getBlockElement(DocumentEvent e)
	{
		JEditorPane editor = getAssignedEditor();
					Caret c = editor.getCaret();
		StyledDocument sdoc = getStyledDocument(editor);
		if (sdoc instanceof HTMLDocument)
		{
			return getSelectionBlock((HTMLDocument)sdoc,c.getDot(),c.getMark());
		}
		else return null;
	}

	/**
	 * Function that fetches the block Element for the given selection in the
	 * given HTMLDocument.
	 * @param sdoc The HTMLDocument.
	 * @param dot The caret dot.
	 * @param mark The caret mark.
	 * @return The Element.
	 */
	private final Element getSelectionBlock(HTMLDocument hdoc, int dot, int mark)
	{
		int start = Math.min(dot,mark);
		return hdoc.getParagraphElement(start);
	}
}
