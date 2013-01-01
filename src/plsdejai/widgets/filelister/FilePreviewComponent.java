package plsdejai.widgets.filelister;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.border.BevelBorder;

/**
 * This class creates the component that shows the preview image
 * of the selected file on a FileLister dialog
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 17/01/2012
 */
public class FilePreviewComponent extends JComponent
        implements PropertyChangeListener
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   private static int PREVIEW_WIDTH = 180;
   private static int PREVIEW_HEIGHT = 160;

   ImageIcon thumbnail;
   File file;

   @SuppressWarnings("LeakingThisInConstructor")
   public FilePreviewComponent(FileLister fc)
   {     
      setBorder(new BevelBorder(BevelBorder.LOWERED));
      fc.addPropertyChangeListener(this);
   }

   public void loadImage(File f)
   {
      if (f == null) {
         thumbnail = null;
         return;
      }
      //Don't use createImageIcon (which is a wrapper for getResource)
      //because the image we're trying to load is probably not one
      //of this program's own resources.
      BufferedImage bi = null;
      try {
         bi = javax.imageio.ImageIO.read(f);
      } catch (Exception e) {
         thumbnail = null;
         return;
      }

      if (bi != null) {
         ImageIcon tmpIcon = new ImageIcon(bi);

         thumbnail = tmpIcon;

         if (tmpIcon != null){
            // Miniaturiaze if the image width or height is greater than the preview window
            double scaleX = (float) tmpIcon.getIconWidth() / PREVIEW_WIDTH;
            double scaleY = (float) tmpIcon.getIconHeight() / PREVIEW_HEIGHT;

            if ( scaleX > 1 || scaleY > 1){

               if (scaleX >= scaleY)
                  thumbnail = new ImageIcon(tmpIcon.getImage().
                          getScaledInstance(PREVIEW_WIDTH, -1, Image.SCALE_DEFAULT));
               else
                  thumbnail = new ImageIcon(tmpIcon.getImage().
                          getScaledInstance(-1, PREVIEW_HEIGHT, Image.SCALE_DEFAULT));
            }

         }
      }

   }

   public Dimension getPreferredSize()
   {
      return new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT);
   }

   public Dimension getMinimumSize() { return getPreferredSize(); }

   public Dimension getMaximumSize() { return getPreferredSize(); }

   public void propertyChange(PropertyChangeEvent e)
   {
      if (! isShowing())
         return;
      
      boolean update = false;
      String prop = e.getPropertyName();

      //If the directory changed, don't show an image.
      if (JFileChooser.DIRECTORY_CHANGED_PROPERTY == prop) {
         file = null;
         update = true;

         //If a file became selected, find out which one.
      } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY == prop) {         
         file = (File) e.getNewValue();
         update = true;
      }

      //Update the preview accordingly.
      if (update) {
         thumbnail = null;
         if (isShowing()) {
            loadImage(file);
            repaint();
         }
      }
   }

   protected void paintComponent(Graphics g) {
       super.paintComponent(g);

      if (thumbnail == null)
         loadImage(file);

      if (thumbnail != null) {
         int x = getWidth() / 2 - thumbnail.getIconWidth() / 2;
         int y = getHeight() / 2 - thumbnail.getIconHeight() / 2;

         if (y < 0) y = 0;
         if (x < 5) x = 5;

         thumbnail.paintIcon(this, g, x, y);
      }

   }
}
