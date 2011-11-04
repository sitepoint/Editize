package com.editize.editorkit;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Extends <code>ParagraphView</code> to support left/right floating images in the margins.
 *
 * TODO:
 *  - Apply margins to subsequent ParagraphViews when appropriate.
 *      Possible alternative: Extend paragraph's lower margin to accomodate floating images for now.
 *
 * @author Kevin Yank
 * @version 1.0
 */
public class ParagraphView extends javax.swing.text.html.ParagraphView
{
	private int justification;
	private float lineSpacing;
	private FloatMarginStack leftMarginStack = new FloatMarginStack();
	private FloatMarginStack rightMarginStack = new FloatMarginStack();
    /**
     * Flag to indicate whether floats intruding on this paragraph may affect
     * subsequent paragraphs. Initially set true so that a newly-created paragraph
     * added above a paragraph, moving it out of the way of an intruding float,
     * will cause that paragraph to be notified of this change.
     */
    private boolean passForwardFloaters = true;

	/**
	 * Creates a new view that represents an HTML paragraph-type element.
	 *
	 * @param elem the element to create a view for
	 */
	public ParagraphView(Element elem)
	{
		super(elem);
		strategy = new FloatingFlowStrategy();
	}

	/**
	 * Set the amount of line spacing
	 */
	protected void setLineSpacing(float ls)
	{
		super.setLineSpacing(ls);
		lineSpacing = ls;
	}

	protected float getLineSpacing()
	{
		return lineSpacing;
	}

	/**
	 * Set the type of justification.
	 */
	protected void setJustification(int j)
	{
		super.setJustification(j);
	        justification = j;
	}

	protected int getJustification()
	{
		return justification;
	}

	protected void setPropertiesFromAttributes()
	{
		super.setPropertiesFromAttributes();
		AttributeSet attr = getAttributes();
		if (attr != null)
		{
			lineSpacing = StyleConstants.getLineSpacing(attr);
		}
	}

	public void paint(Graphics g, Shape a)
	{
		super.paint(g,a);
		paintFloatingImages(g,a);
	}

	/**
	 * Paints floating images in the margins of the paragraph.
	 *
	 * @param g the rendering surface to use
	 * @param a the allocated region to render into
	 */
	protected void paintFloatingImages(Graphics g, Shape a)
	{
		View parent = getParent();
		int n = parent.getViewCount();
		int index = 0; // dummy value
		for (int i=0; i<n; i++)
			if (parent.getView(i) == this)
			{
				index = i;
				break;
			}

		ParagraphView pv = this;
		View prev = null;
		if (index > 0) prev = parent.getView(--index);
		int rewindHeight = 0;

		do
		{
			int pos = pv.getTopInset() - rewindHeight; // Vertical position
			n = pv.getViewCount();

		    if (prev != null && prev instanceof ParagraphView &&
				((ParagraphView)prev).passForwardFloaters)
			{
				pv.leftMarginStack = (FloatMarginStack)((ParagraphView)prev).leftMarginStack.copy();
				pv.rightMarginStack = (FloatMarginStack)((ParagraphView)prev).rightMarginStack.copy();

				pv.leftMarginStack.moveDown(pv.getTopInset());
				pv.rightMarginStack.moveDown(pv.getTopInset());
			}
			else
			{
				pv.leftMarginStack.clear();
				pv.rightMarginStack.clear();
			}

			// For each row
			for (int i=0; i<n; i++)
			{
				Row row = (Row)pv.getView(i);
				int rowHeight = row.calculateMinorAxisRequirements(Y_AXIS,null).preferred;
				int m = row.getViewCount();

				// For each span
				boolean firstView = true;
				for (int j=0; j<m; j++)
				{
					View v = row.getView(j);
					if (v instanceof ImageView && ((ImageView)v).isFloater())
					{
						ImageView iv = (ImageView)v;
						pv.paintImageView(g,a,iv,pos);
						if (iv.getLayoutType() == ImageView.FLOAT_LEFT)
							pv.leftMarginStack.addFloater((int)iv.getPreferredSpan(X_AXIS,false),(int)iv.getPreferredSpan(Y_AXIS,false));
						else
							pv.rightMarginStack.addFloater((int)iv.getPreferredSpan(X_AXIS,false),(int)iv.getPreferredSpan(Y_AXIS,false));
					}
					else if (firstView)
					{
						firstView = false;
						pv.leftMarginStack.moveDown(rowHeight);
						pv.rightMarginStack.moveDown(rowHeight);
						pos += rowHeight;
					}
				}
			}

			pv.leftMarginStack.moveDown(pv.getBottomInset());
			pv.rightMarginStack.moveDown(pv.getBottomInset());

		    if (index-- < 0) break;
			View v = prev;
			prev = index < 0 ? null : parent.getView(index);
			if (!(v instanceof ParagraphView)) break;
			pv = (ParagraphView)v;
			rewindHeight += pv.getPreferredSpan(Y_AXIS);
		}
		while (pv.passForwardFloaters);
	}

	void paintImageView(Graphics g, Shape a, ImageView iv, int pos)
	{
		Rectangle r = a.getBounds();
		if (iv.getLayoutType() == ImageView.FLOAT_RIGHT)
		{
			r.x += r.width - iv.getPreferredSpan(X_AXIS,false) - rightMarginStack.getMarginSize();
		}
		else
		{
			r.x += leftMarginStack.getMarginSize();
		}
		r.y += pos;
		r.width = (int) iv.getPreferredSpan(X_AXIS,false);
		r.height = (int) iv.getPreferredSpan(Y_AXIS,false);

		iv.paint(g,r,false);
	}

	/**
	 * Create a View that should be used to hold a
	 * a row's worth of children in a flow.
	 */
	protected View createRow()
	{
        return new Row(getElement());
	}

    /**
     * Sets the offsets of a row (in particlar, its insets) based on the
     * properties of its containing paragraph. This is called by layoutRow
     * both when a row is newly created, and when a row is reused
     * (in JDK 1.6 or later).
     *
     * @param row The row to adjust
     */
    protected void adjustRowInsets(Row row)
    {
		// Get default insets
		short top = row.getDefaultTopInset();
		short left = row.getDefaultLeftInset();
		short bottom = row.getDefaultBottomInset();
		short right = row.getDefaultRightInset();

		// Adjust for line spacing
		if (lineSpacing > 1)
		{
			float height = row.getPreferredSpan(View.Y_AXIS);
			float addition = (height * lineSpacing) - height;
			if(addition > 0) bottom = (short)addition;
		}

		// Adjust for floating images
		left += leftMarginStack.getMarginSize();
		right += rightMarginStack.getMarginSize();

		row.setInsets(top,left,bottom,right);
    }

    /**
	 * Fetch the constraining span to flow against for
	 * the given child index. This has been re-implemented
	 * because Row was also re-implemented, so the superclass'
	 * version of this method does not use the right Row class.
	 */
	public int getFlowSpan(int index)
	{
		View child = getView(index);
		int adjust = 0;
		if (child instanceof Row)
		{
			Row row = (Row) child;
			adjust = row.getLeftInset() + row.getRightInset();
		}
        return layoutSpan - adjust;
	}

	/**
	 * Fetch the location along the flow axis that the
	 * flow span will start at. This has been re-implemented
	 * because Row was also re-implemented, so the superclass'
	 * version of this method does not use the right Row class.
	 */
	public int getFlowStart(int index)
	{
		View child = getView(index);
		int adjust = 0;
		if (child instanceof Row)
		{
			Row row = (Row) child;
			adjust = row.getLeftInset();
		}
		return (int)getTabBase() + adjust;
	}

    /**
	 * Internally created view that has the purpose of holding
	 * the views that represent the children of the paragraph
	 * that have been arranged in rows.
	 */
	protected class Row extends BoxView
	{
        private short defaultTopInset;
        private short defaultLeftInset;
        private short defaultRightInset;
        private short defaultBottomInset;

        Row(Element elem)
		{
			super(elem, View.X_AXIS);
            defaultTopInset = getTopInset();
            defaultLeftInset = getLeftInset();
            defaultRightInset = getRightInset();
            defaultBottomInset = getBottomInset();
        }

		protected short getTopInset()
		{
			return super.getTopInset();
		}

		protected short getLeftInset()
		{
			return super.getLeftInset();
		}

		protected short getRightInset()
		{
			return super.getRightInset();
		}

		protected short getBottomInset()
		{
			return super.getBottomInset();
		}

        public short getDefaultTopInset() {
            return defaultTopInset;
        }

        public short getDefaultLeftInset() {
            return defaultLeftInset;
        }

        public short getDefaultRightInset() {
            return defaultRightInset;
        }

        public short getDefaultBottomInset() {
            return defaultBottomInset;
        }

		protected void setInsets(short top, short left, short bottom, short right)
		{
			super.setInsets(top,left,bottom,right);
		}

		/**
		 * This is reimplemented to do nothing since the
		 * paragraph fills in the row with its needed
		 * children.
		 */
		protected void loadChildren(ViewFactory f)
		{
		}

		/**
		 * Fetches the attributes to use when rendering.  This view
		 * isn't directly responsible for an element so it returns
		 * the outer classes attributes.
		 */
		public AttributeSet getAttributes()
		{
			View p = getParent();
			return (p != null) ? p.getAttributes() : null;
		}

		public float getAlignment(int axis)
		{
			if (axis == View.X_AXIS)
			{
				switch (justification)
				{
					case StyleConstants.ALIGN_LEFT:
						return 0;
					case StyleConstants.ALIGN_RIGHT:
						return 1;
					case StyleConstants.ALIGN_CENTER:
					case StyleConstants.ALIGN_JUSTIFIED:
						return 0.5f;
				}
			}
			return super.getAlignment(axis);
		}

		/**
		 * Provides a mapping from the document model coordinate space
		 * to the coordinate space of the view mapped to it.  This is
		 * implemented to let the superclass find the position along
		 * the major axis and the allocation of the row is used
		 * along the minor axis, so that even though the children
		 * are different heights they all get the same caret height.
		 *
		 * @param pos the position to convert
		 * @param a the allocated region to render into
		 * @return the bounding box of the given position
		 * @exception BadLocationException  if the given position does not represent a
		 *   valid location in the associated document
		 * @see View#modelToView
		 */
		public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException
		{
			Rectangle r = a.getBounds();
			View v = getViewAtPosition(pos, r);
			if ((v != null) && (!v.getElement().isLeaf()))
			{
				// Don't adjust the height if the view represents a branch.
				return super.modelToView(pos, a, b);
			}
			r = a.getBounds();
			int height = r.height;
			int y = r.y;
			Shape loc = super.modelToView(pos, a, b);
			r = loc.getBounds();
			r.height = height;
			r.y = y;
			return r;
		}

		protected boolean isBefore(int x, int y, Rectangle innerAlloc)
		{
			if (getViewCount() > 0 && getView(0) instanceof ImageView)
			{
				ImageView iv = (ImageView)getView(0);
				int imgWidth = (int) iv.getPreferredSpan(getAxis(),false);
				if (iv.getLayoutType() == ImageView.FLOAT_LEFT)
				{
					x -= imgWidth;
				}
			}
			return super.isBefore(x,y,innerAlloc);
		}

		protected boolean isAfter(int x, int y, Rectangle innerAlloc)
		{
			if (getViewCount() > 0 && getView(0) instanceof ImageView)
			{
				ImageView iv = (ImageView)getView(0);
				int imgWidth = (int) iv.getPreferredSpan(getAxis(),false);
				if (iv.getLayoutType() == ImageView.FLOAT_LEFT)
				{
					x -= imgWidth;
				}
			}
			return super.isAfter(x,y,innerAlloc);
		}

		/**
		 * Range represented by a row in the paragraph is only
		 * a subset of the total range of the paragraph element.
		 */
		public int getStartOffset()
		{
			int offs = Integer.MAX_VALUE;
			int n = getViewCount();
			for (int i = 0; i < n; i++)
			{
				View v = getView(i);
				offs = Math.min(offs, v.getStartOffset());
			}
			return offs;
		}

		public int getEndOffset()
		{
			int offs = 0;
			int n = getViewCount();
			for (int i = 0; i < n; i++)
			{
	        		View v = getView(i);
				offs = Math.max(offs, v.getEndOffset());
			}
			return offs;
		}

		/**
		 * Perform layout for the major axis of the box (i.e. the
		 * axis that it represents).  The results of the layout should
		 * be placed in the given arrays which represent the allocations
		 * to the children along the major axis.
         * Returns the offset and span for each child view in the
         * offsets and spans parameters.
		 *
		 * @param targetSpan the total span given to the view, which
		 *  whould be used to layout the children.
		 * @param axis the axis being layed out.
		 * @param offsets the offsets from the origin of the view for
		 *  each of the child views.  This is a return value and is
		 *  filled in by the implementation of this method.
		 * @param spans the span of each child view.  This is a return
		 *  value and is filled in by the implementation of this method.
		 */
		protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans)
		{
			// Step 1: Calculate the preferred sizes and the flexibility to adjust the sizes.
			long minimum = 0;
			long maximum = 0;
			long preferred = 0;
			int n = getViewCount();
			for (int i = 0; i < n; i++)
			{
				View v = getView(i);
				spans[i] = (int) v.getPreferredSpan(axis);
				preferred += spans[i];
				minimum += v.getMinimumSpan(axis);
				maximum += v.getMaximumSpan(axis);
			}

			// Step 2: expand or contract by as much as possible to reach the target span.
			long desiredAdjustment = targetSpan - preferred;
			float adjustmentFactor = 0.0f;
			if (desiredAdjustment != 0)
			{
				float maximumAdjustment = (desiredAdjustment > 0) ?
				        maximum - preferred : preferred - minimum;
				if (maximumAdjustment == 0.0f)
				{
					adjustmentFactor = 0.0f;
				}
				else
				{
					adjustmentFactor = desiredAdjustment / maximumAdjustment;
					adjustmentFactor = Math.min(adjustmentFactor, 1.0f);
					adjustmentFactor = Math.max(adjustmentFactor, -1.0f);
				}
			}

			// Step 3: Make adjustments and set offsets/spans.
			int totalOffset = 0;
			for (int i = 0; i < n; i++)
			{
				View v = getView(i);

				// Set offset
				offsets[i] = totalOffset;

				// Adjust spans
				int availableSpan = (adjustmentFactor > 0.0f) ?
					(int) v.getMaximumSpan(axis) - spans[i] : spans[i] - (int) v.getMinimumSpan(axis);
				float adjF = adjustmentFactor * availableSpan;
				if (adjF < 0)
				{
					adjF -= .5f;
				}
				else
				{
	        			adjF += .5f;
				}
				int adj = (int)adjF;
				spans[i] += adj;

				totalOffset = (int) Math.min((long) totalOffset + (long) spans[i], Integer.MAX_VALUE);
			}
		}

        /**
         * Perform layout for the minor axis of the box (i.e. the
         * axis orthoginal to the axis that it represents).  The results
         * of the layout should be placed in the given arrays which represent
         * the allocations to the children along the minor axis.
         *
         * This is implemented to do a baseline layout of the children
         * by calling <code>BoxView.baselineLayout</code>.
         *
         * @param targetSpan the total span given to the view, which
         *                   whould be used to layout the children.
         * @param axis       the axis being layed out.
         * @param offsets    the offsets from the origin of the view for
         *                   each of the child views.  This is a return value and is
         *                   filled in by the implementation of this method.
         * @param spans      the span of each child view.  This is a return
         *                   value and is filled in by the implementation of this method.
         */
		protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans)
		{
			baselineLayout(targetSpan, axis, offsets, spans);
		}

        /**
         * Calculates the size requirements for the minor axis <code>axis</code>.
         * @param axis The minor axis (<code>Y_AXIS</code> in LTR/RTL text).
         * @param r The <code>SizeRequirements</code> object to return. If null, a new object will
         * be created.
         * @return The size requirements for the minor axis.
         */
		protected SizeRequirements calculateMinorAxisRequirements(int axis,
			SizeRequirements r)
		{
            return baselineRequirements(axis, r);
        }

		/**
		 * Fetches the child view index representing the given position in
		 * the model.
		 *
		 * @param pos the position >= 0
		 * @return index of the view representing the given position, or
		 *  -1 if no view represents that position
		 */
		protected int getViewIndexAtPosition(int pos)
		{
			// This is expensive, but are views are not necessarily layed
			// out in model order.
			if(pos < getStartOffset() || pos >= getEndOffset())
				return -1;
			for(int counter = getViewCount() - 1; counter >= 0; counter--)
			{
				View v = getView(counter);
				if(pos >= v.getStartOffset() &&
				pos < v.getEndOffset())
				{
			        	return counter;
				}
			}
			return -1;
		}
    }

	public static class FloatingFlowStrategy extends FlowStrategy
	{
        /**
		 * Creates all Rows and their contents from scratch whenever the paragraph's
		 * layout is invalidated.
		 * @param fv The FlowView to lay out.
		 */
		public void layout(FlowView fv)
		{
			if (fv instanceof ParagraphView)
			{
				ParagraphView pv = (ParagraphView)fv;

				// Get previous sibling
				View parent = pv.getParent();
				int n = parent.getViewCount();
				View prevSibling = null;
				View nextSibling = null;
				for (int i=0; i<n; i++)
				{
					if (parent.getView(i) == pv)
					{
						if (i > 0) prevSibling = parent.getView(i-1);
						if (i < n-1) nextSibling = parent.getView(i+1);
						break;
					}
				}

                // If the paragraph needs any layout work, invalidate the major axis to
                // prevent incomplete recalculation of the float intrustions
                // @todo Improve the float intrusion tracking so that partial layouts are supported,
                pv.layoutChanged(Y_AXIS);

                // If the previous sibling is a paragraphView
                if (prevSibling != null && prevSibling instanceof ParagraphView)
                {
                    pv.leftMarginStack = (FloatMarginStack)((ParagraphView)prevSibling).leftMarginStack.copy();
                    pv.rightMarginStack = (FloatMarginStack)((ParagraphView)prevSibling).rightMarginStack.copy();

                    pv.leftMarginStack.moveDown(pv.getTopInset());
                    pv.rightMarginStack.moveDown(pv.getTopInset());
                }
                else
                {
                    // Initialize floating image state
                    pv.leftMarginStack.clear();
                    pv.rightMarginStack.clear();
                }

                super.layout(fv);

				pv.leftMarginStack.moveDown(pv.getBottomInset());
				pv.rightMarginStack.moveDown(pv.getBottomInset());

				boolean wasPassForward = pv.passForwardFloaters;
				pv.passForwardFloaters =
					(pv.leftMarginStack.getRemainingLength() > 0 ||
					pv.rightMarginStack.getRemainingLength() > 0);

				// Invalidate following paragraph if appropriate
				if (nextSibling != null && nextSibling instanceof ParagraphView &&
					(wasPassForward || pv.passForwardFloaters))
				{
                    ((ParagraphView)nextSibling).layoutChanged(X_AXIS);
				    ((ParagraphView)nextSibling).layoutChanged(Y_AXIS);
				}
			}
			else super.layout(fv);
		}

		/**
		 * Fills a row with as many sub-views as will fit. Updates the floating
		 * image indentation state if neccessary.
		 *
		 * @param fv The FlowView for which to lay out a row.
		 * @param rowIndex The index of the row View to lay out.
		 * @param pos The document position to begin the row.
		 * @return The document position at the end of the row.
		 */
		protected int layoutRow(FlowView fv, int rowIndex, int pos)
		{
            if (fv instanceof ParagraphView)
            {
                // Adjust row insets for floats
                Row row = (Row)fv.getView(rowIndex);
                ((ParagraphView)fv).adjustRowInsets(row);

                // Perform row layout
                int x = fv.getFlowStart(rowIndex);
                int spanLeft = fv.getFlowSpan(rowIndex);
                int end = fv.getEndOffset();
                TabExpander te = (TabExpander)fv;

                // Indentation.
                int preX = x;
                int availableSpan = spanLeft;

                // Empty the row (JDK 1.6 reuses rows, so may not be empty)
                row.removeAll();

                boolean forcedBreak = false;
                boolean firstViewImage = true;
                while (pos < end  && spanLeft >= 0)
                {
                    int n = row.getViewCount();
                    View v = createView(fv, pos, spanLeft, rowIndex);
                    if (v == null)
                    {
                        break;
                    }

                    int chunkSpan;
                    if (v instanceof TabableView)
                    {
                        chunkSpan = (int) ((TabableView)v).getTabbedSpan(x, te);
                        firstViewImage = false;
                    }
                    else if (v instanceof ImageView)
                    {
                        chunkSpan = (int) v.getPreferredSpan(X_AXIS);
                        ImageView iv = (ImageView)v;
                        if (!iv.isFloater()) firstViewImage = false;
                        else if (firstViewImage && iv.getLayoutType() == ImageView.FLOAT_RIGHT)
                        {
                            // Right-aligned images that appear first in the row
                            // contribute to the span of this row.
                            chunkSpan += (short) iv.getPreferredSpan(X_AXIS,false);
                        }
                    }
                    else
                    {
                        chunkSpan = (int) v.getPreferredSpan(View.X_AXIS);
                        firstViewImage = false;
                    }

                    // If a forced break is necessary, break
                    if (v.getBreakWeight(View.X_AXIS, pos, spanLeft) >= ForcedBreakWeight)
                    {
                        if (n > 0)
                        {
                            // If this is a forced break and it's not the only view
                            // the view should be replaced with a call to breakView.
                            // If it's it only view, it should be used directly.  In
                            // either case no more children should be added beyond this
                            // view.
                            v = v.breakView(X_AXIS, pos, x, spanLeft);
                            if (v != null)
                            {
                                if (v instanceof TabableView)
                                {
                                    chunkSpan = (int) ((TabableView)v).getTabbedSpan(x, te);
                                }
                                else
                                {
                                    chunkSpan = (int) v.getPreferredSpan(View.X_AXIS);
                                }
                            }
                            else
                            {
                                chunkSpan = 0;
                            }
                        }
                        forcedBreak = true;
                    }

                    spanLeft -= chunkSpan;
                    x += chunkSpan;
                    if (v != null)
                    {
                        row.append(v);
                        pos = v.getEndOffset();
                    }
                    if (forcedBreak)
                    {
                        break;
                    }

                } // while
                if (spanLeft < 0)
                {
                    // This row is too long and needs to be adjusted.
                    adjustRow(fv, rowIndex, availableSpan, preX);
                }
                else if (row.getViewCount() == 0)
                {
                    // Impossible spec... put in whatever is left.
                    View v = createView(fv, pos, Integer.MAX_VALUE, rowIndex);
                    row.append(v);
                }
                int endPos = row.getEndOffset();

                // Process any floating images in the new row
                int n = row.getViewCount();
                firstViewImage = true;
                SizeRequirements sr = row.calculateMinorAxisRequirements(Y_AXIS,null);
                for (int i=0; i<n; i++)
                {
                    View v = row.getView(i);
                    if (v instanceof ImageView)
                    {
                        ImageView iv = (ImageView)v;
                        if (iv.isFloater())
                        {
                            // Update float state
                            if (iv.getLayoutType() == ImageView.FLOAT_LEFT)
                            {
                                ((ParagraphView)fv).leftMarginStack.addFloater((int)iv.getPreferredSpan(X_AXIS,false),(int)iv.getPreferredSpan(Y_AXIS,false));
                            }
                            else
                            {
                                ((ParagraphView)fv).rightMarginStack.addFloater((int)iv.getPreferredSpan(X_AXIS,false),(int)iv.getPreferredSpan(Y_AXIS,false));
                            }
                        }
                        else if (firstViewImage)
                        {
                            firstViewImage = false;
                            ((ParagraphView)fv).leftMarginStack.moveDown(sr.preferred);
                            ((ParagraphView)fv).rightMarginStack.moveDown(sr.preferred);
                        }
                    }
                    else if (firstViewImage)
                    {
                        firstViewImage = false;
                        ((ParagraphView)fv).leftMarginStack.moveDown(sr.preferred);
                        ((ParagraphView)fv).rightMarginStack.moveDown(sr.preferred);
                    }
                }
                if (firstViewImage)
                {
                    ((ParagraphView)fv).leftMarginStack.moveDown(sr.preferred);
                    ((ParagraphView)fv).rightMarginStack.moveDown(sr.preferred);
                }

                // If nothing fit in the row, force-feed it the next inline element to prevent
                // an infinite loop in Java 6, which does not guard against this
                if (row.getViewCount() == 0) {
                    row.append(createView(fv, pos, Integer.MAX_VALUE, rowIndex));
                    endPos = row.getEndOffset();
                }

                return endPos;
            }
            else
            {
                return super.layoutRow(fv, rowIndex, pos);
            }
        }

		/**
		 * Adjusts the given row if possible to fit within the
		 * layout span.  By default this will try to find the
		 * highest break weight possible nearest the end of
		 * the row.  If a forced break is encountered, the
		 * break will be positioned there.
		 *
		 * @param fv the FlowView containing the row to adjust to the current layout
		 *  span.
         * @param rowIndex the row to adjust
		 * @param desiredSpan the current layout span >= 0
		 * @param x the location r starts at.
		 */
		protected void adjustRow(FlowView fv, int rowIndex, int desiredSpan, int x)
		{
			View r = fv.getView(rowIndex);
			int n = r.getViewCount();
			int span = 0;
			int bestWeight = BadBreakWeight;
			int bestSpan = 0;
			int bestIndex = -1;
			//int bestOffset = 0;
			View v;
			for (int i = 0; i < n; i++)
			{
				v = r.getView(i);
				int spanLeft = desiredSpan - span;

				int w = v.getBreakWeight(X_AXIS, x + span, spanLeft);
				if ((w >= bestWeight) && (w > BadBreakWeight))
				{
					bestWeight = w;
					bestIndex = i;
					bestSpan = span;
					if (w >= ForcedBreakWeight)
					{
						// it's a forced break, so there is
						// no point in searching further.
						break;
					}
				}
				span += v.getPreferredSpan(X_AXIS);
				if (i==0 && v instanceof ImageView && ((ImageView)v).getLayoutType() == ImageView.FLOAT_RIGHT)
					span += ((ImageView)v).getPreferredSpan(X_AXIS,false);

			}
			if (bestIndex < 0)
			{
				// there is nothing that can be broken, leave
				// it in it's current state.
				return;
			}

			// Break the best candidate view, and patch up the row.
			int spanLeft = desiredSpan - bestSpan;
			v = r.getView(bestIndex);
			v = v.breakView(X_AXIS, v.getStartOffset(), x + bestSpan, spanLeft);
			View[] va = new View[1];
			va[0] = v;

            // New for Java 1.6 - set parent of broken views
            View lv = getLogicalView(fv);
            int p0 = r.getView(bestIndex).getStartOffset();
            int p1 = r.getEndOffset();
            for (int i = 0; i < lv.getViewCount(); i++) {
                View tmpView = lv.getView(i);
                if (tmpView.getEndOffset() > p1) {
                    break;
                }
                if (tmpView.getStartOffset() >= p0) {
                    tmpView.setParent(lv);
                }
            }

            r.replace(bestIndex, n - bestIndex, va);
		}
	}

	/**
	 * Data structure used to track images floating in the margins of paragraphs.
	 *
	 * @author Kevin Yank
	 * @version 1.0
	 */
	protected static class FloatMarginStack
	{
		private java.util.Stack stack;

		/**
		 * Create an empty FloatMarginStack.
		 */
		protected FloatMarginStack()
		{
			stack = new java.util.Stack();
		}

		/**
		 * Adds a floating image block to begin at the current positon.
		 *
		 * @param size Amount of the margin the floating block consumes.
		 * @param length Length of the floating block (i.e. image height).
		 */
		protected void addFloater(int size, int length)
		{
			stack.push(new Dimension(getMarginSize() + size,length));
		}

		/**
		 * Gets the current amount of the margin occupied by floating image blocks.
		 *
		 * @return The size of the margin due to floating blocks.
		 */
		protected int getMarginSize()
		{
			try
			{
				Dimension dim = (Dimension)stack.peek();
				return dim.width;
			}
			catch (java.util.EmptyStackException ex)
			{
				return 0;
			}
		}

		/**
		 * Calculates the remaining document length before all floating blocks in
		 * the margin end. Assumes that no more floating blocks will be added, of course.
		 *
		 * @return Remaining document length with objects in the margin.
		 */
		protected int getRemainingLength()
		{
			int maxLength = 0;
			java.util.Iterator it = stack.iterator();
		        while (it.hasNext())
			{
				Dimension dim = (Dimension)it.next();
				maxLength = Math.max(maxLength,dim.height);
			}
			return maxLength;
		}

		/**
		 * Advances the document by the specified distance. The values returned by
		 * getMarginSize and getRemainingLength will be adjusted correspondingly.
		 *
		 * @param distance Distance of document to advance.
		 */
		protected void moveDown(int distance)
		{
			Dimension dim;
			java.util.Iterator it = stack.iterator();
			while (it.hasNext())
			{
				dim = (Dimension)it.next();
				dim.height = dim.height - distance;
			}

			// Pop blocks that have ended
			try
			{
				for (dim = (Dimension)stack.peek();
					dim.height <= 0;
					dim = (Dimension)stack.peek())
				{
					stack.pop();
				}
			}
			catch (java.util.EmptyStackException ex) {
                // It's OK if we empty the stack.
            }
		}

		/**
		 * Resets this object to its initial (empty) state.
		 */
		protected void clear()
		{
			stack.clear();
		}

		public Object copy()
		{
            FloatMarginStack copy = new FloatMarginStack();
            java.util.Iterator it = stack.iterator();
			while (it.hasNext())
			{
				copy.stack.push(((Dimension)it.next()).clone());
			}
			return copy;
		}
	}
}