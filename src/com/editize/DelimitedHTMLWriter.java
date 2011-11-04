package com.editize;

import java.io.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import com.editize.editorkit.*;

/**
 * HTMLWriter with the ability to write delimiters.
 */
public class DelimitedHTMLWriter extends HTMLWriter
{
  private static final Object SELFCLOSING = new Object();
	private static final String STARTDELIMITER = "<!--StartFragment-->";
	private static final String ENDDELIMITER = "<!--EndFragment-->";

	private int pos;
	private int len;

	private boolean inlineContent;
	private boolean delimit;
	private boolean writeStartAfterIndent = false;
	private int writeEndBeforeNewline = 0;
	//private boolean wroteStart;
	//private boolean wroteEnd;

	Element firstBlock;
	Element lastBlock;

	/**
	 * Creates a new HTMLWriter.
	 *
	 * @param w   a Writer
	 * @param doc  an HTMLDocument
	 * @param delimit If true, will write delmiters.
	 *
	 */
	public DelimitedHTMLWriter(Writer w, HTMLDocument doc, boolean delimit)
	{
		super(w, doc);
		this.delimit = delimit;
	}

	/**
	 * Creates a new HTMLWriter.
	 *
	 * @param w  a Writer
	 * @param doc an HTMLDocument
	 * @param pos the document location from which to fetch the content
	 * @param len the amount to write out
	 * @param delmit If true, will write delimiters.
	 */
	public DelimitedHTMLWriter(Writer w, HTMLDocument doc, int pos, int len, boolean delimit)
	{
		super(w, doc, pos, len);
		this.pos = pos;
		this.len = len;
		this.delimit = delimit;
	}

	/**
	 * Determines whether to position start/end delimiters for inline
	 * content or for block content.
	 *
	 * @throws IOException
	 * @throws BadLocationException
	 */
	public void write() throws IOException, BadLocationException
	{
		HTMLDocument doc = (HTMLDocument)getDocument();
		int start = getStartOffset();
		int end = getEndOffset();

		firstBlock = doc.getParagraphElement(start);
		while (firstBlock.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMPLIED)
		{
			firstBlock = firstBlock.getParentElement();
		}
		lastBlock = doc.getParagraphElement(end-1);
		while (lastBlock.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMPLIED)
		{
			lastBlock = lastBlock.getParentElement();
		}
		//wroteStart = wroteEnd = false;

		inlineContent = false;
		// Only one paragraph, which is either a table cell or the
		// selection does not cover it
		if (firstBlock == lastBlock &&
				(EditizeEditorKit.getElementName(firstBlock) == HTML.Tag.TD ||
				 EditizeEditorKit.getElementName(firstBlock) == HTML.Tag.TH ||
				 start > firstBlock.getStartOffset() ||
				 end < firstBlock.getEndOffset() - 1))
		{
			inlineContent = true;
		}

		inForm = false;

		super.write();
	}

	/**
	 * Writes start delimiter before or after first block tag.
	 *
	 * @param elem
	 * @throws IOException
	 * @throws BadLocationException
	 */
	protected void startTag(Element elem)
		throws IOException, BadLocationException
	{
		//if (delimit && !wroteStart && elem.getStartOffset() >= firstBlock.getStartOffset() && !inlineContent)
		if (delimit && elem == firstBlock && !inlineContent)
		{
			//wroteStart = true;
			writeStartAfterIndent = true;
		}
		super.startTag(elem);
		//if (delimit && !wroteStart && elem.getStartOffset() >= firstBlock.getStartOffset() && inlineContent)
		if (delimit && elem == firstBlock && inlineContent)
		{
			//wroteStart = true;
			writeStartAfterIndent = true;
		}
	}

		/**
		 * Strips false borders from zero-border tables.
		 * Outputs self-closing slash when AttributeSet dictates.
		 *
		 * @param attr   an AttributeSet
		 * @exception IOException on any I/O error
		 *
		 */
		protected void writeAttributes(AttributeSet attr)
			throws IOException
		{
			if (attr.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.TABLE) {
				MutableAttributeSet mattr = new SimpleAttributeSet(attr);

				if (attr.containsAttribute("trueborder", "0")) {
					mattr.addAttribute(HTML.Attribute.BORDER, "0");
				}
				mattr.removeAttribute("trueborder");
				mattr.removeAttribute(HTML.Attribute.STYLE);
				mattr.removeAttribute(CSS.Attribute.BORDER_STYLE);
				mattr.removeAttribute(CSS.Attribute.BORDER_COLOR);

				attr = mattr;
			}

			super.writeAttributes(attr);

			// Spot self-closing tags and self-close them!
			Object tName = attr.getAttribute(StyleConstants.NameAttribute);
			if (tName == HTML.Tag.IMG || tName == HTML.Tag.BR ||
				tName == HTML.Tag.INPUT || tName == HTML.Tag.HR ||
				tName == HTML.Tag.AREA || tName == HTML.Tag.META ||
				tName == HTML.Tag.BASE || tName == HTML.Tag.BASEFONT ||
				tName == HTML.Tag.LINK) {
				write(" /");
			}
		}


	/**
	 * Writes end delimiter before or after last block tag.
	 *
	 * @param elem
	 * @throws IOException
	 */
	protected void endTag(Element elem)
		throws IOException
	{
		//if (delimit && !wroteEnd && elem.getEndOffset() >= lastBlock.getEndOffset() && inlineContent)
		if (delimit && elem == lastBlock)
		{
			//wroteEnd = true;
			if (inlineContent) write(ENDDELIMITER);
			else writeEndBeforeNewline = 2;
		}
		super.endTag(elem);
		//if (delimit && !wroteEnd && elem.getEndOffset() >= lastBlock.getEndOffset() && !inlineContent)
	}

		/**
		 * Works around Sun JRE bug that writes line separator after an img when
		 * there is no content before it in a block.
		 *
		 * @param elem Element
		 * @throws BadLocationException
		 * @throws IOException
		 */
		protected void emptyTag(Element elem) throws BadLocationException, IOException {
		  AttributeSet attr = elem.getAttributes();
		  if (matchNameAttribute(attr, HTML.Tag.IMG)) {
			skipLineSeparator = true;
		  }
		  super.emptyTag(elem);
		  skipLineSeparator = false;
		  if (matchNameAttribute(attr, HTML.Tag.IMG)) {
			skipNextIndent = true;
		  }
		}
		private boolean skipLineSeparator = false;
		private boolean skipNextIndent = false;

	/**
	 * Line separator can trigger end delimiter output.
	 * @throws IOException
	 */
	protected void writeLineSeparator()
			throws IOException {
				if (skipLineSeparator) return;
		if (writeEndBeforeNewline == 1) write(ENDDELIMITER);
		super.writeLineSeparator();
	}

	/**
	 * Indentation can trigger start delimiter output.
	 * @throws IOException
	 */
	protected void indent()
			throws IOException
	{
		  if (!skipNextIndent) {
			super.indent();
		  }
		  skipNextIndent = false;
		  if (writeStartAfterIndent) write(STARTDELIMITER);
		  writeStartAfterIndent = false;
		  if (writeEndBeforeNewline > 0) writeEndBeforeNewline--;
	}

	/**
	 * Drop indent level by two because we're not writing the body,
	 * @return indent level
	 */
	protected int getIndentLevel() {
		return Math.max(0,super.getIndentLevel()-2);
	}


	/**
	 * Adds support for outputting form tags.
	 *
	 * @exception IOException on any I/O error
	 */
	protected void writeEmbeddedTags(AttributeSet attr) throws IOException
	{
		if (!inForm && attr.isDefined(HTML.Tag.FORM))
		{
			HTML.Tag tag = HTML.Tag.FORM;

			write('<');
			write(tag.toString());
			Object o = attr.getAttribute(tag);
			if (o != null && o instanceof AttributeSet) {
				writeAttributes((AttributeSet)o);
			}
			write('>');

			inForm = true;
		}

		super.writeEmbeddedTags(attr);
	}

	/**
	 * Adds support for outputting form tags.
	 *
	 * @param attr
	 * @throws IOException
	 */
	protected void closeOutUnwantedEmbeddedTags(AttributeSet attr)
			throws IOException
	{
		super.closeOutUnwantedEmbeddedTags(attr);

		if (inForm && (attr == null || attr.isDefined(HTML.Tag.CONTENT) && !attr.isDefined(HTML.Tag.FORM)))
		{
			write('<');
			write('/');
			write(HTML.Tag.FORM.toString());
			write('>');
			inForm = false;
		}

	}

	private boolean inForm;

	/**
	 * Disables code wrapping, which is buggy.
	 * @return boolean
	 */
	protected boolean getCanWrapLines() {
		return false;
	}

}
