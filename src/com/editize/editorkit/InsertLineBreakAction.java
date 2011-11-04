package com.editize.editorkit;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.JEditorPane;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import com.editize.EditizeDocument;

public class InsertLineBreakAction extends EditizeTextAction
{
    /**
     * Dummy attribute used to prevent newly-inserted brs from
     * melding with adjacent brs.
     */
    private static final Object NEWBR = new Object();

	public InsertLineBreakAction()
	{
		super("insert-line-break");
	}

	public void actionPerformed(ActionEvent e)
	{
		JEditorPane target = getEditor(e);
		if (target != null) try
		{
			if ((! target.isEditable()) || (! target.isEnabled()))
			{
				target.getToolkit().beep();
				return;
			}
			EditizeEditorKit kit = getEditizeEditorKit(target);
                        EditizeDocument eDoc = (EditizeDocument)target.getDocument();

			kit.undoMan.start();
                        boolean mspaces = eDoc.isMultiSpacesAllowed();
                        eDoc.setMultiSpacesAllowed(true);

                        // Insert br with a dummy attribute to keep it separate
                        int start = target.getSelectionStart();
                        SimpleAttributeSet sas = new SimpleAttributeSet();
                        sas.addAttribute(StyleConstants.NameAttribute, HTML.Tag.BR);
                        sas.addAttribute(NEWBR, NEWBR);
                        eDoc.insertString(start, " ", sas);

                        // Strip off the dummy attribute
                        sas = new SimpleAttributeSet(eDoc.getCharacterElement(start).getAttributes());
                        sas.removeAttribute(NEWBR);
                        eDoc.setCharacterAttributes(start, 1, sas, true);

			target.setCaretPosition(start + 1);

                        eDoc.setMultiSpacesAllowed(mspaces);
			kit.undoMan.end();
		}
		catch (BadLocationException ex)
		{
			target.getToolkit().beep();
		}
		else Toolkit.getDefaultToolkit().beep();
	}
}
