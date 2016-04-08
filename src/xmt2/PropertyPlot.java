package xmt2;

import astex.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class PropertyPlot extends Plot {

    public static final String defaultFrequency = "xmt2/Rama500noGPc.properties";

    Image ramaImage = null;

    String command = "append %s;";

    public PropertyPlot(){
        super();
        xbackwards = false;
        horizontalLabel = "Residue";
        verticalLabel = "B";
        borderFraction = 0.0;
    }

    public void initializePlotExtents(){
        plotxmin = 0.0;
        plotymin = 0.0;
        plotxmax = 0.0;
        plotymax = 0.0;

        int moleculeCount = moleculeRenderer.getMoleculeCount();

        for(int m = 0; m < moleculeCount; m++){
            Molecule mol = moleculeRenderer.getMolecule(m);
            if(mol.getDisplayed()){
                int chainCount = mol.getChainCount();
                for(int c = 0; c < chainCount; c++){
                    Chain chain = mol.getChain(c);
                    int resCount = chain.getResidueCount();

                    plotxmax += resCount;

                    for(int r = 0; r < resCount; r++){
                        Residue res = chain.getResidue(r);

                        double ball = getResidueProperty(res, "ball");
                        if(ball > plotymax){
                            plotymax = ball;
                        }
                    }
                }
            }
        }

        double dy = plotymax - plotymin;

        plotymax += 0.02 * dy;
        plotymin -= 0.02 * dy;
    }

    public double getResidueProperty(Residue res, String property){
        int atomCount = res.getAtomCount();

        double total = 0.0;

        for(int a = 0; a < atomCount; a++){
            Atom atom = res.getAtom(a);

            if(property == "ball"){
                total += atom.getBFactor();
            }else if(property == "bmain"){
                String name = atom.getAtomLabel();
                if(name.equals("CA") || name.equals("O") ||
                   name.equals("C") || name.equals("N")){
                    total += atom.getBFactor();
                }
            }
        }

        if(atomCount != 0){
            return total/(double)atomCount;
        }else{
            return 0.0;
        }
    }

    public void paintData(Graphics g){
        int moleculeCount = moleculeRenderer.getMoleculeCount();

        int xpos = 0;

        for(int m = 0; m < moleculeCount; m++){
            Molecule mol = moleculeRenderer.getMolecule(m);

            if(mol.getDisplayed()){
                int chainCount = mol.getChainCount();

                for(int c = 0; c < chainCount; c++){
                    Chain chain = mol.getChain(c);
                    int resCount = chain.getResidueCount();
                    for(int i = 0; i < 2; i++){
                        int prevallx = -1;
                        int prevally = -1;
                        Color prevColor = null;
                        
                        for(int r = 0; r < resCount; r++){
                            Residue res = chain.getResidue(r);
                            
                            double b = getResidueProperty(res, (i == 0) ? "ball" : "bmain");

                            Atom colorAtom = res.getAtom("CA");
                            
                            if(colorAtom == null){
                                colorAtom = res.getAtom(0);
                            }
                            
                            int allx = x2sx(xpos + r);
                            int ally = y2sy(b);

                            Color awtColor = null;
                            
                            if(prevallx != -1 && prevally != -1){
                                int color = colorAtom.getColor();
                                
                                awtColor = new Color(color);

                                if(i == 0){
                                    awtColor = awtColor.darker();
                                }
                                
                                if(prevColor == null || prevColor == awtColor){
                                    g.setColor(awtColor);
                                
                                    g.drawLine(prevallx, prevally, allx, ally);
                                }else{
                                    g.setColor(prevColor);
                                    int midx = (prevallx + allx)/2;
                                    int midy = (prevally + ally)/2;
                                    g.drawLine(prevallx, prevally, midx, midy);

                                    g.setColor(awtColor);
                                    g.drawLine(midx, midy, allx, ally);
                                }

                                if(res.hasSelectedAtoms()){
                                    g.setColor(Color.yellow.darker());
                                    g.drawOval(allx - 2, ally - 2, 4, 4);
                                }
                            }
                            
                            prevallx = allx;
                            prevally = ally;
                            prevColor = awtColor;
                        }

                    }
                    xpos += resCount;
                }
            }
        }
    }

    public void mouseClicked(MouseEvent me){
        Residue nearestRes = null;
        int neard2 = 10000000;
        int mx = me.getX();
        int my = me.getY();

        int xpos = 0;

        int moleculeCount = moleculeRenderer.getMoleculeCount();

        for(int m = 0; m < moleculeCount; m++){
            Molecule mol = moleculeRenderer.getMolecule(m);
            int chainCount = mol.getChainCount();
            for(int c = 0; c < chainCount; c++){
                Chain chain = mol.getChain(c);
                int resCount = chain.getResidueCount();
                for(int r = 0; r < resCount; r++){
                    Residue res = chain.getResidue(r);
  
                    double ball = getResidueProperty(res, "ball");
                    double bmain = getResidueProperty(res, "bmain");

                    int x = x2sx(xpos++);
                    int bally = y2sy(ball);
                    int bmainy = y2sy(bmain);

                    int dx = x - mx;
                    int dy = bally - my;

                    int d2 = dx*dx + dy*dy;

                    if(d2 < neard2){
                        neard2 = d2;
                        nearestRes = res;
                    }
                    
                    dy = bmainy - my;

                    d2 = dx*dx + dy*dy;

                    if(d2 < neard2){
                        neard2 = d2;
                        nearestRes = res;
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