package plsdejai.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import plsdejai.Main;
import plsdejai.util.Task;
import plsdejai.widgets.filelister.FileLister;

/**
 * This log dialog is intended to be used by a thread
 * and so closing this dialogs causes the thread to be terminated also.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 13/02/2012
 */
public class LogDialog
{
   public static final String LOG_PROPERTY = "log";

   private JFrame owner;

   private JDialog dialog;   

   private JTextArea textArea;
   private JButton okCommand;

   
   private JButton saveLog;

   private Task task;

   public LogDialog (JFrame owner,  String title, Task task){
      dialog = new JDialog(owner, title, Dialog.ModalityType.MODELESS);

      this.owner = owner;

      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener( new WindowAdapter(){
         public void windowClosing(WindowEvent e)
         {
            cancelTasks();
         }
      });

      dialog.setResizable(false);

      setContentPane();

      showDialog();
      addTask(task);
   }


   public boolean existsActiveTask()
   {
      return task != null && ! task.isDone() ;
   }

   private void cancelTasks()
   {

      if (okCommand.getActionCommand().equals("Cancel")  && existsActiveTask() ){

         if (JOptionPane.showConfirmDialog(null,
                 "Do you want to cancel the current thread?",
                 "Cancel thread", JOptionPane.ERROR_MESSAGE) == JOptionPane.OK_OPTION) {
            Main.cancelTask(this.task.getName());

            //dialog.dispose();
         }

      } else{
         dialog.dispose();
         owner.dispose();
      }
         
   }

   public void prepareTask(Task task)
   {
      task.addPropertyChangeListener(new PropertyChangeListener()
      {
         public void propertyChange(PropertyChangeEvent evt)
         {
            String prop = evt.getPropertyName();
            if (prop == LOG_PROPERTY){

               LogDialog.this.log((String) evt.getNewValue());

            } else if (prop.equals("state")){
               if( (SwingWorker.StateValue)evt.getNewValue() == SwingWorker.StateValue.DONE){
                   //LogDialogThread.this.task.removePropertyChangeListener(this);
                   
                  okCommand.setActionCommand("Close");
                  okCommand.setText("Close");
               }
            } else if (prop.equals("progress")){
                  int progress = ((Integer) evt.getNewValue()).intValue();
                  log("\nProgress=" + progress);

            }
         }
      });
   }

   private boolean addTask(Task task)
   {

      if (existsActiveTask() || task == null) {         
         return false;
      }

      prepareTask(task);
      this.task = task;
      Main.addTask(task);
      task.execute();

      return true;
   }



   private void showDialog()
   {
      dialog.pack();

      if  (owner != null){
         Rectangle owner_bounds = owner.getBounds();
         Dimension size = dialog.getSize();
         dialog.setLocation(new Point(owner_bounds.x
                 + Math.max( 50, (owner_bounds.width - size.width) / 2),
                 + Math.max( 50, (owner_bounds.y + owner_bounds.height - size.width) / 2)));
      }

      dialog.setVisible(true);

   }


   public void log(String text)
   {
      if(text != null)
         textArea.append(text);
   }

   public static final int STATUS_OK = 0;
   public static final int STATUS_FAILED = 1;
   public static final int STATUS_CANCELLED = 2;
   public void log(int status, String comment)
   {
      Color oldColor = textArea.getForeground();
      if (status == STATUS_OK){
         textArea.setForeground(Color.green);
         textArea.append(" OK " );
      } else{
         textArea.setForeground(Color.red);
         if (status == STATUS_FAILED)
            textArea.append(" FAILED " );
         else if (status == STATUS_CANCELLED)
            textArea.append(" CANCELLED " );
      }

      if(comment != null)
         textArea.append(" -- " + comment);

      textArea.append("\n");

      textArea.setForeground(oldColor);

   }

   public void log(int status) { log(status, null); }


   private void setContentPane()
   {
      JPanel contentPane = new JPanel();
      contentPane.setLayout(new BorderLayout());
      contentPane.setOpaque(true);

      textArea = new JTextArea(20, 60);
      textArea.setBorder(BorderFactory.createEmptyBorder(15, 6, 10, 6));
      textArea.setMinimumSize(textArea.getPreferredSize());
      contentPane.add(new JScrollPane(textArea), BorderLayout.CENTER);

      contentPane.add(createCommandPanel(), BorderLayout.SOUTH);

      dialog.setContentPane(contentPane);
   }

   private JPanel createCommandPanel()
   {
      JPanel commandPanel = new JPanel();
      commandPanel.setBorder(BorderFactory.createEmptyBorder(20, 4, 10, 4));
      commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.LINE_AXIS));

      commandPanel.add(Box.createHorizontalStrut(10));

      okCommand = new JButton("Cancel");
      okCommand.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {
             if (okCommand.getActionCommand().equals("Cancel")){
                 cancelTasks();
                 okCommand.setActionCommand("Close");
                 okCommand.setText("Close");

             } else{ // okCommand.getActionCommand().equals("Cancel")
                dialog.dispose();
                owner.dispose();
             }
                 

         }
      });
      commandPanel.add(okCommand);

      commandPanel.add(Box.createHorizontalStrut(20));
      
      saveLog = new JButton("Save");
      saveLog.setActionCommand("Save");
      saveLog.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {
            File f = null;
            FileLister fl = new FileLister();
            fl.setOkButtonText("Save");
            fl.setCancelButtonText("Cancel");
            fl.setFilePreviewSupported(false);
            fl.setDefaultSaveAsTypesExtention("log");
            fl.setSaveAsTypesExtensions(new String[]{"log"});

            if (fl.showDialog(owner, FileLister.SAVE_DIALOG, "Save Log") ==
                    FileLister.APPROVE_OPTION){
               f = fl.getSelectedFile();
               if (f != null){
                  PrintWriter fileLog = null;
                  try {
                     fileLog = new PrintWriter(
                             new FileOutputStream(f, true) );
                     fileLog.print(textArea.getText());

                  } catch (IOException exc) {
                     JOptionPane.showConfirmDialog(null, "Cannot create log file '"
                             + f.getName() + "'", "Directory access denied",
                             JOptionPane.ERROR_MESSAGE);

                  } finally {
                     if (fileLog != null)
                        fileLog.close();
                  }
               }

            }




         }
      });
      commandPanel.add(saveLog);
      
      return commandPanel;
   }

   static JFrame dialogOwner;

   public static void main(String[] args)
   {
      SwingUtilities.invokeLater(new Runnable()
      {

         public void run()
         {


            if (dialogOwner == null) {
               dialogOwner = new JFrame();
               dialogOwner.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            }

            

            Task task1 = new Task<Void, Void>("Log Thread")
            {

               public Void doInBackground()
               {

                  this.firePropertyChange(LogDialog.LOG_PROPERTY, null, "A new message before sleep\n");

                  for (int i = 0; i < 3; ++i) {

                     try {
                        Thread.sleep(1000);
                     } catch (Exception e) {

                     }
                     setProgress(15);
                     this.firePropertyChange(LogDialog.LOG_PROPERTY, null, "I am awake\n------------\n");

                     if (isCancelled()) {
                        return null;
                     }

                  }


                  return null;
               }
            };


            new LogDialog(dialogOwner, "Task dialog", task1);

         }
      });

   }
}
