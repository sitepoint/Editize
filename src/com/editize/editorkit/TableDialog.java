package com.editize.editorkit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Vector;
import java.util.Iterator;

import com.editize.*;

/**
 * Dialog to choose an image and its attributes.
 */
public class TableDialog extends JDialog implements ActionListener
{
	public static final int OK = 0;
	public static final int CANCEL = 1;
	public static final int CLOSED = 2;

	public static final String NOTABLECLASSLABEL = "none";

	private JTextField      rowsField;
	private JTextField      colsField;
	private JTextField      widthField;
	private JTextField      heightField;
	private JTextField      borderField;
	private JComboBox       alignBox;
	private JButton         advancedButton;
	private JButton         okButton;
	private JButton         cancelButton;
	private JPanel          tablePanel;
	private JPanel          sizePanel;
	private JPanel          stylePanel;
	private JPanel          buttonPanel;

	private static WindowAdapter    wa = new DialogAdapter();

	private boolean         showAdvanced = false;

	private int             exitType = CLOSED;
	private JLabel classLabel;
	private JComboBox classCombo;

  public TableDialog(JEditorPane editor)
	{
		super(JOptionPane.getFrameForComponent(editor),"Insert Table",true);
		layoutUI();

		// Set defaults
		colsField.setText("5");
		rowsField.setText("2");
		borderField.setText("1");

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
	}

	/**
	 * Gets the method used to close the dialog.
	 * @return OK, CANCEL, or CLOSED
	 */
	public int getCloseOption()
	{
		return exitType;
	}

	public String getRows()
	{
		String rows = rowsField.getText().trim();
		if (rows.length() == 0) rows = "1";
		return rows;
	}

	public void setRows(String value)
	{
		rowsField.setText(value);
	}

	public String getCols()
	{
		String cols = colsField.getText().trim();
		if (cols.length() == 0) cols = "1";
		return cols;
	}

	public void setCols(String value)
	{
		colsField.setText(value);
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
	 * Sets the selected table class. Has no effect if the specified class is
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
				break;
			}
		}
	}

	public String getTableBorder()
	{
		String border = borderField.getText().trim();
		if (border.length() == 0) border = "0";
		return border;
	}

	public void setTableBorder(String b)
	{
		borderField.setText(b);
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
				break;
			}
		}
	}

	public void actionPerformed(ActionEvent ev)
	{
		Object src = ev.getSource();
		if (src == advancedButton)
		{
			showAdvanced = !showAdvanced;
			stylePanel.setVisible(showAdvanced);
			advancedButton.setText(showAdvanced ? "Simple" : "Advanced");
			pack();
		}
		else if (src == okButton)
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

	protected void eventWireup()
	{
		advancedButton.addActionListener(this);
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		addWindowListener(wa);
	}

	protected void layoutUI()
	{
		Component c;

		Container rootPane = getContentPane();
		rootPane.setLayout(new BorderLayout());

		JPanel root = new JPanel();
		rootPane.add(root,BorderLayout.CENTER);

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		root.setLayout(gbl);

		gbc.gridx       = GridBagConstraints.RELATIVE;
		gbc.gridy       = GridBagConstraints.RELATIVE;
		gbc.gridwidth   = GridBagConstraints.REMAINDER;
		gbc.gridheight  = 1;
		gbc.weightx     = 0;
		gbc.weighty     = 0;
		gbc.insets      = new Insets(2,2,2,2);

		layoutTablePanel();
		c = tablePanel;
		gbc.anchor      = GridBagConstraints.NORTH;
		gbc.fill        = GridBagConstraints.HORIZONTAL;
		gbc.weightx     = 1;
		gbc.weighty     = 1;
		gbl.setConstraints(c,gbc);
		root.add(c);

		layoutButtonPanel();
		c = buttonPanel;
		gbc.gridwidth   = GridBagConstraints.REMAINDER;
		gbc.insets      = new Insets(2,2,2,2);
		gbl.setConstraints(c,gbc);
		root.add(c);
	}

	protected void layoutTablePanel()
	{
		if (tablePanel == null)
		{
			Component c;
			tablePanel = new JPanel();

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			tablePanel.setLayout(gbl);

			gbc.anchor      = GridBagConstraints.WEST;
			gbc.gridx       = GridBagConstraints.RELATIVE;
			gbc.gridy       = GridBagConstraints.RELATIVE;
			gbc.gridwidth   = GridBagConstraints.REMAINDER;
			gbc.gridheight  = 1;
			gbc.fill        = GridBagConstraints.BOTH;
			gbc.weightx     = 1.0;
			gbc.weighty     = 1.0;
			gbc.insets      = new Insets(0,0,0,0);

			layoutSizePanel();
			c = sizePanel;
			gbl.setConstraints(c,gbc);
			tablePanel.add(c);

			layoutStylePanel();
			c = stylePanel;
			gbl.setConstraints(c,gbc);
			tablePanel.add(c);

			c.setVisible(showAdvanced);
		}
	}

	protected void layoutSizePanel()
	{
		if (sizePanel == null)
		{
			Component c;
			sizePanel = new JPanel();
			sizePanel.setBorder(new TitledBorder("Table Size"));

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			sizePanel.setLayout(gbl);

			gbc.anchor      = GridBagConstraints.WEST;
			gbc.gridx       = GridBagConstraints.RELATIVE;
			gbc.gridy       = GridBagConstraints.RELATIVE;
			gbc.gridwidth   = 1;
			gbc.gridheight  = 1;
			gbc.fill        = GridBagConstraints.HORIZONTAL;
			gbc.weightx     = 0.0;
			gbc.weighty     = 0.0;
			gbc.insets      = new Insets(2,2,2,2);

			c = new JLabel("Number of Rows:");
			gbl.setConstraints(c,gbc);
			sizePanel.add(c);

			c = rowsField = new ClipTextField(new PositiveIntegerDocument(),"1",5);
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbl.setConstraints(c,gbc);
			sizePanel.add(c);

			c = new JLabel("Number of Columns:");
			gbc.fill        = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth   = 1;
			gbc.weightx     = 0.0;
			gbl.setConstraints(c,gbc);
			sizePanel.add(c);

			c = colsField = new ClipTextField(new PositiveIntegerDocument(),"1",5);
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx     = 1.0;
			gbl.setConstraints(c,gbc);
			sizePanel.add(c);
		}
	}

	protected void layoutStylePanel()
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
			gbc.gridwidth   = 1;
			gbc.gridheight  = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.insets = new Insets(2,2,2,2);

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
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = borderField = new ClipTextField(new PositiveIntegerDocument(true),"0",3);
			gbc.fill = GridBagConstraints.NONE;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = new JLabel("pixels");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = new JLabel("Width:");
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = widthField = new ClipTextField(5);
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = new JLabel("Height:");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = heightField = new ClipTextField(5);
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = new JLabel("Alignment:");
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			stylePanel.add(c);

			c = alignBox = new JComboBox(
					new Object[] {"default","left","center","right"});
			gbc.fill = GridBagConstraints.NONE;
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
			buttonPanel.add(advancedButton = new JButton(
					showAdvanced ? "Simple" : "Advanced"));
		}
	}

	private static class DialogAdapter extends WindowAdapter
	{
		public void windowClosed(WindowEvent ev)
		{
			TableDialog src = (TableDialog)ev.getWindow();
			src.exitType = CLOSED;
		}
	}
}
