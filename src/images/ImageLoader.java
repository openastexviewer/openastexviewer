//////////////////////license & copyright header///////////////////////
//                                                                   //
//                Copyright (c) 1998 by Kevin Kelley                 //
//                                                                   //
// This program is free software; you can redistribute it and/or     //
// modify it under the terms of the GNU General Public License as    //
// published by the Free Software Foundation; either version 2 of    //
// the License, or (at your option) any later version.               //
//                                                                   //
// This program is distributed in the hope that it will be useful,   //
// but WITHOUT ANY WARRANTY; without even the implied warranty of    //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the     //
// GNU General Public License for more details.                      //
//                                                                   //
// You should have received a copy of the GNU General Public License //
// along with this program in the file 'gpl.html'; if not, write to  //
// the Free Software Foundation, Inc., 59 Temple Place - Suite 330,  //
// Boston, MA 02111-1307, USA, or contact the author:  Kevin Kelley  //
// (kelley@iguana.ruralnet.net).                                     //
//                                                                   //
////////////////////end license & copyright header/////////////////////
package images;
import java.util.Hashtable;
import java.io.InputStream;
import java.awt.*;


/** loads images which are in the same directory as this class. */

public class ImageLoader {

private static Hashtable cache = new Hashtable();

private static Component trackerComponent = new Canvas();


public static Image loadImage(String resourceName) {
    Image cached = (Image) cache.get(resourceName);
    if (cached != null) return cached;
    try {
        InputStream resource = ImageLoader.class.getResourceAsStream(resourceName);
        byte[] bytes = new byte[resource.available()];
        resource.read(bytes);
        Image image = Toolkit.getDefaultToolkit().createImage(bytes);
        MediaTracker tracker = new MediaTracker(trackerComponent);
        tracker.addImage(image, 0);
        tracker.waitForID(0);
        cache.put(resourceName, image);
        return image;
    }
    catch(Exception ex) {
        System.out.println("ImageLoader() can't load '" + resourceName
            + "' with class '" + ImageLoader.class.getName() + "'.");
        ex.printStackTrace();
    }
    return null;
}

public static void flushCache() { cache.clear(); cache = new Hashtable(); }
}
