package xmt2;

import thinlet.*;
import astex.*;
import java.awt.*;
import java.util.*;
import astex.splitter.*;
import astex.thinlet.*;

public class test extends Thinlet {

    static Hashtable userinterfaces = new Hashtable();

    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){

        String xml = args.getString("-xml", null);

        Container parent = mv.getParent();
        parent.removeAll();

        System.out.println("parent " + parent);

        if(!(parent.getLayout() instanceof SplitterLayout)){
            System.out.println("not splitter " + parent);

            parent.setLayout(new SplitterLayout(SplitterLayout.HORIZONTAL));

            parent.add("3", mv);

            mv.setSize((int)(mv.getSize().width * 0.75), mv.getSize().height);
            SplitterBar sb = new SplitterBar();
            
            parent.add(sb);
            
            ThinletUI tui = (ThinletUI)userinterfaces.get(mv);

            if(tui == null){
                tui = new ThinletUI(mv, xml);
            }
                
            parent.add("1", tui);

            userinterfaces.put(mv, tui);
        }else{
            parent.setLayout(new BorderLayout());
            
            parent.add(mv, BorderLayout.CENTER);
        }

        parent.layout();

    }

    public boolean destroy(){
        return false;
    }

    public void setContent(String xml){
        try{
            add(parse(xml));
        }catch(Exception e){
            System.out.println("Exception: " + e);
        }
    }

    public test(){
    }
}