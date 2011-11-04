package com.editize;

import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import java.io.*;
import javax.swing.text.html.parser.AttributeList;
import javax.swing.text.html.parser.DTDConstants;

public class ParserDelegator extends javax.swing.text.html.parser.ParserDelegator implements DTDConstants
{
	private static DTD dtd;

	protected static void setDefaultDTD()
	{
		if (dtd == null)
		{
			// (PENDING) Hate having to hard code!
			String nm = "html32";
			try
			{
				dtd = DTD.getDTD(nm);
			}
			catch (IOException e)
			{
				// (PENDING) UGLY!
				System.out.println("Throw an exception: could not get default dtd: " + nm);
			}
			dtd = createDTD(dtd, nm);
		}

		// Add support for summary attribute of table tag
		Element table = dtd.getElement("table");
		AttributeList summary = new AttributeList("summary", CDATA, IMPLIED, "", null, table.atts);
		table.atts = summary;

		// Adjust SPAN tag handling
		Element span = dtd.getElement("span");
		span.type = Element.MODEL;
		span.content = dtd.getElement("b").content;

		ContentModel content, temp;

		// Allow SPAN inside tags
		addElementToTagModel(dtd, span, "p");
		addElementToTagModel(dtd, span, "h1");
		addElementToTagModel(dtd, span, "h2");
		addElementToTagModel(dtd, span, "h3");
		addElementToTagModel(dtd, span, "h4");
		addElementToTagModel(dtd, span, "h5");
		addElementToTagModel(dtd, span, "h6");
		addElementToTagModel(dtd, span, "blockquote");
		addElementToTagModel(dtd, span, "pre");
		addElementToTagModel(dtd, span, "li");
		addElementToTagModel(dtd, span, "div");
		addElementToTagModel(dtd, span, "b");
		addElementToTagModel(dtd, span, "strong");
		addElementToTagModel(dtd, span, "i");
		addElementToTagModel(dtd, span, "em");
		addElementToTagModel(dtd, span, "u");
		addElementToTagModel(dtd, span, "font");
		addElementToTagModel(dtd, span, "center");
		addElementToTagModel(dtd, span, "td");
		addElementToTagModel(dtd, span, "th");
		addElementToTagModel(dtd, span, "tt");
		addElementToTagModel(dtd, span, "code");
		addElementToTagModel(dtd, span, "a");
		addElementToTagModel(dtd, span, "body");
	}

	private static void addElementToTagModel(DTD dtd, Element e, String tagName)
	{
		ContentModel content, temp;

//		System.out.println("Tag: " + tagName);

		content = dtd.getElement(tagName).content; // (tags)*

//		System.out.println("Before: " + content);

		content = (ContentModel)content.content; // (tag)s
		content = (ContentModel)content.content; // firsttag
		temp = new ContentModel(0,e,content.next);
		content.next = temp;

//		System.out.println("After: " + dtd.getElement(tagName).content + "\n");
	}

	public ParserDelegator() {
		if (dtd == null) {
			setDefaultDTD();
		}
	}

	public void parse(Reader r, HTMLEditorKit.ParserCallback cb, boolean ignoreCharSet) throws IOException {
		new DocumentParser(dtd).parse(r, cb, ignoreCharSet);
	}
}
