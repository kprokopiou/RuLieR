package plsdejai.widgets.filelister;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

/**
 * A combo box that displays the directory tree of the current directory.
 * and also all the root directories
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 17/01/2012
 */
public class FileComboBox extends javax.swing.JComboBox
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   FileLister owner;

   FileSystemView fileSystemView;   

   File[] fileComboBoxData;

   DefaultComboBoxModel comboModel = null;

   @SuppressWarnings("OverridableMethodCallInConstructor")
   public FileComboBox(FileLister owner)
   {
      this.owner = owner;
      
      fileSystemView = owner.getFileSystemView();
      
      updateComboBox(owner.getCurrentDirectory());

   }
/*
   public void setSortMode (int mode)
   {
      if (mode != FILES_ONLY && mode != DIRECTORIES_ONLY &&
              mode != FILES_AND_DIRECTORIES)
         throw new IllegalArgumentException("setSortMode: Invalid sort mode");

      sortMode = mode;
   }
*/
  // public int getSortMode (){ return sortMode; }

   public void updateComboBox(File currentDir)
   {
      if (currentDir == null) // Precaution ...
         return;

      List <File> data = new ArrayList<File>(6);

      if (currentDir.isFile())
         currentDir = currentDir.getParentFile();

      File dir = currentDir;
      // Get all the file tree elements
      while ( dir != null && ! fileSystemView.isRoot(dir)
              // This condition is crucial for the Windows platform
              && ! fileSystemView.isFileSystemRoot(dir) ){
         data.add(dir);
         dir = dir.getParentFile();
      }
      // Now 'dir' keeps the root dir, where the current directory belongs
   

      if (comboModel == null){
         comboModel = new DefaultComboBoxModel();
         setModel(comboModel); // Specify a List model. Note there is no default List Model

         setRenderer(new FileListCellRenderer(owner.getFileView()));

         addListeners();
      }
      else{         
         comboModel.removeAllElements();
      }

      File[] roots = File.listRoots();
      for (int c = 0; c < 2; ++c){

         for (int j = 0, lenRoots = roots.length; j < lenRoots; ++j) {
            comboModel.addElement(roots[j]);

            if (roots[j].equals(dir)) {

               int maxNameLen = 0;
               File maxF = null;

               for (int i = data.size() - 1; i >= 0; --i) {
                  File f = data.get(i);
                  comboModel.addElement(f);

                  String name = f.getName();
                  int len = name.length();
                  if (len > maxNameLen) {
                     maxNameLen = len;
                     maxF = f;
                  }

               }

               // Order the ListCellRenderer to use this default dimension
               // for all File elements it renders
               setPrototypeDisplayValue(maxF);

            }
         }

         if (System.getProperty("os.name").equalsIgnoreCase("Linux"))
            break;
         roots = fileSystemView.getRoots();
      }

      comboModel.setSelectedItem(currentDir);

      if (this.isShowing()){
         revalidate();
         repaint();
      }
   }

   private void addListeners()
   {      
      addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {            
            owner.setCurrentDirectory((File) FileComboBox.this.getSelectedItem());
         }
      });

      owner.addPropertyChangeListener(
              new PropertyChangeListener() {
                 public void propertyChange(PropertyChangeEvent evt)
                 {                    
                    String propertyName = evt.getPropertyName();
                    if (propertyName ==FileLister.FILE_VIEW_CHANGED_PROPERTY) {
                       FileListCellRenderer lcr =  (FileListCellRenderer) FileComboBox.this.getRenderer();
                       lcr.setFileView((FileView) evt.getNewValue());

                    } else if (propertyName == FileLister.DIRECTORY_CHANGED_PROPERTY) {
                       updateComboBox( (File) evt.getNewValue() );                       
                    }
                     
                 }
              });

   }


   class FileListCellRenderer extends JLabel implements ListCellRenderer
   {
      // Every Serializable class should define an ID
      public static final long serialVersionUID = 42L;

      private FileView fileView = null;

      int fileTreeLevel = 0;

      public FileListCellRenderer()
      {
         setOpaque(true);
      }
      public FileListCellRenderer(FileView fv)
      {
         setOpaque(true);
         this.fileView = fv;
      }

      public void setFileView( FileView fv)
      {
         this.fileView = fv;
      }

     
      public Component getListCellRendererComponent(JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus)
      {
         File f = (File) value;


         String path = f.getPath();
         int separatorPos = 0;
         fileTreeLevel = 0;

         while( separatorPos >= 0 ){
            ++separatorPos;
            separatorPos = path.indexOf(File.separator, separatorPos);

            ++fileTreeLevel;
         }         
         
        

         if (fileView != null){
            setIcon(fileView.getIcon(f));
            setIconTextGap(4);
         }

         if (fileSystemView.isRoot(f)
                 // This condition is crucial for the Windows platform
                 || fileSystemView.isFileSystemRoot(f))
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
         else
            setBorder(BorderFactory.createEmptyBorder(2, fileTreeLevel * 10, 2, 0));            

         // This condition is crucial for the Windows platform
         if (fileSystemView.isFileSystemRoot(f))
             setText(f.getAbsolutePath());
         else
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


         return this;
      }
   }


}
