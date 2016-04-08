package xmt2;

import thinlet.*;
import astex.*;
import astex.splitter.*;
import astex.thinlet.*;
import java.awt.*;
import java.util.*;

public class plots extends ThinletUI {
    public static plots xrayPlots = null;

    static Hashtable userinterfaces = new Hashtable();

    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        String xml = args.getString("-xml", null);

        DialogLauncher dialogLauncher = (DialogLauncher)userinterfaces.get(mv + xml);

        if(xrayPlots == null){
            xrayPlots = new plots(mv, xml);
            dialogLauncher = xrayPlots.displayThinlet(mv, args);
            if(dialogLauncher != null){
                userinterfaces.put(mv+xml, dialogLauncher);
            }
        }else{
            dialogLauncher.setVisible(true);
        }

    }

    public void init(Object plotComponent){
        Plot plot = (Plot)getComponent(plotComponent, "bean");

        plot.setMoleculeRenderer(moleculeRenderer);
        plot.setMoleculeViewer(moleculeViewer);

        installProperties(plotComponent, plotComponent);
    }

    public void installProperties(Object source, Object plotComponent){
        Plot plot = (Plot)getComponent(plotComponent, "bean");

        if(getClass(source) == "combobox"){
            source = getSelectedItem(source);
        }

        // :bind is private to thinlet really
        Hashtable properties = getProperties(source);

        if(properties != null){

            Enumeration e = properties.keys();

            while(e.hasMoreElements()){
                Object key = e.nextElement();
                Object value = properties.get(key);
                
                if(key.equals("image")){
                    value = getIcon((String)value, true);
                }

                plot.set(key, value);
            }
        }

        repaint();
    }

    public plots(MoleculeViewer mv, String xml){
        super(mv, xml);
    }

    public plots(){
    }

}