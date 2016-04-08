/*
 * This file is part of OpenAstexViewer.
 *
 * OpenAstexViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenAstexViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenAstexViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package astex;

import java.util.*;

public class group {
    public group(){
    }

    public static void handleCommand(MoleculeViewer mv,
                                     MoleculeRenderer mr,
                                     Arguments args){
        if(args.defined("-create")){
            String groupName = args.getString("-create", null);
            DynamicArray selection = (DynamicArray)args.get("-selection");

            Hashtable atoms = new Hashtable();

            if(selection != null){
                for(int i = 0; i < selection.size(); i++){
                    Object o = selection.get(i);
                    atoms.put(o, o);
                }
            }

            print.f("created group " + groupName + " with "+ atoms.size() + " atoms");

            mr.groups.put(groupName, atoms);

        }else if(args.defined("-append")){
            String groupName = args.getString("-append", null);
            Hashtable group = (Hashtable)mr.groups.get(groupName);

            if(group == null){
                Log.error("no such group " + groupName);

                return;
            }

            DynamicArray selection = (DynamicArray)args.get("-selection");

            if(selection == null){
                Log.error("you must specify a selection to append");
                return;
            }

            for(int i = 0; i < selection.size(); i++){
                Object o = selection.get(i);
                if(group.containsKey(o) == false){
                    group.put(o, o);
                }
            }

            print.f("group " + groupName + " now has " + group.size() + " atoms");

        }else if(args.defined("-exclude")){
            String groupName = args.getString("-exclude", null);
            Hashtable group = (Hashtable)mr.groups.get(groupName);

            if(group == null){
                Log.error("no such group " + groupName);

                return;
            }

            DynamicArray selection = (DynamicArray)args.get("-selection");

            if(selection == null){
                Log.error("you must specify a selection to append");
                return;
            }

            for(int i = 0; i < selection.size(); i++){
                Object o = selection.get(i);
                if(group.containsKey(o)){
                    group.remove(o);
                }
            }

            print.f("group " + groupName + " now has " + group.size() + " atoms");

        }else if(args.defined("-delete")){
            String groupName = args.getString("-delete", null);

            print.f("removed group " + groupName);

            mr.groups.remove(groupName);
        }else{
            Log.error("no valid action for group command");
        }
    }
}