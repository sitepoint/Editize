package com.editize;

import java.awt.*;
import java.beans.*;
import javax.swing.*;
import com.editize.editorkit.*;


/**
 * A JToolBar that creates TogglButtons for ArticleEditorKit.BooleanStateAwareTextActions.
 *
 * @author Kevin Yank
 */
public class EditizeToolBar extends HighlightedToolBar
{
	JEditorPane editor;

	/**
	 * Associates a JEditorPane with the toolbar. This is assigned to
	 * BooleanStateAwareTextActions that are added to the toolbar.
	 * @param e A JEditorPane.
	 */
	public EditizeToolBar(JEditorPane editor)
	{
		this.editor = editor;
		this.setLayout(new ToolBarLayout(ToolBarLayout.LEADING));
	}

	/**
	 * Adds a button for an Action to the toolbar. Creates JToggleButtons for
	 * BooleanStateAwareTextActions.
	 *
	 * @param a The Action for which a button is to be created.
	 * @return The button for the action.
	 */
	public AbstractButton addAction(Action a)
	{
		AbstractButton b;
		if (a instanceof BooleanStateAwareTextAction)
		{
			BooleanStateAwareTextAction bsa = (BooleanStateAwareTextAction)a;
			bsa.setAssignedEditor(editor);

			String text = a!=null? (String)a.getValue(Action.NAME) : null;
			Icon icon   = a!=null? (Icon)a.getValue(Action.SMALL_ICON) : null;
			boolean enabled = a!=null? a.isEnabled() : true;
			String tooltip = a!=null?
			    (String)a.getValue(Action.SHORT_DESCRIPTION) : null;
			b = new BSATextActionToggleButton(text, icon, bsa.getState());
			b.setAction(a);
			// By default, don't display the action name if the action has an icon
			if (icon !=null) {
			    b.putClientProperty("hideActionText", Boolean.TRUE);
			}
			b.setHorizontalTextPosition(JButton.CENTER);
			b.setVerticalTextPosition(JButton.BOTTOM);
			b.setEnabled(enabled);
			b.setToolTipText(tooltip);
			add(b);
		}
		else b = add(a);
		b.setMargin(new Insets(2,2,2,2));
		b.setFocusPainted(false);
		return b;
	}

	/**
	 * The type of JToggleButton created for BooleanStateAwareTextActions. Tracks the state of the
	 * action with its selected property.
	 *
	 * @author Kevin Yank
	 */
	private class BSATextActionToggleButton extends JButton implements PropertyChangeListener
	{
		boolean state;

		BSATextActionToggleButton(String text, Icon icon, boolean state)
		{
			super(text,icon);
			setState(state);
		}

		public void setAction(Action a)
		{
			super.setAction(a);
			if (a instanceof BooleanStateAwareTextAction)
			{
				a.addPropertyChangeListener(this);
			}
		}

		public void propertyChange(PropertyChangeEvent pce)
		{
			if (pce.getPropertyName().equals("state"))
			{
				setState(Boolean.TRUE.equals(pce.getNewValue()));
			}
		}

		public void setState(boolean state)
		{
			boolean oldState = this.state;
			this.state = state;
			if (state != oldState) repaint();
		}

		public boolean getState()
		{
			return state;
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (state)
			{
				Insets i = getInsets();
				Insets m = getMargin();
				int x = 1;//i.left - m.left + 1;
				int y = 1;//i.top - m.top + 1;
				int w = getWidth()-3;// - (i.left - m.left) - (i.right - m.right) - 3;
				int h = getHeight()-3;// - (i.top - m.top) - (i.bottom - m.bottom) - 3;

				Color fg = getForeground();
				Color fgtransp = new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),64);
				g.setColor(fgtransp);
				g.fillRect(x,y,w,h);
				g.setColor(fg);
				g.drawRect(x,y,w,h);
			}
		}
	}
}
