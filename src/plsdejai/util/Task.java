package plsdejai.util;

import java.awt.Toolkit;
import javax.swing.SwingWorker;

/**
 * This class implements a SwingWorker thread, that have a name
 * and can accept any number of arguments
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 13/12/2011
 */
public abstract class Task <T, V > extends SwingWorker<T,V>
   {

      String name;

      public Object [] params;

      public Task(String name, Object... args) {
         this.name = name;
         if (args != null){
            int len = args.length;
            if (len != 0){
               params = new Object[len];
               for(int i = 0; i < len; ++i)
                  params[i] = args[i];
            }

         }


      }

      public void setName(String name){this.name = name;}

      public String getName(){ return name;}


      // Override this method.
      protected T doInBackground()
      {
         T retVal = null;
         
         int progress = 0;         
         setProgress(progress);
         
         //...
         
         // Put in the loop accounts for the execution load
         // the excess condition !isCanceled()
         // and sent a intervals a setProgress() call.
         
         // After the loop, take care of the situation that cancelling occured.
         if (isCancelled()){
            
         }
         

         return retVal;
      }

      protected void done() {
         Toolkit.getDefaultToolkit().beep();
         
      }
      
   }
