
package plsdejai.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;

/**
 * A utility that is used to ease logging to a file
 * or/and to stderr or stdout
 * 
 * @author Prokopiou Konstantinos
 * @version 1.0, 26/12/2011
 */
public class Logger
{

   /**
    * The stderr, which can be used to log synchronously
    */
   private static Logger staticLogger = new Logger();

   /**
    * It can be the stdout or stderr
    */
   private PrintStream log;

   /**
    * A file where the log data are written
    */
   private PrintWriter fileLog;

   private StringBuilder msg;

   private Logger() { this(System.err, null); }

   public Logger(PrintStream log, String logFilename)
   {
      if (log == null && logFilename == null)
         throw new IllegalArgumentException("Logger constructor: both arguments are null");

      msg = new StringBuilder("");

      if ( log == System.err || log == System.out)
         this.log = log;

      if (logFilename != null) {

            try {
               this.fileLog = new PrintWriter(
                       //new GZIPOutputStream(
                       new FileOutputStream(logFilename, true) //)
                       );
               
            } catch (IOException evt) {
               JOptionPane.showConfirmDialog(null, "Cannot create log file '"
                       + logFilename +"'", "Directory access denied", JOptionPane.ERROR_MESSAGE);
               if (fileLog != null)
                  fileLog.close();
            }

         }

   }

   public Logger(PrintStream log){ this(log, null); }

   public Logger(String logFilename) { this(null, logFilename); }

   public static Logger getDefaultLogger() { return staticLogger; };

   public Logger append(Object m) {
      if (double[].class.isInstance(m)){
         double [] dArr = (double []) m;
         DecimalFormat text = new DecimalFormat("#0.000");
         msg.append("[");
         for (int j = 0, len = dArr.length; j < len; j++) {
            msg.append(Double.valueOf(dArr[j]).isNaN() ? "NaN"
                    : text.format(dArr[j])).append(", ");
         }
         msg.append("]");

      } else
         msg.append(m); return this;

   }

   public void log()
   {
         // try {

         if (log != null){
            log.print(msg);
            log.flush();
         }
            
         if (fileLog != null){            
            fileLog.print(msg);
            fileLog.flush();
         }            

         clear();
   }

   public void clear() { msg.delete(0, msg.length());  }


   public void close()
   {
      log();

      try {
         if (fileLog != null) {
            fileLog.flush();
            fileLog.close();
         }

         if (log != null)
            log.flush();

      } catch (Throwable e) { }
   }


}

