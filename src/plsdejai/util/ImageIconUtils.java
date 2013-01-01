package plsdejai.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.ImageIcon;

/**
 * A utility that specifies useful functions concerning images in GUI elements
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 17/10/2011
 */
public class ImageIconUtils
{
   /**
    * The image extensions supported by the system
    */
   private final static Set<String> supportedExtensions = new TreeSet<String>();
   static{
      String[] suffixes = javax.imageio.ImageIO.getReaderFileSuffixes();
      for (int i = 0, len = suffixes.length; i < len ; ++i) {
         if (suffixes[i].length() == 0 || suffixes[i] == null)
            continue;

         suffixes[i] = suffixes[i].toLowerCase();
         supportedExtensions.add(suffixes[i]);

         // Add known synonyms
         if (suffixes[i].equals("tif"))
            supportedExtensions.add("tiff");
         else if (suffixes[i].equals("jpg"))
            supportedExtensions.add("jpeg");
      }
   }

   /**
    * @return the image extensions supported by the system
    */
   public static String [] getSupportedExtensions()
   {
      String[] s = new String[supportedExtensions.size()];
      int index = 0;
      for(Iterator<String> it = supportedExtensions.iterator(); it.hasNext();++index)
         s[index] = it.next();    

      return s;
   }


   /**
    * @param f
    * @return the extension of the file f
    */
   public static String getExtension(File f)
   {
      String ext = null;
      String name = f.getName();
      if (name != null) {
         int extensionIndex = name.lastIndexOf('.');
         if (extensionIndex > 0 && extensionIndex < name.length() - 1)
            ext = name.substring(extensionIndex + 1).toLowerCase();
      }

      return ext;
   }


   /**
    *
    * @param ext
    * @return true if ext is a supported image extension, otherwise false
    */
   public static boolean isSupportedExtension(String ext)
   {
      String extensions [] = getSupportedExtensions();
      for (int i = 0, len = extensions.length; i < len; ++i )
         if (extensions[i].equalsIgnoreCase(ext))
            return true;

      return false;
   }

   /**
    * @param path the filename containing an image
    * @return  an ImageIcon, or null if the path was invalid.
    */
    public static ImageIcon createImageIcon(String path) {       
        java.net.URL imgURL = plsdejai.Main.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            return null;
        }
    }

    public ImageIcon createCustomImageIcon(String filename, String description)
   {
      String path = /*@customize: "resources/" +*/
              filename;
      return new ImageIcon(/*@customize: */ plsdejai.Main.class.getResource(path),
              description);
   }

    /**
     * A class that is used to create a custom icon
     */
   class CustomImageIcon extends ImageIcon
   {
      // Every Serializable class should define an ID
      public static final long serialVersionUID = 42L;

      public CustomImageIcon(String filename) { super(filename); }

      public synchronized void paintIcon(Component c, Graphics g, int x, int y)
      {
         g.setColor(Color.white);
         g.fillRect(0, 0, c.getWidth(), c.getHeight());
         if (getImageObserver() == null)
            g.drawImage(
                    getImage(),
                    c.getWidth() / 2 - getIconWidth() / 2,
                    c.getHeight() / 2 - getIconHeight() / 2,
                    c);
         else
            g.drawImage(
                    getImage(),
                    c.getWidth() / 2 - getIconWidth() / 2,
                    c.getHeight() / 2 - getIconHeight() / 2,
                    getImageObserver());
      }
   }

}
