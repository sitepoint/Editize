package com.editize;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * A hacked version of JComboBox that will always support
 * Ctrl-X, Ctrl-C, and Ctrl-V for cut, copy, and paste respectively.
 */
public class ClipComboBox extends JComboBox
{
	public ClipComboBox()
	{
		super();
	}

	public ClipComboBox(ComboBoxModel aModel)
	{
		super(aModel);
	}

	public ClipComboBox(final Object items[])
	{
		super(items);
	}

	public ClipComboBox(java.util.Vector items)
	{
		super(items);
	}

	/**
	 * Allows the assigned UI to assign its keys, then overrides
	 * the clipboard hotkeys we want to assign.
	 */
	public void updateUI() {
		super.updateUI();
		addClipboardHotkeys();
	}

	/**
	 * Overrides default keymap to add Ctrl-C/X/V clipboard actions.
	 */
	protected void addClipboardHotkeys()
	{
		InputMap map = ((JTextComponent)getEditor().getEditorComponent()).getInputMap();

		map.put(KeyStroke.getKeyStroke(
			KeyEvent.VK_X,
			InputEvent.CTRL_MASK),DefaultEditorKit.cutAction);
		map.put(KeyStroke.getKeyStroke(
			KeyEvent.VK_C,
			InputEvent.CTRL_MASK),DefaultEditorKit.copyAction);
		map.put(KeyStroke.getKeyStroke(
			KeyEvent.VK_V,
			InputEvent.CTRL_MASK),DefaultEditorKit.pasteAction);
	}
}