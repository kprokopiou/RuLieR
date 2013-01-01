package plsdejai;

import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import plsdejai.util.Task;
import plsdejai.widgets.LogDialog;
import plsdejai.widgets.filelister.FileNameExtensionFilter;
import plsdejai.widgets.filelister.FileLister;

/**
 * This class implements a set of operations relating to user interaction
 * in the Main class frame.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 01/02/2012
 */
public class FileOperationsUI
{
   // Constants used by the doOperation method in order to identify the type of
   // operation to be executed
   public final static int OPEN_FILE = 0;
   public final static int SAVE_FILE = 1;
   public final static int SAVEAS_FILE = 2;
   public final static int CLOSE_FILE = 3;
   public final static int QUIT = 4;
   public final static int LOAD_FILTER = 5;
   public final static int BATCH_FILTER = 6;
   public final static int ADD_DATASET_GROUNDTRUTH = 7;
   public final static int ADD_DATASET_SYNTHETIC = 8;

   // The Main object, which delegates the operations to this class
   private plsdejai.Main owner;

   // The set of FileLister objects, which are responsible to show
   // the dialogs relating to each type of operation to be executed
   private FileLister fl_openFile = new FileLister();
   private FileLister fl_openJAR = new FileLister();
   private FileLister fl_saveFile = new FileLister();
   private FileLister fl_batchFilter = new FileLister();
   private FileLister fl_addDataset = new FileLister();

   // The files selected by the more recent FileLister dialog.
   File [] selectedFiles;

   /**
    * Constructor
    * @param owner The Main object, which delegates the operations to this class
    */
   public FileOperationsUI(plsdejai.Main owner)
   {
      super();
      this.owner = owner;

      // Initialize the FileLister dialog for opening an image file
      fl_openFile.setOkButtonText("Open");
      fl_openFile.setCancelButtonText("Cancel");
      fl_openFile.setFilePreviewSupported(true);
      fl_openFile.setFilePreviewEnabled(false);

      String[] supportedExtensions = plsdejai.util.ImageIconUtils.getSupportedExtensions();
      FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Images",
              supportedExtensions);
      fl_openFile.addChoosableFileFilter(imageFilter);

      // Initialize the FileLister dialog for saving an image file
      fl_saveFile.setOkButtonText("Save");
      fl_saveFile.setCancelButtonText("Cancel");
      fl_saveFile.setFilePreviewSupported(true);
      fl_saveFile.setFilePreviewEnabled(false);
      fl_saveFile.setDefaultSaveAsTypesExtention("bmp");

      fl_saveFile.addChoosableFileFilter(imageFilter);


      // Initialize the FileLister dialog for opening an JAR file
      // which implements a filtering operations
      fl_openJAR.setOkButtonText("Open");
      fl_openJAR.setCancelButtonText("Cancel");
      fl_openJAR.setFilePreviewSupported(false);
      //fl_openJAR.setFilePreviewEnabled(false);

      FileNameExtensionFilter jarFilter = new FileNameExtensionFilter("JAR files", "jar");
      fl_openJAR.addChoosableFileFilter(jarFilter);


      // Initialize the FileLister dialog for applying a filter on a batch of image files
      fl_batchFilter.setOkButtonText("Filter selected files");
      fl_batchFilter.setOpenAllFilesButtonText("Filter All files");
      fl_batchFilter.setCancelButtonText("Cancel");
      fl_batchFilter.setFilePreviewSupported(true);
      fl_batchFilter.setFilePreviewEnabled(false);

      fl_batchFilter.addChoosableFileFilter(imageFilter);

      // Initialize the FileLister dialog for adding a dataset of image files
      // that are either the ground truth images or the synthetic images
      // that are used by a <code>LSubspace</code> object
      fl_addDataset.setOkButtonText("Add selected files");
      fl_addDataset.setOpenAllFilesButtonText("Add All files");
      fl_addDataset.setCancelButtonText("Cancel");
      fl_addDataset.setFilePreviewSupported(true);
      fl_addDataset.setFilePreviewEnabled(false);
      fl_addDataset.setBatchFilterInfoPanelSupported(false);

      fl_addDataset.addChoosableFileFilter(imageFilter);

   }

   /**
    * @return The files selected by the more recent FileLister dialog.
    */
   public File [] getSelectedFiles() { return selectedFiles; }
   

   /**
    * This method implements all the operations that are delegated
    * from the <code>Main</code> class to the FileOperationUI class
    * @param dialogType the type of operation to be performed
    */
   public void doOperation(int dialogType)
   {
      switch (dialogType){

         // Open an image file
         case OPEN_FILE:
            if (isCloseFileActionSafe("Open New Image File")){
               owner.setCanvasContent(null);
               owner.setTitle(owner.getName() + "(empty)");
               openFileDialog(fl_openFile, "Open file");
            }
               
            break;

         // Save the image file that is displayed on the canvas
         case SAVE_FILE:
            saveFile(false);
            break;

         // Save the image file that is displayed on the canvas, with a different name
         case SAVEAS_FILE:
            saveFile(true);
            break;

         // Close the image file that is displayed on the canvas
         case CLOSE_FILE:
            if (isCloseFileActionSafe("Close file ")) {
               
               owner.setCanvasContent(null);
               owner.setTitle(owner.getName() + "(empty)");
            }
            break;

         // Quit the application
         case QUIT:
            if (isCloseFileActionSafe("Quit RuLiEr")){
               owner.saveFilterParameters();
               System.exit(0);
            }
               
            break;

         // Add a new filter from a JAR file
         case LOAD_FILTER:
            openJARDialog(fl_openJAR, "Add filter");
            break;
         
         case BATCH_FILTER:
            batchFilterFilesDialog(fl_batchFilter, "Filter a batch of files");
            break;

         case ADD_DATASET_GROUNDTRUTH:
            addDatasetDialog(fl_addDataset, "Add ground truth images");
            break;

         case ADD_DATASET_SYNTHETIC:
            addDatasetDialog(fl_addDataset, "Add synthetic images");
            break;

         default:
            throw new IllegalArgumentException("showDialog: Dialog type is invalid");
      }
   }

   public StandardBinaryOp loadFilterFromJAR (File path)
   {
      if (path == null)
         return null;

      URL url = null;

      // The class that implements a filter.
      // It must be a subclass of StandardBinaryOperation
      Class<?> filterClass = null;

      try {
         // Create the url, from path
         url = new URL("jar", "", path.toURI().toURL() + "!/");

         URLClassLoader cl = new URLClassLoader(new URL[]{url});

         // Open the jar file for reading
         JarURLConnection con = (JarURLConnection) url.openConnection();
         JarFile archive = con.getJarFile();

         /* Search for the first entry that is a subclass of "plsdejai.StandardBinaryOp" */
         Enumeration<JarEntry> entries = archive.entries();
         while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            int index  = name.lastIndexOf(".class");

            if (index > 0){
               name = name.substring(0, index);
               
               String className = name.replaceAll("/", ".");

               Class<?> clazz = null;
               try {
                  clazz = cl.loadClass(className);

                  if (Main.FILTER_SUPERCLASS.isAssignableFrom(clazz) &&
                          ! clazz.isAssignableFrom(Main.FILTER_SUPERCLASS)){
                     
                     filterClass = clazz;

                     break;
                  }

               } catch (ClassNotFoundException e) {

                  return null;

               } catch (ClassFormatError e) {

                   return null;
               }

            }

         } // end while

      } catch (java.net.MalformedURLException e) {

      } catch (IOException e){

      }

      StandardBinaryOp filter = null;
      if (filterClass != null){
         try {
            filter = (StandardBinaryOp) filterClass.getConstructor().newInstance();
         } catch (InstantiationException e) {
            filter = null;
         } catch (NoSuchMethodException e) {
            filter = null;
         } catch (IllegalAccessException e) {
            filter = null;
         } catch (java.lang.reflect.InvocationTargetException e) {
            filter = null;
         }
      } else {

         return null;
      }
      
      return filter;
   }

   
   private void openJARDialog(FileLister fl, String title)
   {
      int retVal = fl.showDialog(owner, FileLister.OPEN_FILE_DIALOG, title);
      

      // Process the results.
      if (retVal == FileLister.APPROVE_OPTION) {

         File f = fl.getSelectedFile();

         StandardBinaryOp filter =  loadFilterFromJAR (f);
         if (filter != null){
            owner.addFilter(filter);
            owner.addAddedFilterPath(f);
         }
         

      }

      // Reset the file lister for the next time it's shown.
      fl.setSelectedFile(null);
   }   

   /**
    * This method shows a dialog that notifies the user, in the case that
    * the content displayed on the canvas is modified since the image was opened
    * @param title the title of the dialog asking for user confirmation
    * @return true if the content was not modified, otherwise false.
    */
   private boolean isCloseFileActionSafe(String title)
   {
      if (owner.getCanvas().isContentModified()) {

         int retVal = JOptionPane.showConfirmDialog(owner,
                 "Do you want to save changes to current image?",
                 title, JOptionPane.YES_NO_CANCEL_OPTION,
                 JOptionPane.WARNING_MESSAGE);

         switch (retVal) {

            case JOptionPane.YES_OPTION:
               saveFile(false); // Save image
               return true;

            case JOptionPane.NO_OPTION:
               return true;

            default: // JOptionPane.CANCEL_OPTION:
               return false;
         }
      }

      return true;
   }


   private void openFileDialog(FileLister fl, String title)
   {
      int retVal = fl.showDialog(owner, FileLister.OPEN_FILE_DIALOG, title);

      // Process the results.
      if (retVal == FileLister.APPROVE_OPTION) {
         File f = fl.getSelectedFile();

         if ( f != null)
            owner.setCanvasContent(f);

      }

      // Reset the file lister for the next time it's shown.
      fl.setSelectedFile(null);
   }


   JFrame dialogOwner;

   private void batchFilterFilesDialog(FileLister fl, String title)
   {

      fl.setBatchFilterInfoPanelSupported(true);
      int retVal = fl.showDialog(owner, FileLister.OPEN_FILES_DIALOG, title);

      // The directory where the batch of filtered files will be saved
      File targetDir = new File (fl.getCurrentDirectory(),
              FileLister.BATCH_TARGET_SUBDIRECTORY);

      // Process the results.
      if (retVal == FileLister.APPROVE_OPTION) {

         selectedFiles = fl.getSelectedFiles();         

         // Get a clone of the current filter
         StandardBinaryOp filter = owner.getCurrentFilter(true);
         
         if (filter == null){
            JOptionPane.showMessageDialog(null,
                    "There exists no valid batch filter",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
         }

         if (dialogOwner == null){
            dialogOwner = new JFrame();
            dialogOwner.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
         }
                  
         
         Task task = new Task<Void, Void>("Log Thread", filter, targetDir)
         {
            public Void doInBackground()
            {
               owner.setBatchFilteringEnabled(false);

               StandardBinaryOp filter = (StandardBinaryOp) params[0];
               File targetDir = (File) params[1];

               if (selectedFiles != null)
                  for (File f : selectedFiles) {
                     this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                             "Processing file '" + f.getPath() + "' : ");

                     BufferedImage bi = plsdejai.io.ImageIO.fileLoad(f);

                     if (bi == null) {
                        this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                             "FAILED -- not an image file\n");
                        continue;
                     }

                     // Apply current filter
                     try {
                        filter.setTask(this);
                        bi = filter.filter(bi, null);
                     } catch (ImagingOpException exc) {
                        this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                             "FAILED -- not a binary image\n");
                        continue;
                     } catch (Exception exc) {
                        this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                             "FAILED -- uknown restriction of the filtering operation\n");
                        continue;
                     }

                     if (bi != null && ! isCancelled()) {
                        String extension = plsdejai.io.ImageIO.getExtension(f);
                        if (!plsdejai.io.ImageIO.isValidImageExtensionName(
                                extension)) {
                           extension = "jpg";
                           // Put filtered files in the target directory
                           plsdejai.io.ImageIO.fileStore(bi, new File(targetDir,
                                   "(filtered) " + f.getName() + "." + extension));
                        } else
                           plsdejai.io.ImageIO.fileStore(bi, new File(targetDir,
                                   "(filtered) " + f.getName()));


                        this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                             "OK --\n");

                     }  else break;

                  } // On failure the called FileLister should take care of info messages

               this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                       "******************* DONE *******************\n");
               owner.setBatchFilteringEnabled(true);
               
               return null;
            }

            public void done()
            {
               if (isCancelled())
                  this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                          "CANCELLED -- \n It can take some time. Please wait ...\n");

               
            }
         };
         owner.addTask(task);
         task.addPropertyChangeListener(owner);
         new LogDialog(dialogOwner, "Simulated Annealing", task);
         
      } else {
         //log.append("Attachment cancelled by user." + newline);
      }
      //log.setCaretPosition(log.getDocument().getLength());

      // Reset the file lister for the next time it's shown.
      fl.setSelectedFile(null);
   }

   public void addDatasetDialog(FileLister fl, String title)
   {
      selectedFiles = null;

      int retVal = fl.showDialog(owner, FileLister.OPEN_FILES_DIALOG, title);

      if (retVal == FileLister.APPROVE_OPTION) {
         selectedFiles = fl.getSelectedFiles();
      }

      fl.setSelectedFile(null);
   }

   private void saveFile(boolean saveAs)
   {
      plsdejai.Canvas canvas = owner.getCanvas();
      File f = canvas.getContentFile();
      BinaryImageComponent content = canvas.getCurrentContent();

      if (null == f || saveAs){
         f = saveFileDialog(fl_saveFile, "Save File");
      } else {
         // If this.content is the current content, there is no need to save it
         if (canvas.getContent() == content)
            return;
      }

      if (null == f)
         return;

      if (saveAs){
         canvas.save(content, f, false);
         if (f != null)
            owner.setTitle(owner.getName() + f.getAbsolutePath());
         else
             owner.setTitle(owner.getName() +   "(empty)");
      } else {
         canvas.save(content, f, true);
      }

   }

   
   private File saveFileDialog(FileLister fl, String title)
   {
      File f = null;
      
      int retVal = fl.showDialog(owner, FileLister.SAVE_DIALOG, title);

      // Process the results.
      if (retVal == FileLister.APPROVE_OPTION)
         f = fl.getSelectedFile();
      else
         f = null;
      
      // Reset the file lister for the next time it's shown.
      fl.setSelectedFile(null);
      return f;
   }
}

