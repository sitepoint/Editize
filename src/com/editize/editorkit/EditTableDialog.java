package com.editize.editorkit;

import java.util.Iterator;
import java.util.Vector;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

import com.editize.*;

/**
 * Provides an interface for specifying HTML table properties.
 * @author Kevin Yank
 */

public class EditTableDialog extends JDialog implements ActionListener
{
	// Exit types
	public static final int OK = 0;
	public static final int CANCEL = 1;
	public static final int CLOSED = 2;

	public static final String NOTABLECLASSLABEL = "none";

	private static WindowAdapter wa = new DialogAdapter();

	private int exitType = CLOSED;

	private JButton okButton, cancelButton;

	private JPanel stylePanel, buttonPanel;

	private JLabel classLabel;

	private JTextField borderField;
	private JTextField cellPadField;
	private JTextField cellSpaceField;
	private JTextField widthField;
	private JTextField heightField;
	private JComboBox alignBox;
	private JComboBox classCombo;
	private JPanel accessPanel;
	private JTextArea summaryArea;
	private JPanel tablePanel;

	public EditTableDialog(JEditorPane editor)
	{
		super(JOptionPane.getFrameForComponent(editor), "Edit Table", true);
		layoutUI();

		// Detect and load available table classes
		EditizeDocument doc = (EditizeDocument)editor.getDocument();
		Vector availClasses = doc.getTableClasses();
		if (availClasses != null) {
			Iterator it = availClasses.iterator();
			while (it.hasNext()) {
				classCombo.addItem(it.next());
			}
		} else {
			classLabel.setVisible(false);
			classCombo.setVisible(false);
		}

		eventWireup();
		pack();
		setResizable(true);

		// Position dialog in centre of screen
		Dimension screen = editor.getToolkit().getScreenSize();
		setLocation(((int)screen.getWidth() - getWidth()) / 2,
					((int)screen.getHeight() - getHeight()) / 2);
	}

	/**
	 * Gets the chosen table class.
	 * @return String The chosen table class, or null if none selected.
	 */
	public String getTableClass()
	{
		String tableClass = classCombo.getSelectedItem().toString();
		if (tableClass.equals(NOTABLECLASSLABEL)) return null;
		return tableClass.trim();
	}

	/**
	 * Sets the selected table class. Sets to "none" if the specified class is
	 * not available in the list of available classes.
	 * @param tableClass String The class name to select.
	 */
	public void setTableClass(String tableClass)
	{
		int c = classCombo.getItemCount();
		for (int i = 0; i < c; i++)
		{
			if (classCombo.getItemAt(i).toString().equalsIgnoreCase(tableClass))
			{
				classCombo.setSelectedIndex(i);
				return;
			}
		}
		// Default selection
		classCombo.setSelectedIndex(0);
	}

	public String getTableBorder()
	{
		String border = borderField.getText().trim();
		if (border.length() == 0) border = "0";
		return border;
	}

	public void setTableBorder(String b)
	{
		if (b == null) b = "0";
		borderField.setText(b);
	}

	public String getTableSpacing()
	{
		String spacing = cellSpaceField.getText().trim();
		if (spacing.length() == 0) spacing = null;
		return spacing;
	}

	public void setTableSpacing(String spacing)
	{
		cellSpaceField.setText(spacing);
	}

	public String getTablePadding()
	{
		String padding = cellPadField.getText().trim();
		if (padding.length() == 0) padding = null;
		return padding;
	}

	public void setTablePadding(String padding)
	{
		cellPadField.setText(padding);
	}

	public String getTableWidth()
	{
		String width = widthField.getText().trim();
		if (width.length() == 0) width = null;
		return width;
	}

	public void setTableWidth(String b)
	{
		widthField.setText(b);
	}

	public String getTableHeight()
	{
		String height = heightField.getText().trim();
		if (height.length() == 0) height = null;
		return height;
	}

	public void setTableHeight(String b)
	{
		heightField.setText(b);
	}

	public String getTableAlign()
	{
		String align = alignBox.getSelectedItem().toString();
		if (align.equals("default")) return null;
		else return align;
	}

	public void setTableAlign(String align)
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

	public String getTableSummary()
	{
		String summary = summaryArea.getText();
		if (summary.trim().length() == 0) return null;
		else return summary.trim();
	}

	public void setTableSummary(String summary)
	{
		summaryArea.setText(summary);
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
	 * Initializes the data shown in the dialog by reading an existing table
	 * element.
	 * @param table The existing table element.
	 */
	public void init(Element table)
			throws IllegalArgumentException
	{
		// Verify we have a table
		if (table == null ||
			EditizeEditorKit.getElementName(table) != HTML.Tag.TABLE)
			throw new IllegalArgumentException(
					"EditTableDialog must be initialized with a valid table!");

		AttributeSet attrs = table.getAttributes();

		String attr;
		// Align
		attr = (String)attrs.getAttribute(HTML.Attribute.ALIGN);
		setTableAlign(attr);
		// Border
		attr = (String)attrs.getAttribute("trueborder");
		if (attr == null) attr = (String)attrs.getAttribute(HTML.Attribute.BORDER);
		setTableBorder(attr);
		// Class
		attr = (String)attrs.getAttribute(HTML.Attribute.CLASS);
		setTableClass(attr);
		// Height
		attr = (String)attrs.getAttribute(HTML.Attribute.HEIGHT);
		setTableHeight(attr);
		// Width
		attr = (String)attrs.getAttribute(HTML.Attribute.WIDTH);
		setTableWidth(attr);
		// Padding
		attr = (String)attrs.getAttribute(HTML.Attribute.CELLPADDING);
		setTablePadding(attr);
		// Spacing
		attr = (String)attrs.getAttribute(HTML.Attribute.CELLSPACING);
		setTableSpacing(attr);
		// Summary
		attr = (String)attrs.getAttribute("summary");
		setTableSummary(attr);
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

		layoutTablePanel();
		c = tablePanel;
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

	private void layoutTablePanel()
	{
		if (tablePanel == null) {
			Component c;
			tablePanel = new JPanel();
			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			tablePanel.setLayout(gbl);

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = GridBagConstraints.RELATIVE;
			gbc.gridy = GridBagConstraints.RELATIVE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			gbc.weighty = 0.0;
			gbc.insets = new Insets(0, 0, 0, 0);

			layoutStylePanel();
			c = stylePanel;
			gbl.setConstraints(c, gbc);
			tablePanel.add(c);

			layoutAccessPanel();
			c = accessPanel;
			gbc.weighty = 1.0;
			gbc.fill = GridBagConstraints.BOTH;
			gbl.setConstraints(c, gbc);
			tablePanel.add(c);
		}
	}

	private void layoutStylePanel()
	{
		if (stylePanel == null)
		{
			Component c;
			stylePanel = new JPanel();
			stylePanel.setBorder(new TitledBorder("Table Style"));

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			stylePanel.setLayout(gbl);

			gbc.anchor      = GridBagConstraints.WEST;
			gbc.gridx       = GridBagConstraints.RELATIVE;
			gbc.gridy       = GridBagConstraints.RELATIVE;
			gbc.gridheight  = 1;
			gbc.fill        = GridBagConstraints.HORIZONTAL;
			gbc.weightx     = 0.0;
			gbc.weighty     = 0.0;
			gbc.insets      = new Insets(2, 2, 2, 2);

			c = classLabel = new JLabel("Class:");
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = classCombo = new JComboBox(new Object[] {NOTABLECLASSLABEL});
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = new JLabel("Border size:");
			gbc.gridwidth = 1;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = borderField = new ClipTextField(new PositiveIntegerDocument(true),
												"0", 3);
			gbc.fill = GridBagConstraints.NONE;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = new JLabel("pixels");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = new JLabel("Cell padding:");
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = cellPadField = new ClipTextField(new PositiveIntegerDocument(true),
												 "0", 3);
			gbc.fill = GridBagConstraints.NONE;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = new JLabel("pixels");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = new JLabel("Cell spacing:");
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = cellSpaceField =
					new ClipTextField(new PositiveIntegerDocument(true), "0", 3);
			gbc.fill = GridBagConstraints.NONE;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = new JLabel("pixels");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbl.setConstraints(c, gbc);
			stylePanel.add(c);

			c = new JLabel("Width:");
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = widthField = new ClipTextField(5);
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = new JLabel("Height:");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = heightField = new ClipTextField(5);
			gbc.weightx = 1.0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = new JLabel("Alignment:");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = alignBox = new JComboBox(
					new Object[] {"default", "left", "center", "right"});
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);
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

	private void layoutAccessPanel()
	{
		if (accessPanel == null)
		{
			Component c;
			accessPanel = new JPanel();
			accessPanel.setBorder(new TitledBorder("Accessibility"));

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			accessPanel.setLayout(gbl);

			gbc.anchor      = GridBagConstraints.WEST;
			gbc.gridx       = GridBagConstraints.RELATIVE;
			gbc.gridy       = GridBagConstraints.RELATIVE;
			gbc.gridheight  = 1;
			gbc.fill        = GridBagConstraints.HORIZONTAL;
			gbc.weightx     = 1.0;
			gbc.weighty     = 0.0;
			gbc.insets      = new Insets(2, 2, 2, 2);

			c = new JLabel("Summary:");
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c, gbc);
			accessPanel.add(c);

			c = new JScrollPane(summaryArea = new JTextArea(3, 30),
								JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
								JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			summaryArea.setLineWrap(true);
			summaryArea.setWrapStyleWord(true);
			gbc.weighty = 1.0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c, gbc);
			accessPanel.add(c);
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
			EditTableDialog src = (EditTableDialog)ev.getWindow();
			src.exitType = CLOSED;
		}
	}
}
