package com.editize.editorkit;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public abstract class BSACharacterAction extends BooleanStateAwareTextAction
{
	public BSACharacterAction(String name)
	{
		super(name);
	}

	public BSACharacterAction(String name, JEditorPane editor)
	{
		super(name,editor);
	}

	public boolean getStateFromCaretLocation(DocumentEvent evt)
	{
		return getStateFromAttributeSet(getInputAttributes(evt));
	}

	public boolean getStateFromCaretLocation(CaretEvent evt)
	{
		return getStateFromAttributeSet(getInputAttributes(evt));
	}

	/**
	 * Determines the state based on an AttributeSet. Concrete subclasses
	 * should implement this, which will allow the state at the Caret to
	 * be tracked automatically by this class.
	 * @param a The AttributeSet for which the state must be determined.
	 * @return The state for the given AttributeSet.
	 */
	public abstract boolean getStateFromAttributeSet(AttributeSet a);

	/**
	 * Convenience function that fetches the AttributeSet at the new caret
	 * position indicated by a CaretEvent.
	 * @param e A CaretEvent.
	 * @return The AttributeSet at the new caret position.
	 */
	protected AttributeSet getInputAttributes(CaretEvent e)
	{
		StyledDocument sdoc = getStyledDocument(getAssignedEditor());
		return getSelectionAttributes(sdoc,e.getDot(),e.getMark());
	}

	/**
	 * Convenience function that fetches the AttributeSet at the current
	 * caret position in the document indicated by a DocumentEvent.
	 * @param e A DocumentEvent
	 * @return The Attribute set at the current caret position.
	 */
	protected AttributeSet getInputAttributes(DocumentEvent e)
	{
		Caret c = getAssignedEditor().getCaret();
		return getSelectionAttributes((StyledDocument)e.getDocument(),c.getDot(),c.getMark());
	}

	/**
	 * Function that fetches the AttributeSet for the given selection in the
	 * given StyledDocument.
	 * @param sdoc The StyledDocument.
	 * @param dot The caret dot.
	 * @param mark The caret mark.
	 * @return The AttributeSet.
	 */
	private final AttributeSet getSelectionAttributes(StyledDocument sdoc, int dot, int mark)
	{
		int start = Math.min(dot,mark);
		// Input attributes come from the character before the caret, unless
		// that character is in a different paragraph or there is a selection,
		// in which case the character after the caret or the first character in
		// the selection, respectively, is used.
		Element run;
		Element currentParagraph = sdoc.getParagraphElement(start);
		if (currentParagraph.getStartOffset() == start || dot != mark)
		{
			run = sdoc.getCharacterElement(start);
		}
		else
		{
			run = sdoc.getCharacterElement(Math.max(start-1,0));
		}

		return run.getAttributes();
	}

}
