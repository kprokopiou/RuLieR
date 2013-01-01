package plsdejai.widgets.filelister;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileView;

/**
 * This class defines how the elements of a FileList object will be rendered on screen
 * 
 * @author Prokopiou Konstantinos
 * @version 1.0, 17/01/2012
 */
public class FileListCellRenderer extends JLabel implements ListCellRenderer
   {

   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

      FileView fileView = null;

      /**
       * This variable is set, if setDefaultSupportedExtensions() is overriden
       * in order to specify which items in the list will be shown as enabled
       * Also, user can specify the defaultSupportedExtensions calling setSupportedExtensions
       */
      String [] supportedExtensions = null;

      public FileListCellRenderer()
      {
         //setDefaultSupportedExtensions();
         setOpaque(true);
      }

      public FileListCellRenderer(FileView fv)
      {
         this();
         this.fileView = fv;
      }

      public void setFileView(FileView fv)
      {
         this.fileView = fv;
      }


      public void setSupportedExtensions(String [] extensions)
      {
         if (extensions == null){
            supportedExtensions = null;
         } else {
            int len = extensions.length;
            supportedExtensions = new String[len];
            for (int i = 0; i < len; ++i)
               supportedExtensions[i] = extensions[i];
         }

      }

      /**
       * This method should be overriden, in order to define an array of supported extensions
       * The array supportedExtensions is used by isSupportedExtension to define
       * if a list item should be shown as enabled or disabled.
       */
      private void setDefaultSupportedExtensions()
      {
         // Leave an empty body if you want all items to be shown as enabled

         supportedExtensions = plsdejai.util.ImageIconUtils.getSupportedExtensions();
      }

      /**
       * @param ext
       * @return true for an item that should be shown in the list as enabled
       *         false for an item that should be shown in the list as disabled
       * if supportedExtensions is null, all items will be shown as enabled
       */
      private boolean isSupportedExtension(String ext)
      {
         
         if (supportedExtensions == null)
            return true;
        
         for (int i = 0, len = supportedExtensions.length; i < len; ++i){            
            if (supportedExtensions[i].equalsIgnoreCase(ext)){               
               return true;
            }
               
         }

         return false;
      }

   /**
    * @param f
    * @return the extension of the file f
    */
   private String getExtension(File f)
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

      public Component getListCellRendererComponent(JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus)
      {

         File f = (File) value;

         if (fileView != null) {
            setIcon(fileView.getIcon(f));
            setIconTextGap(4);
         }
         setText(f.getName());

         Color background;
         Color foreground;

         // check if this cell represents the current DnD drop location
         JList.DropLocation dropLocation = list.getDropLocation();
         if (dropLocation != null
                 && !dropLocation.isInsert()
                 && dropLocation.getIndex() == index) {

            background = Color.BLUE;
            foreground = Color.WHITE;

            // check if this cell is selected
         } else if (isSelected) {
            background = Color.RED;
            foreground = Color.WHITE;

            // unselected, and not the DnD drop location
         } else {
            if (index % 2 == 0)
               background = Color.YELLOW;
            else
               background = Color.GREEN;

            foreground = Color.BLACK;
         }

         setBackground(background);
         setForeground(foreground);

         if (!f.isDirectory() && !isSupportedExtension(getExtension(f)))
            this.setEnabled(false);
         else
            this.setEnabled(true);

         return this;
      }
   }

