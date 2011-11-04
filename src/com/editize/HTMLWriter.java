package com.editize;

import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.io.IOException;
import java.io.Writer;

public class HTMLWriter extends javax.swing.text.html.HTMLWriter {

	private boolean writingAttributes = false;
	private boolean writingAttributeValue = false;
	private char[] tempChars = null;

	public HTMLWriter(Writer w, HTMLDocument doc) {
		super(w, doc);
	}

	public HTMLWriter(Writer w, HTMLDocument doc, int pos, int len) {
		super(w, doc, pos, len);
	}

	/**
	 * Writes out the attribute set.  Ignores all
	 * attributes with a key of type HTML.Tag,
	 * attributes with a key of type StyleConstants,
	 * and attributes with a key of type
	 * HTML.Attribute.ENDTAG.
	 *
	 * @param attr   an AttributeSet
	 * @exception IOException on any I/O error
	 *
	 */
	protected void writeAttributes(AttributeSet attr) throws IOException {
		writingAttributes = true;
		super.writeAttributes(attr);
		writingAttributes = false;
	}

	protected void write(String content) throws IOException {
		// Apply HTML escaping to the attribute value
		if (writingAttributes) {
			int startQuote = content.indexOf('"');
			super.write(content.substring(0, startQuote + 1));
			writingAttributeValue = true;
			super.write(content.substring(startQuote + 1, content.length() - 1));
			writingAttributeValue = false;
			super.write("\"");
			return;
		}
		super.write(content);
	}

	/**
	 * This method is overriden to map any character entities, such as
	 * &lt; to &amp;lt;. <code>super.output</code> will be invoked to
	 * write the content. Implementation inherited from JRE 1.5.0_01.
	 */
	protected void output(char[] chars, int start, int length)
			   throws IOException {
		if (!writingAttributeValue) {
			super.output(chars, start, length);
			return;
		}
		int last = start;
		length += start;
		for (int counter = start; counter < length; counter++) {
		// This will change, we need better support character level
		// entities.
		switch(chars[counter]) {
		// Character level entities.
		case '<':
		if (counter > last) {
			super.output(chars, last, counter - last);
		}
		last = counter + 1;
		output("&lt;");
		break;
		case '>':
		if (counter > last) {
			super.output(chars, last, counter - last);
		}
		last = counter + 1;
		output("&gt;");
		break;
		case '&':
		if (counter > last) {
			super.output(chars, last, counter - last);
		}
		last = counter + 1;
		output("&amp;");
		break;
		case '"':
		if (counter > last) {
			super.output(chars, last, counter - last);
		}
		last = counter + 1;
		output("&quot;");
		break;
		// Special characters
		case '\n':
		case '\t':
		case '\r':
		break;
		default:
		if (chars[counter] < ' ' || chars[counter] > 127) {
			if (counter > last) {
			super.output(chars, last, counter - last);
			}
			last = counter + 1;
			// If the character is outside of ascii, write the
			// numeric value.
			output("&#");
			output(String.valueOf((int)chars[counter]));
			output(";");
		}
		break;
		}
	}
	if (last < length) {
		super.output(chars, last, length - last);
	}
	}

	/**
	 * This directly invokes super's <code>output</code> after converting
	 * <code>string</code> to a char[].
     * @param string The string to output.
     * @throws IOException if something goes wrong.
	 */
	private void output(String string) throws IOException {
		int length = string.length();
		if (tempChars == null || tempChars.length < length) {
			tempChars = new char[length];
		}
		string.getChars(0, length, tempChars, 0);
		super.output(tempChars, 0, length);
        tempChars = null;
    }

    /**
     * Writes out text that is contained in a TEXTAREA form
     * element.
     *
     * @param attr  an AttributeSet
     * @exception IOException on any I/O error
     * @exception javax.swing.text.BadLocationException if pos represents an invalid
     *            location within the document.
     */
    protected void textAreaContent(AttributeSet attr) throws BadLocationException, IOException {
        Document doc = (Document)attr.getAttribute(StyleConstants.ModelAttribute);
        if (doc != null && doc.getLength() > 0) {
            if (segment == null) {
            segment = new Segment();
            }
            doc.getText(0, doc.getLength(), segment);
            if (segment.count > 0) {
                output(segment.array, segment.offset, segment.count);
            }
        }
    }


    protected void endTag(Element elem) throws IOException {
        if (matchNameAttribute(elem.getAttributes(), HTML.Tag.TEXTAREA))
        {
            write('<');
            write('/');
            write(elem.getName());
            write('>');
        	writeLineSeparator();
        }
        else
        {
            super.endTag(elem);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    private Segment segment;
}
