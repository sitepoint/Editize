package com.editize;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.swing.*;

public class EditizeApplet extends JApplet implements Runnable
{
	private static final String VERSION = "2.13";

	private static final boolean DEBUG = false;
	private static final int MAC_CHUNK_SIZE = 1024;
	private static final int NS4_CHUNK_SIZE = 1024;
	private static final int MAC_SUBMIT_DELAY = 2000;

	private Editize aEditor;
	private String fieldID = null;
	private boolean osxSubmitMode = false;
	private boolean ns4SubmitMode = false;
	private boolean codeView = false;
	private JApplet submitButton = null;
	private EditizeCodeWrapper codeWrapper = null;

	private String licenseFileExtension = "lic";
	private JPanel background;

    private boolean isFirstRun = true;

	/**
	 * Default constructor.
	 * Creation date: (10/07/2001 12:31:21 PM)
	 */
	public EditizeApplet()
	{
		super();

        //System.err.println("Instantiating applet.");

		// Set the Look And Feel:
		initializeLookAndFeel();

		background = new JPanel(new BorderLayout());
		background.setOpaque(false);

		getContentPane().add(background,BorderLayout.CENTER);

	}

	/**
	 * Returns information about this applet.
	 * @return a string of information about this applet
	 */
	public String getAppletInfo()
	{
		return "Editize Applet\n\nAn applet to edit HTML content.";
	}

	/**
	 * Convert a color string such as "RED" or "#NNNNNN" to a Color.
	 * Borrowed and adapted from Java's CSS class.
	 * Note: This will only convert the HTML3.2 color strings
	 *       or a string of length 7;
	 *       otherwise, it will return null.
     * @param str the HTML color name or hex string
     * @return the equivalent Color
	 */
	static Color stringToColor(String str) {
		if (str == null || str.length() == 0) return null;

        Color color;
		if (str.charAt(0) == '#') color = hexToColor(str);
        else if (str.equalsIgnoreCase("Black"))
          color = hexToColor("#000000");
        else if (str.equalsIgnoreCase("Silver"))
          color = hexToColor("#C0C0C0");
        else if (str.equalsIgnoreCase("Gray"))
          color = hexToColor("#808080");
        else if (str.equalsIgnoreCase("White"))
          color = hexToColor("#FFFFFF");
        else if (str.equalsIgnoreCase("Maroon"))
          color = hexToColor("#800000");
        else if (str.equalsIgnoreCase("Red"))
          color = hexToColor("#FF0000");
        else if (str.equalsIgnoreCase("Purple"))
          color = hexToColor("#800080");
        else if (str.equalsIgnoreCase("Fuchsia"))
          color = hexToColor("#FF00FF");
        else if (str.equalsIgnoreCase("Green"))
          color = hexToColor("#008000");
        else if (str.equalsIgnoreCase("Lime"))
          color = hexToColor("#00FF00");
        else if (str.equalsIgnoreCase("Olive"))
          color = hexToColor("#808000");
        else if (str.equalsIgnoreCase("Yellow"))
          color = hexToColor("#FFFF00");
        else if (str.equalsIgnoreCase("Navy"))
          color = hexToColor("#000080");
        else if (str.equalsIgnoreCase("Blue"))
          color = hexToColor("#0000FF");
        else if (str.equalsIgnoreCase("Teal"))
          color = hexToColor("#008080");
        else if (str.equalsIgnoreCase("Aqua"))
          color = hexToColor("#00FFFF");
        else
          color = hexToColor(str); // sometimes no leading #
		return color;
	}

	/**
	 * Convert a "#FFFFFF" hex string to a Color.
	 * If the color specification is bad (e.g. no leading #), an attempt
	 * will be made to fix it up.
	 * Borrowed from Java's CSS class.
     * @param value the hex color string (e.g. "#FFFFFF")
     * @return the equivalent Color
	 */
	static Color hexToColor(String value) {
		String digits;
		if (value.startsWith("#")) {
			digits = value.substring(1, Math.min(value.length(), 7));
		} else {
			digits = value;
		}
		String hstr = "0x" + digits;
		Color c;
		try {
			c = Color.decode(hstr);
		} catch (NumberFormatException nfe) {
			c = null;
		}
		return c;
	}

	/**
	 * Makes getDocumentBase final to prevent circumvention of
	 * licensing by overriding this to return a fixed value.
	 *
	 * @return The URL of the document responsible for loading this applet.
	 */
	public final java.net.URL getDocumentBase()
	{
		return super.getDocumentBase();
	}

	/**
	 * Get an Applet parameter, translating special characters
	 * appearing in the string. The following translations are
	 * performed in the retrieved parameter values:
	 *
	 *   "\n" -> newline
	 *   "\r" -> carriage return
	 *   "\t" -> tab
	 *   "\\" -> "\"
	 *
	 * This is a workaround to a limitation in getParam that
	 * appeared in the Java Plug-In as of JRE 1.3.1_01a. See Java
	 * Bug Parade Bug #4528782.
	 *
	 * @return The retrieved parameter value (after translation), or null if no such parameter specified.
	 * @param name The name of the parameter to fetch.
	 */
	public String getParameterWithWhitespace(String name)
	{

		String param = super.getParameter(name);
		if (param == null) return null;

		StringBuffer sb = new StringBuffer(param);
		for (int i=0;i<sb.length();i++)
		{
			if (sb.charAt(i) == '\\')
			{
				switch (sb.charAt(i+1))
				{
					case 'n':
						sb.replace(i,i+2,"\n");
						break;
					case 'r':
						sb.replace(i,i+2,"\r");
						break;
					case 't':
						sb.replace(i,i+2,"\t");
						break;
					case '\\':
						sb.deleteCharAt(i);
						break;
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Initializes the applet.
	 *
	 * @see #start
	 * @see #stop
	 * @see #destroy
	 */
	public void init()
	{
		Container cp = getContentPane();
		String p;
		Color c;

        //System.err.println("Initializing applet.");
		aEditor = new Editize(this);

		// Customizable license file extension (for Windows .NET Server)
		p = getParameter("licenseext");
		if (p != null) licenseFileExtension = p;

		c = stringToColor(getParameter("appletbgcolor"));
		if (c != null)
		{
			background.setOpaque(true);
			background.setBackground(c);
			setBackground(c);
		}

		// Use Mac OSX Submit method
		if (getParameter("osx")!= null) System.out.println("MacOS detected by Editize: " + getParameter("osx"));
		if ("true".equalsIgnoreCase(getParameter("osx"))) osxSubmitMode = true;

		// Use NS4 submit chunking
		if ("true".equalsIgnoreCase(getParameter("ns4"))) ns4SubmitMode = true;

		// Code view available
		if ("true".equalsIgnoreCase(getParameter("codeview"))) codeView = true;

		// HTML Clipboard importing enabled
		if ("false".equalsIgnoreCase(getParameter("htmlclipboardimport")))
		{
			aEditor.setHtmlClipboardEnabled(false);
		}

		// XHTML Alignment classes enabled
		if ("true".equalsIgnoreCase(getParameter("xhtmlstrict")))
		{
			aEditor.setXHTMLCompliantAlignment(true);
		}

		// Form elements allowed
		if ("true".equalsIgnoreCase(getParameter("formelementsallowed")))
		{
			aEditor.setFormElementsAllowed(true);
		}

		// Opera requires Java 1.4+
		if ("true".equalsIgnoreCase(getParameter("opera")))
		{
			String ver = System.getProperty("java.version");
				ver = ver.substring(0,ver.indexOf('.',ver.indexOf('.')+1));
			if (Float.parseFloat(ver) < 1.4f)
			{
				GridBagLayout gbl = new GridBagLayout();
				GridBagConstraints gbc = new GridBagConstraints();
				JPanel errPanel = new JPanel(gbl);
				cp.add(errPanel,BorderLayout.CENTER);
				JLabel msg = new JLabel("This Applet requires the Java Runtime Environment (JRE) 1.4 or later in Opera.",JLabel.CENTER);
				gbc.anchor = GridBagConstraints.SOUTH;
				gbc.insets = new Insets(5,5,5,5);
				gbl.setConstraints(msg,gbc);
				errPanel.add(msg);
				JButton dlBtn = new JButton();
				gbc.gridy = 1;
				gbc.anchor = GridBagConstraints.NORTH;
				gbl.setConstraints(dlBtn,gbc);
				dlBtn.setAction(new AbstractAction()
				{
					public void actionPerformed(ActionEvent e)
					{
						try
						{
							getAppletContext().showDocument(new URL("http://www.java.com/"),"_blank");
						}
						catch (MalformedURLException ignored) {
                            // Should not happen.
                        }
					}
				});
				dlBtn.setForeground(Color.blue);
				dlBtn.setText("Download it Now!");
				errPanel.add(dlBtn);
				return;
			}
		}

		// Get field ID
		fieldID = getParameter("fieldid");

		// Display own submit button
		if ("true".equalsIgnoreCase(getParameter("showsubmitbutton")))
		{
			createSubmitButton();
			if (submitButton != null) submitButton.init();
		}

		// Fetch and apply presentation parameters
		try
		{
			// Document background color
			c = stringToColor(getParameter("bgcolor"));
			if (c != null) aEditor.setBackgroundColor(c);

			// Base URL for images
			p = getParameter("docbaseurl");
			if (p != null) aEditor.setBaseUrl(p);

			// URL for image list
			p = getParameter("imglisturl");
			if (p != null) aEditor.setImgListUrl(p);

			// Base font
			p = getParameter("basefontface");
			if (p != null) aEditor.setBaseFontFamily(p);
			p = getParameter("basefontsize");
			if (p != null) aEditor.setBaseFontSize(Integer.parseInt(p));
			c = stringToColor(getParameter("basefontcolor"));
			if (c != null) aEditor.setBaseFontColor(c);

			// Heading
			p = getParameter("headingfontface");
			if (p != null) aEditor.setHeadingFontFamily(p);
			p = getParameter("headingfontsize");
			if (p != null) aEditor.setHeadingFontSize(Integer.parseInt(p));
			c = stringToColor(getParameter("headingfontcolor"));
			if (c != null) aEditor.setHeadingFontColor(c);

			// Subheading
			p = getParameter("subheadingfontface");
			if (p != null) aEditor.setSubheadingFontFamily(p);
			p = getParameter("subheadingfontsize");
			if (p != null) aEditor.setSubheadingFontSize(Integer.parseInt(p));
			c = stringToColor(getParameter("subheadingfontcolor"));
			if (c != null) aEditor.setSubheadingFontColor(c);

			// Block Quote
			p = getParameter("blockquotefontface");
			if (p != null) aEditor.setBlockquoteFontFamily(p);
			p = getParameter("blockquotefontsize");
			if (p != null) aEditor.setBlockquoteFontSize(Integer.parseInt(p));
			c = stringToColor(getParameter("blockquotefontcolor"));
			if (c != null) aEditor.setBlockquoteFontColor(c);

			// Code block
			c = stringToColor(getParameter("codebackgroundcolor"));
			if (c != null) aEditor.setCodeBlockBackgroundColor(c);

			// Highlighting
			c = stringToColor(getParameter("highlightcolor"));
			if (c != null) aEditor.setHighlightColor(c);

			// Block highlighting
			c = stringToColor(getParameter("blockhighlightcolor"));
			if (c != null) aEditor.setBlockHighlightColor(c);

			// Links
			c = stringToColor(getParameter("linkcolor"));
			if (c != null) aEditor.setLinkColor(c);

		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(aEditor,
				"Invalid font size specified (not a number).", "ERROR", JOptionPane.ERROR_MESSAGE);
		}
		catch (MalformedURLException e)
		{
			JOptionPane.showMessageDialog(aEditor,
				"Invalid base URL specified (not a valid URL).", "ERROR", JOptionPane.ERROR_MESSAGE);
		}

		// Fetch and apply feature settings
		if ("false".equalsIgnoreCase(getParameter("about")))
			aEditor.setAboutEnabled(false);
		p = getParameter("paragraphstyles");
		if (p != null)
		{
			aEditor.setParagraphStylesEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("headingstyle");
		if (p != null)
		{
			aEditor.setHeadingStyleEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("subheadingstyle");
		if (p != null)
		{
			aEditor.setSubheadingStyleEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("blockquotestyle");
		if (p != null)
		{
			aEditor.setIndentEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("codeblockstyle");
		if (p != null)
		{
			aEditor.setCodeblockStyleEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("paragraphalignments");
		if (p != null)
		{
			aEditor.setParagraphAlignmentEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("bulletlists");
		if (p != null)
		{
			aEditor.setBulletListEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("numberedlists");
		if (p != null)
		{
			aEditor.setNumberedListEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("boldtext");
		if (p != null)
		{
			aEditor.setBoldEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("italictext");
		if (p != null)
		{
			aEditor.setItalicEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("underlinetext");
		if (p != null)
		{
			aEditor.setUnderlineEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("highlighttext");
		if (p != null)
		{
			aEditor.setHighlightEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("highlightblock");
		if (p != null)
		{
			aEditor.setBlockHighlightEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("inlinecode");
		if (p != null)
		{
			aEditor.setCodeEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("hyperlinks");
		if (p != null)
		{
			aEditor.setHyperlinksEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("linkurls");
		if (p != null)
		{
			try
			{
				int numUrls = Integer.parseInt(p);
				if (numUrls > 0)
				{
					Vector linkurls = new Vector();
					for (int i=1; i<=numUrls; i++)
					{
						p = getParameter("linkurls." + i);
						if (p != null) linkurls.add(p);
					}
					aEditor.setLinkURLs(linkurls);
				}
			}
			catch (NumberFormatException ex) {
				// If an invalid number is specified, ignore the parameter
			}
		}
		p = getParameter("tableclasses");
		if (p != null)
		{
			try
			{
				int numUrls = Integer.parseInt(p);
				if (numUrls > 0)
				{
					Vector linkurls = new Vector();
					for (int i=1; i<=numUrls; i++)
					{
						p = getParameter("tableclasses." + i);
						if (p != null) linkurls.add(p);
					}
					aEditor.setTableClasses(linkurls);
				}
			}
			catch (NumberFormatException ex) {
				// If an invalid number is specified, ignore the parameter
			}
		}
		p = getParameter("images");
		if (p != null)
		{
			aEditor.setImagesEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("tables");
		if (p != null)
		{
			aEditor.setTablesEnabled(
				Boolean.valueOf(p).booleanValue()
			);
		}
		p = getParameter("editbuttons");
		if (p != null)
		{
			aEditor.setStandardButtonsVisible(
				Boolean.valueOf(p).booleanValue()
			);
		}

	}

	/**
	 * Obtains the contents of the hidden form field that Editize is assigned to edit.
	 *
	 * @return String containing the HTML content of the form field.
	 */
	private String getFieldContents()
	{
		if (fieldID == null || osxSubmitMode || ns4SubmitMode)
		{
			return getParameterWithWhitespace("articleText");
		}
		try
		{
			// Read the document from the hidden form field
			Class jsObject = Class.forName("netscape.javascript.JSObject");
			Method getWindow = jsObject.getMethod("getWindow",new Class[] { Applet.class });
			Object win = getWindow.invoke(jsObject,new Object[] {this});
			Method eval = jsObject.getMethod("eval",new Class[] { String.class });
			return (String) eval.invoke(win,new Object[] {"__getObj('"+fieldID+"').value"});
		}
		catch (Exception e)
		{
			System.err.println(e.toString());
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(
				this,
				"Editize could not access the form element.",
				"ERROR",
				JOptionPane.ERROR_MESSAGE);
			return null;
		}

	}

	/**
	 * Determines whether or not this software is licensed
	 * for use on this server. Final to prevent overriding,
	 * which might make copy protection easily circumventable.
	 *
     * @throws GeneralSecurityException if an error occurs while attempting to verify license status.
	 * @return True if licensed, false if not.
	 */
	private boolean isLicensed() throws GeneralSecurityException
	{
		InputStream is;
		ObjectInputStream ois;
		PublicKey pk;

		// Beta timeout
		//if ((new Date()).after(new Date(60975694800000l))) return false;

		// Get the current hostname
		String curhost = getDocumentBase().getHost().toLowerCase();
		if (curhost.length() == 0)
		{
			System.out.println("Editize running in test mode (no local hostname found).");
			return true;
		}

		// Special exceptions
		if (curhost.endsWith("cysticfibrosis.sitepoint.com.au") ||
			curhost.endsWith("cysticfibrosis.com.au"))
			return true;

		// Exceptional servers (127.* and localhost) automatically authorized
		InetAddress ip;
		try
		{
			ip = InetAddress.getByName(curhost);
			if (ip.getHostAddress().startsWith("127.") ||
				curhost.equalsIgnoreCase("localhost"))
				return true;
		}
		catch (UnknownHostException ex)
		{
			System.err.println(ex.getMessage());
			throw new GeneralSecurityException("Editize could not resolve hostname '" + curhost + "' to an IP address.");
		}

		System.out.println("Hostname '" + curhost + "' resolves to IP address " + ip.getHostAddress());

		try
		{
			// Get the public key
			is = getClass().getResourceAsStream("pub.key");
			ois = new ObjectInputStream(is);
			RSAPublicKeySpec ks = new RSAPublicKeySpec(
				(BigInteger) ois.readObject(),
				(BigInteger) ois.readObject());
			KeyFactory kf = KeyFactory.getInstance("RSA");
			pk = kf.generatePublic(ks);
		}
		catch (Exception ex)
		{
			throw new GeneralSecurityException("The Editize component in use in this page is damaged ("+ex.toString()+": "+ex.getMessage()+").");
		}

		try
		{
			// Disregard 'www' at the front of the hostname
			if (curhost.startsWith("www.")) curhost = curhost.substring(4);

			// Keep trying to fetch licenses for enclosing domains until
			// one is found or we reach the top-level domain
			boolean domainLicenseRequired = false;
			boolean hostnameLicenseFound = false;
			do
			{
				// Get the signature for the licensed hostname
				URL licURL = new URL(getCodeBase(),"editize."+curhost+"."+licenseFileExtension);
				try
				{
					System.out.println("Trying to find license for: " + curhost);
					is = licURL.openStream();
					hostnameLicenseFound = true;
					if (isLicenseFileValid(is, curhost, domainLicenseRequired, pk)) return true;
					break;
				}
				catch (SignatureException se)
				{
					// Trim lowest-level domain off the hostname
					curhost = curhost.substring(curhost.indexOf('.')+1);
					domainLicenseRequired = true;
				}
				catch (ClassFormatError cfe)
				{
					// Trim lowest-level domain off the hostname
					curhost = curhost.substring(curhost.indexOf('.')+1);
					domainLicenseRequired = true;
				}
				catch (IOException ioe)
				{
					// Trim lowest-level domain off the hostname
					curhost = curhost.substring(curhost.indexOf('.')+1);
					domainLicenseRequired = true;
				}
			}
			while (curhost.indexOf('.') >= 0);

			if (!hostnameLicenseFound)
			{
				System.out.println("Hostname-specific license file not found. Is this a private network server?");

				// Determine if we're dealing with a private network IP
				boolean isPrivateIP = false;
				if (ip.getHostAddress().startsWith("10.") ||
					ip.getHostAddress().startsWith("192.168.")) isPrivateIP = true;
				else if (ip.getHostAddress().startsWith("172."))
				{
					// 172.[16-31].*
					byte[] ipaddr = ip.getAddress();
					isPrivateIP = (ipaddr[0] == (byte)172 &&
								   ipaddr[1] >= 16 && ipaddr[1] < 32);
				}
				if (!isPrivateIP)
				{
					System.out.println(ip.getHostAddress() + " is not a private network IP. Failed license check.");
					return false;
				}

				System.out.println(ip.getHostAddress() + " is a private network IP! Checking for editize.lic...");

				// If the current IP is a private network IP, try
				// validating a private network license
				domainLicenseRequired = false;
				curhost = "localnetwork";
				URL licURL = new URL(getCodeBase(),"editize."+licenseFileExtension);
				is = licURL.openStream();
				if (isLicenseFileValid(is, curhost, domainLicenseRequired, pk)) return true;
			}

			return false;
		}
		catch (ClassNotFoundException ex)
		{
			throw new GeneralSecurityException("A required Java class could not be found on your system.\n" + ex.getMessage());
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new GeneralSecurityException("A required Java security component could not be found on your system.\n" + ex.getMessage());
		}
		catch (InvalidKeyException ex)
		{
			throw new GeneralSecurityException("The Editize component in use in this page is damaged.\n"+ex.getMessage());
		}
		catch (ClassFormatError er)
		{
			throw new GeneralSecurityException("The license file for Editize in this Web page is invalid.");
		}
		catch (SignatureException ex)
		{
			throw new GeneralSecurityException("The license file for Editize in this Web page is invalid.");
		}
		catch (IOException ex)
		{
			System.err.println("IOError while attempting to download license file: " + ex.getMessage());
			ex.printStackTrace(System.err);
			throw new GeneralSecurityException("The license file for Editize in this Web page is not found.");
		}
	}

	private boolean isLicenseFileValid(InputStream is, String curhost, boolean domainLicenseRequired, PublicKey pk)
			throws GeneralSecurityException, ClassNotFoundException, IOException, ClassFormatError
	{
		ObjectInputStream ois;

		ois = new ObjectInputStream(is);
		String expirydate = new String((byte[]) ois.readObject());
		byte[] hostsignature = (byte[]) ois.readObject();
		byte[] expirysignature = (byte[]) ois.readObject();

		// Prep a Signature object and verify the validity of the license for this host
		Signature s = Signature.getInstance("MD5withRSA");
		s.initVerify(pk);
		s.update(("."+curhost).getBytes()); // Check for domain license
		if (!s.verify(hostsignature))
		{
			if (domainLicenseRequired) return false;
			s.update(curhost.getBytes()); // Check for non-domain license
			if (!s.verify(hostsignature)) return false;
		}

		// Check signature of license date
		s.update(expirydate.getBytes());
		if (!s.verify(expirysignature)) return false;

		// Parse license date
		int day, month, year;
		try
		{
			year = Integer.parseInt(expirydate.substring(0,4));
			month = Integer.parseInt(expirydate.substring(5,7));
			day = Integer.parseInt(expirydate.substring(8));
		}
		catch (NumberFormatException e)
		{
			return false;
		}

		// Check if the license is expired
			Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.YEAR) > year || cal.get(Calendar.YEAR) == year &&
			(cal.get(Calendar.MONTH)+1 > month || cal.get(Calendar.MONTH)+1 == month &&
					cal.get(Calendar.DAY_OF_MONTH) > day))
			throw new GeneralSecurityException(
				"The Trial License for Editize has expired. To continue using\n" +
				"Editize, the Webmaster of this site must purchase a License\n" +
				"from Editize.com.");

		if (!expirydate.equals("9999-99-99"))
			System.out.println("Your Editize license is valid until " + expirydate + " (YYYY-MM-DD).");


		// If we've made it this far, the license key is A-OK!
		System.out.println("Editize license file verified.");
		return true;
	}

	/**
	 * Called to start the applet. You never need to call this method
	 * directly, it is called when the applet's document is visited.
	 *
	 * This is declared final to prevent overriding the license check.
	 * Sorry 'bout dat. :) I'm open to suggestions.
	 *
	 * @see #init
	 * @see #stop
	 * @see #destroy
	 */
	public final void start()
	{
        // This stuff really belongs in init(), but historically some of the
        // UI massaging that goes on here didn't work at that stage of the
        // lifecycle. It might these days. Still a good candidate for
        // refactoring in future, possibly by moving it into an
        // EventQueue.invokeLater task.
        if (isFirstRun)
        {
            isFirstRun = false;
            System.out.println("Starting Editize version: " + VERSION);

            // Verify license validity
            try
            {
                if (!this.isLicensed())
                    throw new GeneralSecurityException("Editize is not licensed for use on " + getDocumentBase().getHost() + ".");
            }
            catch (GeneralSecurityException ex)
            {
                JOptionPane.showMessageDialog(aEditor, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                remove(this.aEditor);
                return;
            }

            if (submitButton != null) {
                submitButton.init();
                submitButton.start();
            }

            SwingUtilities.invokeLater(this);

            // Display Editize version in browser status bar
            getAppletContext().showStatus("Edtize version " + VERSION + " started.");
        }
        else
        {
            if (submitButton != null) submitButton.start();
        }
	}

	public void stop()
	{
		if (submitButton != null) submitButton.stop();
	}

	/**
	 * Updates the component tree for this applet.
	 */
	public final void run()
	{
	  // Do the update:
	  SwingUtilities.updateComponentTreeUI(aEditor.getTextPane());

	  // Load supplied article code
	  //String aText = getParameterWithWhitespace("articletext");
	  String aText = getFieldContents();
	  if (aText == null || aText.length() == 0)
		aEditor.clear();
	  else
		aEditor.setHtml(aText);

	  background.add(codeView ?
					 (Component) (codeWrapper = new EditizeCodeWrapper(aEditor)) :
					 aEditor, BorderLayout.CENTER);
	  aEditor.getTextPane().requestFocus();

	  // Delayed call to revalidate applet (circumvents bug in Java 1.4.2)
	  // Signal layout of dynamically added component
	  SwingUtilities.invokeLater(new Runnable()
								 {
								   public void run()
								   {
									 EditizeApplet.this.aEditor.revalidate();
								   }
								 });
	  //aEditor.revalidate(); // Signal layout of dynamically added component
	}

	public void writeToField()
	{
		String html;
		try
		{
			// Obtain the coded version of the document
			html = codeView ? codeWrapper.getCode() : aEditor.getHtml(true);

			if (DEBUG)
			{
				javax.swing.text.ElementIterator it = new javax.swing.text.ElementIterator(aEditor.getTextPane().getDocument());
				javax.swing.text.Element e;
				while ((e = it.next()) != null)
				{
					System.out.println("Element: " + e.toString());
					javax.swing.text.AttributeSet atts = e.getAttributes();
					Enumeration attEnum = atts.getAttributeNames();
					while (attEnum.hasMoreElements())
					{
						Object at = attEnum.nextElement();
						Object val = atts.getAttribute(at);
						System.out.println(" -> " + at.toString() + " = " + val.toString());
					}
					System.out.println();
				}

				System.out.println("HTML Document:\n"+html);
			}
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(
			this,
			"Text could not be written due to an I/O Error.",
			"ERROR",
			JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (fieldID == null) return;

		if (osxSubmitMode)
		{
			try
			{
				// Mac OS X submit mode
				URL url = new URL(getCodeBase(),"osx.html?id="+fieldID+"&clear=1");
//System.out.println("Loading " + url.toExternalForm() + " into frame " + fieldID + "_submitframe.");
				getAppletContext().showDocument(url,fieldID+"_submitframe");
				Thread.sleep(MAC_SUBMIT_DELAY);
				while (html.length() > 0)
				{
					// As of Java 1.4 should be:
					// String chunk = URLEncoder.encode(html.substring(0,Math.min(MAC_CHUNK_SIZE,html.length())), "UTF-8");
					String chunk = URLEncoder.encode(html.substring(0,Math.min(MAC_CHUNK_SIZE,html.length())));
					url = new URL(getCodeBase(),"osx.html?id="+fieldID+"&txt="+chunk);
//System.out.println("Loading " + url.toExternalForm() + " into frame " + fieldID + "_submitframe.");
					getAppletContext().showDocument(url,fieldID+"_submitframe");
					if (html.length() <= MAC_CHUNK_SIZE) html="";
					else html = html.substring(MAC_CHUNK_SIZE);
					Thread.sleep(MAC_SUBMIT_DELAY);
				}
			}
			catch (MalformedURLException ex)
			{
				System.err.println("Malformed URL while submitting document.");
			}
			catch (InterruptedException ex)
			{
				System.err.println("Interrupted while submitting document.");
			}
		}
		else
		{
			// Store the encoded document into a hidden form field
			try
			{
				Class jsObject = Class.forName("netscape.javascript.JSObject");
				Method getWindow = jsObject.getMethod("getWindow",new Class[] { Applet.class });
				Object win = getWindow.invoke(jsObject,new Object[] {this});
				Method eval = jsObject.getMethod("eval",new Class[] { String.class });
				if (ns4SubmitMode)
				{
					eval.invoke(win, new Object[] {"__getObj('" + fieldID + "').value = '';"});
					do
					{
						String value = html.substring(0, Math.min(NS4_CHUNK_SIZE, html.length()));
						html = html.substring(Math.min(NS4_CHUNK_SIZE, html.length()));
						value = javaScriptSpecialChars(value);
						if ("true".equalsIgnoreCase(getParameter("opera6")))
							value = htmlSpecialChars(value); // Compensate for Opera bug!
						eval.invoke(win, new Object[] {"__ns4submit('" + fieldID + "', '" + value + "');"});
					}
					while (html.length() > 0);
				}
				else
				{
					html = javaScriptSpecialChars(html);
					if ("true".equalsIgnoreCase(getParameter("opera6")))
						html = htmlSpecialChars(html); // Compensate for Opera bug!
					eval.invoke(win, new Object[] {"__getObj('" + fieldID + "').value = '" + html + "';"});
				}
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
				JOptionPane.showMessageDialog(
					this,
					"Editize could not access the form element",
					"ERROR",
					JOptionPane.ERROR_MESSAGE);
            }
		}
	}

	/**
	 * Installs the Kunststoff Look And Feel.
	 */
	public final void initializeLookAndFeel() {
		if (System.getProperty("os.name").startsWith("Mac OS")) return;

		UIManager.put("TabbedPane.contentBorderInsets", new Insets(1,1,2,2));

		UIManager.put("ClassLoader", EditizeApplet.class.getClassLoader());
		try {
			//LookAndFeel lnf = new MetalLookAndFeel();
			Class kClass = Class.forName("com.incors.plaf.kunststoff.KunststoffLookAndFeel");
			Class tClass = Class.forName("com.incors.plaf.kunststoff.KunststoffTheme");
			Class gClass = Class.forName("com.incors.plaf.kunststoff.KunststoffGradientTheme");
			LookAndFeel lnf = (LookAndFeel)kClass.newInstance();
			Method setCurrentTheme = kClass.getMethod("setCurrentTheme",new Class[] {Class.forName("javax.swing.plaf.metal.MetalTheme")});
			setCurrentTheme.invoke(lnf,new Object[]{tClass.newInstance()});
			Method setCurrentGradTheme = kClass.getMethod("setCurrentGradientTheme",new Class[] {Class.forName("com.incors.plaf.kunststoff.GradientTheme")});
			setCurrentGradTheme.invoke(lnf,new Object[]{gClass.newInstance()});
			//lnf.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
			//lnf.setCurrentGradientTheme(new com.incors.plaf.kunststoff.KunststoffGradientTheme());
			UIManager.setLookAndFeel(lnf);
		} catch (Exception ignored) {
            // If we can't load Kunststoff, proceed without it
        }
	}

	/*private JSObject getWindow() throws JSException
	{
		if (win == null) win = JSObject.getWindow(this);
		return win;
	}*/

	public synchronized static String htmlSpecialChars(String htmlString)
	{
		int length = htmlString.length();
		StringBuffer out = new StringBuffer(length);
		char[] tempChars = new char[length];
		htmlString.getChars(0,length,tempChars,0);

		int last = 0;
		for (int counter = 0; counter < length; counter++)
		{
			switch(tempChars[counter])
		{
				// Character level entities.
				case '<':
					if (counter > last)
					{
						out.append(tempChars,last,counter-last);
					}
					last = counter + 1;
					out.append("&lt;");
					break;
				case '>':
					if (counter > last)
					{
						out.append(tempChars,last,counter-last);
					}
					last = counter + 1;
					out.append("&gt;");
					break;
				case '&':
					if (counter > last)
					{
						out.append(tempChars,last,counter-last);
					}
					last = counter + 1;
					out.append("&amp;");
					break;
				case '"':
					if (counter > last)
					{
						out.append(tempChars,last,counter-last);
					}
					last = counter + 1;
					out.append("&quot;");
					break;
				// Special characters
				/*case '\n':
				case '\t':
				case '\r':
					break;*/
				default:
					if (tempChars[counter] < ' ' || tempChars[counter] > 127)
					{
						if (counter > last)
						{
							out.append(tempChars,last,counter-last);
						}
						last = counter + 1;
						// If the character is outside of ascii, write the
						// numeric value.
						out.append("&#");
						out.append(String.valueOf((int)tempChars[counter]));
						out.append(";");
					}
					break;
			}
		}

		return out.toString();
	}

	protected void createSubmitButton()
	{
		submitButton = new EditizeSubmitter(this);
		if (background.isOpaque()) submitButton.setBackground(getBackground());

		JPanel submitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		if (background.isOpaque()) submitPanel.setBackground(getBackground());
		submitPanel.add(submitButton);

		background.add(submitPanel,BorderLayout.SOUTH);
	}

	private String javaScriptSpecialChars(String html)
	{
		// Escape JavaScript-sensitive characters in article text
		StringBuffer articleText = new StringBuffer(html);
		int i = 0;
		char lastChar = ' ', newChar;
		if (articleText.length() > 0) do
		{
			switch (newChar = articleText.charAt(i))
			{
				// Escape backslashes and apostrophes
				case '\'':
				case '\\':
				articleText.insert(i++,'\\');
				break;

				// Replace "\r\n" or "\n" with newline code
				case '\n':
					if (lastChar == '\r')
					{
						//System.out.println("Escaping \\r\\n.");
						articleText.deleteCharAt(--i);
					}
					//else System.out.println("Escaping solo \\n.");
					articleText.replace(i,++i,"\\n");
				break;

				default:
				// Replace "\r" (alone) with newline code
				if (lastChar == '\r')
				{
					//System.out.println("Escaping solo \\r.");
					articleText.replace(i-1,i++,"\\n");
				}
			}
			lastChar = newChar;
		} while (++i < articleText.length());

		return articleText.toString();
	}
}
