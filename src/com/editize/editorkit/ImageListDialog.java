package com.editize.editorkit;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class ImageListDialog extends JDialog implements ActionListener
{
	private int exitType;
	private URL listUrl;

	private boolean listFilled = false;

	private JList   imgList;
	private JButton okButton;
	private JButton cancelButton;

	public ImageListDialog(String listUrl, Dialog owner) throws MalformedURLException
	{
		super(owner);

		this.listUrl = new URL(listUrl);

		setTitle("Select Image");
		layoutUI();
		eventWireup();
		pack();
		setResizable(true);
	}

	protected void layoutUI()
	{
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());

		imgList = new JList();
		JScrollPane listScroller = new JScrollPane(imgList);
		cp.add(listScroller,BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		cp.add(buttonPanel,BorderLayout.SOUTH);

		okButton = new JButton("OK");
		buttonPanel.add(okButton);

		cancelButton = new JButton("Cancel");
		buttonPanel.add(cancelButton);
	}

	protected void eventWireup()
	{
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		imgList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				if (evt.getClickCount() > 1) okButton.doClick();
			}
		});
	}

	protected void fillList() throws IOException
	{
		if (listUrl == null) return;

		BufferedReader in = new BufferedReader(
			new InputStreamReader(listUrl.openStream()));

		String line, url, alt;
		int endOfUrl;
		Vector v = new Vector();
		while ((line = in.readLine()) != null)
		{
			if (line.trim().length() <= 0) continue;

			// endOfUrl = index of first whitespace char in string
			endOfUrl = line.indexOf(' ');
			if (line.indexOf('\t') >= 0)
			{
				if (endOfUrl < 0) endOfUrl = line.indexOf('\t');
				else endOfUrl = Math.min(line.indexOf('\t'),endOfUrl);
			}
			if (line.indexOf('\n') >= 0)
			{
				if (endOfUrl < 0) endOfUrl = line.indexOf('\n');
				else endOfUrl = Math.min(line.indexOf('\n'),endOfUrl);
			}
			if (line.indexOf('\r') >= 0)
			{
				if (endOfUrl < 0) endOfUrl = line.indexOf('\r');
				else endOfUrl = Math.min(line.indexOf('\r'),endOfUrl);
			}

			if (endOfUrl < 0)
			{
				url = line;
				alt = "";
			}
			else
			{
				url = line.substring(0,endOfUrl);
				alt = line.substring(endOfUrl+1);
			}
			v.addElement(new ListItem(url,alt));
		}

		// Sort removed
		//Collections.sort(v);

		imgList.setListData(v);

		listFilled = true;
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object src = evt.getSource();
		if (src == okButton)
		{
				exitType = JOptionPane.OK_OPTION;
			hide();
		}
		else if (src == cancelButton)
		{
			exitType = JOptionPane.CANCEL_OPTION;
			hide();
		}
	}

	/**
	 * Overrides deprecated method as of JDK 1.5. Replaced with setVisible.
	 */
	public void show()
	{
		exitType = JOptionPane.CLOSED_OPTION;
		try
		{
				if (!listFilled) fillList();
		}
		catch (IOException ex)
		{
			/* Display user message */
		}
		super.show();
	}

	public void setVisible(boolean visible)
	{
		if (visible) {
			exitType = JOptionPane.CLOSED_OPTION;
			try {
				if (!listFilled)fillList();
			} catch (IOException ex) {
				/* Display user message */
			}
		}
		super.setVisible(visible);
	}

	/**
	 * Gets the method used to close the dialog.
	 * @return OK, CANCEL, or CLOSED
	 */
	public int getCloseOption()
	{
		return exitType;
	}

	public ListItem getSelectedImage()
	{
		return (ListItem)imgList.getSelectedValue();
	}

	public static void main(String[] args) throws MalformedURLException
	{
		ImageListDialog ild = new ImageListDialog(args[0], null);
		ild.setModal(true);
		ild.show();
		System.exit(ild.getCloseOption());
	}

	public static class ListItem implements Comparable
	{
		protected String url;
		protected String alt;

		public ListItem(String url, String alt)
		{
			this.url = url;
			this.alt = alt;
		}

		public String getUrl()
		{
			return url;
		}

		public String getAlt()
		{
			return alt;
		}

		public String toString()
		{
			if (alt == null || alt.length() == 0) return url;
			return alt;
		}

		public int compareTo(Object o)
		{
			ListItem li = (ListItem)o;
			return this.toString().compareTo(li.toString());
		}
	}
}
