package plsdejai.widgets;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * A utility that creates many of the GUI elements in the Main class
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 07/11/2011
 */
public class JMenuFactory {

   public static JMenu createJMenu(String name, Container container
           /* boolean isEnabled, ActionListener actionListener */)
   {
      JMenu menu = null;
      if (name != null)
         menu = new JMenu(name);
      else
         menu = new JMenu();
      
      setCommonAttr(menu);

      container.add(menu);
      
      return menu;
   }

   protected static void setCommonAttr(JMenuItem menu)
   {
      menu.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
      menu.setForeground(Color.blue);
      menu.setBorder(BorderFactory.createMatteBorder(1, 2, 1, 2, Color.white));

   }
}
