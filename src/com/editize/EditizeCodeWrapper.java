package com.editize;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.util.Hashtable;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

public class EditizeCodeWrapper extends JTabbedPane implements CaretListener
{
	private Hashtable actionHash = new Hashtable();

	private Editize editize;
	private JEditorPane codeEditor;
	private Action cutAction, copyAction;

	static final String WW_LABEL = "WYSIWYG View";
	static final String CODE_LABEL = "Code View";
	private boolean codeChanged = false;

    public EditizeCodeWrapper(Editize editize)
    {
		this.editize = editize;

		codeEditor = new JEditorPane();
		codeEditor.setFont(new Font("Monospaced",Font.PLAIN,12));
                codeEditor.setContentType("text/plain");
                Keymap map = JTextComponent.addKeymap("HotkeyStylesMap", codeEditor.getKeymap());
                codeEditor.setKeymap(map);

		hashActions();

		JToolBar tb = new HighlightedToolBar();
		tb.setBorderPainted(true);
		tb.setFloatable(false);

		// Add "standard" buttons to toolbar
		final Insets bInsets = new Insets(1,2,2,2);
		cutAction = getHashedAction(DefaultEditorKit.cutAction);
		JButton cutButton = tb.add(cutAction);
		cutButton.setText("");
		cutButton.setToolTipText("Cut (Ctrl-X)");
		cutButton.setMargin(bInsets);
		copyAction = getHashedAction(DefaultEditorKit.copyAction);
		JButton copyButton = tb.add(copyAction);
		copyButton.setText("");
		copyButton.setToolTipText("Copy (Ctrl-C)");
		copyButton.setMargin(bInsets);
		Action pasteAction = getHashedAction(DefaultEditorKit.pasteAction);
		JButton pasteButton = tb.add(pasteAction);
		pasteButton.setText("");
		pasteButton.setToolTipText("Paste (Ctrl-V)");
		pasteButton.setMargin(bInsets);
//		Action undoAction = getHashedAction("undo");
//		JButton undoButton = tb.add(undoAction);
//		undoButton.setText("");
//		undoButton.setToolTipText("Undo (Ctrl-Z)");
//		Action redoAction = getHashedAction("redo");
//		JButton redoButton = tb.add(redoAction);
//		redoButton.setText("");
//		redoButton.setToolTipText("Redo (Ctrl-Y)");

                // Add "alternative" keystrokes
                KeyStroke stroke;
                stroke =
                    KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK, false);
                map.addActionForKeyStroke(stroke,cutAction);
                stroke =
                    KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK, false);
                map.addActionForKeyStroke(stroke,copyAction);
                stroke =
                    KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK, false);
                map.addActionForKeyStroke(stroke,pasteAction);

		JPanel codeEditorPanel = new JPanel(new BorderLayout());
		codeEditorPanel.add(new JScrollPane(codeEditor,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),BorderLayout.CENTER);
		codeEditorPanel.add(tb, BorderLayout.NORTH);

		addTab(WW_LABEL,editize);
		addTab(CODE_LABEL,codeEditorPanel);

		addChangeListener(new TabChangeListener());
		codeEditor.addCaretListener(this);
		codeEditor.getDocument().addDocumentListener(docListener);
	}

	private class TabChangeListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent evt)
		{
			String selected = getTitleAt(getSelectedIndex());
			if (selected.equals(WW_LABEL) && codeChanged)
			{
				// Read code from code editor
				editize.setHtml(codeEditor.getText(),true);
			}
			else if (selected.equals(CODE_LABEL))
			{
				try
				{
					codeEditor.setText(editize.getHtml(true));
					codeEditor.setCaretPosition(0);
					codeChanged = false;
				}
				catch (java.io.IOException ex)
				{
					System.err.println(ex);
				}
			}
		}
	}

	/**
	 * Obtains all of the actions supported by the JTextComponent
	 * and places them into a hash. Called by initialization code.
	 * Creation date: (10/07/2001 12:26:12 PM)
	 */
	protected void hashActions()
	{
		Action[] actions = codeEditor.getActions();
		for (int i=0; i<actions.length; i++)
		{
			String name = (String)actions[i].getValue(Action.NAME);
			actionHash.put(name,actions[i]);
		}
	}

	protected Action getHashedAction(String name) {
		return (Action)actionHash.get(name);
	}

	public String getCode()
			throws java.io.IOException
	{
		if (getTitleAt(getSelectedIndex()).equals(CODE_LABEL))
		{
			return codeEditor.getText();
		}
		else return editize.getHtml(true);
	}

	/**
	 * Invoked whenever the caret moves in the text pane.
	 *
	 * @param evt javax.swing.event.CaretEvent
	 */
	public void caretUpdate(CaretEvent evt)
	{
		// Enable/disable clipboard actions
		if (codeEditor.getSelectedText() == null)
		{
			if (cutAction != null)
				cutAction.setEnabled(false);
			if (copyAction != null)
				copyAction.setEnabled(false);
		}
		else
		{
			if (cutAction != null)
				cutAction.setEnabled(true);
			if (copyAction != null)
				copyAction.setEnabled(true);
		}
	}

	private DocumentListener docListener = new DocumentListener()
	{
		public void changedUpdate(DocumentEvent evt)
		{
			codeChanged=true;
		}
		public void removeUpdate(DocumentEvent evt)
		{
			codeChanged=true;
		}
		public void insertUpdate(DocumentEvent evt)
		{
			codeChanged=true;
		}
	};
}
