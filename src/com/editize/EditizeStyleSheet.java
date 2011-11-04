package com.editize;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.StyleConstants;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.Style;
import javax.swing.text.html.StyleSheet;
import java.awt.Color;

public class EditizeStyleSheet extends javax.swing.text.html.StyleSheet {
	/**
	 * Works like getStyle, but also searches linked style sheets if the
	 * requested style isn't found in this style sheet.
	 */
	public Style lookupStyle(String name) {
		return lookupStyle(name, this);
	}

	private Style lookupStyle(String name, StyleSheet ss)
	{
		Style style = ss.getStyle(name);
		if (style == null) {
			StyleSheet[] sheets = ss.getStyleSheets();
			if (sheets != null) for (int i = 0; i < sheets.length; i++)
			{
				style = lookupStyle(name, sheets[i]);
				if (style != null) break;
			}
		}
		return style;
	}

  public AttributeSet translateHTMLToCSS(AttributeSet htmlAttrSet)
  {
	AttributeSet cssAttrSet = super.translateHTMLToCSS(htmlAttrSet);

	Element elem = (Element)htmlAttrSet;
	HTML.Tag tag = getHTMLTag(htmlAttrSet);
	if ((tag == HTML.Tag.TD) || (tag == HTML.Tag.TH))
	{
	  AttributeSet tableAttr = elem.getParentElement().getParentElement().getAttributes();
	  if (tableAttr.containsAttribute("trueborder", "0")) {
		  tableAttr = super.translateHTMLToCSS(tableAttr);
		  ((MutableAttributeSet)cssAttrSet).addAttribute(
				  CSS.Attribute.BORDER_STYLE, "solid");
		  ((MutableAttributeSet)cssAttrSet).addAttribute(
				  CSS.Attribute.BORDER_COLOR,
				  tableAttr.getAttribute(CSS.Attribute.BORDER_COLOR));
	  }
	}

	return cssAttrSet;
  }

  private HTML.Tag getHTMLTag(AttributeSet htmlAttrSet)
  {
	Object o = htmlAttrSet.getAttribute(StyleConstants.NameAttribute);
	if (o instanceof HTML.Tag)
	{
	  HTML.Tag tag = (HTML.Tag) o;
	  return tag;
	}
	return null;
  }
}
