package com.editize.editorkit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.io.*;
import java.net.*;
import java.beans.*;

import com.editize.*;

/**
 * Dialog to choose an image and its attributes.
 */
public class ImageDialog extends JDialog implements ActionListener, PropertyChangeListener
{
	public static final int OK = 0;
	public static final int CANCEL = 1;
	public static final int CLOSED = 2;

	private JTextField      urlField;
	private JTextField      altField;
	private JTextField      hSizeField;
	private JTextField      vSizeField;
	private JTextField      borderField;
	private JTextField      hSpaceField;
	private JTextField      vSpaceField;
	private JComboBox       alignBox;
	private JCheckBox       shapeCheckBox;
	private JButton         sizeResetButton;
	private JButton         advancedButton;
	private JButton         okButton;
	private JButton         cancelButton;
	private JButton         previewButton;
	private JButton         listButton;
	private JPanel          imageInfoPanel;
	private JPanel          sizePanel;
	private JPanel          spacingPanel;
	private JPanel          advancedPanel;
	private JPanel          buttonPanel;
	private JLabel          previewImage;

	private JLabel          spacingLabel;
		private JLabel          borderLabel;
		private JLabel          borderUnitsLabel;

	private Icon            verticalIcon;
	private Icon            horizontalIcon;

	private static WindowAdapter    wa = new DialogAdapter();

	private boolean         showAdvanced = false;
	private boolean         editMode = false;
		private boolean         xhtmlMode = false;

	private Dimension       defaultDims;

	private int             exitType = CLOSED;

	private URL             baseUrl;
	private String          imgListUrl;

	private ImageListDialog imgListDlg;

	private static final int PREVIEW_SIZE = 100;

	public ImageDialog(Component parentComponent)
	{
		super(JOptionPane.getFrameForComponent(parentComponent),"Insert Image",true);
		layoutUI();
		eventWireup();
		pack();
		setResizable(true);

		// Set defaults
		getBorderField().setText("0");
		getShapeCheckBox().setSelected(true);
	}

	/**
	 * Overrides deprecated method as of JDK 1.5. Replaced with setVisible.
	 */
	public void show()
	{
		if (getUrlField().getText().length() > 0)
		{
			getPreviewButton().doClick();
		}
		super.show();
	}

	public void setVisible(boolean visible)
	{
		if (visible && getUrlField().getText().length() > 0)
		{
			getPreviewButton().doClick();
		}
		super.setVisible(visible);
	}

	public void setEditMode(boolean edit)
	{
		if (editMode != edit)
		{
				editMode = edit;
			setTitle(edit?"Edit Image":"Insert Image");
		}
	}

	public boolean getEditMode()
	{
		return editMode;
	}

		public void setXHTMLCompliantAlignment(boolean enabled)
		{
		  xhtmlMode = enabled;

		  // Hide non-XHTML attributes
		  JComboBox alignbox = getAlignBox();
		  alignbox.setModel(new DefaultComboBoxModel(
			enabled ? new Object[] {"inline", "left", "right"} :
					  new Object[] {"bottom", "top", "middle", "left", "right"}
		  ));
		  borderLabel.setVisible(!enabled);
		  borderUnitsLabel.setVisible(!enabled);
		  getBorderField().setVisible(!enabled);
		  spacingLabel.setVisible(!enabled);
		  getSpacingPanel().setVisible(!enabled);
		}

		public boolean isXHTMLCompliantAlignment()
		{
		  return xhtmlMode;
		}

	/**
	 * Gets the method used to close the dialog.
	 * @return OK, CANCEL, or CLOSED
	 */
	public int getCloseOption()
	{
		return exitType;
	}

	public URL getBaseUrl()
	{
		return baseUrl;
	}

	public void setBaseUrl(URL baseUrl)
	{
		this.baseUrl = baseUrl;
	}

	public String getImgListUrl()
	{
		return imgListUrl;
	}

	public void setImgListUrl(String listUrl)
	{
		this.imgListUrl = listUrl;
		getListButton().setVisible(listUrl != null);
	}

	public String getImageSrc()
	{
		return getUrlField().getText();
	}

	public void setImageSrc(String src)
	{
		getUrlField().setText(src);
	}

	public String getAltText()
	{
		return getAltField().getText();
	}

	public void setAltText(String alt)
	{
		getAltField().setText(alt);
	}

	public String getImgWidth()
	{
		return getHSizeField().getText();
	}

	public void setImgWidth(String w)
	{
		getHSizeField().setText(w);
	}

	public String getImgHeight()
	{
		return getVSizeField().getText();
	}

	public void setImgHeight(String h)
	{
		getVSizeField().setText(h);
	}

	public String getImgBorder()
	{
		return getBorderField().getText();
	}

	public void setImgBorder(String b)
	{
		getBorderField().setText(b);
	}

	public String getImgVSpace()
	{
		return getVSpaceField().getText();
	}

	public void setImgVSpace(String space)
	{
		getVSpaceField().setText(space);
	}

	public String getImgHSpace()
	{
		return getHSpaceField().getText();
	}

	public void setImgHSpace(String space)
	{
		getHSpaceField().setText(space);
	}

	public String getImgAlign()
	{
		return getAlignBox().getSelectedItem().toString();
	}

	public void setImgAlign(String align)
	{
		JComboBox box = getAlignBox();
		int c = box.getItemCount();
				box.setSelectedIndex(0);
		for (int i=0; i<c; i++)
		{
			if (box.getItemAt(i).toString().equalsIgnoreCase(align))
			{
				box.setSelectedIndex(i);
				break;
			}
		}
	}

	public void actionPerformed(ActionEvent ev)
	{
		Object src = ev.getSource();
		if (src == getAdvancedButton())
		{
			showAdvanced = !showAdvanced;
			getAdvancedPanel().setVisible(showAdvanced);
			getAdvancedButton().setText(
				showAdvanced ? "Simple" : "Advanced");
			pack();
		}
		else if (src == getOkButton())
		{
				exitType = OK;
			hide();
		}
		else if (src == getCancelButton())
		{
			exitType = CANCEL;
			hide();
		}
		else if (src == getListButton())
		{
			if (imgListDlg == null)
			{
				try
				{
					imgListDlg = new ImageListDialog(imgListUrl,this);
				}
				catch (MalformedURLException ex) {
					/* Should notify user here... */
					return;
				}
				imgListDlg.setSize(500,400);
				Dimension screenSize =
					Toolkit.getDefaultToolkit().getScreenSize();
				imgListDlg.setLocation(
					(screenSize.width - imgListDlg.getWidth()) / 2,
					(screenSize.height - imgListDlg.getHeight()) / 2
				);
				imgListDlg.setModal(true);
			}
			imgListDlg.show();
			if (imgListDlg.getCloseOption() == JOptionPane.OK_OPTION)
			{
				ImageListDialog.ListItem li = imgListDlg.getSelectedImage();
				setImageSrc(li.getUrl());
				setAltText(li.getAlt());
				getPreviewButton().doClick();
			}
		}
		else if (src == getPreviewButton())
		{
				  JLabel previewImage = getPreviewImage();
				  Icon icon = null;
				  try
				  {
					// If dimensions for previous image were left at their defaults,
					// blank the directions so that the new image's defaults will take over.
					if ((getImgWidth().length() == 0 || defaultDims != null && Integer.parseInt(getImgWidth()) == defaultDims.width) &&
						(getImgHeight().length() == 0 || defaultDims != null && Integer.parseInt(getImgHeight()) == defaultDims.height))
					{
					  setImgWidth("");
					  setImgHeight("");
					}

					defaultDims = new Dimension();
					icon = makeIcon(
						new URL(baseUrl, getUrlField().getText()),
						PREVIEW_SIZE, defaultDims);

					if (getHSizeField().getText().length() == 0 &&
						getVSizeField().getText().length() == 0)
					  getSizeResetButton().doClick();
				  }
				  catch (IOException ex)
				  {
					try
					{
					  icon = makeIcon("icons/image-failed.gif");
					}
					catch (IOException ex2)
					{
					}
				  }
				  previewImage.setIcon(icon);
		}
		else if (src == getSizeResetButton())
		{
			if (defaultDims != null)
			{
				// Write image width and height to size fields
				getHSizeField().setText(Integer.toString(defaultDims.width));
				getVSizeField().setText(Integer.toString(defaultDims.height));
			}
		}
	}

	/**
	 * Reacts to changes in image width/height by preserving aspect ratio.
	 *
	 * @param evt
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (!updatingDims && getShapeCheckBox().isSelected() && defaultDims != null)
		{
			updatingDims = true;
			if (evt.getSource() == getHSizeField().getDocument())
			{
				// Width changed -- update height
				float newHeight = ((Integer)evt.getNewValue()).intValue() * defaultDims.height / (float)defaultDims.width;
				setImgHeight((int)Math.ceil(newHeight) + "");
			}
			else if (evt.getSource() == getVSizeField().getDocument())
			{
				// Height changed -- update width
				float newWidth = ((Integer)evt.getNewValue()).intValue() * defaultDims.width / (float)defaultDims.height;
				setImgWidth((int)Math.ceil(newWidth) + "");
			}
			updatingDims = false;
		}
	}
	protected boolean updatingDims = false;

	protected void eventWireup()
	{
		getAdvancedButton().addActionListener(this);
		getOkButton().addActionListener(this);
		getCancelButton().addActionListener(this);
		getListButton().addActionListener(this);
		getPreviewButton().addActionListener(this);
		getSizeResetButton().addActionListener(this);

		((PositiveIntegerDocument)getHSizeField().getDocument()).addPropertyChangeListener(this);
		((PositiveIntegerDocument)getVSizeField().getDocument()).addPropertyChangeListener(this);

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
		gbc.gridwidth   = 1;
		gbc.gridheight  = 1;
		gbc.weightx     = 0;
		gbc.weighty     = 0;
		gbc.insets      = new Insets(2,2,2,2);

		c = getImageInfoPanel();
		gbc.gridheight  = 2;
		gbc.anchor      = GridBagConstraints.NORTH;
		gbc.fill        = GridBagConstraints.HORIZONTAL;
		gbc.weightx     = 1;
		gbc.weighty     = 1;
		gbl.setConstraints(c,gbc);
		root.add(c);

		c = getPreviewImage();
		gbc.gridwidth   = GridBagConstraints.REMAINDER;
		gbc.gridheight  = 1;
		gbc.weightx     = 0;
		gbc.insets      = new Insets(4,4,4,4);
		gbl.setConstraints(c,gbc);
		root.add(c);

		c = getAdvancedButton();
		gbc.anchor      = GridBagConstraints.CENTER;
		gbc.fill        = GridBagConstraints.HORIZONTAL;
		gbc.weighty     = 0;
		gbl.setConstraints(c,gbc);
		root.add(c);

		c = getButtonPanel();
		gbc.gridwidth   = GridBagConstraints.REMAINDER;
		gbc.insets      = new Insets(2,2,2,2);
		gbl.setConstraints(c,gbc);
		root.add(c);

	}

	protected JPanel getImageInfoPanel()
	{
		if (imageInfoPanel == null)
		{
			Component c;
			imageInfoPanel = new JPanel();
			imageInfoPanel.setBorder(new TitledBorder("Image Info"));

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			imageInfoPanel.setLayout(gbl);

			gbc.anchor      = GridBagConstraints.WEST;
			gbc.gridx       = GridBagConstraints.RELATIVE;
			gbc.gridy       = GridBagConstraints.RELATIVE;
			gbc.gridwidth   = 1;
			gbc.gridheight  = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.insets = new Insets(2,2,2,2);

			JLabel urlLabel = new JLabel("URL:");
				c = urlLabel;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

			c = getUrlField();
			gbc.weightx = 1.0;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

			c = getListButton();
				gbc.weightx = 0;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);
			c.setVisible(imgListUrl != null);

			c = getPreviewButton();
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

				c = new JLabel("Alt.:");
			gbc.gridwidth = 1;
			gbc.weightx     = 0.0;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

			c = getAltField();
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx     = 1.0;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

			c = new JLabel("Align:");
			gbc.gridwidth   = 1;
			gbc.weightx     = 0.0;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

			c = getAlignBox();
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill        = GridBagConstraints.NONE;
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

			c = getAdvancedPanel();
			gbc.insets = new Insets(0,0,0,0);
			gbl.setConstraints(c,gbc);
			imageInfoPanel.add(c);

			// HACK: Assign preferred width of the longest
			// label ("Spacing:") to the URL field to ensure
			// all the label widths match.
			urlLabel.setPreferredSize(spacingLabel.getPreferredSize());

			c.setVisible(showAdvanced);
		}
		return imageInfoPanel;
	}

	protected JPanel getAdvancedPanel()
	{
		if (advancedPanel == null)
		{
			Component c;
			advancedPanel = new JPanel();

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			advancedPanel.setLayout(gbl);

			gbc.anchor      = GridBagConstraints.WEST;
			gbc.gridx       = GridBagConstraints.RELATIVE;
			gbc.gridy       = GridBagConstraints.RELATIVE;
			gbc.gridwidth   = 1;
			gbc.gridheight  = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.insets = new Insets(2,2,2,2);

			c = new JLabel("Size:");
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			advancedPanel.add(c);

			c = getSizePanel();
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill = GridBagConstraints.NONE;
			gbl.setConstraints(c,gbc);
			advancedPanel.add(c);

			c = spacingLabel = new JLabel("Spacing:");
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			advancedPanel.add(c);

			c = getSpacingPanel();
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill = GridBagConstraints.NONE;
			gbl.setConstraints(c,gbc);
			advancedPanel.add(c);

			c = borderLabel = new JLabel("Border:");
			gbc.gridwidth = 1;
			gbl.setConstraints(c,gbc);
			advancedPanel.add(c);

			c = getBorderField();
			gbl.setConstraints(c,gbc);
			advancedPanel.add(c);

			c = borderUnitsLabel = new JLabel("pixels");
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(c,gbc);
			advancedPanel.add(c);
		}
		return advancedPanel;
	}

	protected JPanel getSizePanel()
	{
		if (sizePanel == null)
		{
			// A BoxLayout would be more appropriate, but it does
			// not support insets without more work.
			sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

			sizePanel.add(new JLabel(getHorizontalIcon()));
			sizePanel.add(getHSizeField());
			sizePanel.add(new JLabel("px"));
			sizePanel.add(new JLabel(getVerticalIcon()));
			sizePanel.add(getVSizeField());
			sizePanel.add(new JLabel("px"));
			sizePanel.add(getSizeResetButton());
			sizePanel.add(getShapeCheckBox());
		}
		return sizePanel;
	}

	protected JLabel getPreviewImage()
	{
		if (previewImage == null)
		{
			previewImage = new JLabel();
			previewImage.setHorizontalAlignment(JLabel.CENTER);
			previewImage.setPreferredSize(new Dimension(PREVIEW_SIZE,PREVIEW_SIZE));
			previewImage.setBorder(new LineBorder(Color.black,1));
			try
			{
				previewImage.setIcon(makeIcon("icons/image-delayed.gif"));
			}
			catch (IOException ex)
			{
			}
		}
		return previewImage;
	}

	protected JButton getPreviewButton()
	{
		if (previewButton == null)
		{
			previewButton = new JButton("Preview");
		}
		return previewButton;
	}

	protected JButton getListButton()
	{
		if (listButton == null)
		{
			listButton = new JButton("List");
		}
		return listButton;
	}

	protected JPanel getButtonPanel()
	{
		if (buttonPanel == null)
		{
			buttonPanel = new JPanel();

			buttonPanel.add(getOkButton());
			buttonPanel.add(getCancelButton());
		}
		return buttonPanel;
	}

	protected JButton getOkButton()
	{
		if (okButton == null)
		{
			okButton = new JButton("OK");
		}
		return okButton;
	}

	protected JButton getCancelButton()
	{
		if (cancelButton == null)
		{
			cancelButton = new JButton("Cancel");
		}
		return cancelButton;
	}

	protected JPanel getSpacingPanel()
	{
		if (spacingPanel == null)
		{
			// A BoxLayout would be more appropriate, but it does
			// not support insets without more work.
			spacingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

			spacingPanel.add(new JLabel(getHorizontalIcon()));
			spacingPanel.add(getHSpaceField());
			spacingPanel.add(new JLabel("px"));
			spacingPanel.add(new JLabel(getVerticalIcon()));
			spacingPanel.add(getVSpaceField());
			spacingPanel.add(new JLabel("px"));
		}
		return spacingPanel;
	}

	protected Icon getVerticalIcon()
	{
		if (verticalIcon == null)
		{
			try
			{
					verticalIcon = makeIcon("icons/height.gif");
			}
			catch (IOException e)
			{
			}
		}
		return verticalIcon;
	}

	protected Icon getHorizontalIcon()
	{
		if (horizontalIcon == null)
		{
			try
			{
				horizontalIcon = makeIcon("icons/width.gif");
			}
			catch (IOException e)
			{
			}
		}
		return horizontalIcon;
	}

	protected JTextField getUrlField()
	{
		if (urlField == null)
		{
			urlField = new ClipTextField(20);
		}
		return urlField;
	}

	protected JTextField getAltField()
	{
		if (altField == null)
		{
			altField = new ClipTextField(25);
		}
		return altField;
	}

	protected JTextField getHSizeField()
	{
		if (hSizeField == null)
		{
			hSizeField = new ClipTextField(new PositiveIntegerDocument(false),"",3);
		}
		return hSizeField;
	}

	protected JTextField getVSizeField()
	{
		if (vSizeField == null)
		{
			vSizeField = new ClipTextField(new PositiveIntegerDocument(false),"",3);
		}
		return vSizeField;
	}

	protected JCheckBox getShapeCheckBox()
	{
		if (shapeCheckBox == null)
		{
			shapeCheckBox = new JCheckBox("Preserve shape");
		}
		return shapeCheckBox;
	}

	protected JTextField getHSpaceField()
	{
		if (hSpaceField == null)
		{
			hSpaceField = new ClipTextField(new PositiveIntegerDocument(true),"",3);
		}
		return hSpaceField;
	}

	protected JTextField getVSpaceField()
	{
		if (vSpaceField == null)
		{
			vSpaceField = new ClipTextField(new PositiveIntegerDocument(true),"",3);
		}
		return vSpaceField;
	}

	protected JTextField getBorderField()
	{
		if (borderField == null)
		{
			borderField = new ClipTextField(new PositiveIntegerDocument(true),"",3);
		}
		return borderField;
	}

	protected JComboBox getAlignBox()
	{
		if (alignBox == null)
		{
			alignBox = new JComboBox(new Object[]
			{
				"bottom",
				"top",
				"middle",
				"left",
				"right"
			});
		}
		return alignBox;
	}

	protected JButton getSizeResetButton()
	{
		if (sizeResetButton == null)
		{
			sizeResetButton = new JButton("Reset");
		}
		return sizeResetButton;
	}

	protected JButton getAdvancedButton()
	{
		if (advancedButton == null)
		{
			advancedButton = new JButton(showAdvanced ? "Simple" : "Advanced");
		}
		return advancedButton;
	}

	private Icon makeIcon(final URL gifUrl, int maxSize, Dimension dims) throws IOException
	{
		InputStream in = gifUrl.openStream();
		return makeIcon(in,maxSize,dims);
	}

	private Icon makeIcon(final String gifFile) throws IOException
	{
		return makeIcon(gifFile,Integer.MAX_VALUE,null);
	}

	private Icon makeIcon(final String gifFile, int maxSize, Dimension dims) throws IOException
	{
		InputStream resource = getClass().getResourceAsStream(gifFile);
		if (resource == null)
		{
			System.err.println(ImageView.class.getName() + "/" +
				gifFile + " not found.");
			return null;
		}
		return makeIcon(resource,maxSize,dims);
	}

	private Icon makeIcon(final InputStream stream, int maxSize, Dimension dims) throws IOException
	{
		/* Copy resource into a byte array.  This is
		 * necessary because several browsers consider
		 * Class.getResource a security risk because it
		 * can be used to load additional classes.
		 * Class.getResourceAsStream just returns raw
		 * bytes, which we can convert to an image.
		 */
		BufferedInputStream in = new BufferedInputStream(stream);
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		byte[] buffer = new byte[1024];
		int n;
		while ((n = in.read(buffer)) > 0)
		{
			out.write(buffer, 0, n);
		}
		in.close();
		out.flush();

		buffer = out.toByteArray();
		if (buffer.length == 0)
		{
			System.err.println("warning: image data is zero-length");
			return null;
		}

		// Load unscaled image
		Image image = Toolkit.getDefaultToolkit().createImage(buffer);
		tracker.addImage(image,0);
		try
		{
			tracker.waitForID(0,0);
		}
		catch (InterruptedException e)
		{
				System.out.println("INTERRUPTED while loading Image");
		}
		//loadStatus = tracker.statusID(0, false);
		tracker.removeImage(image, 0);
		int width = image.getWidth(null);
		int height = image.getHeight(null);

		// Record size of unscaled image in passback parameter
		if (dims != null)
		{
			dims.width = width;
			dims.height = height;
		}

		if (width >= height && width > maxSize)
		{
			image = image.getScaledInstance(maxSize,-1,Image.SCALE_SMOOTH);
		}
		else if (height > width && height > maxSize)
		{
			image = image.getScaledInstance(-1,maxSize,Image.SCALE_SMOOTH);
		}

		return new ImageIcon(image);
	}

	protected final static Component component = new Component() {};
	protected final static MediaTracker tracker = new MediaTracker(component);

	public static void main(String[] args)
	{
		ImageDialog imageDialog1 = new ImageDialog(null);
		imageDialog1.setModal(true);
		imageDialog1.show();

		System.out.println("Dialog closed with status: " + imageDialog1.getCloseOption());

		System.exit(0);
	}

	private static class DialogAdapter extends WindowAdapter
	{
		public void windowClosed(WindowEvent ev)
		{
			ImageDialog src = (ImageDialog)ev.getWindow();
			src.exitType = CLOSED;
		}
	}
}
