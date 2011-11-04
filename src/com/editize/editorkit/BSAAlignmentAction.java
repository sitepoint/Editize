package com.editize.editorkit;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import com.editize.*;

/**
 * An action to set paragraph alignment.  This sets the
 * <code>StyleConstants.Alignment</code> attribute for the
 * currently selected range of the target JEditorPane.
 * This is done by calling
 * <code>StyledDocument.setParagraphAttributes</code>
 * on the styled document associated with the target
 * JEditorPane.
 * <p>
 * If the target text component is specified as the
 * source of the ActionEvent and there is a command string,
 * the command string will be interpreted as an integer
 * that should be one of the legal values for the
 * <code>StyleConstants.Alignment</code> attribute.
 * <p>
			 * If the document's XHTMLALIGNMENT property is set to
			 * Boolean.TRUE, the CSS classes "leftalign", "centeralign",
			 * and "rightalign" will be used instead.
			 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases.  The current serialization support is appropriate
 * for short term storage or RMI between applications running the same
 * version of Swing.  A future release of Swing will provide support for
 * long term persistence.
 */
public class BSAAlignmentAction extends BSABlockAction {

	/**
	 * Alignment for this action. Should be one of StyleConstants.ALIGN_*.
	 */
	private int a;

	/**
	 * Creates a new AlignmentAction.
	 *
	 * @param nm the action name
	 * @param a the alignment >= 0
	 */
	public BSAAlignmentAction(String nm, int a) {
			super(nm);
			this.a = a;
	}

	/**
	 * Sets the alignment.
	 *
	 * @param e the action event
	 */
	public void actionPerformed(ActionEvent e)
	{
		JEditorPane editor = getAssignedEditor(e);
		EditizeEditorKit kit = getEditizeEditorKit(editor);
		if (editor != null) {

												// Get the alignment (if available) from the ActionEvent
												// command string
												int a = this.a;
												if ((e != null) && (e.getSource() == editor))
												{
													String s = e.getActionCommand();
													try
													{
														a = Integer.parseInt(s, 10);
													}
													catch (NumberFormatException nfe)
													{
                                                        // Invalid value - ignore it
                                                    }
												}

												kit.undoMan.start();

												clearParagraphAttributes(editor, new Object[]
																								 {HTML.Attribute.ALIGN});
												MutableAttributeSet attr = new SimpleAttributeSet();

												Document doc = editor.getDocument();
												if (doc instanceof EditizeDocument &&
														((EditizeDocument)doc).isXHTMLCompliantAlignment())
												{
													switch (a)
													{
														case StyleConstants.ALIGN_LEFT:
															attr.addAttribute(HTML.Attribute.CLASS,"leftalign");
															break;
														case StyleConstants.ALIGN_CENTER:
															attr.addAttribute(HTML.Attribute.CLASS,"centeralign");
															break;
														case StyleConstants.ALIGN_RIGHT:
															attr.addAttribute(HTML.Attribute.CLASS,"rightalign");
															break;
														default:
															break;
													}
												} else {
													StyleConstants.setAlignment(attr, a);
												}
												setParagraphAttributes(editor, attr, false);

												kit.undoMan.end();
												editor.requestFocus();
		}
	}

	public boolean getStateFromBlockElement(Element e)
	{
								AttributeSet a = e.getAttributes();
                                String htmlAlign = (String) a.getAttribute(HTML.Attribute.ALIGN);
								String htmlClass = (String) a.getAttribute(HTML.Attribute.CLASS);
								return
										this.a == StyleConstants.ALIGN_LEFT &&
										("left".equalsIgnoreCase(htmlAlign) || "leftalign".equals(htmlClass)) ||
										this.a == StyleConstants.ALIGN_CENTER &&
										("center".equalsIgnoreCase(htmlAlign) || "centeralign".equals(htmlClass)) ||
										this.a == StyleConstants.ALIGN_RIGHT &&
										("right".equalsIgnoreCase(htmlAlign) || "rightalign".equals(htmlClass)) ||
										htmlClass == null && htmlAlign == null && StyleConstants.getAlignment(a) == this.a;
	}
}
