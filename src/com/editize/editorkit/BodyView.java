package com.editize.editorkit;

import javax.swing.text.html.BlockView;
import javax.swing.text.Element;
import javax.swing.SizeRequirements;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.StyleConstants;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.Rectangle;
import javax.swing.event.DocumentEvent;
import javax.swing.text.ViewFactory;

public class BodyView extends BlockView
{
  public BodyView(Element elem, int axis)
  {
    super(elem,axis);
  }

  /**
   * Indicates the body element is flexible along the major axis, so that it
   * will fill the pane even if the content is not long enough.
   * @param axis
   * @param r
   * @return
   */
  protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
      r = super.calculateMajorAxisRequirements(axis, r);
      r.maximum = Integer.MAX_VALUE;
      return r;
  }

  /**
   * Renders using the given rendering surface and area on that
   * surface.  This is implemented to delgate to the superclass
   * after stashing the base coordinate for tab calculations.
   *
   * @param g the rendering surface to use
   * @param a the allocated region to render into
   * @see View#paint
   */
  public void paint(Graphics g, Shape a) {
      Rectangle r;
      if (a instanceof Rectangle) {
          r = (Rectangle) a;
      } else {
          r = a.getBounds();
      }
      painter.paint(g, r.x, r.y, r.width, r.height, this);
      super.paint(g, a);
  }

  /**
   * Fetches the attributes to use when rendering.  This is
   * implemented to multiplex the attributes specified in the
   * model with a StyleSheet.
   */
  public AttributeSet getAttributes() {
      if (attr == null) {
          StyleSheet sheet = getStyleSheet();
          attr = sheet.getViewAttributes(this);
      }
      return attr;
  }

  /**
   * Sets up the body from css attributes instead of
   * the values found in StyleConstants (i.e. which are used
   * by the superclass).
   */
  protected void setPropertiesFromAttributes() {
      StyleSheet sheet = getStyleSheet();
      attr = sheet.getViewAttributes(this);
      painter = sheet.getBoxPainter(attr);
      if (attr != null) {
          super.setPropertiesFromAttributes();
          setInsets((short) painter.getInset(TOP, this),
                    (short) painter.getInset(LEFT, this),
                    (short) painter.getInset(BOTTOM, this),
                    (short) painter.getInset(RIGHT, this));
      }
  }

  /**
   * The body needs to pay attention to changes that affect the entire document.
   * @param changes
   * @param a
   * @param f
   */
  public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
      super.changedUpdate(changes, a, f);
      int pos = changes.getOffset();
      if (pos <= getStartOffset() && (pos + changes.getLength()) >=
          getEndOffset()-1) {
          setPropertiesFromAttributes();
      }
  }

  private AttributeSet attr;
  private StyleSheet.BoxPainter painter;

}