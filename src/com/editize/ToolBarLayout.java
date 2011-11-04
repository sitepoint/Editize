package com.editize;

import java.awt.*;
import javax.swing.JToolBar;

/**
 * Layout manager for use by JToolBars that need to wrap to a second row/column
 * when there is insufficient room for all buttons to appear in a single line.
 * Uses JSeparators as ideal breaking points, hiding them if they are used as
 * such.
 *
 * @author Kevin Yank
 */

public class ToolBarLayout extends FlowLayout
{
    public ToolBarLayout(int align)
    {
		super(align,0,0);
    }

	Dimension prefDim = new Dimension(0,0);

	public void layoutContainer(Container target)
	{
		synchronized (target.getTreeLock())
		{
			Insets insets = target.getInsets();
			int hgap = getHgap();
			int vgap = getVgap();
			int maxwidth = target.getWidth() - (insets.left + insets.right + hgap*2);
			int nmembers = target.getComponentCount();
			int x = 0, y = insets.top + vgap;
			int rowh = 0, start = 0;
			int lastSeparator = -1;
			int backupRowh = 0, backupX = 0;

			boolean ltr = target.getComponentOrientation().isLeftToRight();

			// Calculate minimum/preferred dimensions on the fly during layout
			prefDim = new Dimension(0,0);

			for (int i = 0 ; i < nmembers ; i++)
			{
				Component m = target.getComponent(i);
				if (m.isVisible())
				{
					Dimension d = m.getPreferredSize();
					m.setSize(d.width, d.height);

					if (m instanceof JToolBar.Separator)
					{
						lastSeparator = i;
						backupRowh = rowh;
						backupX = x;
					}

					if ((x == 0) || ((x + d.width) <= maxwidth))
					{
						// Normal Component placement
						if (x > 0)
						{
							x += hgap;
						}
						x += d.width;
						rowh = Math.max(rowh, d.height);
					}
					else
					{
						// Start a new row

						if (lastSeparator >= 0)
						{
							// Rewind to before last separator
							i = lastSeparator;
							rowh = backupRowh;
							x = backupX;
						}

						prefDim.width = Math.max(x,prefDim.width);

						moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh, start, i, ltr);

						if (lastSeparator >= 0)
						{
							// Place separator out of harm's way
							Component sep = target.getComponent(lastSeparator);
							sep.setLocation(target.getWidth(),y);

							// Continue from element following separator
							lastSeparator = -1;
							x = 0;
							y += vgap + rowh;
							rowh = 0;
							start = i + 1;
						}
						else
						{
							x = d.width;
							y += vgap + rowh;
							rowh = d.height;
							start = i;
						}
					}
				}
			}
			prefDim.width = Math.max(x,prefDim.width);
			prefDim.height = y + vgap + rowh;
			moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh, start, nmembers, ltr);
		}
	}

	public Dimension preferredLayoutSize(Container target)
	{
		layoutContainer(target);
		return prefDim;
	}

	public Dimension minimumLayoutSize(Container target)
	{
		return preferredLayoutSize(target);
	}

	/**
	 * Centers the elements in the specified row, if there is any slack.
	 * @param target the component which needs to be moved
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param width the width dimensions
	 * @param height the height dimensions
	 * @param rowStart the beginning of the row
	 * @param rowEnd the the ending of the row
	 */
	private void moveComponents(Container target, int x, int y, int width, int height,
								int rowStart, int rowEnd, boolean ltr)
	{
		int newAlign = getAlignment();

		synchronized (target.getTreeLock())
		{
			switch (newAlign)
			{
				case LEFT:
					x += ltr ? 0 : width;
					break;
				case CENTER:
					x += width / 2;
					break;
				case RIGHT:
					x += ltr ? width : 0;
					break;
				case LEADING:
					break;
				case TRAILING:
					x += width;
					break;
			}
			for (int i = rowStart ; i < rowEnd ; i++)
			{
				Component m = target.getComponent(i);
				if (m.isVisible())
				{
					if (ltr)
					{
						m.setLocation(x, y + (height - m.getHeight()) / 2);
					}
					else
					{
						m.setLocation(target.getWidth() - x - m.getWidth(), y + (height - m.getHeight()) / 2);
					}
					x += m.getWidth() + getHgap();
				}
			}
		}
	}
}