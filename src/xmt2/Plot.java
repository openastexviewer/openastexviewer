package xmt2;

import thinlet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import astex.*;

public class Plot extends CustomComponent implements MouseListener, MouseMotionListener, KeyListener {

    int width      = 0;
    int height     = 0;
    double plotWidth  = 0.0;
    double plotHeight = 0.0;
    double borderFraction   = 0.03;
    int left   = 50;
    int right  = 20;
    int top    = 20;
    int bottom = 40;

    boolean xbackwards = true;
    boolean axes   = true;
    boolean zoomed = false;

    double plotxmin = 0.0;
    double plotxmax = 0.0;
    double plotymin = 0.0;
    double plotymax = 0.0;

    Font font        = null;
    FontMetrics fm   = null;
    int ascent       = 0;

    int horizontalDP = 2;
    int verticalDP = 2;

    String horizontalLabel = "phi";
    String verticalLabel = "psi";

    MoleculeRenderer moleculeRenderer = null;

    public void setMoleculeRenderer(MoleculeRenderer mr){
        moleculeRenderer = mr;
    }

    MoleculeViewer moleculeViewer = null;

    public void setMoleculeViewer(MoleculeViewer mv){
        moleculeViewer = mv;

        moleculeViewer.addRepaintListener(this);
    }

    Hashtable properties = new Hashtable();

    public void set(Object key, Object value){
        properties.put(key, value);
    }

    public Object get(Object key, Object def){
        Object value = properties.get(key);

        return value != null ? value : def;
    }

    /** Override to make sure boudns are ok. */
    public void checkBounds(){
    }

    public void paint(Graphics g){
        Dimension size = getSize();
        width            = size.width;
        height           = size.height;
        plotWidth        = width - left - right;
        plotHeight       = height - top - bottom;

        checkBounds();

        if(font == null){
            font   = getFont();
            fm     = g.getFontMetrics(font);
            ascent = fm.getMaxAscent();
        }

        g.setColor(Color.white);

        g.fillRect(0, 0, size.width, size.height);

        if(!zoomed){
            initializePlotExtents();
        }

        //g.setClip(left, top, width - right - left, height - bottom - top);

        Rectangle r = g.getClipRect();

        g.clipRect(left, top, width - right - left, height - bottom - top);

        g.setColor(Color.gray);

        paintData(g);

        g.setClip(0, 0, width, height);

        g.setColor(Color.black);

        // plot the axes

        paintAxes(g);

        // border of the plot area
        g.drawLine(left, top,
                   width - right, top);
        g.drawLine(left, height - bottom,
                   width - right, height - bottom);
        g.drawLine(left, top,
                   left,   height - bottom);
        g.drawLine(width - right, top,
                   width - right, height - bottom);

        g.setClip(r);
    }

    /** Override to paint specific data values. */
    public void paintData(Graphics g){
    }

    /** Override to set plot dimensions. */
    public void initializePlotExtents(){
    }

    /** Override these for custom axis labelling. */
    public void paintAxes(Graphics g){
        horizontalDP = plotAxis(g, fm, ascent, 15, plotxmin, plotxmax, false, null, 0) + 1;
        
        verticalDP = plotAxis(g, fm, ascent, 7, plotymin, plotymax, true, null, 0) + 1;
    }

    FloatArray ticks = new FloatArray();

    public int plotAxis(Graphics g, FontMetrics fm, int ascent,
                        int tickCount,
                        double min, double max, boolean vertical, FloatArray ticks, int frac){
        if(ticks == null){
            ticks = new FloatArray();
            frac = 0;
        
            frac = looseLabel(min, max, tickCount, ticks);
        
            //if(frac == 0) frac = 1;
        }

        // ticks
        for(int i = 0; i < ticks.size(); i++){
            double v = ticks.get(i);
            if(vertical == false){
                int axp = x2sx(v);

                if(axp >= left && axp <= width - right){
                    g.drawLine(axp, height - bottom, axp, height - bottom + 4);
                }
            }else{
                int axy = y2sy(v);

                if(axy <= height - bottom && axy >= top){
                    g.drawLine(left, axy, left - 4, axy);
                }
            }
        }

        // labels
        for(int i = 0; i < ticks.size(); i++){
            double v = ticks.get(i);

            String s = null;
            if(frac == 0){
                s = FILE.sprint("%d", (int)v);
            }else{
                s = FILE.sprint("%." + frac + "f", v);
            }
            int w = fm.stringWidth(s);

            if(vertical == false){
                int axp = x2sx(v);
                if(axp >= left && axp <= width - right){
                    g.drawString(s, axp - w/2, height - bottom + 8 + ascent);
                }
            }else{
                int axy = y2sy(v);
                if(axy <= height - bottom && axy >= top){
                    g.drawString(s, left - 4 - 4 - w, axy + ascent/2);
                }
            }
        }

        if(vertical){
            int w = fm.stringWidth(verticalLabel);
            g.drawString(verticalLabel, left/2 - w/2 - 10, ascent/2 + top + (height - bottom - top)/2);
        }else{
            int w = fm.stringWidth(horizontalLabel);
            g.drawString(horizontalLabel, left + (width - left - right)/2 -w/2, height - bottom/2 + ascent + 5);
        }

        return frac;
    }

    public int x2sx(double x){
        int xs = 0;
        
        if(xbackwards){
            xs = (int)(right + 
                       (plotWidth) * (x - plotxmin)/(plotxmax - plotxmin));
        }else{
            xs = (int)(left + 
                       (plotWidth) * (x - plotxmin)/(plotxmax - plotxmin));
        }

        if(xbackwards){
            return width - xs;
        }else{
            return xs;
        }
    }

    public int y2sy(double y){
        int ys = (int)(bottom +
                       (plotHeight) * (y - plotymin)/(plotymax - plotymin));

        return height - ys;
    }


    public double xs2x(int xs){
        if(xbackwards){
            xs = width - (xs + right);
        }else{
            xs = xs - left;
        }
        double x = plotxmin + (plotxmax - plotxmin)*((xs)/plotWidth);

        return x;
    }

    public double ys2y(int ys){
        double y = plotymin + (plotymax - plotymin)*(((height - ys) - bottom)/plotHeight);

        return y;
    }

    /** 
     * This calculates the labelling format 
     * and the nicely rounded extents of the graph. 
     */
    private int looseLabel(double min, double max, int nTicks, FloatArray ticks){

        double d;             // tick mark spacing.
        double gmin, gmax;    // graph range min and max.
        double range, x;
        int nfrac;

        range = niceNumber(max - min, false);
        d     = niceNumber(range/(nTicks - 1), true);
        gmin  = Math.floor(min/d) * d;
        gmax  = Math.ceil(max/d) * d;
        
        /* Calculate number of fractional digits to show. */
        nfrac = (int)Math.max(-Math.floor(log10(d)), 0);
        
        for(x = gmin; x <= (gmax + (0.5 * d)); x += d){
            ticks.add((float)x); 
        }

        return nfrac;
    }
    
    /** 
     * This finds a nice number approximately
     * equal to x. Round, if round is true, ceiling 
     * if round is false. This is from Graphic Gems.
     */
    private double niceNumber(double x, boolean round){
        
        int exp;        // exponent of x.
        double f;       // fractional part of x.
        double nf;      // nice, rounded fraction.

        //System.out.println("x " + x);

        exp = (int)Math.floor(log10(x));
        //System.out.println("exp " + exp);
        f   = x/Math.pow(10.0, exp);
        //System.out.println("f " + f);
        
        if(round){
            if(f < 1.5) nf = 1.0;
            else if(f < 3.0) nf = 2.0;
            else if(f < 7.0) nf = 5.0;
            else nf = 10.0;
        } else {
            if(f <= 1.0) nf = 1.0;
            else if(f <= 2.0) nf = 2.0;
            else if(f <= 5.0) nf = 5.0;
            else nf = 10.0;
        }
        
        //System.out.println("nf " + nf);       
        
        return (nf * Math.pow(10.0, exp));
    }
    
    /** Log10 function. */
    private double log10(double x){
        return (Math.log(x)/Math.log(10)); 
    }

    MouseEvent down = null;
    MouseEvent lastDrag = null;

    MouseEvent lastPosition = null;

    String lastString = null;

    public Plot(){
        addMouseListener(this);
        addMouseMotionListener(this);
        setFont(new Font("Arial", Font.PLAIN, 10));
    }

    public void mouseClicked(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mousePressed(MouseEvent me) {
        down = me;
    }

    public boolean onPlot(MouseEvent me){
        if(me.getX() < left || me.getX() > width - right ||
           me.getY() < top || me.getY() > height - bottom){
            return false;
        }

        return true;
    }

    public void zoomOnSpot(MouseEvent me){
        double px = xs2x(me.getX());
        double py = ys2y(me.getY());

        double dx = plotxmax - plotxmin;
        double dy = plotymax - plotymin;

        plotxmax = px + dx * 0.5 * 0.7;
        plotymax = py + dy * 0.5 * 0.9;

        plotxmin = px - dx * 0.5 * 0.7;
        plotymin = py - dy * 0.5 * 0.9;

    }

    public void mouseReleased(MouseEvent me) {
        int mx = me.getX();
        int my = me.getY();

        if(down != null &&
           Math.abs(mx - down.getX()) < 3 &&
           Math.abs(my - down.getY()) < 3){
            // just picked on a spot
            
            if(onPlot(me)){
                mouseClicked(me);
            }else{
                zoomed = false;
            }
        }else{
            
            double xmax = xs2x(down.getX());
            double xmin = xs2x(mx);
            double ymax = ys2y(down.getY());
            double ymin = ys2y(my);
            
            if(xmin > xmax){
                plotxmin = xmax;
                plotxmax = xmin;
            }else{
                plotxmax = xmax;
                plotxmin = xmin;
            }
            
            if(ymin > ymax){
                plotymin = ymax;
                plotymax = ymin;
            }else{
                plotymax = ymax;
                plotymin = ymin;
            }
            zoomed = true;
        }
        

        lastString = null;
        lastDrag   = null;
        
        repaint();
    }

    public boolean labelling = false;

    public void mouseDragged(MouseEvent me) {
        Graphics g = getGraphics();

        g.setXORMode(Color.white);

        g.setColor(Color.gray);

        if(down != null && lastDrag != null){
            g.drawRect(down.getX(), down.getY(),
                       lastDrag.getX() - down.getX(),
                       lastDrag.getY() - down.getY());
        }

        g.drawRect(down.getX(), down.getY(),
                   me.getX() - down.getX(),
                   me.getY() - down.getY());

        lastDrag = me;

        g.setXORMode(Color.black);

        mouseMoved(me);
    }

    public void mouseMoved(MouseEvent me) {
        if(onPlot(me)){
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }else{
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        //paintPosition(me);
    }

    public void drawString(Graphics g, int x, int y, String s){
        g.drawString(s, x - fm.stringWidth(s)/2, y - 12);
    }

    public void paintPosition(MouseEvent me){
        int mex = me.getX();
        int mey = me.getY();

        
        Graphics g = getGraphics();

        g.setXORMode(Color.white);

        g.setColor(Color.gray);

        if(lastPosition != null && lastString != null){
            //System.out.println("lastString " + lastString);

            drawString(g, lastPosition.getX(), lastPosition.getY(), lastString);
        }

        if(onPlot(me)){
            lastString  = FILE.sprint("%." + horizontalDP + "f, ", xs2x(mex));
            lastString += FILE.sprint("%." + verticalDP + "f", ys2y(mey));

            //System.out.println("newString " + lastString);

            drawString(g, me.getX(), me.getY(), lastString);

            lastPosition = me;
        }else{
            lastPosition = null;
            lastString = null;
        }

        g.setXORMode(Color.black);
    }

    /* Implementation of KeyListener */

    public void keyPressed(java.awt.event.KeyEvent ke){
    }

    public void keyReleased(java.awt.event.KeyEvent ke){
    }

    public void keyTyped(java.awt.event.KeyEvent ke){
    }
}