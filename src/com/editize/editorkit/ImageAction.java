package com.editize.editorkit;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;

import com.editize.*;

public class ImageAction extends BooleanStateAwareTextAction
{
	private ImageDialog idlg;
	String listUrl;

	public ImageAction()
	{
		super("insert-image");
	}

	public void actionPerformed(ActionEvent e)
	{
		final JEditorPane target = getAssignedEditor(e);
		if (target != null) try
		{
			if ((! target.isEditable()) || (! target.isEnabled()))
			{
				target.getToolkit().beep();
				return;
			}

			StyledDocument doc = getStyledDocument(target);
			Element img = doc.getCharacterElement(target.getSelectionStart());
			AttributeSet attr = img.getAttributes();

			// Detect existing image
			boolean edit;
			edit = attr.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMG
				&& attr.getAttribute(HTML.Attribute.SRC) != null;

			// Display image selection dialog
			if (idlg == null) idlg = new ImageDialog(target);
			idlg.setEditMode(edit);
			idlg.setBaseUrl(getHTMLDocument(target).getBase());
			idlg.setImgListUrl(listUrl);
															if (doc instanceof EditizeDocument)
																idlg.setXHTMLCompliantAlignment(((EditizeDocument)doc).isXHTMLCompliantAlignment());
			if (edit)
			{
				idlg.setImageSrc((String)attr.getAttribute(HTML.Attribute.SRC));
				idlg.setAltText((String)attr.getAttribute(HTML.Attribute.ALT));
				idlg.updatingDims = true;
				idlg.setImgWidth((String)attr.getAttribute(HTML.Attribute.WIDTH));
				idlg.setImgHeight((String)attr.getAttribute(HTML.Attribute.HEIGHT));
				idlg.updatingDims = false;
				idlg.setImgVSpace((String)attr.getAttribute(HTML.Attribute.VSPACE));
				idlg.setImgHSpace((String)attr.getAttribute(HTML.Attribute.HSPACE));

																			String htmlClass = (String)attr.getAttribute(HTML.Attribute.CLASS);
																			if (htmlClass != null && htmlClass.equals("imgleft"))
																				idlg.setImgAlign("left");
																			else if (htmlClass != null && htmlClass.equals("imgright"))
																				idlg.setImgAlign("right");
				else idlg.setImgAlign((String)attr.getAttribute(HTML.Attribute.ALIGN));
				idlg.setImgBorder((String)attr.getAttribute(HTML.Attribute.BORDER));
			}
			Dimension screenSize =
				Toolkit.getDefaultToolkit().getScreenSize();
			idlg.setLocation(
				(screenSize.width - idlg.getWidth()) / 2,
				(screenSize.height - idlg.getHeight()) / 2
			);
			idlg.show();
			if (idlg.getCloseOption() == ImageDialog.OK)
			{
				// Set image attributes
				String attrib;
				MutableAttributeSet sas = new SimpleAttributeSet();
				sas.addAttribute(StyleConstants.NameAttribute, HTML.Tag.IMG);
				if ((attrib = idlg.getImageSrc()) != null && attrib.length() > 0)
					sas.addAttribute(HTML.Attribute.SRC, attrib);
				if ((attrib = idlg.getAltText()) != null)
					sas.addAttribute(HTML.Attribute.ALT, attrib);
				else
					sas.addAttribute(HTML.Attribute.ALT, "");
				if ((attrib = idlg.getImgWidth()) != null && attrib.length() > 0)
					sas.addAttribute(HTML.Attribute.WIDTH, attrib);
				if ((attrib = idlg.getImgHeight()) != null && attrib.length() > 0)
					sas.addAttribute(HTML.Attribute.HEIGHT, attrib);
				if ((attrib = idlg.getImgHSpace()) != null && attrib.length() > 0)
					sas.addAttribute(HTML.Attribute.HSPACE, attrib);
				if ((attrib = idlg.getImgVSpace()) != null && attrib.length() > 0)
					sas.addAttribute(HTML.Attribute.VSPACE, attrib);
				if ((attrib = idlg.getImgAlign()) != null && attrib.length() > 0)
				{
					if (idlg.isXHTMLCompliantAlignment())
					{
						if (attrib.equals("left"))
							sas.addAttribute(HTML.Attribute.CLASS, "imgleft");
						else if (attrib.equals("right"))
							sas.addAttribute(HTML.Attribute.CLASS, "imgright");
						else
							sas.addAttribute(HTML.Attribute.CLASS, "imginline");
					} else
						sas.addAttribute(HTML.Attribute.ALIGN, attrib);
				}
				if (!idlg.isXHTMLCompliantAlignment() &&
						(attrib = idlg.getImgBorder()) != null && attrib.length() > 0)
					sas.addAttribute(HTML.Attribute.BORDER, attrib);

				EditizeEditorKit kit = getEditizeEditorKit(target);
				kit.undoMan.start();

				if (edit)
				{
					// Update image
					target.setSelectionStart(img.getStartOffset());
					target.setSelectionEnd(img.getEndOffset());
					setCharacterAttributes(target,sas,false);
				}
				else
				{
					// Insert image
					//int start = target.getSelectionStart();
					target.replaceSelection("");
					setCharacterAttributes(target,sas,false);
					target.replaceSelection(" ");
				}

				kit.undoMan.end();

			}

			// This doesn't work when the dialog is cancelled...(fixed?)
			target.requestFocus();
			// ...so we use a delayed call instead.
//			SwingUtilities.invokeLater(
//				new Runnable()
//				{
//					public void run()
//					{
//						target.requestFocus();
//					}
//				}
//			);
		}
		catch (IllegalArgumentException ex)
		{
			target.getToolkit().beep();
		}
		else Toolkit.getDefaultToolkit().beep();
	}

	public boolean getStateFromCaretLocation(CaretEvent evt)
	{
		return getStateFromCaretLocation(Math.min(evt.getDot(),evt.getMark()));
	}

	public boolean getStateFromCaretLocation(DocumentEvent evt)
	{
		JEditorPane editor = getAssignedEditor();
		return getStateFromCaretLocation(editor.getSelectionStart());
	}

	protected boolean getStateFromCaretLocation(int pos)
	{
		StyledDocument doc = getStyledDocument(getAssignedEditor());
		AttributeSet attr = doc.getCharacterElement(pos).getAttributes();
					return attr.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMG
			&& attr.getAttribute(HTML.Attribute.SRC) != null;
	}
}
