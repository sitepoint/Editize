package com.editize;

import java.applet.Applet;
import java.applet.AppletContext;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class EditizeSubmitter extends JApplet implements ActionListener, Runnable
{
	private boolean osxSubmitMode = false, immediateSubmitMode = false;
	private JApplet applet = null;
	private static final int MAC_SUBMIT_DELAY = 2000;
	private JButton submitButton;

	public EditizeSubmitter()
	{
	}

	/**
	 * Constructor used by EditizeApplet to instantiate this Applet
	 * as an internal component.
	 */
	protected EditizeSubmitter(JApplet editizeApplet)
	{
		this.applet = editizeApplet;
	}

	public String getParameter(String key)
	{
		try
		{
			return super.getParameter(key);
		}
		catch (NullPointerException ex)
		{
			return applet.getParameter(key);
		}
	}

	public void init()
	{
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());

		String label = getParameter("submitbuttonlabel");
		if (label == null) label = "Submit";
		submitButton = new JButton(label);
		cp.add(submitButton, BorderLayout.CENTER);
		submitButton.addActionListener(this);

		try
		{
			if ("true".equalsIgnoreCase(getParameter("osx"))) osxSubmitMode = true;
			if ("true".equalsIgnoreCase(getParameter("immediate"))) immediateSubmitMode = true;
		}
		catch (NullPointerException ex)
		{
		}

		submitButton.setVisible(!immediateSubmitMode);
	}

	public void start()
	{
		if (immediateSubmitMode) submitButton.doClick();
	}

	public AppletContext getAppletContext()
	{
		try
		{
			return super.getAppletContext();
		}
		catch (NullPointerException ex)
		{
			return applet.getAppletContext();
		}
	}

	public URL getCodeBase()
	{
		try
		{
			return super.getCodeBase();
		}
		catch (NullPointerException ex)
		{
			return applet.getCodeBase();
		}
	}

	public void actionPerformed(ActionEvent evt)
	{
		// Launch submission process in a separate thread
		//System.out.println("Spawning submit thread...");
		Thread submitThread = new Thread(this,"Editize submit");
		submitThread.start();
	}

	public void run()
	{
		// Loop through all running applets and make any
		// RapidEditApplets among them write their contents
		// to their hidden form fields.
		Enumeration applets = getAppletContext().getApplets();
		Object a = null;
		while (applets.hasMoreElements())
		{
			a = applets.nextElement();
			if (a instanceof EditizeApplet)
			{
				((EditizeApplet)a).writeToField();
			}
		}

		String submitFieldID = getParameter("submitbuttonname");
		String fieldID = getParameter("fieldid");
		if (fieldID == null)return;

		// Write the label to the submitter hidden field
		if (submitFieldID != null) {
			if (osxSubmitMode) {
				try {
					// Mac OS X submit mode
					URL url = new URL(getCodeBase(),
									  "osx.html?id=" + submitFieldID +
									  "&clear=1");
					getAppletContext().showDocument(url,
							fieldID + "_submitframe");
					Thread.sleep(MAC_SUBMIT_DELAY);
					url = new URL(getCodeBase(),
								  "osx.html?id=" + submitFieldID + "&txt=" +
								  getParameter("submitbuttonlabel"));
					getAppletContext().showDocument(url,
							fieldID + "_submitframe");
					Thread.sleep(MAC_SUBMIT_DELAY);
				} catch (MalformedURLException ex) {
					System.err.println(
							"Malformed URL while submitting submit button value.");
				} catch (InterruptedException ex) {
					System.err.println(
							"Interrupted while submitting submit button value.");
				}
			} else {
				// Escape JavaScript-sensitive characters in article text
				StringBuffer submitValue = new StringBuffer(getParameter(
						"submitbuttonlabel"));
				int i = 0;
				char lastChar = ' ', newChar;
				if (submitValue.length() > 0)
					do {
						switch (newChar = submitValue.charAt(i)) {
							// Escape backslashes and apostrophes
							case '\'':
							case '\\':
								submitValue.insert(i++, '\\');
								break;

								// Replace "\r\n" or "\n" with newline code
							case '\n':
								if (lastChar == '\r') {
									//System.out.println("Escaping \\r\\n.");
									submitValue.deleteCharAt(--i);
								}

								//else System.out.println("Escaping solo \\n.");
								submitValue.replace(i, ++i, "\\n");
								break;

							default:

								// Replace "\r" (alone) with newline code
								if (lastChar == '\r') {
									//System.out.println("Escaping solo \\r.");
									submitValue.replace(i - 1, i++, "\\n");
								}
						}
						lastChar = newChar;
					} while (++i < submitValue.length());

				String value = submitValue.toString();

				// Store the encoded document into a hidden form field
				if ("true".equalsIgnoreCase(getParameter("opera6")))
					value = EditizeApplet.htmlSpecialChars(value); // Compensate for Opera bug!

				try {
					Class jsObject = Class.forName(
							"netscape.javascript.JSObject");
					Method getWindow = jsObject.getMethod("getWindow",
							new Class[] {Applet.class});
					Object win = getWindow.invoke(jsObject, new Object[] {this});
					Method eval = jsObject.getMethod("eval",
							new Class[] {String.class});
					eval.invoke(win,
								new Object[] {"__getObj('" + submitFieldID +
								"').value = '" + value + "';"});
				} catch (Exception e) {
					e.printStackTrace(System.err);
					JOptionPane.showMessageDialog(
							this,
							"Editize could not access the form element",
							"ERROR",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		} // Write label to submitter hidden field

		// Submit the form for this applet
		/*if (immediateSubmitMode)
		{
			System.out.println("Triggering immediate submission.");
			try {
				URL url = new URL(getCodeBase(),
								  "osx.html?id=" + fieldID + "&submit=1");
				getAppletContext().showDocument(url, "submit_applet_frame");
			}
			catch (MalformedURLException ex)
			{
				System.err.println("Malformed URL encountered during submission.");
			}
		}
		else */if (osxSubmitMode)
		{
			try
			{
				// Mac OS X submit mode
				URL url = new URL(getCodeBase(), "osx.html?id=" + fieldID + "&submit=1");
				getAppletContext().showDocument(url, fieldID + "_submitframe");
			}
			catch (MalformedURLException ex)
			{
				System.err.println("Malformed URL encountered during submission.");
			}
		}
		else
		{
			try
			{
				// Netscape 4 / Opera submit mode
				Class jsObject = Class.forName("netscape.javascript.JSObject");
				Method getWindow = jsObject.getMethod("getWindow",new Class[] { Applet.class });
				Object win = getWindow.invoke(jsObject,new Object[] {this});
				Method eval = jsObject.getMethod("eval",new Class[] {String.class });
				eval.invoke(win,new Object[] {"__submitCallback('" + fieldID + "');"});
			}
			catch (Exception ex)
			{
				System.err.println("Error during submission (fieldID = "+fieldID+"): " + ex.getMessage());
			}
		}
		//System.out.println("Submit thread complete.");
	}
}
