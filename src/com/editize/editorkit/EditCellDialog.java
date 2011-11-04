package com.editize.editorkit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

/**
 * Provides an interface for specifying HTML table cell properties.
 * @author Kevin Yank
 */

public class EditCellDialog extends JDialog implements ActionListener
{
	// Exit types
	public static final int OK = 0;
	public static final int CANCEL = 1;
	public static final int CLOSED = 2;

	public static final String NOTABLECLASSLABEL = "none";

	private static WindowAdapter wa = new DialogAdapter();

	private int exitType = CLOSED;

	private JButton okButton, cancelButton;

	private JPanel cellPanel, buttonPanel;

	private JComboBox alignBox, vAlignBox, cellTypeBox;

	public EditCellDialog(JEditorPane editor)
	{
		super(JOptionPane.getFrameForComponent(editor), "Edit Cell", true);
		layoutUI();
		eventWireup();
		pack();
		setResizable(true);

		// Position dialog in centre of screen
		Dimension screen = editor.getToolkit().getScreenSize();
		setLocation(((int)screen.getWidth() - getWidth()) / 2,
					((int)screen.getHeight() - getHeight()) / 2);
	}

	public String getHAlign()
	{
		String align = alignBox.getSelectedItem().toString();
		if (align.equals("default")) return null;
		else return align;
	}

	public void setHAlign(String align)
	{
		int c = alignBox.getItemCount();
		for (int i=0; i<c; i++)
		{
			if (alignBox.getItemAt(i).toString().equalsIgnoreCase(align))
			{
				alignBox.setSelectedIndex(i);
				return;
			}
		}
		// Select default alignment
		alignBox.setSelectedIndex(0);
	}

	public String getVAlign()
	{
		String align = vAlignBox.getSelectedItem().toString();
		if (align.equals("default")) return null;
		else return align;
	}

	public void setVAlign(String align)
	{
		int c = vAlignBox.getItemCount();
		for (int i=0; i<c; i++)
		{
			if (vAlignBox.getItemAt(i).toString().equalsIgnoreCase(align))
			{
				vAlignBox.setSelectedIndex(i);
				return;
			}
		}
		// Select default alignment
		vAlignBox.setSelectedIndex(0);
	}

	public boolean isCellHeader()
	{
		return cellTypeBox.getSelectedIndex() != 0;
	}

	public void setCellHeader(boolean header)
	{
		cellTypeBox.setSelectedIndex(header ? 1 : 0);
	}

	/**
	 * Gets the method used to close the dialog.
	 * @return OK, CANCEL, or CLOSED
	 */
	public int getCloseOption()
	{
		return exitType;
	}

	public void actionPerformed(ActionEvent ev)
	{
		Object src = ev.getSource();
		if (src == okButton)
		{
			exitType = OK;
			hide();
		}
		else if (src == cancelButton)
		{
			exitType = CANCEL;
			hide();
		}
	}

	/**
	 * Initializes the data shown in the dialog by reading an existing cell
	 * element.
	 * @param table The existing cell element.
	 */
	public void init(Element cell)
			throws IllegalArgumentException
	{
		// Verify we have a cell
		if (cell == null ||
			(EditizeEditorKit.getElementName(cell) != HTML.Tag.TD &&
			EditizeEditorKit.getElementName(cell) != HTML.Tag.TH))
			throw new IllegalArgumentException(
					"EditCellDialog must be initialized with a valid cell!");

		AttributeSet attrs = cell.getAttributes();

		String attr;
		// Type
		setCellHeader(EditizeEditorKit.getElementName(cell) == HTML.Tag.TH);

		// Horz. Align
		attr = (String)attrs.getAttribute(HTML.Attribute.ALIGN);
		setHAlign(attr);

		// Vert. Align
		attr = (String)attrs.getAttribute(HTML.Attribute.VALIGN);
		setVAlign(attr);
	}

	private void layoutUI()
	{
		Component c;

		Container rootPane = getContentPane();
		rootPane.setLayout(new BorderLayout());

		JPanel root = new JPanel();
		rootPane.add(root, BorderLayout.CENTER);

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		root.setLayout(gbl);

		gbc.fill        = GridBagConstraints.BOTH;
		gbc.gridx		= GridBagConstraints.RELATIVE;
		gbc.gridy		= GridBagConstraints.RELATIVE;
		gbc.gridwidth	= GridBagConstraints.REMAINDER;
		gbc.gridheight	= 1;
		gbc.weightx		= 0;
		gbc.weighty		= 0;
		gbc.insets		= new Insets(2, 2, 2, 2);

		layoutCellPanel();
		c = cellPanel;
		gbc.anchor      = GridBagConstraints.NORTH;
		gbc.weightx     = 1;
		gbc.weighty     = 1;
		gbl.setConstraints(c, gbc);
		root.add(c);

		layoutButtonPanel();
		c = buttonPanel;
		gbc.gridwidth   = GridBagConstraints.REMAINDER;
		gbc.weighty     = 0;
		gbc.insets      = new Insets(2, 2, 2, 2);
		gbl.setConstraints(c, gbc);
		root.add(c);
	}

	private void layoutCellPanel()
	{
		if (cellPanel == null)
		{
			Component c;
			cellPanel = new JPanel();

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			cellPanel.setLayout(gbl);

			gbc.anchor      = GridBagConstraints.WEST;
			gbc.gridx       = GridBagConstraints.RELATIVE;
			gbc.gridy       = GridBagConstraints.RELATIVE;
			gbc.gridheight  = 1;
			gbc.fill        = GridBagConstraints.HORIZONTAL;
			gbc.weightx     = 0.0;
			gbc.weighty     = 0.0;
			gbc.insets      = new Insets(2, 2, 2, 2);

			c = new JLabel("Cell type:");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			gbl.setConstraints(c, gbc);
			cellPanel.add(c);

			c = cellTypeBox = new JComboBox(
					new Object[] {"Normal", "Header"});
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c, gbc);
			cellPanel.add(c);

			c = new JLabel("Vertical align:");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			gbl.setConstraints(c, gbc);
			cellPanel.add(c);

			c = vAlignBox = new JComboBox(
					new Object[] {"default", "top", "middle", "bottom", "baseline"});
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c, gbc);
			cellPanel.add(c);

			c = new JLabel("Horizontal align:");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			gbl.setConstraints(c, gbc);
			cellPanel.add(c);

			c = alignBox = new JComboBox(
					new Object[] {"default", "left", "center", "right"});
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c, gbc);
			cellPanel.add(c);
		}
	}

	protected void layoutButtonPanel()
	{
		if (buttonPanel == null)
		{
			buttonPanel = new JPanel();

			buttonPanel.add(okButton = new JButton("OK"));
			buttonPanel.add(cancelButton = new JButton("Cancel"));
		}
	}

	private void eventWireup()
	{
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		addWindowListener(wa);
	}

	private static class DialogAdapter extends WindowAdapter
	{
		public void windowClosed(WindowEvent ev)
		{
			EditCellDialog src = (EditCellDialog)ev.getWindow();
			src.exitType = CLOSED;
		}
	}
}
