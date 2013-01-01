package plsdejai.widgets;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

/**
 * A utility class for creating standard Button GUI elements
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 19/11/2011
 */
public class ButtonFactory
{

   public static JMenuItem createJMenuItem(String name, Container container,
           boolean isEnabled, ActionListener actionListener)
   {
      JMenuItem menuItem = null;
      if (name != null)
         menuItem = new JMenuItem(name);
      else
         menuItem = new JMenuItem();

      setCommonMenuItemAttr(menuItem);

      if (container != null)
         container.add(menuItem);
      menuItem.setEnabled(isEnabled);
      if (actionListener != null)
         menuItem.addActionListener(actionListener);

      return menuItem;
   }

   public static JRadioButtonMenuItem createJRadioButtonMenuItem(ButtonGroup group, String name,
           Container container, boolean isEnabled, ItemListener itemListener)
   {
      JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(name);

      setCommonMenuItemAttr(menuItem);

      group.add(menuItem);

      container.add(menuItem);
      menuItem.setEnabled(isEnabled);
      menuItem.addItemListener(itemListener);

      return menuItem;
   }

   protected static void setCommonMenuItemAttr(JMenuItem menuItem)
   {
      menuItem.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
      menuItem.setForeground(Color.magenta);
      menuItem.setBackground(Color.yellow);
      menuItem.setBorder(BorderFactory.createMatteBorder(1, 2, 1, 2, Color.lightGray));
      menuItem.setOpaque(true);
   }

   public static JButton createButton(String text, String iconPath, Container container)
   {
      JButton button = new JButton();
      if (text != null)
         button.setText(text);

      if (iconPath != null) {
         URL url = ButtonFactory.class.getClassLoader().getResource(iconPath);
         if (url != null)
            button.setIcon(new ImageIcon(url));

      }

      if (container != null)
         container.add(button);

      return button;
   }

}
