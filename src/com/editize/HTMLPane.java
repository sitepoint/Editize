package com.editize;

import java.io.*;

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import com.editize.editorkit.*;

/**
 * A JTextPane that supports copy/paste in its EditorKit's native format, as
 * well as plain text.
 *
 * @author Kevin Yank
 */
public class HTMLPane extends JTextPane
{
	private static final boolean DEBUG = false;

	private static final String STARTDELIMITER = "StartFragment";
	private static final String ENDDELIMITER = "EndFragment";

	private DataFlavor nativeFlavor, nativeFlavor2;
	private boolean htmlClipboardEnabled = true;

	/**
	 * Private Clipboard for use by objects of this class when they cannot
	 * access the system clipboard.
	 */
	private static Clipboard privClip;
	private boolean canIAccessSystemClipboard = true;

        /**
         * Automatically keeps the caret visible on different background colors.
         *
         * @todo Make this handle neutral grays better.
         *
         * @param c
         */
        public void setBackground(Color c) {
          super.setBackground(c);

          // Invert the background color
          Color caretColor = new Color(255-c.getRed(),255-c.getGreen(),255-c.getBlue());
          setCaretColor(caretColor);
          setSelectionColor(caretColor);
		  // Copy the color so that Swing doesn't see it as a "default" value
          setSelectedTextColor(new Color(c.getRed(),c.getGreen(),c.getBlue()));
        }

	public void setText(String t) {
		try {
			Document doc = getDocument();
			EditorKit kit = getEditorKit();
			EditizeEditorKit ekit = null;
			if (kit instanceof EditizeEditorKit) ekit = (EditizeEditorKit)kit;
			if (ekit != null) ekit.startCompoundEdit();
			doc.remove(1, Math.max(0,doc.getLength()-1));
			Reader r = new StringReader(t);
			kit.read(r, doc, 1);
			if (ekit != null) ekit.endCompoundEdit();
		} catch (IOException ioe) {
			getToolkit().beep();
		} catch (BadLocationException ble) {
			getToolkit().beep();
		}
	}

	public void cut()
	{
		if (isEditable() && isEnabled())
		{
			try
			{
				Clipboard clipboard = getClipboard();
				Caret caret = getCaret();
				int p0 = Math.min(caret.getDot(), caret.getMark());
				int p1 = Math.max(caret.getDot(), caret.getMark());
				if (p0 != p1)
				{
					Document doc = getDocument();
					TextTransferable contents = new TextTransferable(p0,p1-p0);
					clipboard.setContents(contents, contents);
					doc.remove(p0, p1 - p0);
				}
			}
			catch (BadLocationException e)
			{
			}
		}
		else
		{
			getToolkit().beep();
		}
	}

	public void copy()
	{
		try
		{
			Clipboard clipboard = getClipboard();
			Caret caret = getCaret();
			int p0 = Math.min(caret.getDot(), caret.getMark());
			int p1 = Math.max(caret.getDot(), caret.getMark());
			if (p0 != p1)
			{
				Document doc = getDocument();
				TextTransferable contents = new TextTransferable(p0,p1-p0);
				clipboard.setContents(contents, contents);
			}
		}
		catch (BadLocationException e)
		{
		}
	}

	public void paste()
	{
		// Obtain objects for the two native data flavors
		try
		{
			if (nativeFlavor == null) nativeFlavor = new DataFlavor(getEditorKit().getContentType()+"; class=java.lang.String");
			if (nativeFlavor2 == null) nativeFlavor2 = new DataFlavor(getEditorKit().getContentType()+"; class=java.io.InputStream; charset=utf-8");
		}
		catch (ClassNotFoundException e)
		{
		}

		if (isEditable() && isEnabled())
		{
			Clipboard clipboard = getClipboard();
			Transferable content = clipboard.getContents(this);
			if (content != null)
			{
				// Debug output reports supported data flavours
				if (DEBUG) {
					DataFlavor[] flavs = content.getTransferDataFlavors();
					System.out.println("Paste operation. Available data flavours:");
					for (int i = 0; i < flavs.length; i++) {
						System.out.println(flavs[i].toString());
					}
				}

				try
				{
					if (content.isDataFlavorSupported(nativeFlavor)) {
						Object nativeData = content.getTransferData(nativeFlavor);
						String htmlDoc = null;

						if (nativeData instanceof String) {
							if (DEBUG) {
								System.out.println(
										"Clipboard data was a Windows HTML String:\n" +
										(String)nativeData);
							}
							/* Win 32 */
							htmlDoc = (String)nativeData;
						}
						else if (nativeData instanceof ByteArrayInputStream) {
							/* Mac OS X */
							BufferedReader br = new BufferedReader(nativeFlavor.
									getReaderForText(content));
							StringBuffer htmlDocBuffer = new StringBuffer();
							String line;

							// Read HTML document into StringBuffer
							while ((line = br.readLine()) != null) {
								htmlDocBuffer.append(line + "\n");
							}

							if (DEBUG) {
								System.out.println(
										"Clipboard data was a Mac OS X HTML String:\n" +
										htmlDocBuffer.toString());
							}

							htmlDoc = htmlDocBuffer.toString();
						}
                        try {
                            readDelimitedHtml(htmlDoc);
                        }
                        catch (IOException ex) {
                            // Fallback is plain text paste
                            String dstData = (String)(content.getTransferData(DataFlavor.stringFlavor));
                            replaceSelection(dstData);
                        }
                    }
					else if (content.isDataFlavorSupported(nativeFlavor2) &&
									 htmlClipboardEnabled) {
						// This is the flavor produced by Microsoft Office and Internet Explorer.
						BufferedReader br = new BufferedReader(nativeFlavor2.
								getReaderForText(content));
						StringBuffer htmlDocBuffer = new StringBuffer();
						String line;

						// Read HTML document into StringBuffer
						while ((line = br.readLine()) != null) {
							htmlDocBuffer.append(line + "\n");
						}

						if (DEBUG) {
							System.out.println(
									"Clipboard data was a Windows MSOffice/MSIE HTML String:\n" +
									htmlDocBuffer.toString());
						}

						String htmlDoc = htmlDocBuffer.toString();

                        try {
                            readDelimitedHtml(htmlDoc);
                        }
                        catch (IOException ex) {
                            // Fallback is plain text paste
                            String dstData = (String)(content.getTransferData(DataFlavor.stringFlavor));
                            replaceSelection(dstData);
                        }
					}
					else {
						String dstData = (String)(content.getTransferData(DataFlavor.
								stringFlavor));
						if (DEBUG) {
							System.out.println(
									"Clipboard data was plain text." + dstData);
						}
						replaceSelection(dstData);
					}
				}
				catch (UnsupportedFlavorException e)
				{
					System.err.println("Can't paste; clipboard contains non-textual data.");
					getToolkit().beep();
				}
				catch (Exception e)
				{
					System.err.println("Error during paste operation (" + e.getMessage() + "). Trace data follows:");
					e.printStackTrace(System.err);
					getToolkit().beep();
				}
			}
			else
			{
				System.err.println("Error: Paste with null clipboard contents.");
				getToolkit().beep();
			}
		}
	}

	public void readDelimitedHtml(String htmlDoc) throws IOException, BadLocationException
	{
		EditizeDocument theDoc = (EditizeDocument)getDocument();

		int srcStart = indexOfStartDelimiterEnd(htmlDoc);
		if (srcStart < 0)
			throw new IOException("Fragment start delimiter not found.");
		int srcEnd = indexOfEndDelimiterStart(htmlDoc, srcStart);
		if (srcEnd < 0)
			throw new IOException("Fragment end delimiter not found.");
		htmlDoc = htmlDoc.substring(srcStart, srcEnd);

		EditizeEditorKit kit = (EditizeEditorKit)getEditorKit();

		//undoMan.start();

		replaceSelection("");

		if (isInlineHtml(htmlDoc))
		{
			//System.out.println("Pasting as run: " + htmlDoc);
			kit.insertInlineHtml(theDoc,htmlDoc,getCaretPosition());
		}
		else
		{
			//System.out.println("Pasting as block: " + htmlDoc);
			kit.insertBlockHtml(theDoc,htmlDoc,getCaretPosition());
		}

		//undoMan.end();
	}

        private static int indexOfStartDelimiterEnd(String document)
        {
          if (document == null || document.length() == 0) return -1;

          // Search for start of comment
          int commentStart;
          int commentEnd = 0;
          boolean foundIt = false;
          while ((commentStart = document.indexOf("<!--",commentEnd)) >= 0)
          {
            foundIt = true;

            // Search for start of comment content
            int contentStart = commentStart + "<!--".length();
            int contentChar = document.charAt(contentStart);
            while (contentChar == ' ' || contentChar == '\t' || contentChar == '\n' ||
                   contentChar == '\r')
            {
              if (contentStart == document.length())
              {
                foundIt = false;
                break;
              }
              contentChar = document.charAt(++contentStart);
            }
            commentEnd = contentStart;

            // Check the comment content
            if (foundIt && document.substring(contentStart).startsWith(STARTDELIMITER))
            {
              commentEnd += STARTDELIMITER.length();
            }
            else foundIt = false;

            // Search for any spurious content and find comment end
            while (commentEnd < document.length() && !document.substring(commentEnd).startsWith("-->"))
            {
              contentChar = document.charAt(commentEnd++);
              if (contentChar != ' ' && contentChar != '\t' && contentChar != '\n' &&
                contentChar == '\r') foundIt = false;
            }
            commentEnd += "-->".length();
            commentEnd = Math.min(commentEnd,document.length());

            if (foundIt) break;
          }

          // No delimiter? Use the body tag
          if (!foundIt)
          {
            commentEnd = 0;
            while ((commentStart = document.indexOf("<body", commentEnd)) >= 0 || (commentStart = document.indexOf("<BODY", commentEnd)) >= 0)
            {
                foundIt = true;

                // Search for end of body tag
                int contentStart = commentStart + "<body".length();
                int contentChar = document.charAt(contentStart);
                while (contentChar != '>')
                {
                  if (contentStart == document.length())
                  {
                    foundIt = false;
                    break;
                  }
                  contentChar = document.charAt(++contentStart);
                }
                commentEnd = contentStart;
                
                if (foundIt) break;
            }
          }

          return foundIt ? commentEnd : -1;
        }

        private static int indexOfEndDelimiterStart(String document, int fromIndex)
        {
          if (document == null || document.length() <= fromIndex) return -1;

          // Search for start of comment
          int commentStart = fromIndex;
          int commentEnd = fromIndex;
          boolean foundIt = false;
          while ((commentStart = document.indexOf("<!--",commentEnd)) >= 0)
          {
            foundIt = true;

            // Search for start of comment content
            int contentStart = commentStart + "<!--".length();
            int contentChar = document.charAt(contentStart);
            while (contentChar == ' ' || contentChar == '\t' || contentChar == '\n' ||
                   contentChar == '\r')
            {
              if (contentStart == document.length())
              {
                foundIt = false;
                break;
              }
              contentChar = document.charAt(++contentStart);
            }
            commentEnd = contentStart;

            // Check the comment content
            if (foundIt && document.substring(contentStart).startsWith(ENDDELIMITER))
            {
              commentEnd += ENDDELIMITER.length();
            }
            else foundIt = false;

            // Search for any spurious content and find comment end
            while (commentEnd < document.length() && !document.substring(commentEnd).startsWith("-->"))
            {
              contentChar = document.charAt(commentEnd++);
              if (contentChar != ' ' && contentChar != '\t' && contentChar != '\n' &&
                contentChar == '\r') foundIt = false;
            }
            commentEnd += "-->".length();
            commentEnd = Math.min(commentEnd,document.length());

            if (foundIt) break;
          }

          // No delimiter? Use the body tag
          if (!foundIt)
          {
            commentEnd = fromIndex;
            if ((commentStart = document.indexOf("</body", commentEnd)) >= 0 || (commentStart = document.indexOf("</BODY", commentEnd)) >= 0)
            {
                foundIt = true;
            }
          }

          return foundIt ? commentStart : -1;
        }

	/**
	 * Reads an HTML string and attempts to determine if it is suitable
	 * for inline insertion in an HTML document.
	 *
	 * @param html
	 * @return
	 */
	private boolean isInlineHtml(String html)
	{
		boolean foundTagStart = false;
		boolean foundTagNameStart = false;
		int len = html.length();
		char[] chars = new char[len];
		html.getChars(0,len,chars,0);

		StringBuffer firstTagName = new StringBuffer();

		// Get the name of the first HTML tag in the string
		for (int pos=0; pos < len; pos++)
		{
			if (foundTagStart)
			{
				char theChar = chars[pos];
				if (!foundTagNameStart)
				{
					if (theChar >= 'A' && theChar <= 'Z' ||
						theChar >= 'a' && theChar <= 'z')
					{
						firstTagName.append(theChar);
	        				foundTagNameStart = true;
		        		}
				}
				else if (theChar >= 'A' && theChar <= 'Z' ||
					theChar >= 'a' && theChar <= 'z')
				{
					firstTagName.append(theChar);
				}
				else break;
			}
			else
			{
				if (chars[pos] == '<') foundTagStart = true;
			}
		}

		HTML.Tag firstTag = HTML.getTag(firstTagName.toString().toLowerCase());
		//System.out.println("First tag is: " + firstTagName.toString());
		return !foundTagNameStart || firstTag == null || !firstTag.isBlock();
	}

	/**
	 * Returns the clipboard to use for cut/copy/paste. Because of the design of pre-Java 1.4
	 * clipboard support in JTextComponent (where several functions that would be needed here are
	 * private), when the system clipboard cannot be accessed, components of this class must use
	 * their own private clipboard.
	 */
	private Clipboard getClipboard() {
		if (canIAccessSystemClipboard()) {
			return getToolkit().getSystemClipboard();
		}
		if (privClip == null) privClip = new Clipboard("Editize private clipboard.");
		return privClip;
	}

	/**
	 * Returns true if it is safe to access the system Clipboard.
	 */
	private boolean canIAccessSystemClipboard() {
		if (canIAccessSystemClipboard) {
			SecurityManager sm = System.getSecurityManager();

			if (sm != null) {
				try {
					sm.checkSystemClipboardAccess();
					return true;
				} catch (SecurityException se) {
					System.err.println("Access to system clipboard denied: " + se.getMessage());
					canIAccessSystemClipboard = false;
					return false;
				}
			}
			return true;
		}
		return false;
	}

	class TextTransferable extends StringSelection
	{
		private String nativeData;
		private String nativeType;
		private DataFlavor nativeFlavor, nativeFlavor2;
		private DataFlavor[] flavs;

		TextTransferable(int pos, int len) throws BadLocationException
		{
		        super(getText(pos,len));

			// Get the data in its native format
			EditizeEditorKit kit = (EditizeEditorKit)getEditorKit();
			StringWriter sw = new StringWriter(len); // String will be at least len characters long
			try
			{
				kit.write(sw,getDocument(),pos,len,true);
				nativeData = sw.toString();
//System.out.println("HTML in clipboard: " + nativeData);
				nativeType = kit.getContentType();
			}
			catch (IOException e)
			{
			}

			DataFlavor[] superflavors = super.getTransferDataFlavors();
			flavs = new DataFlavor[superflavors.length+1];
			int i;
			for (i=0; i<superflavors.length; i++)
			{
				flavs[i+1] = superflavors[i];
			}
			try
			{
				flavs[0] = nativeFlavor = new DataFlavor(nativeType+"; class=java.lang.String");
				flavs[1] = nativeFlavor2 = new DataFlavor(nativeType+"; class=java.io.InputStream");
			}
			catch (ClassNotFoundException e)
			{
			}
		}

		/**
		 * Returns an array of flavors in which this <code>Transferable</code>
		 * can provide the data. <code>DataFlavor.stringFlavor</code>
		 * is properly supported.
		 * Support for <code>DataFlavor.plainTextFlavor</code> is
		 * <b>deprecated</b>.
		 *
		 * @return an array of length three, whose elements are <code>DataFlavor.
		 *         stringFlavor</code>, <code>DataFlavor.plainTextFlavor</code>,
		 *         and the DataFlavor supported by this ArticlePane.
		 */
     		public DataFlavor[] getTransferDataFlavors() {
			return (DataFlavor[])flavs.clone();
		}

		/**
		 * Returns whether the requested flavor is supported by this
		 * <code>Transferable</code>.
		 *
		 * @param flavor the requested flavor for the data
		 * @return true if <code>flavor</code> is equal to
		 *   <code>DataFlavor.stringFlavor</code>,
		 *   <code>DataFlavor.plainTextFlavor</code>, or the native Data Flavour
		 *   supported by this <code>ArticlePane</code>; false if <code>flavor</code>
		 *   is not one of the above flavors
		 * @throws NullPointerException if flavor is <code>null</code>
		 */
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			for (int i = 0; i < flavs.length; i++) {
				if (flavor.equals(flavs[i])) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Returns the <code>Transferable</code>'s data in the requested
		 * <code>DataFlavor</code> if possible. If the desired flavor is
		 * <code>DataFlavor.stringFlavor</code>, or an equivalent flavor,
		 * the <code>String</code> representing the selection is
		 * returned. If the desired flavor is </code>DataFlavor.plainTextFlavor
		 * </code>, or an equivalent flavor, a <code>Reader</code> is returned.
		 * <b>Note:<b> The behavior of this method for
		 * </code>DataFlavor.plainTextFlavor</code>
		 * and equivalent <code>DataFlavor</code>s is inconsistent with the
		 * definition of <code>DataFlavor.plainTextFlavor</code>.
		 *
		 * @param flavor the requested flavor for the data
		 * @return the data in the requested flavor, as outlined above
		 * @throws UnsupportedFlavorException if the requested data flavor is
		 *         not supported.
		 * @throws IOException if an IOException occurs while retrieving the data.
		 *         By default, StringSelection never throws this exception, but a
		 *         subclass may.
		 * @throws NullPointerException if flavor is <code>null</code>
		 */
		public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException
		{
			if (flavor.equals(nativeFlavor))
			{
				if (nativeData == null) throw new IOException();
				return nativeData;
			}
			else if (flavor.equals(nativeFlavor2))
			{
				if (nativeData == null) throw new IOException();
				return new ByteArrayInputStream(nativeData.getBytes());
			}
			else return super.getTransferData(flavor);
		}
	}

	/**
	 * Prevents custom KeyMap getting overwritten on Look & Feel change.
	 * This is a bit of a hack; in theory RapidEdit should restore custom
	 * key mappings in its updateUI method, or something like that. For now,
	 * this works.
	 */
	public void updateUI() {
		Keymap map = getKeymap();
		super.updateUI();
		if (map != null) setKeymap(map);
	}

	public boolean isHtmlClipboardEnabled()
	{
		return htmlClipboardEnabled;
	}

	public void setHtmlClipboardEnabled(boolean htmlClipboardEnabled)
	{
		this.htmlClipboardEnabled = htmlClipboardEnabled;
	}

	/**
	 * Reads in a a document from a Reader. Supports asynchronous reading.
	 *
	 * @param in The Reader from which to load the document.
	 * @param desc A descriptive Object for the stream. May be left null.
	 * @param async If true, document will be read in a separate Thread.
	 */
	/*public void read(Reader in, Object desc, boolean async)
	{
		if (async)
		{
			Thread loader = new DocLoader(in, desc);
			loader.start();
		}
		// Read syncronously
		else
		{
			try
			{
				read(in,desc);
			}
			catch (IOException ioe)
			{
				// Java 1.4
				// UIManager.getLookAndFeel().provideErrorFeedback(this);

				java.awt.Toolkit.getDefaultToolkit().beep();
			}
		}
	}*/

	/**
	 * Thread to load a document from a Reader. Calls parent object's
	 * read(Reader,Object) method.
	 */
	/*class DocLoader extends Thread
	{
		Reader in;
		Object desc;

		DocLoader(Reader in, Object desc)
		{
			super("Document loader.");
			this.in = in;
			this.desc = desc;
		}

		public void run()
		{
			try
			{
				read(in,desc);
			}
			catch (IOException ioe)
			{
				// Java 1.4 only
				// UIManager.getLookAndFeel().provideErrorFeedback(ArticlePane.this);

				java.awt.Toolkit.getDefaultToolkit().beep();
			}
		}
	}*/
}
