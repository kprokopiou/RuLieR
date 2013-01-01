package plsdejai.widgets.filelister;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.ImageIcon;

/**
 *
 * @author konsprok
 */
public class CustomImageIcon extends ImageIcon
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   public CustomImageIcon(String filename)
   {
      super(filename);
   }

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
