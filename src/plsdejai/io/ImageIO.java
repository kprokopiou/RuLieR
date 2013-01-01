package plsdejai.io;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.FileImageInputStream;
import javax.swing.JOptionPane;

import java.awt.image.BufferedImage;

/**
 * This class contains static methods for loading and saving image files
 * and also some utility methods
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 16/11/2011
 */
public class ImageIO
{
   /**
    * Loads an image from a file
    * @param f the file containing the image
    * @return the image or null if file cannot be opened, or it doesn't contain an image
    */
   public static BufferedImage fileLoad(File f)
   {
      BufferedImage bi = null;

      if (f != null)
         try {
            bi = fileLoad(f.getCanonicalPath());            
         } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed path resolution for '"
                    + f.getName() + "'", "File Open Error", JOptionPane.ERROR_MESSAGE);
            bi = null;
         } catch (SecurityException e) {
            JOptionPane.showMessageDialog(null, "Security violation, during path resolution for '"
                    + f.getName() + "'", "File Open Error", JOptionPane.ERROR_MESSAGE);
            bi = null;
         }

      return bi;
   }



   /**
    * Loads an image from a file
    * @param imgFile the file name containing the image
    * @return the image or null if file cannot be opened, or it doesn't contain an image
    */
   public static BufferedImage fileLoad(String imgFile)
   {      
      FileImageInputStream inputStream = null;
      RenderedImage ri = null;
      ImageReader reader = null;

      if (imgFile == null){
         JOptionPane.showMessageDialog(null, "Error! null file name.");
         return null;
      }

       File f = new File(imgFile);

      if (f == null || ! f.isFile()  ){
         JOptionPane.showMessageDialog(null,
                 "Error! This is not a valid file name. Could not load image"
                 + "from '" + imgFile + "'");
         return null;
      }

      try {
         inputStream = new FileImageInputStream(f);

         Iterator<ImageReader> it = javax.imageio.ImageIO.getImageReaders(inputStream);
         if (it.hasNext()) {
            reader = it.next(); // Read using the first ImageReader

         } else {

         
            JOptionPane.showMessageDialog(null, "The specified file '"
                    + f.getName() + "' is not an image file type.",
                    "File Open Error", JOptionPane.ERROR_MESSAGE);

            return null;
         }
         reader.setInput(inputStream);


         if (reader.getNumImages(true) > 0) // Number of images, excluding thumbnails
            
            ri = reader.read(0);


      } catch (IOException e) {
         JOptionPane.showMessageDialog(null, "Could not load image from file '"
                 + imgFile + "'", "File Open Error", JOptionPane.ERROR_MESSAGE);
         return null;
      } finally {
         try {
            if (inputStream != null)
               inputStream.close();
            if (reader != null)
               reader.dispose();
         } catch (Exception e) {
         }
      }

      return (BufferedImage) ri;
   }

   
   public static void fileStore(BufferedImage bi, File f) { fileStore(bi, f , true); }

   public static void fileStore(BufferedImage bi, String extension, File f){
      fileStore(bi, extension, f ,true);
   }

   /**
    * Stores an image to a file.
    * @param bi the image to be stored. The type of the image is defined by the
    *           file name extension
    * @param f the file where the image should be stored
    * @param replaceFile if true replaces a file if it already exists
    */
   public static void fileStore(BufferedImage bi, File f, boolean replaceFile)
   {
      String extension = getExtension(f);
      fileStore(bi, extension, f , replaceFile);
   }

   /**
    * Stores an image to a file
    * @param bi the image to be stored
    * @param extension the type of image to be stored
    * @param f the file where the image should be stored
    * @param replaceFile if true replaces a file if it already exists
    */
   public static void fileStore(BufferedImage bi, String extension, File f , boolean replaceFile)
   {

      // Check if extension if valid
      if (extension == null || ! isValidImageExtensionName(extension)){
         JOptionPane.showMessageDialog(null,
                    "The specified extention type '" + extension
                    + "' is not a valid image file extension", "Error",
                    JOptionPane.ERROR_MESSAGE);
         return;
      }


      String path = null;
      try {
         path = f.getCanonicalPath();

         if (null == extension) {
            JOptionPane.showMessageDialog(null,
                    "The specified extention type '" + path
                    + "' is not a valid image file extension", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
         }

         if (javax.imageio.ImageIO.getImageWriters(new ImageTypeSpecifier(bi), extension).hasNext())
            javax.imageio.ImageIO.write(bi, extension, f);
         

      } catch (IOException e) {
         JOptionPane.showMessageDialog(null, "Failed path resolution for '"
                 + f.getName() + "'", "File Open Error", JOptionPane.ERROR_MESSAGE);
         return;
      } catch (SecurityException e) {
         JOptionPane.showMessageDialog(null, "Security violation, during path resolution for '"
                 + f.getName() + "'", "File Open Error", JOptionPane.ERROR_MESSAGE);
         return;
      }
   }

   
   /**
    *
    * @param extension
    * @return true if extension is a valid image extension supported by the Java platform
    */
   public static boolean isValidImageExtensionName( String extension ) {

      String supportedImageFilenameExtensions[] = javax.imageio.ImageIO.getWriterFormatNames();

      if (extension != null)
         for (String ext : supportedImageFilenameExtensions)
            if (extension.equalsIgnoreCase(ext))
               return true;

      return false;
   }

   /**
    * @param f
    * @return the extension of a file, or null if there is no valid image extension
    */
   public static String getExtension(File f) {
      String ext = null;
      String s = f.getName();
      int i = s.lastIndexOf('.');

      if (i > 0 && i < s.length() - 1) {
         ext = s.substring(i + 1).toLowerCase();
      }
      return ext;
   }
}
