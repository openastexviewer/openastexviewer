/* Thinlet GUI toolkit - www.thinlet.com
 * Copyright (C) 2002-2003 Robert Bajzat (robert.bajzat@thinlet.com) */
package thinlet;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

/**
 * <code>FrameLauncher</code> is a double buffered frame
 * to launch any <i>thinlet</i> component as an application
 */
public class ComponentLauncher extends Container {
	
    private transient Thinlet content;
    private transient Image doublebuffer;
	
	/**
	 * Construct and show a new frame with the specified title, including the
	 * given <i>thinlet</i> component. The frame is centered on the screen, and its
	 * preferred size is specified (excluding the frame's borders). The icon is
	 * the thinlet logo
	 * 
	 * @param title the title to be displayed in the frame's border
	 * @param content a <i>thinlet</i> instance
	 * @param width the preferred width of the content
	 * @param height the preferred height of the content
	 */
    public ComponentLauncher(Thinlet content){
        super();
        this.content = content;
        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        layout();
    }
		
    /**
     * Call the paint method to redraw this component without painting a
     * background rectangle
     */
    public void update(Graphics g) {
        paint(g);
    }
    
    /**
     * Create a double buffer if needed,
     * the <i>thinlet</i> component paints the content
     */
    public void paint(Graphics g) { 
        if (doublebuffer == null) {
            Dimension d = getSize();
            System.out.println("paint and allocate " + d);
            doublebuffer = content.createImage(d.width, d.height);
        }
        Graphics dg = doublebuffer.getGraphics();
        dg.setClip(g.getClipBounds());

        System.out.println("getClipBounds "+ g.getClipBounds());

        System.out.println("content " + content);
        content.paint(dg);
        dg.dispose();
        g.drawImage(doublebuffer, 0, 0, this);
    }
    
    /**
     * Clear the double buffer image (because the frame has been resized),
     * the overriden method lays out its components
     * (centers the <i>thinlet</i> component)
     */
    public void doLayout() {
        if (doublebuffer != null) {
            doublebuffer.flush();
            doublebuffer = null;
        }
        super.doLayout();
    }
}