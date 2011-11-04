package com.editize.editorkit;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.HTMLEditorKit.*;

import com.editize.*;

/**
 * An extended version of StyledEditorKit.StyledTextAction that adds a couple of
 * convenience functions.
 *
 * @author Kevin Yank
 */
public abstract class EditizeTextAction extends HTMLEditorKit.HTMLTextAction
{
	/**
	 * Creates a new ArticleTextAction from a string action name.
	 *
	 * @param nm the name of the action
	 */
	public EditizeTextAction(String nm)
	{
          super(nm);
	}

	/**
	 * Gets the ArticleDocument in a JEditorPane.
	 * @param e The JEditorPane from which to obtain the ArticleDocument.
	 * @return The ArticleDocument.
	 */
	protected final EditizeDocument getArticleDocument(JEditorPane e)
	{
          Document d = e.getDocument();
          if (d instanceof HTMLDocument)
          {
            return (EditizeDocument) d;
          }
          throw new IllegalArgumentException("document must be ArticleDocument");
	}

	/**
	 * Clears the given attributes from character
	 * content.  If there is a selection, the attributes
	 * are cleared from the selection range.  If there
	 * is no selection, the attributes are cleared from
	 * the input attribute set which defines the attributes
	 * for any new text that gets inserted.
	 *
	 * @param editor the editor
	 * @param attributes the attributes to clear
	 */
	protected final void clearCharacterAttributes(JEditorPane editor, Object[] attributes)
	{
          int p0 = editor.getSelectionStart();
          int p1 = editor.getSelectionEnd();
          if (p0 != p1)
          {
            EditizeDocument doc = getArticleDocument(editor);
            doc.clearCharacterAttributes(p0, p1 - p0, attributes);
          }
          StyledEditorKit k = getStyledEditorKit(editor);
          MutableAttributeSet inputAttributes = k.getInputAttributes();
          for (int i = 0; i < attributes.length; i++) inputAttributes.removeAttribute(
              attributes[i]);
	}

	/**
	 * Clears the given attributes from paragraphs.  If
	 * there is a selection, the attributes are cleared
	 * from the paragraphs that intersect the selection.
	 * If there is no selection, the attributes are cleared
	 * from the paragraph at the current caret position.
	 *
	 * @param editor the editor
	 * @param attributes the attributes to clear
	 */
	protected final void clearParagraphAttributes(JEditorPane editor, Object[] attributes)
	{
          int p0 = editor.getSelectionStart();
          int p1 = editor.getSelectionEnd();
          EditizeDocument doc = getArticleDocument(editor);
          doc.clearParagraphAttributes(p0, p1 - p0, attributes);
	}

	/**
	 * @return EditizeDocument of <code>e</code>.
	 */
	protected static EditizeDocument getEditizeDocument(JEditorPane e) {
          Document d = e.getDocument();
          if (d instanceof EditizeDocument)
          {
            return (EditizeDocument) d;
          }
          throw new IllegalArgumentException("document must be EditizeDocument");
	}

	/**
	 * @return EditizeEditorKit for <code>e</code>.
	 */
	protected static EditizeEditorKit getEditizeEditorKit(JEditorPane e) {
          EditorKit k = e.getEditorKit();
          if (k instanceof EditizeEditorKit)
          {
            return (EditizeEditorKit) k;
          }
          throw new IllegalArgumentException("EditorKit must be EditizeEditorKit");
	}
}
