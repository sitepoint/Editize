package com.editize.editorkit;

import java.util.Vector;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;

import com.editize.*;

public class HyperlinkAction extends BSACharacterAction {

	private String url = "";
	private boolean newWindow = false;
	/**
	 * HighlightAction constructor comment.
	 */
	public HyperlinkAction() {
		super("link");
	}
	/**
	 * actionPerformed method comment.
	 */
	public void actionPerformed(ActionEvent e)
	{
		final JEditorPane editor = getAssignedEditor(e);
		if (editor != null)
		{
			StyledDocument doc = getStyledDocument(editor);
			EditizeEditorKit kit = getEditizeEditorKit(editor);
			MutableAttributeSet attr = kit.getInputAttributes();

			boolean inLink = false;
			AttributeSet linkTag = (AttributeSet)attr.getAttribute(HTML.Tag.A);
			String oldUrl = linkTag == null ? null : (String)linkTag.getAttribute(HTML.Attribute.HREF);
			String oldTarget = linkTag == null ? null : (String)linkTag.getAttribute(HTML.Attribute.TARGET);
			inLink = oldUrl != null;

			// If no selection exists and we're in a link, select the link
			int start = editor.getSelectionStart();
			if (start == editor.getSelectionEnd() && inLink)
			{
				Element paragraph = doc.getParagraphElement(start);
				int startIndex, endIndex;
				startIndex = endIndex = paragraph.getElementIndex(start);
				while (startIndex > 0 && paragraph.getElement(startIndex-1).getAttributes().containsAttribute(HTML.Tag.A,linkTag))
				{
					startIndex--;
				}
				while (endIndex < paragraph.getElementCount() && paragraph.getElement(endIndex+1) != null && paragraph.getElement(endIndex+1).getAttributes().containsAttribute(HTML.Tag.A,linkTag))
				{
					endIndex++;
				}
				editor.setSelectionStart(paragraph.getElement(startIndex).getStartOffset());
				editor.setSelectionEnd(paragraph.getElement(endIndex).getEndOffset());
			}

			// Get the URL
			JPanel message = new JPanel(new BorderLayout());
			JTextField urlField = new ClipTextField();
			JComboBox urlCombo = null;
			JCheckBox newWinCheckBox = new JCheckBox("Open in new window",inLink ? oldTarget != null && oldTarget.equals("_blank") : newWindow);

			// Load suggested link URLs
			try
			{
				Vector linkurls = (Vector)doc.getProperty("linkurls");
				urlCombo = new ClipComboBox(linkurls);
				urlCombo.setEditable(true);
				urlCombo.setSelectedIndex(-1);
				urlField = (JTextField)urlCombo.getEditor().getEditorComponent();
			}
			catch (ClassCastException ex) {}
			catch (NullPointerException ex) {}

			urlField.setText(inLink ? oldUrl : url);

			JPanel fieldPanel = new JPanel(new BorderLayout());
			if (urlCombo == null)
				fieldPanel.add(urlField, BorderLayout.CENTER);
			else
				fieldPanel.add(urlCombo, BorderLayout.CENTER);
			fieldPanel.add(new JLabel("Include http:// for absolute links"), BorderLayout.SOUTH);

			message.add(new JLabel("Type a URL for this link"), BorderLayout.NORTH);
			message.add(newWinCheckBox, BorderLayout.SOUTH);
			message.add(fieldPanel, BorderLayout.CENTER);

			String[] options = inLink ? new String[] { "OK", "Cancel", "Remove Link" } : new String[] { "OK", "Cancel" };
			JOptionPane jop = new JOptionPane(
				message,
				JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION,
				null,
				options,
				options[0]
			);
			JDialog jopdlg = jop.createDialog(editor.getParent(),inLink ? "Edit Hyperlink" : "Create Hyperlink");
			urlField.requestFocus(); // Only works in JDK 1.4 :(
			jopdlg.show();
			Object selValue = jop.getValue();

			String newUrl = urlField.getText();
			if (selValue.equals("OK") && newUrl.length() > 0)
			{
				url = newUrl;
				newWindow = newWinCheckBox.isSelected();
				MutableAttributeSet sas = new SimpleAttributeSet();
				MutableAttributeSet attrs = new SimpleAttributeSet();
				attrs.addAttribute(HTML.Attribute.HREF,url);
				if (newWindow) attrs.addAttribute(HTML.Attribute.TARGET,"_blank");
				sas.addAttribute(HTML.Tag.A,attrs);
				setCharacterAttributes(editor, sas, false);

				// HACK: Update EditorKit's end-of-link condition flag
				Caret caret = editor.getCaret();
				if (caret.getDot() >= caret.getMark()) {
					kit.endOfLink = true;
				}
			}
			else if (selValue.equals("Remove Link") || selValue.equals("OK") && newUrl.length() == 0)
			{
				clearCharacterAttributes(editor, new Object[] {HTML.Tag.A});
			}

			// If this doesn't work...
			editor.requestFocus();
			// ...we can use a delayed call.
//			SwingUtilities.invokeLater(
//				new Runnable()
//				{
//					public void run()
//					{
//						editor.requestFocus();
//					}
//				}
//			);
		}
	}

	public boolean getStateFromAttributeSet(AttributeSet attrs)
	{
		Object linkTag = attrs.getAttribute(HTML.Tag.A);
		if (linkTag == null) return false;
		else return ((AttributeSet)linkTag).getAttribute(HTML.Attribute.HREF) != null;
	}
}
