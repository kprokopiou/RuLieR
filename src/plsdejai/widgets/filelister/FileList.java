package plsdejai.widgets.filelister;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import plsdejai.widgets.event.JListMouseListeners;

/**
 * Create a FileList using one of the constructors, for e.g.
 * FileList fileList = new FileList();
 *
 * Then, if you want to change dynamically the appearance of a File list, call
 * A. You first set the filtering criteria you want to change:
 *    The current directory with: fileList.setCurrentDirectory(File);
 *   The sort mode with:         fileList.setSortMode(int),
 *   The selection mode          fileList.setSelectionMode(int)
 *    If hidden files are shown: fileList.setShowHiddenFilesEnabled(boolean)
 * B.and then you must call:  *   updateList();
 *
 * A component that displays a list of files
 * 
 * @author Prokopiou Konstantinos
 * @version 1.0, 17/01/2012
 */
public class FileList extends javax.swing.JList implements Comparator<File>
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   // show only files in sorted order
   public static final int FILES_ONLY = 0;
   // Show directories only in sorted order
   public static final int DIRECTORIES_ONLY = 1;
   // Show directories first and then files, in sorted order
   public static final int FILES_AND_DIRECTORIES = 2;
   private int sortMode = FILES_AND_DIRECTORIES;

   FileListCellRenderer renderer = null;

   FileLister owner;
   FileSystemView fileSystemView;
   DefaultListModel listModel = null;

   FileNameExtensionFilter filter;
   File currentDirectory;

   private int selectionMode = ListSelectionModel.SINGLE_SELECTION;

   private boolean isShowHiddenFilesEnabled = false;

   @SuppressWarnings("OverridableMethodCallInConstructor")
   public FileList(FileLister owner, File dir, int sortMode, int selectionMode,
           boolean isShowHiddenFilesEnabled)
   {
      this.owner = owner;
      setSortMode(sortMode);

      setFilter(filter);
      setCurrentDirectory(dir);

      this.isShowHiddenFilesEnabled = isShowHiddenFilesEnabled;

      // Note we call setSelectionMode only after we change the ListModel
      // in order to be called the method super.setSelectionMode().
      this.selectionMode = selectionMode;

      // Provide a better L&F for selecting items from the list by dragging the mouse
      JListMouseListeners listener =
              new JListMouseListeners(this);
      listener.addMouseListeners();
      listener.addPropertyChangeListener( new PropertyChangeListener() {
         public void propertyChange(PropertyChangeEvent e){
            String prop = e.getPropertyName();
            if (JListMouseListeners.LIST_SELECTED_INDEX_CHANGED_PROPERTY == prop)
               FileList.this.owner.setSelectedFile( (File) e.getNewValue());
         }
      });

      updateList();

      fileSystemView = owner.getFileSystemView();
   }

   /**
    * Uses the default values for:
    * Current Directory: owner.getCurrentDirectory()
    * Sort mode:         FILES_AND_DIRECTORIES
    * Selection Mode:   ListSelectionModel.SINGLE_SELECTION
    * Show hidden files: false
    * @param owner
    */
   public FileList(FileLister owner)
   {
      this(owner, null, FILES_AND_DIRECTORIES, ListSelectionModel.SINGLE_SELECTION, false);
   }

    public FileList(FileLister owner, int sortMode)
   {
      this(owner, null, sortMode, ListSelectionModel.SINGLE_SELECTION, false);
   }

    /**
     * Overrides method of JList, in order to accept only a FileListCellRenderer
     * @param renderer
     */
   public void setCellRenderer(ListCellRenderer renderer)
   {
      if ( renderer instanceof FileListCellRenderer){
         this.renderer = (FileListCellRenderer) renderer;
      }
   }

   /**
    * Overrides method of JList, in order to return a FileListCellRenderer
    * @return FileListCellRenderer
    */
   public FileListCellRenderer getCellRenderer()
   {
      return renderer;
   }

   public void updateList()
   {      
      // note: if filter == null, then all files are accepted
      final File[] data;
      if (sortMode == DIRECTORIES_ONLY) // This line is crucial for the Windows platform
         data = getSortedFiles(currentDirectory.listFiles(), sortMode);
      else
         data = getSortedFiles(currentDirectory.listFiles(this.filter), sortMode);

      if (listModel == null) {
         listModel = new DefaultListModel();
         setModel(listModel); // Specify a List model. Note there is no default List Model

         // Set List Selection Model -- there is a default model: DefaultListSelectionModel
         // The convenience methods in JList suffice for manipulating the ListSelectionModel
         setSelectionMode(selectionMode);

         setCellRenderer(new FileListCellRenderer(owner.getFileView()));

         addListeners();

         // Set Attributes
         // setLayoutOrientation(JList.VERTICAL_WRAP);
         // setVisibleRowCount(-1);

      } else
         listModel.clear();

      for (File f : data) // add Elements to the List Model
      {
         listModel.addElement(f);
      }

      //@test: it seems to work perfectly without these lines
      if (this.isShowing()) {
         revalidate();
         repaint();
      }
   }

   public void setFilter(FileNameExtensionFilter filter)
   {
      if (filter != null)
         this.filter = filter;
      else
         filter = owner.getFileFilter();
   }
   public FileNameExtensionFilter getFilter() { return filter; }

   public void setCurrentDirectory(File dir)
   {
      if (dir != null)
         currentDirectory = dir;
      else
         currentDirectory = owner.getCurrentDirectory();
   }

   public void setSortMode(int sortMode)
   {
      if (sortMode == FILES_ONLY ||
              sortMode == DIRECTORIES_ONLY ||
              sortMode == FILES_AND_DIRECTORIES)
         this.sortMode = sortMode;
   }

   public int getSortMode() { return sortMode; }

   /**
    * Override method of JList, in order to update the selectionMode
    * It seems that every time I set a new ListModel, I have also to specify
    * the selectionMode, otherwise the default is used
    */
   public void setSelectionMode(int selectionMode)
   {
      super.setSelectionMode(selectionMode);
      this.selectionMode = selectionMode;
   }

   public int getSelectionMode(){ return selectionMode; }

   public void setShowHiddenFilesEnabled(boolean isShowHiddenFilesEnabled)
   {
      this.isShowHiddenFilesEnabled = isShowHiddenFilesEnabled;
   }
   public boolean getShowHiddenFilesEnabled() {return isShowHiddenFilesEnabled;}

   private void addListeners()
   {
      addKeyListener(new KeyAdapter(){
         public void keyPressed( KeyEvent evt){
            if (evt.getKeyCode() == KeyEvent.VK_ENTER ){

            }
         }
      });

      addMouseListener(new MouseAdapter()
      {

         public void mouseClicked(MouseEvent evt)
         {
            if (evt.getClickCount() > 1) {
               File f = (File) FileList.this.getSelectedValue();

               if (f == null) return;

               if (f.isDirectory())
                  owner.setCurrentDirectory(f);
               else
                  owner.activateOkButton();
            }

         }
      });

      owner.addPropertyChangeListener(
              new PropertyChangeListener()
              {

                 public void propertyChange(PropertyChangeEvent evt)
                 {
                    String propertyName = evt.getPropertyName();
                    if (propertyName == FileLister.FILE_VIEW_CHANGED_PROPERTY) {
                       FileListCellRenderer lcr = FileList.this.getCellRenderer();
                       lcr.setFileView((FileView) evt.getNewValue());

                    } else if (propertyName == FileLister.DIRECTORY_CHANGED_PROPERTY) {
                       setCurrentDirectory((File) evt.getNewValue());
                       updateList();

                    } else if (propertyName == FileLister.FILE_FILTER_CHANGED_PROPERTY) {
                       FileList.this.setFilter( (FileNameExtensionFilter) evt.getNewValue() );
                       updateList();
                    }

                 }
              });

   }


   public JScrollPane getScrollableList()
   {
      JScrollPane listScroller = null;


      //initializeListData(JListTemplate.listModel, JListTemplate.data);
      //displayList();
      //setSelectionListModel( /* ListSelectionModel */ null, /* ListSelectionListener */ null);
      //searchForElem();
      //setDragAndDrop();
      //renderCells();

      listScroller = new JScrollPane(this);
      listScroller.setPreferredSize(new Dimension(250, 200));
      listScroller.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

      return listScroller;
   }

   /*
    * Sort files in order to get directories first and then files in sorted order
    */
   private File[] getSortedFiles(File[] files, int mode)
   {
      
      int hiddenFiles = 0;
      File[] sortedFiles = null;
      SortedSet<File> sortedNames = new TreeSet<File>(this);
      SortedSet<File> sortedDirs = new TreeSet<File>(this);

      for (File f : files) {
         if (! isShowHiddenFilesEnabled && f.isHidden())
            ++hiddenFiles;
         else if (f.isDirectory())
            sortedDirs.add(f);
         else
            sortedNames.add(f);
      }

      int index = 0;
      switch (mode) {
         case FILES_ONLY:
            sortedFiles = new File[sortedNames.size()];
            for (Iterator<File> it = sortedNames.iterator(); it.hasNext();) {
               sortedFiles[index++] = it.next();
            }
            break;

         case DIRECTORIES_ONLY:
            sortedFiles = new File[sortedDirs.size()];
            for (Iterator<File> it = sortedDirs.iterator(); it.hasNext();) {
               sortedFiles[index++] = it.next();
            }
            break;

         case FILES_AND_DIRECTORIES:
            sortedFiles = new File[sortedDirs.size() + sortedNames.size()];
            // or sortedFiles = new File[files.length - hiddenFiles];
            for (Iterator<File> it = sortedDirs.iterator(); it.hasNext();) {
               sortedFiles[index++] = it.next();
            }
            for (Iterator<File> it = sortedNames.iterator(); it.hasNext();) {
               sortedFiles[index++] = it.next();
            }
            break;

      }

      return sortedFiles;
   }

   public int compare(File f1, File f2)
   {
      String name1 = f1.getName();
      String name2 = f2.getName();

      return name1.compareTo(name2);
   }
   
}
