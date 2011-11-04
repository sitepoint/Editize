package com.editize;

import java.util.*;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.JTextPane;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import com.editize.editorkit.*;

/**
 * A ComboBox designed to contain a list of HTMLBlockTypeActions.
 * The ComboBox acts as a CaretListener, and always attempts to
 * display the HTMLBlockTypeAction that matches the current block's
 * tag name.
 *
 * @author: Kevin Yank
 */
class HTMLBlockTypeActionComboBox extends ActionComboBox implements CaretListener {
	private boolean suppressActionEvents = false;
	private Vector styleActions;
	private Vector hideActions;

	private static final String IMPLIED_LABEL = "Plain";
	private static final String TD_LABEL = "Table cell";
	private static final String TH_LABEL = "Table heading";
	private static final String LI_LABEL = "List item";

	public void actionPerformed(ActionEvent evt)
	{
          removeItem(IMPLIED_LABEL);
          removeItem(TD_LABEL);
          removeItem(TH_LABEL);
          removeItem(LI_LABEL);
          super.actionPerformed(evt);
	}

	/**
	 * Adds and item to the end of the list.
	 *
	 * @param o java.lang.Object
	 */
	public void addAction(HTMLBlockTypeAction a) {
		if (styleActions == null) styleActions = new Vector();
		styleActions.add(a);

		if (hideActions == null || !hideActions.contains(a))
		{
			super.addAction(a);
		}
	}

        /**
         * Responds to caret position changes by matching the currently
         * selected value to the paragraph Style at the caret. If a match
         * cannot be made, the first item in the drop-down is selected.
         *
         * @param e javax.swing.event.CaretEvent
         */
        public synchronized void caretUpdate(CaretEvent e)
        {
          Object src = e.getSource();
          if (src instanceof JTextPane)
          {
            JTextPane docPane = (JTextPane) e.getSource();
            StyledDocument doc = docPane.getStyledDocument();
            if (doc instanceof HTMLDocument)
            {
              HTMLDocument hDoc = (HTMLDocument) doc;
              Element el = hDoc.getParagraphElement(docPane.getCaretPosition());
              Object tag = EditizeEditorKit.getElementName(el);
              boolean found = false;
              suppressActionEvents = true;

              // Spot IMPLIED elements inside PRE blocks
              if (tag == HTML.Tag.IMPLIED &&
                  EditizeEditorKit.getElementName(el.getParentElement()) ==
                  HTML.Tag.PRE)
              {
                tag = HTML.Tag.PRE;
              }
              // Spot IMPLIED elements inside LI blocks
              if (tag == HTML.Tag.IMPLIED &&
                  EditizeEditorKit.getElementName(el.getParentElement()) ==
                  HTML.Tag.LI)
              {
                tag = HTML.Tag.LI;
              }
              // Spot IMPLED elements inside TD blocks
              if (tag == HTML.Tag.IMPLIED &&
                  EditizeEditorKit.getElementName(el.getParentElement()) ==
                  HTML.Tag.TD)
              {
                tag = HTML.Tag.TD;
              }
              // Spot IMPLED elements inside TH blocks
              if (tag == HTML.Tag.IMPLIED &&
                  EditizeEditorKit.getElementName(el.getParentElement()) ==
                  HTML.Tag.TD)
              {
                tag = HTML.Tag.TH;
              }

              // Remove special block type names
              removeItem(IMPLIED_LABEL);
              removeItem(TD_LABEL);
              removeItem(TH_LABEL);
              removeItem(LI_LABEL);

              if (tag == HTML.Tag.TD)
              {
                insertItemAt(TD_LABEL, 0);
                setSelectedIndex(1);
                setSelectedIndex(0);
              }
              else if (tag == HTML.Tag.TH)
              {
                insertItemAt(TH_LABEL, 0);
                setSelectedIndex(1);
                setSelectedIndex(0);
              }
              else if (tag == HTML.Tag.LI)
              {
                insertItemAt(LI_LABEL, 0);
                setSelectedIndex(1);
                setSelectedIndex(0);
              }
              else if (tag == HTML.Tag.IMPLIED)
              {
                insertItemAt("Plain", 0);
                setSelectedIndex(1);
                setSelectedIndex(0);
              }
              else
              {
                for (int i = 0; i < getItemCount(); i++)
                {
                  if ( ( (HTMLBlockTypeAction) getItemAt(i)).getTag() == tag)
                  {
                    setSelectedIndex(i);
                    found = true;
                    break;
                  }
                }
                if (!found)
                {
                  setSelectedIndex(1); // Workaround Java 1.4.0 bug
                  setSelectedIndex(0);
                }
              }
              suppressActionEvents = false;
            }
          }
        }

	/**
	 * Protected method used internally to trigger ActionEvents
	 * only when they are not suppressed. ActionEvents are suppressed
	 * by the caretUpdate method to allow the currently selected item
	 * to track the current Style at the caret without causing
	 * ActionEvents in the process. To allow ActionEvents to occur in
	 * response to a caretUpdate causes an IllegalStateException.
	 * Creation date: (07/07/2001 4:59:07 PM)
	 * @param e java.awt.event.ActionEvent
	 */
	protected void fireActionEvent()
	{
		if (!suppressActionEvents) super.fireActionEvent();
	}

	public Dimension getMaximumSize() {
		return this.getPreferredSize();
	}

	/**
	 * Hides a style in the list
	 *
	 * @param styleName java.lang.String
	 */
	public void hideTag(HTML.Tag tag)
	{
		if (hideActions == null) hideActions = new Vector();

		hideActions.add(tag);

		updateList();
	}

	/**
	 * Determines if a tag is hidden in the list
	 *
	 * @param tag
	 */
	public boolean isTagHidden(HTML.Tag tag)
	{
		if (hideActions == null) return false;
		return hideActions.contains(tag);
	}

	/**
	 * Loads the styles in a document as items in this list. Lists
	 * 'well known styles' in a particular order, then tacks any extras
	 * onto the end.
	 *
	 * @param doc javax.swing.text.html.HTMLDocument
	 */
	public void loadStyles(JTextPane pane)
	{
		Document doc = pane.getDocument();
		EditorKit kit = pane.getEditorKit();
		if (doc instanceof HTMLDocument && kit instanceof EditizeEditorKit)
		{
			HTMLDocument hDoc = (HTMLDocument)doc;
			EditizeEditorKit eKit = (EditizeEditorKit)kit;
			Enumeration styles = hDoc.getStyleNames();

			// Put the style names into a List
			Vector styleList = new Vector();
			while (styles.hasMoreElements())
			{
				styleList.insertElementAt(styles.nextElement(),0);
			}

			suppressActionEvents = true;

			// Pick out any known styles and add them to the list in ideal order
			while (styleList.remove(HTML.Tag.IMPLIED.toString()));
			while (styleList.remove(HTML.Tag.BODY.toString()));
			if (styleList.contains(HTML.Tag.P.toString()))
			{
				while (styleList.remove(HTML.Tag.P.toString()));
				addAction(eKit.getHTMLBlockTypeAction(HTML.Tag.P, "Paragraph"));
			}
			if (styleList.contains(HTML.Tag.H1.toString()))
			{
				while (styleList.remove(HTML.Tag.H1.toString()));
				addAction(eKit.getHTMLBlockTypeAction(HTML.Tag.H1, "Heading"));
			}
			if (styleList.contains(HTML.Tag.H2.toString()))
			{
				while (styleList.remove(HTML.Tag.H2.toString()));
				addAction(eKit.getHTMLBlockTypeAction(HTML.Tag.H2, "Subheading"));
			}
			if (styleList.contains(HTML.Tag.PRE.toString()))
			{
				while (styleList.remove(HTML.Tag.PRE.toString()));
				addAction(eKit.getHTMLBlockTypeAction(HTML.Tag.PRE, "Monospaced"));
			}

			// Suppress block types we are not interested in
			while (styleList.remove(HTML.Tag.BLOCKQUOTE.toString()));
			while (styleList.remove(HTML.Tag.UL.toString()));
			while (styleList.remove(HTML.Tag.OL.toString()));
			while (styleList.remove(HTML.Tag.DL.toString()));
			while (styleList.remove(HTML.Tag.DT.toString()));
			while (styleList.remove(HTML.Tag.DD.toString()));
			while (styleList.remove(HTML.Tag.MENU.toString()));
			while (styleList.remove(HTML.Tag.DIR.toString()));
			while (styleList.remove(HTML.Tag.TABLE.toString()));
			while (styleList.remove(HTML.Tag.TR.toString()));
			while (styleList.remove(HTML.Tag.TD.toString()));
			while (styleList.remove(HTML.Tag.TH.toString()));
			while (styleList.remove(HTML.Tag.H3.toString()));
			while (styleList.remove(HTML.Tag.H4.toString()));
			while (styleList.remove(HTML.Tag.H5.toString()));
			while (styleList.remove(HTML.Tag.H6.toString()));

			Iterator it = styleList.iterator();
			Vector addedTags = new Vector();
			while (it.hasNext())
			{
				String tagName = (String)it.next();
				HTML.Tag tag = HTML.getTag(tagName);
				if (tag != null && tag.isBlock() && !addedTags.contains(tag))
				{
					addedTags.add(tag);
					addAction(eKit.getHTMLBlockTypeAction(tag));
				}
			}
			setSelectedIndex(0);
			suppressActionEvents = false;
		}
	}

	/**
	 * Shows a style in the list if previously hidden.
	 *
	 * @param styleName java.lang.String
	 */
	public void showTag(HTML.Tag tag)
	{
		if (hideActions == null) hideActions = new Vector();

		hideActions.remove(tag);

		updateList();
	}

	/**
	 * Updates the list in the combo box to reflect hidden/shown styles
	 */
	private void updateList()
	{
		if (styleActions == null) return;

		Vector tempActions = styleActions;
		Object sel = getSelectedItem();
		suppressActionEvents = true;
		removeAllItems();
		styleActions = new Vector();
		Enumeration e = tempActions.elements();
		while (e.hasMoreElements())
		{
			addAction((HTMLBlockTypeAction)e.nextElement());
		}
		setSelectedItem(sel);
		suppressActionEvents = false;
	}
}
