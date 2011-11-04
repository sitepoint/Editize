package com.editize;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Hashtable;

/**
 * A JToolBar that adds a 'light up on mouseover' effect to components added to it.
 * @author Kevin Yank
 */
public class HighlightedToolBar extends JToolBar
{

	/**
	 * MouseEvent handler that is assigned to all buttons created by
	 * createActionComponent to make them light up on mousover.
	 */
	protected static final MouseListener hoverHighlighter = new MouseAdapter()
	{
		/**
		 * Stores the original background colors of components so that
		 * the mouseExited method knows what to set it back to.
		 */
		Hashtable colorTracker = new Hashtable(5);

		/**
		 * Stores the current background color in colorTracker and then
		 * sets the background color brighter.
		 * @param evt A MouseEvent.
		 */
		public void mouseEntered(MouseEvent evt)
		{
			Component src = evt.getComponent();
			if (!src.isEnabled()) return;
			Color bg = src.getBackground();
			colorTracker.put(src,bg); // Remember the color
			src.setBackground(bg.brighter());
			if (src instanceof AbstractButton) ((AbstractButton)src).setBorderPainted(true);
		}
		/**
		 * Sets the background color back to its original color, as
		 * stored in colorTracker.
		 *
		 * @param evt A MouseEvent.
		 */
		public void mouseExited(MouseEvent evt)
		{
			Component src = evt.getComponent();
			Color bg = (Color)colorTracker.remove(src);
			if (bg != null) src.setBackground(bg);
			if (src instanceof AbstractButton) ((AbstractButton)src).setBorderPainted(false);
		}
	};

	/**
	 * Adds highlighting mouseover effect to Components that are added.
	 */
	public Component add(Component c)
	{
		c.addMouseListener(hoverHighlighter);
		if (c instanceof AbstractButton) ((AbstractButton)c).setBorderPainted(false);
		return super.add(c);
	}

	public static class Separator extends JToolBar.Separator
	{
		public Separator()
		{
			super(new Dimension(10,27));
		}

		public void paintComponent(Graphics g)
		{
		        Color lightColor = getBackground().brighter();
			Color darkColor = getBackground().darker();
			g.setColor(darkColor);
			g.fillRect(getWidth()/2 - 1,0,1,getHeight()-1);
		        g.setColor(lightColor);
			g.fillRect(getWidth()/2,0,1,getHeight()-1);
		}
	}
}