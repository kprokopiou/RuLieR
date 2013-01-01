package plsdejai.widgets.filelister;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

/**
 * A convenience implementation of the FileView abstract class that
 * manages name, icon, traversable, and file type information.
 *
 * This implemention will work well with file systems that use
 * "dot" extensions to indicate file type. For example: "picture.gif"
 * as a gif image.
 *
 * Example:
 *    JFileLister ls = new JFileLister();
 *    fileView = new CustomFileView();
 *    fileView.putIcon("jpg", new ImageIcon("images/jpgIcon.jpg"));
 *    fileView.putIcon("gif", new ImageIcon("images/gifIcon.gif"));
 *    ls.setFileView(fileView);
 *
 * Note we use the special extensions "folder", "root", "drive" in getIcon()
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 17/01/2012
 */
class CustomFileView extends FileView
{

   private Map<String, Icon> icons = new HashMap<String, Icon>(5);
   // private Map<File, String> fileDescriptions = new HashMap(5);
   private Map<String, String> typeDescriptions = new HashMap<String, String>(5);
   private FileSystemView fileSystemView;

   public CustomFileView()
   {
      fileSystemView = FileSystemView.getFileSystemView();
   }

   public CustomFileView(FileSystemView fsv)
   {
      this.fileSystemView = fsv;
   }

   /**
    *
    * @param f
    * @return The name of the file. Normally this would be simply f.getName().
    */
   public String getName(File f)
   {
      return null; // Let the system file view handle this.
   }

   /**
    * Adds a human readable description of the file.
    * @param f
    * @param fileDescription

   public void putDescription(File f, String fileDescription) {
   if(f != null)
   fileDescriptions.put(f, fileDescription);
   }
    */
   /**
    *
    * @param f
    * @return A human readable description of the file. For e.g.,
    *       for jpg we might have a description "A JPEG Compressed Image File"
    * @overrides FileView.getDescription
    */
   public String getDescription(File f)
   {
      //return (String) fileDescriptions.get(f);
      return null; // Let the L&F FileView figure this out
   }

   ;

   /**
    * Adds a human readable type description for files. Based on "dot"
    * extension strings, e.g: ".gif". Case is ignored.
    * @param extension
    * @param typeDescription
    */
   public void putTypeDescription(String extension, String typeDescription)
   {
      if (extension != null)
         typeDescriptions.put(extension, typeDescription);
   }

   /**
    * Adds a human readable type description for files. Based on "dot"
    * extension strings, e.g: ".gif". Case is ignored.
    * @param f
    * @param typeDescription
    */
   public void putTypeDescription(File f, String typeDescription)
   {
      putTypeDescription(getExtension(f), typeDescription);
   }

   /**
    * @param f
    * @return A human readable description of the type of the file.
    * @overrides FileView.getTypeDescription
    */
   public String getTypeDescription(File f)
   {
      return typeDescriptions.get(getExtension(f));
   }

   /**
    *
    * @param f
    * @return  the "dot" extension for the given file, or null if there is no extension.
    */
   public String getExtension(File f)
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
    * Adds an icon based on the file type "dot" extension
    * string, e.g: ".gif". Case is ignored.
    * @param extension
    * @param icon
    */
   public void putIcon(String extension, Icon icon)
   {
      if (extension != null && icon != null) {
         icons.put(extension, icon);

         if (icon instanceof ImageIcon)
            putTypeDescription(extension, ((ImageIcon) icon).getDescription());
      }

   }

   /**
    * @param f
    * @return The icon that represents this file in the JFileChooser.
    * @overrides FileView.getIcon (Default implementation returns null)
    */
   public Icon getIcon(File f)
   {
      Icon icon = null;

      if (fileSystemView.isDrive(f)){
         icon = icons.get("drive");
         return icon;
      }

      if (fileSystemView.isRoot(f)) {
         icon = icons.get("root");
         return icon;
      }

      if (f.isDirectory()) {
         icon = icons.get("folder");
         return icon;
      }

      String extension = getExtension(f);
      if (extension != null)
         icon = icons.get(extension);

      if (icon == null)
         icon = icons.get("non-image");

      return icon;
   }

   public ImageIcon createImageIcon(String filename, String description)
   {
      String path = "resources/" + filename;
      return new ImageIcon(plsdejai.Main.class.getResource(path), description);
   }

   /**
    *
    * Whether the directory is traversable or not. Generic implementation
    * returns true for all directories and special folders.
    *
    * You might want to subtype CustomFileView to do somethimg more interesting,
    * such as recognize compound documents directories; in such a case you might
    * return a special icon for the directory that makes it look like a regular
    * document, and return false for isTraversable to not allow users to
    * descend into the directory.
    *
    * @param f
    * @return
    * @overrides FileView.isTraversable
    */
   public Boolean isTraversable(File f)
   {
      return null;	// let the L&F FileView figure this out
   }

   ;
}
