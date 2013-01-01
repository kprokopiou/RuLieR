package plsdejai;

import java.io.File;
import javax.swing.JOptionPane;

/**
 * This class offers a variety set of methods, that are generally useful.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 07/02/2012
 */
public class Environment {  
   public static String PROGRAM_NAME = "RuLieR";
   
   private static long startTime = 0;

   /**
    * Sets the start time
    * @param methodName
    */
   public static void start(String methodName)
   {
      startTime = System.currentTimeMillis();
      System.err.println("\n---------------\n" + methodName + " started ...");
   }

   /**
    * Calculates the process time since the start() method was called
    * @param methodName
    */
   public static void end(String methodName)
   {
      long time = System.currentTimeMillis() - startTime;
      long secs = time / 1000;
      long msecs = time % 1000;
      long min = 0, hours = 0;

      if (secs >= 60) {
         min = (secs / 60);
         secs = secs % 60;
      }

      if (min >= 60) {
         hours = min / 24;
         min = min % 24;
      }

      StringBuilder msg = new StringBuilder(methodName).append(" ended ...: Time=");
      if (hours > 0)
         msg.append(hours).append("h:").append(min).append("m:");
      else if (min > 0)
         msg.append(hours).append(min).append("m:");

      msg.append(secs).append(".").append(msecs).append("s");
      msg.append("s\n---------------\n");
      System.err.print(msg);
   }

   public static File WORK_DIR = getWorkingDirectory();
   public static final File ADDED_FILTERS_FILENAMES = new File(Environment.WORK_DIR, "added-filters.pref");
   public static final File FILTERS_PREF = new File(Environment.WORK_DIR, "filters.pref");

   private static File getWorkingDirectory()
   {
      File dir = Environment.getSystemDirectory("user.home");
      if (dir == null)
         System.exit(1);

      dir = new File(dir, PROGRAM_NAME);
      if (! dir.isDirectory() && !dir.mkdir()){
         JOptionPane.showConfirmDialog(null, "Cannot access the home directory.",
                 "Home Directory access denied", JOptionPane.ERROR_MESSAGE);
         System.exit(1);
      }

      return dir;
   }

   /**
    *
    * @param propertyName the property name specifying the system directory
    * @return a system directory, making sure that it exists and it is accessible
    */
   public static File getSystemDirectory(String propertyName)
   {
      File home;
      try {
         home = new File(System.getProperty(propertyName));
         if (home != null && !home.exists()) {
            if (!home.mkdir()) {
               JOptionPane.showConfirmDialog(null, "Cannot create the home directory.",
                       "Home Directory creation failed", JOptionPane.ERROR_MESSAGE);
               return null;
            }

         } else if (!home.isDirectory()) {
            JOptionPane.showConfirmDialog(null, "Cannot create the home directory.",
                    "Home Directory creation failed", JOptionPane.ERROR_MESSAGE);
            return null;
         }

      } catch (SecurityException e) {
         JOptionPane.showConfirmDialog(null, "Cannot access the home directory.",
                 "Home Directory access denied", JOptionPane.ERROR_MESSAGE);
         return null;
      }

      return home;
   }

}
