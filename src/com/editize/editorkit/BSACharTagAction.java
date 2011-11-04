package com.editize.editorkit;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * A character-level HTML tag toggling action that reports the current
 * state at the cursor.
 *
 * @author: Kevin Yank
 */
public class BSACharTagAction extends BSACharacterAction
{
	private HTML.Tag tag;
	private HTML.Tag alternateTag;
	private CSS.Attribute cssAttr;
	private String cssValue;
	private AttributeSet attrs;

	public BSACharTagAction(HTML.Tag tag, String name)
	{
		this(tag, null, name);
	}

	public BSACharTagAction(HTML.Tag tag, AttributeSet attrs, String name)
	{
		super(name);
		this.tag = tag;
		if (tag == HTML.Tag.B || tag == HTML.Tag.STRONG)
		{
			cssAttr = CSS.Attribute.FONT_WEIGHT;
			cssValue = "bold";
			tag = HTML.Tag.STRONG;
			alternateTag = HTML.Tag.B;
		}
		else if (tag == HTML.Tag.I || tag == HTML.Tag.EM)
		{
			cssAttr = CSS.Attribute.FONT_STYLE;
			cssValue = "italic";
			tag = HTML.Tag.EM;
			alternateTag = HTML.Tag.I;
		}
		else if (tag == HTML.Tag.U)
		{
			cssAttr = CSS.Attribute.TEXT_DECORATION;
			cssValue = "underline";
		}
		if (attrs == null) this.attrs = new SimpleAttributeSet();
		else this.attrs = attrs.copyAttributes();
	}

	/**
	 * actionPerformed method comment.
	 * @param e The ActionEvent
	 */
	public void actionPerformed(ActionEvent e)
	{
		JEditorPane editor = getAssignedEditor(e);
		if (editor != null)
		{
			StyledEditorKit kit = getStyledEditorKit(editor);
			MutableAttributeSet attr = kit.getInputAttributes();

			boolean set = getStateFromAttributeSet(attr);

			if (!set)
			{
				MutableAttributeSet sas = new SimpleAttributeSet();
				sas.addAttribute(tag,attrs);
				setCharacterAttributes(editor, sas, false);
			}
			else
			{
				Object[] toClear;
				if (cssAttr != null)
				{
					if (alternateTag != null)
					{
						toClear = new Object[] {tag,alternateTag,cssAttr};
					}
					else
					{
						toClear = new Object[] {tag, cssAttr};
					}
				}
				else
				{
					if (alternateTag != null)
					{
						toClear = new Object[] {tag,alternateTag};
					}
					else
					{
						toClear = new Object[] {tag};
					}
				}
				clearCharacterAttributes(editor,toClear);
			}
			setState(!set);
			editor.requestFocus();
		}
	}

	/**
	 * Responds to CaretEvents by updating this Action's state.
	 * @param e The CaretEvent
	 */
	public boolean getStateFromAttributeSet(AttributeSet a)
	{
		boolean set = a.isDefined(tag) ||
			alternateTag != null && a.isDefined(alternateTag) ||
			cssAttr != null && a.getAttribute(cssAttr) == cssValue;
		return set;
	}
}
