package com.editize.editorkit;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.ImageObserver;
import java.io.*;
import java.net.*;
import java.util.Dictionary;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.text.html.*;


/**
 * View of an Image, intended to support the HTML &lt;IMG&gt; tag.
 * Supports scaling via the HEIGHT and WIDTH attributes of the tag.
 * If the image is unable to be loaded any text specified via the
 * <code>ALT</code> attribute will be rendered.
 * <p>
 * While this class has been part of swing for a while now, it is public
 * as of 1.4.
 *
 * @author  Scott Violet
 * @version 1.50 12/03/01
 * @see IconView
 * @since 1.4
 */
public class ImageView extends View {
	/**
	 * Inline layout type constant.
	 */
	public static final int INLINE = 0;
	/**
	 * Left-aligned layout type constant.
	 */
	public static final int FLOAT_LEFT = 1;
	/**
	 * Right-aligned layout type constant.
	 */
	public static final int FLOAT_RIGHT = 2;

    /**
     * If true, when some of the bits are available a repaint is done.
     * <p>
     * This is set to false as swing does not offer a repaint that takes a
     * delay. If this were true, a bunch of immediate repaints would get
     * generated that end up significantly delaying the loading of the image
     * (or anything else going on for that matter).
     */
    private static boolean sIsInc = false;
    /**
     * Repaint delay when some of the bits are available.
     */
    private static int sIncRate = 100;
    /**
     * Icon used while the image is being loaded.
     */
    private static Icon sPendingImageIcon;
    /**
     * Icon used if the image could not be found.
     */
    private static Icon sMissingImageIcon;
    /**
     * Icon used if the image is floating.
     */
    private static Icon sFloatingImageIcon;
    /**
     * File name for <code>sPendingImageIcon</code>.
     */
    private static final String PENDING_IMAGE_SRC = "icons/image-delayed.gif";
    /**
     * File name for <code>sMissingImageIcon</code>.
     */
    private static final String MISSING_IMAGE_SRC = "icons/image-failed.gif";
    /**
     * File name for <code>sFloatingImageIcon</code>.
     */
    private static final String FLOATING_IMAGE_SRC = "icons/image-floating.gif";

    /**
     * Document property for image cache.
     */
    private static final String IMAGE_CACHE_PROPERTY = "imageCache";

    // Height/width to use before we know the real size, these should at least
    // the size of <code>sMissingImageIcon</code> and
    // <code>sPendingImageIcon</code>
    private static final int DEFAULT_WIDTH = 38;
    private static final int DEFAULT_HEIGHT= 38;

    /**
     * Default border to use if one is not specified.
     */
    private static final int DEFAULT_BORDER = 2;

    // Bitmask values
    private static final int LOADING_FLAG = 1;
    private static final int LINK_FLAG = 2;
    private static final int WIDTH_FLAG = 4;
    private static final int HEIGHT_FLAG = 8;
    private static final int RELOAD_FLAG = 16;
    private static final int RELOAD_IMAGE_FLAG = 32;
    private static final int SYNC_LOAD_FLAG = 64;

    private AttributeSet attr;
    private Image image;
    private int width;
    private int height;
    /** Bitmask containing some of the above bitmask values. Because the
     * image loading notification can happen on another thread access to
     * this is synchronized (at least for modifying it). */
    private int state;
    private Container container;
    private Rectangle fBounds;
    private Color borderColor;
    // Size of the border, the insets contains this valid. For example, if
    // the HSPACE attribute was 4 and BORDER 2, leftInset would be 6.
    private short borderSize;
    // Insets, obtained from the painter.
    private short leftInset;
    private short rightInset;
    private short topInset;
    private short bottomInset;
    /**
     * We don't directly implement ImageObserver, instead we use an instance
     * that calls back to us.
     */
    private ImageObserver imageObserver;
    /**
     * Used for alt text. Will be non-null if the image couldn't be found,
     * and there is valid alt text.
     */
    private View altView;
    /** Alignment along the vertical (Y) axis. */
    private float vAlign;
    private int hAlign; // One of the static layout constants (E.g. INLINE)

    /**
     * Creates a new view that represents an IMG element.
     *
     * @param elem the element to create a view for
     */
    public ImageView(Element elem) {
    	super(elem);
	fBounds = new Rectangle();
        imageObserver = new ImageHandler();
        state = RELOAD_FLAG | RELOAD_IMAGE_FLAG;
    }

    /**
     * Returns the text to display if the image can't be loaded. This is
     * obtained from the Elements attribute set with the attribute name
     * <code>HTML.Attribute.ALT</code>.
     */
    public String getAltText() {
        return (String)getElement().getAttributes().getAttribute
            (HTML.Attribute.ALT);
    }

    /**
     * Return a URL for the image source,
     * or null if it could not be determined.
     */
    public URL getImageURL() {
 	String src = (String)getElement().getAttributes().
                             getAttribute(HTML.Attribute.SRC);
 	if (src == null) {
            return null;
        }

	URL reference = ((HTMLDocument)getDocument()).getBase();
        try {
 	    URL u = new URL(reference,src);
	    return u;
        } catch (MalformedURLException e) {
	    return null;
        }
    }

    /**
     * Returns the icon to use if the image couldn't be found.
     */
    public Icon getNoImageIcon() {
        loadDefaultIconsIfNecessary();
        return sMissingImageIcon;
    }

    /**
     * Returns the icon to use while in the process of loading the image.
     */
    public Icon getLoadingImageIcon() {
        loadDefaultIconsIfNecessary();
        return sPendingImageIcon;
    }

    /**
     * Returns the icon to use while in the process of loading the image.
     */
    public Icon getFloatingImageIcon()
    {
	loadDefaultIconsIfNecessary();
	return sFloatingImageIcon;
    }

    /**
     * Returns the image to render.
     */
    public Image getImage() {
        sync();
        return image;
    }

    /**
     * Sets how the image is loaded. If <code>newValue</code> is true,
     * the image we be loaded when first asked for, otherwise it will
     * be loaded asynchronously. The default is to not load synchronously,
     * that is to load the image asynchronously.
     */
    public void setLoadsSynchronously(boolean newValue) {
        synchronized(this) {
            if (newValue) {
                state |= SYNC_LOAD_FLAG;
            }
            else {
                state = (state | SYNC_LOAD_FLAG) ^ SYNC_LOAD_FLAG;
            }
        }
    }

    /**
     * Returns true if the image should be loaded when first asked for.
     */
    public boolean getLoadsSynchronously() {
        return ((state & SYNC_LOAD_FLAG) != 0);
    }

    /**
     * Convenience method to get the StyleSheet.
     */
    protected StyleSheet getStyleSheet() {
	HTMLDocument doc = (HTMLDocument) getDocument();
	return doc.getStyleSheet();
    }

    /**
     * Fetches the attributes to use when rendering.  This is
     * implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    public AttributeSet getAttributes() {
        sync();
	return attr;
    }

    /**
     * For images the tooltip text comes from text specified with the
     * <code>ALT</code> attribute. This is overriden to return
     * <code>getAltText</code>.
     *
     * @see JTextComponent#getToolTipText
     */
    public String getToolTipText(float x, float y, Shape allocation) {
        return getAltText();
    }

	/**
	* Update any cached values that come from attributes.
	*/
	protected void setPropertiesFromAttributes() {
		StyleSheet sheet = getStyleSheet();
		this.attr = sheet.getViewAttributes(this);

		// Gutters
		borderSize = (short)getIntAttr(HTML.Attribute.BORDER, isLink() ?
			DEFAULT_BORDER : 0);

		if (borderSize == 0 && image == null)
		{
			borderSize = 1;
		}

		AttributeSet attr = getElement().getAttributes();

		// Alignment.
		// PENDING: This needs to be changed to support the CSS versions
		// when conversion from ALIGN to VERTICAL_ALIGN is complete.
		Object alignment = attr.getAttribute(HTML.Attribute.ALIGN);
                Object htmlClass = attr.getAttribute(HTML.Attribute.CLASS);

		vAlign = 1.0f;
		hAlign = INLINE;
		if (alignment != null)
		{
			alignment = alignment.toString();
			if ("top".equals(alignment))
			{
				vAlign = 0f;
			}
			else if ("middle".equals(alignment))
			{
				vAlign = .5f;
			}
			else if ("left".equals(alignment))
			{
				hAlign = FLOAT_LEFT;
			}
			else if ("right".equals(alignment))
			{
				hAlign = FLOAT_RIGHT;
			}
		}
                if (htmlClass != null)
                {
                  htmlClass = htmlClass.toString();
                  if ("imgleft".equals(htmlClass))
                  {
                    hAlign = FLOAT_LEFT;
                    vAlign = 1.0f;
                  }
                  else if ("imgright".equals(htmlClass))
                  {
                    hAlign = FLOAT_RIGHT;
                    vAlign = 1.0f;
                  }
                }

		leftInset = rightInset = (short)(getIntAttr(HTML.Attribute.HSPACE,
	        	isFloater()?3:0) + borderSize);
		topInset = bottomInset = (short)(getIntAttr(HTML.Attribute.VSPACE,
		        isFloater()?3:0) + borderSize);

		borderColor = ((StyledDocument)getDocument()).getForeground
			(getAttributes());

		AttributeSet anchorAttr = (AttributeSet)attr.getAttribute(HTML.Tag.A);
		if (anchorAttr != null && anchorAttr.isDefined
		        (HTML.Attribute.HREF))
		{
			synchronized(this)
			{
				state |= LINK_FLAG;
			}
		}
		else
		{
			synchronized(this)
			{
				state = (state | LINK_FLAG) ^ LINK_FLAG;
			}
		}
	}

	/**
	 * Returns the layout type for the image.
	 *
	 * @return FLOAT_LEFT, FLOAT_RIGHT, or INLINE
	 */
	public int getLayoutType()
	{
		return hAlign;
	}

	public boolean isFloater()
	{
		int layoutType = getLayoutType();
		return (layoutType != INLINE);
	}

    /**
     * Establishes the parent view for this view.
     * Seize this moment to cache the AWT Container I'm in.
     */
    public void setParent(View parent) {
        View oldParent = getParent();
	super.setParent(parent);
	container = (parent != null) ? getContainer() : null;
        if (oldParent != parent) {
            synchronized(this) {
                state |= RELOAD_FLAG;
            }
        }
    }

    /**
     * Invoked when the Elements attributes have changed. Recreates the image.
     */
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
    	super.changedUpdate(e,a,f);

        synchronized(this) {
            state |= RELOAD_FLAG | RELOAD_IMAGE_FLAG;
        }

        // Assume the worst.
        preferenceChanged(null, true, true);
    }

    /**
     * Paints the view
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     */
    public void paint(Graphics g, Shape a)
    {
	paint(g,a,true);
    }

    /**
     * Paints the View. If image is a floater and usePlaceholder is
     * true, the placeholder icon will be painted instead of the actual
     * image.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     * @param usePlaceholder whether or not to paint the placeholder icon for floating images
     * @see View#paint
     */
    public void paint(Graphics g, Shape a, boolean usePlaceholder) {
        sync();

	Rectangle rect = (a instanceof Rectangle) ? (Rectangle)a :
                         a.getBounds();

	if (isFloater() && usePlaceholder)
	{
		sFloatingImageIcon.paintIcon(getContainer(),g,rect.x+rect.width-sFloatingImageIcon.getIconWidth(),rect.y);
		return;
	}

        Image image = getImage();
        Rectangle clip = g.getClipBounds();

	fBounds.setBounds(rect);
        paintHighlights(g, a);
        paintBorder(g, rect);
        if (clip != null) {
            g.clipRect(rect.x + leftInset, rect.y + topInset,
                       rect.width - leftInset - rightInset,
                       rect.height - topInset - bottomInset);
        }
        if (image != null) {
            if (!hasPixels(image)) {
                // No pixels yet, use the default
                Icon icon = (image == null) ? getNoImageIcon() :
                                               getLoadingImageIcon();

                if (icon != null) {
                    icon.paintIcon(getContainer(), g, rect.x + leftInset,
                                   rect.y + topInset);
                }
            }
            else {
                // Draw the image
                g.drawImage(image, rect.x + leftInset, rect.y + topInset,
                            width, height, imageObserver);
            }
        }
        else {
            Icon icon = getNoImageIcon();

            if (icon != null) {
                icon.paintIcon(getContainer(), g, rect.x + leftInset,
                               rect.y + topInset);
            }
            View view = getAltView();
            // Paint the view representing the alt text, if its non-null
            if (view != null && ((state & WIDTH_FLAG) == 0 ||
                                 width > DEFAULT_WIDTH)) {
                // Assume layout along the y direction
                Rectangle altRect = new Rectangle
                    (rect.x + leftInset + DEFAULT_WIDTH, rect.y + topInset,
                     rect.width - leftInset - rightInset - DEFAULT_WIDTH,
                     rect.height - topInset - bottomInset);

                view.paint(g, altRect);
            }
        }
        if (clip != null) {
            // Reset clip.
            g.setClip(clip.x, clip.y, clip.width, clip.height);
        }
    }

    private void paintHighlights(Graphics g, Shape shape) {
	if (container instanceof JTextComponent) {
	    JTextComponent tc = (JTextComponent)container;
	    Highlighter h = tc.getHighlighter();
	    if (h instanceof LayeredHighlighter) {
		((LayeredHighlighter)h).paintLayeredHighlights
		    (g, getStartOffset(), getEndOffset(), shape, tc, this);
	    }
	}
    }

    private void paintBorder(Graphics g, Rectangle rect) {
        Color color = borderColor;

        if (borderSize > 0 && color != null) {
            int xOffset = leftInset - borderSize;
            int yOffset = topInset - borderSize;
            g.setColor(color);
	    for (int counter = 0; counter < borderSize; counter++) {
	        g.drawRect(rect.x + xOffset + counter,
                           rect.y + yOffset + counter,
                           rect.width - counter - counter - xOffset -xOffset-1,
                           rect.height - counter - counter -yOffset-yOffset-1);
            }
        }
    }

	/**
	 * Determines the preferred span for this view along an
	 * axis. If image is a floater (left/right aligned), this will
	 * return the dimension of the placeholder icon.
	 *
	 * @param axis may be either X_AXIS or Y_AXIS
	 * @return   the span the view would like to be rendered into;
	 *           typically the view is told to render into the span
	 *           that is returned, although there is no guarantee;
	 *           the parent may choose to resize or break the view
	 */
	public float getPreferredSpan(int axis)
	{
		return getPreferredSpan(axis,true);
	}

	/**
	 * Gets preferred size along a given axis for the image. If image
	 * is a floater (left/right aligned) and usePlaceholder is true,
	 * this will return the dimension of the placeholder icon.
	 *
	 * @param axis may be either X_AXIS or Y_AXIS
	 * @param usePlaceholder If true and image is a floater, return the
	 *                       dimension of the placeholder image. If false,
	 *                       return the actual image dimension.
	 * @return   the span the view would like to be rendered into;
	 *           typically the view is told to render into the span
	 *           that is returned, although there is no guarantee;
	 *           the parent may choose to resize or break the view.
	 */
	public float getPreferredSpan(int axis, boolean usePlaceholder)
	{
		sync();

		if (isFloater() && usePlaceholder)
		{
			loadDefaultIconsIfNecessary();

			View parentView = getParent();
			if (axis == View.X_AXIS)
			{
				View v;
				int i=0;
				// Find out if this view is among any floating images that lead off the paragraph
				if (parentView != null /* <- HACK: Find out why this happens sometimes */
					&& getLayoutType()==FLOAT_LEFT)
				{
					do v = parentView.getView(i++);
					while (v != this && v instanceof ImageView && ((ImageView)v).isFloater());
	        			if (v == this) return sFloatingImageIcon.getIconWidth() + getPreferredSpan(axis,false);
					else return sFloatingImageIcon.getIconWidth();
				}
				else return sFloatingImageIcon.getIconWidth();
			}
			if (axis == View.Y_AXIS) return sFloatingImageIcon.getIconHeight();
		}

		// If the attributes specified a width/height, always use it!
		if (axis == View.X_AXIS && (state & WIDTH_FLAG) == WIDTH_FLAG)
		{
			getPreferredSpanFromAltView(axis);
			return width + leftInset + rightInset;
		}
		if (axis == View.Y_AXIS && (state & HEIGHT_FLAG) == HEIGHT_FLAG)
		{
			getPreferredSpanFromAltView(axis);
			return height + topInset + bottomInset;
		}

		Image image = getImage();

		if (image != null)
		{
			switch (axis)
			{
				case View.X_AXIS:
		        		return width + leftInset + rightInset;
				case View.Y_AXIS:
	        			return height + topInset + bottomInset;
				default:
					throw new IllegalArgumentException("Invalid axis: " + axis);
			}
		}
		else
		{
			View view = getAltView();
			float retValue = 0f;

			if (view != null)
			{
				retValue = view.getPreferredSpan(axis);
			}
			switch (axis)
			{
				case View.X_AXIS:
					return retValue + (float)(width + leftInset + rightInset);
				case View.Y_AXIS:
	        			return retValue + (float)(height + topInset + bottomInset);
				default:
		        		throw new IllegalArgumentException("Invalid axis: " + axis);
			}
		}
	}

	/**
	 * Fetches the maximum span for the View along a given axis. Images floating in
	 * the right margin occupy as much horizontal space as there is available, so
	 * as to always remain flush with the right margin.
	 */
	/*public float getMaximumSpan(int axis)
	{
		if (axis == View.X_AXIS && isFloater())
		{
			AttributeSet attr = getElement().getAttributes();
			String alignment = (String)attr.getAttribute(HTML.Attribute.ALIGN);
			if (alignment.equals("right")) return Float.MAX_VALUE;
		}
		return getMinimumSpan(axis);
	}*/

    /**
     * Determines the desired alignment for this view along an
     * axis.  This is implemented to give the alignment to the
     * bottom of the icon along the y axis, and the default
     * along the x axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the desired alignment; this should be a value
     *   between 0.0 and 1.0 where 0 indicates alignment at the
     *   origin and 1.0 indicates alignment to the full span
     *   away from the origin; an alignment of 0.5 would be the
     *   center of the view
     */
    public float getAlignment(int axis) {
	switch (axis) {
	case View.Y_AXIS:
	    return vAlign;
	default:
	    return super.getAlignment(axis);
	}
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert
     * @param a the allocated region to render into
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does not represent a
     *   valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
	int p0 = getStartOffset();
	int p1 = getEndOffset();
	if ((pos >= p0) && (pos <= p1)) {
	    Rectangle r = a.getBounds();
	    if (pos == p1) {
		r.x += r.width;
	    }
	    r.width = 0;
	    return r;
	}
	return null;
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *  given point of view
     * @see View#viewToModel
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
	Rectangle alloc = (Rectangle) a;
	if (x < alloc.x + alloc.width) {
	    bias[0] = Position.Bias.Forward;
	    return getStartOffset();
	}
	bias[0] = Position.Bias.Backward;
	return getEndOffset();
    }

    /**
     * Sets the size of the view.  This should cause
     * layout of the view if it has any layout duties.
     *
     * @param width the width >= 0
     * @param height the height >= 0
     */
    public void setSize(float width, float height) {
        sync();

        if (getImage() == null) {
            View view = getAltView();

            if (view != null) {
		view.setSize(Math.max(0f, width - (float)(DEFAULT_WIDTH + leftInset + rightInset)),
			     Math.max(0f, height - (float)(topInset + bottomInset)));
            }
        }
    }

    /**
     * Returns true if this image within a link?
     */
    private boolean isLink() {
	return ((state & LINK_FLAG) == LINK_FLAG);
    }

    /**
     * Returns true if the passed in image has a non-zero width and height.
     */
    private boolean hasPixels(Image image) {
        return image != null &&
            (image.getHeight(imageObserver) > 0) &&
            (image.getWidth(imageObserver) > 0);
    }

    /**
     * Returns the preferred span of the View used to display the alt text,
     * or 0 if the view does not exist.
     */
    private float getPreferredSpanFromAltView(int axis) {
        if (getImage() == null) {
            View view = getAltView();

            if (view != null) {
                return view.getPreferredSpan(axis);
            }
        }
        return 0f;
    }

    private Icon makeIcon(final String gifFile) throws IOException {
        /* Copy resource into a byte array.  This is
         * necessary because several browsers consider
         * Class.getResource a security risk because it
         * can be used to load additional classes.
         * Class.getResourceAsStream just returns raw
         * bytes, which we can convert to an image.
         */
	InputStream resource = EditizeEditorKit.getResourceAsStream(gifFile);

        if (resource == null) {
            System.err.println(ImageView.class.getName() + "/" +
                               gifFile + " not found.");
            return null;
        }
        BufferedInputStream in =
            new BufferedInputStream(resource);
        ByteArrayOutputStream out =
            new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        in.close();
        out.flush();

        buffer = out.toByteArray();
        if (buffer.length == 0) {
            System.err.println("warning: " + gifFile +
                               " is zero-length");
            return null;
        }
        return new ImageIcon(buffer);
    }

    /**
     * Request that this view be repainted.
     * Assumes the view is still at its last-drawn location.
     */
    private void repaint(long delay) {
    	if (container != null && fBounds != null) {
	    container.repaint(delay, fBounds.x, fBounds.y, fBounds.width,
                               fBounds.height);
    	}
    }

    private void loadDefaultIconsIfNecessary() {
        try {
            if (sPendingImageIcon == null)
            	sPendingImageIcon = makeIcon(PENDING_IMAGE_SRC);
            if (sMissingImageIcon == null)
            	sMissingImageIcon = makeIcon(MISSING_IMAGE_SRC);
            if (sFloatingImageIcon == null)
            	sFloatingImageIcon = makeIcon(FLOATING_IMAGE_SRC);
	} catch(Exception x) {
	    System.err.println("ImageView: Couldn't load image icons");
	}
    }

    /**
     * Convenience method for getting an integer attribute from the elements
     * AttributeSet.
     */
    private int getIntAttr(HTML.Attribute name, int deflt) {
    	AttributeSet attr = getElement().getAttributes();
    	if (attr.isDefined(name)) {		// does not check parents!
    	    int i;
 	    String val = (String)attr.getAttribute(name);
 	    if (val == null) {
 	    	i = deflt;
            }
 	    else {
 	    	try{
 	            i = Math.max(0, Integer.parseInt(val));
 	    	}catch( NumberFormatException x ) {
 	    	    i = deflt;
 	    	}
            }
	    return i;
	} else
	    return deflt;
    }

    /**
     * Makes sure the necessary properties and image is loaded.
     */
    private void sync() {
        int s = state;
        if ((s & RELOAD_IMAGE_FLAG) != 0) {
            refreshImage();
        }
        s = state;
        if ((s & RELOAD_FLAG) != 0) {
            synchronized(this) {
                state = (state | RELOAD_FLAG) ^ RELOAD_FLAG;
            }
            setPropertiesFromAttributes();
        }
    }

    /**
     * Loads the image and updates the size accordingly. This should be
     * invoked instead of invoking <code>loadImage</code> or
     * <code>updateImageSize</code> directly.
     */
    private void refreshImage() {
	synchronized(this) {
            // clear out width/height/realoadimage flag and set loading flag
            state = (state | LOADING_FLAG | RELOAD_IMAGE_FLAG | WIDTH_FLAG |
                     HEIGHT_FLAG) ^ (WIDTH_FLAG | HEIGHT_FLAG |
                                     RELOAD_IMAGE_FLAG);
            image = null;
            width = height = 0;
        }

        try {
            // Load the image
            loadImage();

            // And update the size params
            updateImageSize();
        }
        finally {
            synchronized(this) {
                // Clear out state in case someone threw an exception.
                state = (state | LOADING_FLAG) ^ LOADING_FLAG;
            }
        }
    }

    /**
     * Loads the image from the URL <code>getImageURL</code>. This should
     * only be invoked from <code>refreshImage</code>.
     */
    private void loadImage() {
        URL src = getImageURL();
        Image newImage = null;
        if (src != null) {
            Dictionary cache = (Dictionary)getDocument().
                                    getProperty(IMAGE_CACHE_PROPERTY);
            if (cache != null) {
                newImage = (Image)cache.get(src);
            }
            else {
                newImage = Toolkit.getDefaultToolkit().getImage(src);
                if (newImage != null && getLoadsSynchronously()) {
                    // Force the image to be loaded by using an ImageIcon.
                    ImageIcon ii = new ImageIcon();
                    ii.setImage(newImage);
                }
            }
        }
        image = newImage;
    }

    /**
     * Recreates and reloads the image.  This should
     * only be invoked from <code>refreshImage</code>.
     */
    private void updateImageSize() {
	int newWidth = 0;
	int newHeight = 0;
        int newState = 0;
        Image newImage = getImage();

        if (newImage != null) {
            Element elem = getElement();
	    AttributeSet attr = elem.getAttributes();

            // Get the width/height and set the state ivar before calling
            // anything that might cause the image to be loaded, and thus the
            // ImageHandler to be called.
	    newWidth = getIntAttr(HTML.Attribute.WIDTH, -1);
            if (newWidth > 0) {
                newState |= WIDTH_FLAG;
            }
	    newHeight = getIntAttr(HTML.Attribute.HEIGHT, -1);
            if (newHeight > 0) {
                newState |= HEIGHT_FLAG;
            }

            if (newWidth <= 0) {
		newWidth = newImage.getWidth(imageObserver);
                if (newWidth <= 0) {
                    newWidth = DEFAULT_WIDTH;
                }
            }

            if (newHeight <= 0) {
		newHeight = newImage.getHeight(imageObserver);
                if (newHeight <= 0) {
                    newHeight = DEFAULT_HEIGHT;
                }
            }

	    // Make sure the image starts loading:
            if ((newState & (WIDTH_FLAG | HEIGHT_FLAG)) != 0) {
                Toolkit.getDefaultToolkit().prepareImage(newImage, newWidth,
                                                         newHeight,
                                                         imageObserver);
            }
            else {
                Toolkit.getDefaultToolkit().prepareImage(newImage, -1, -1,
                                                         imageObserver);
            }

            boolean createText = false;
	    synchronized(this) {
                // If imageloading failed, other thread may have called
                // ImageLoader which will null out image, hence we check
                // for it.
                if (image != null) {
                    if ((newState & WIDTH_FLAG) == WIDTH_FLAG || width == 0) {
                        width = newWidth;
                    }
                    if ((newState & HEIGHT_FLAG) == HEIGHT_FLAG ||
                        height == 0) {
                        height = newHeight;
                    }
                }
                else {
                    createText = true;
                    if ((newState & WIDTH_FLAG) == WIDTH_FLAG) {
                        width = newWidth;
                    }
                    if ((newState & HEIGHT_FLAG) == HEIGHT_FLAG) {
                        height = newHeight;
                    }
                }
                state = state | newState;
                state = (state | LOADING_FLAG) ^ LOADING_FLAG;
            }
            if (createText) {
                // Only reset if this thread determined image is null
                updateAltTextView();
	    }
	}
        else {
            width = height = DEFAULT_HEIGHT;
            updateBorderForNoImage();
            updateAltTextView();
        }
    }

    /**
     * Updates the view representing the alt text.
     */
    private void updateAltTextView() {
        String text = getAltText();

        if (text != null) {
            ImageLabelView newView;

            newView = new ImageLabelView(getElement(), text);
            synchronized(this) {
                altView = newView;
            }
        }
    }

    /**
     * Returns the view to use for alternate text. This may be null.
     */
    private View getAltView() {
        View view;

        synchronized(this) {
            view = altView;
        }
        if (view != null && view.getParent() == null) {
            view.setParent(getParent());
        }
        return view;
    }

    /**
     * Invokes <code>preferenceChanged</code> on the event dispatching
     * thread.
     */
    private void safePreferenceChanged() {
        if (SwingUtilities.isEventDispatchThread()) {
            preferenceChanged(null, true, true);
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        preferenceChanged(null, true, true);
                    }
                });
        }
    }

    /**
     * Invoked if no image is found, in which case a default border is
     * used if one isn't specified.
     */
    private void updateBorderForNoImage() {
        if (borderSize == 0) {
            borderSize = 1;
            leftInset += borderSize;
            rightInset += borderSize;
            bottomInset += borderSize;
            topInset += borderSize;
        }
    }


    /**
     * ImageHandler implements the ImageObserver to correctly update the
     * display as new parts of the image become available.
     */
    private class ImageHandler implements ImageObserver {
        // This can come on any thread. If we are in the process of reloading
        // the image and determining our state (loading == true) we don't fire
        // preference changed, or repaint, we just reset the fWidth/fHeight as
        // necessary and return. This is ok as we know when loading finishes
        // it will pick up the new height/width, if necessary.
        public boolean imageUpdate(Image img, int flags, int x, int y,
                                   int newWidth, int newHeight ) {
            if (image == null || image != img) {
                return false;
            }

            // Bail out if there was an error:
            if ((flags & (ABORT|ERROR)) != 0) {
                repaint(0);
                synchronized(ImageView.this) {
                    if (image == img) {
                        // Be sure image hasn't changed since we don't
                        // initialy synchronize
                        image = null;
                        if ((state & WIDTH_FLAG) != WIDTH_FLAG) {
                            width = DEFAULT_WIDTH;
                        }
                        if ((state & HEIGHT_FLAG) != HEIGHT_FLAG) {
                            height = DEFAULT_HEIGHT;
                        }
                        // No image, use a default border.
                        updateBorderForNoImage();
                    }
                    if ((state & LOADING_FLAG) == LOADING_FLAG) {
                        // No need to resize or repaint, still in the process
                        // of loading.
                        return false;
                    }
                }
                updateAltTextView();
                safePreferenceChanged();
                return false;
            }

            // Resize image if necessary:
            short changed = 0;
            if ((flags & ImageObserver.HEIGHT) != 0 && !getElement().
                  getAttributes().isDefined(HTML.Attribute.HEIGHT)) {
                changed |= 1;
            }
            if ((flags & ImageObserver.WIDTH) != 0 && !getElement().
                  getAttributes().isDefined(HTML.Attribute.WIDTH)) {
		changed |= 2;
            }

            synchronized(ImageView.this) {
                if (image != img) {
                    return false;
                }
                if ((changed & 1) == 1 && (state & WIDTH_FLAG) == 0) {
                    width = newWidth;
                }
                if ((changed & 2) == 2 && (state & HEIGHT_FLAG) == 0) {
                    height = newHeight;
                }
                if ((state & LOADING_FLAG) == LOADING_FLAG) {
                    // No need to resize or repaint, still in the process of
                    // loading.
                    return true;
                }
            }
            if (changed != 0) {
                // May need to resize myself, asynchronously:
                Document doc = getDocument();
                try {
                    if (doc instanceof AbstractDocument) {
                        ((AbstractDocument)doc).readLock();
                    }
                    safePreferenceChanged();
                } finally {
                    if (doc instanceof AbstractDocument) {
                        ((AbstractDocument)doc).readUnlock();
                    }
                }
                return true;
            }

            // Repaint when done or when new pixels arrive:
            if ((flags & (FRAMEBITS|ALLBITS)) != 0) {
                repaint(0);
            }
            else if ((flags & SOMEBITS) != 0 && sIsInc) {
                repaint(sIncRate);
            }
            return ((flags & ALLBITS) == 0);
        }
    }


    /**
     * ImageLabelView is used if the image can't be loaded, and
     * the attribute specified an alt attribute. It overriden a handle of
     * methods as the text is hardcoded and does not come from the document.
     */
    private class ImageLabelView extends InlineView {
        private Segment segment;
        private Color fg;

        ImageLabelView(Element e, String text) {
            super(e);
            reset(text);
        }

        public void reset(String text) {
            segment = new Segment(text.toCharArray(), 0, text.length());
        }

        public void paint(Graphics g, Shape a) {
            // Don't use supers paint, otherwise selection will be wrong
            // as our start/end offsets are fake.
            GlyphPainter painter = getGlyphPainter();

            if (painter != null) {
                g.setColor(getForeground());
                painter.paint(this, g, a, getStartOffset(), getEndOffset());
            }
        }

        public Segment getText(int p0, int p1) {
            if (p0 < 0 || p1 > segment.array.length) {
                throw new RuntimeException("ImageLabelView: Stale view");
            }
            segment.offset = p0;
            segment.count = p1 - p0;
            return segment;
        }

        public int getStartOffset() {
            return 0;
        }

        public int getEndOffset() {
            return segment.array.length;
        }

        public View breakView(int axis, int p0, float pos, float len) {
            // Don't allow a break
            return this;
        }

        public Color getForeground() {
            View parent;
            if (fg == null && (parent = getParent()) != null) {
                Document doc = getDocument();
                AttributeSet attr = parent.getAttributes();

                if (attr != null && (doc instanceof StyledDocument)) {
                    fg = ((StyledDocument)doc).getForeground(attr);
                }
            }
            return fg;
        }
    }
}