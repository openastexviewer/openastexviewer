package xmt2;

import astex.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class RamachandranPlot extends Plot {

    public RamachandranPlot(){
        super();
        xbackwards = false;
        horizontalLabel = "Phi";
        verticalLabel = "Psi";
        borderFraction = 0.0;
    }

    public void initializePlotExtents(){
        plotxmin = -180.0;
        plotymin = -180.0;
        plotxmax =  180.0;
        plotymax =  180.0;

        // put a small border around the plot
        if(false){
            double dx = plotxmax - plotxmin;
            double dy = plotymax - plotymin;

            plotxmax += dx * borderFraction;
            plotymax += dy * borderFraction;
            plotxmin -= dx * borderFraction;
            plotymin -= dy * borderFraction;
        }
    }

    public void plotResidue(Graphics g, Residue res, int size, boolean shadow){
        int size2 = 2 * size;
        double phipsi[] = new double[2];

        if(getPhiPsi(res, phipsi)){
            int x = x2sx(phipsi[0]);
            int y = y2sy(phipsi[1]);

            Atom ca = res.getAtom("CA");

            if(shadow){
                g.setColor(Color.gray);
            }else{
                int color = ca.getColor();

                Color awtColor = new Color(color);

                g.setColor(awtColor);
            }

            int xoffset = -size;
            int yoffset = -size;

            if(shadow){
                xoffset = -size - 1;
                yoffset = -size + 1;
            }

            if(res.getName().equals("PRO")){
                g.fillRect(x + xoffset, y + yoffset, size2, size2);
            }else{
                g.fillOval(x + xoffset, y + yoffset, size2, size2);
            }

            boolean selected = false;

            int atomCount = res.getAtomCount();
            for(int a = 0; a < atomCount; a++){
                Atom atom = res.getAtom(a);
                if(atom.isSelected()){
                    selected = true;
                    break;
                }
            }

            if(selected && !shadow){
                Color oldColor = g.getColor();

                g.setColor(Color.yellow);
                if(res.getName().equals("PRO")){
                    g.drawRect(x - size - 1, y - size - 1, size2 + 2, size2 + 2);
                }else{
                    g.drawOval(x - size - 1, y - size - 1, size2 + 2, size2 + 2);
                }

                g.setColor(oldColor);
            }
        }
    }
    
    public void paintFrequency(Graphics g){
        Image ramaImage = (Image)get("image", null);
        int iw = ramaImage.getWidth(this);
        int ih = ramaImage.getHeight(this);

        // not entirely precise...
        int ix1 = (int)(iw * (plotxmin - (-180.0)) / 360.0);
        int ix2 = (int)(iw * (plotxmax - (-180.0)) / 360.0);
        int iy1 = (int)(ih * (plotymax - (-180.0)) / 360.0);
        int iy2 = (int)(ih * (plotymin - (-180.0)) / 360.0);

        g.drawImage(ramaImage,
                    left, top,
                    width - right, height - bottom,
                    ix1, iy1, ix2, iy2, this);

    }

    public void paintData(Graphics g){
        paintFrequency(g);

        String symbolSizeString = (String)get("symbolsize", "2");

        int symbolSize = Integer.parseInt(symbolSizeString);

        int moleculeCount = moleculeRenderer.getMoleculeCount();

        for(int i = 0; i < 2; i++){
            boolean shadow = (i == 0);

            for(int m = 0; m < moleculeCount; m++){
                Molecule mol = moleculeRenderer.getMolecule(m);
                if(mol.getDisplayed()){
                    int chainCount = mol.getChainCount();
                    for(int c = 0; c < chainCount; c++){
                        Chain chain = mol.getChain(c);
                        int resCount = chain.getResidueCount();
                        for(int r = 0; r < resCount; r++){
                            Residue res = chain.getResidue(r);
                            
                            plotResidue(g, res, symbolSize, shadow);
                        }
                    }
                }
            }
        }
    }

    /** Check that we have bounds we like. */
    public void checkBounds(){
        if(plotxmax > 180.0) plotxmax = 180.0;
        if(plotymax > 180.0) plotymax = 180.0;
        if(plotxmin < -180.0) plotxmin = -180.0;
        if(plotymin < -180.0) plotymin = -180.0;
    }

    /**
     * Get the phi,psi angles for this residue.
     * Return false if not all atoms are defined
     * or the residue should not be displayed for
     * some reason.
     */
    public boolean getPhiPsi(Residue res, double phipsi[]){
	Atom Ni = res.getAtom("N");
	Atom CAi = res.getAtom("CA");
	Atom Ci = res.getAtom("C");
	Atom Cim1 = null;
	Atom Nip1 = null;

        if(Ni != null){
            Cim1 = Ni.getBondedAtom("C");
        }

        if(Ci != null){
            Nip1 = Ci.getBondedAtom("N");
        }
        
        if(Ni != null && CAi != null && Ci != null &&
           Cim1 != null && Nip1 != null){
            if(CAi.isDisplayed()){
                phipsi[0] = Point3d.torsionDegrees(Cim1, Ni, CAi, Ci);
                phipsi[1] = Point3d.torsionDegrees(Ni, CAi, Ci, Nip1);

                return true;
            }
        }

        return false;
    }

    public void paintAxes(Graphics g){
        FloatArray ticks = new FloatArray();

        for(int i = -180; i <= 180; i += 60){
            ticks.add((float)i);
        }

        horizontalDP = plotAxis(g, fm, ascent, 5, plotxmin, plotxmax, false, ticks, 0) + 1;
        
        verticalDP = plotAxis(g, fm, ascent, 5, plotymin, plotymax, true, ticks, 0) + 1;
    }

    public void mouseClicked(MouseEvent me){
        Residue nearestRes = null;
        int neard2 = 10000000;
        int mx = me.getX();
        int my = me.getY();

        int moleculeCount = moleculeRenderer.getMoleculeCount();

        for(int m = 0; m < moleculeCount; m++){
            Molecule mol = moleculeRenderer.getMolecule(m);
            int chainCount = mol.getChainCount();
            for(int c = 0; c < chainCount; c++){
                Chain chain = mol.getChain(c);
                int resCount = chain.getResidueCount();
                for(int r = 0; r < resCount; r++){
                    Residue res = chain.getResidue(r);
  
                    double phipsi[] = new double[2];

                    if(getPhiPsi(res, phipsi)){
                        int x = x2sx(phipsi[0]);
                        int y = y2sy(phipsi[1]);

                        int dx = x - mx;
                        int dy = y - my;
                        
                        int d2 = dx*dx + dy*dy;

                        if(d2 < neard2){
                            nearestRes = res;
                            neard2 = d2;
                        }
                    }
                }
            }
        }

        if(nearestRes != null && neard2 < 200){
            String selExpr = nearestRes.selectStatement();
            String command = (String)get("command", null);
            String substCommand = Util.replace(command, "%s", selExpr);
            substCommand = Util.replace(substCommand, "|", ";");
            if(substCommand.endsWith(";") == false){
                substCommand += ";";
            }

            moleculeViewer.execute(substCommand);
        }

        moleculeViewer.dirtyRepaint();
    }
}