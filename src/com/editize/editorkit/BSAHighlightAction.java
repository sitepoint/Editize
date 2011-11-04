package com.editize.editorkit;

import java.beans.*;
import java.io.*;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import com.editize.*;

public class BSAHighlightAction extends BSACharTagAction implements ChangeListener {

	/**
	 * A character-level highlighted text toggling action.
	 *
	 * @todo Icon update routine needs to use document's StyleSheet.
	 *
	 * @author: Kevin Yank
	 */
	private static MutableAttributeSet highlightAttribute;
	static
	{
		highlightAttribute = new SimpleAttributeSet();
		highlightAttribute.addAttribute(HTML.Attribute.CLASS,"highlighted");
	}

	/**
	 * Default Constructor
	 */
	public BSAHighlightAction() {
		super(HTML.Tag.SPAN,highlightAttribute,"font-highlighted");

		try
		{
			InputStream i = getClass().getResourceAsStream("toolbarButtonGraphics/textcolor.gif");
			byte[] img = new byte[i.available()];
			i.read(img);
			ImageIcon icon = new ImageIcon(img);
			putValue(Action.SMALL_ICON, icon);
		}
		catch (IOException e)
		{ /* Could not load Action graphic */ }
	}

	/**
	 * Adjusts icon to reflect the highlighted text color of the assigned editor's document.
	 * @param e The assigned JEditorPane.
	 */
	public void setAssignedEditor(JEditorPane e)
	{
		super.setAssignedEditor(e);

		Document doc = e.getDocument();
		if (doc instanceof EditizeDocument)
		{
			EditizeStyleSheet ss = (EditizeStyleSheet)((HTMLDocument)doc).getStyleSheet();
			Style s = ss.lookupStyle("span.highlighted");
			Color c = StyleConstants.getForeground(s);

			if (c != null) updateIcon((Color)c);

			// Listen for changes to highlightcolor
			ss.addChangeListener(this);
			doc.addDocumentListener(this);
		}
		// Listen for document changes
		e.addPropertyChangeListener(this);
	}

	public void changedUpdate(DocumentEvent evt) {
		super.changedUpdate(evt);
		HTMLDocument doc = (HTMLDocument)getAssignedEditor().getDocument();
		updateIconFromDocument(doc);
	}

	/**
	 * Responds to changes in the assigned editor's document property.
	 * @param evt
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		Object newValue = evt.getNewValue();
		if (evt.getPropertyName().equals("document") && newValue != null) // New document loaded
		{
			// Register to listen for highlight color changes from the new document
			if (newValue instanceof EditizeDocument) {
				EditizeDocument doc = (EditizeDocument) newValue;
				doc.getStyleSheet().addChangeListener(this);
				doc.addDocumentListener(this);
				updateIconFromDocument(doc);
			}
		}
	}

	/**
	 * Responds to changes in the stylesheet.
	 * @param evt
	 */
	public void stateChanged(ChangeEvent evt)
	{
		JEditorPane e = getAssignedEditor();
		Document doc = e.getDocument();
		if (doc instanceof HTMLDocument)
		{
			updateIconFromDocument((HTMLDocument)doc);
		}
	}

		protected void updateIconFromDocument(HTMLDocument doc)
		{
			// Update the highlight color from this document
			EditizeStyleSheet ss = (EditizeStyleSheet)doc.getStyleSheet();
			Style s = ss.lookupStyle("span.highlighted");
			Color c = StyleConstants.getForeground(s);
			if (c != null) updateIcon( (Color) c);
		}
	/**
	 * Adjusts icon to reflect the highlighted text color of the specified editor's document.
	 * @param e The JEditorPane from which to obtain the color.
	 */
	protected void updateIcon(Color c)
	{
		// Modify the image to display the highlight color assigned to the document
		if (c != null && c instanceof java.awt.Color)
		{
			Color color = (Color)c;
			Image icon = ((ImageIcon)getValue(Action.SMALL_ICON)).getImage();

			BufferedImage img = new BufferedImage(16,16,BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = img.getGraphics();
			g.drawImage(icon,0,0,getAssignedEditor());
			g.setColor(color);
			g.fillRect(1,11,14,4);
			putValue(Action.SMALL_ICON, new ImageIcon(img));
		}
	}

}
