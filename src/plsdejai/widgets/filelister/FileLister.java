package plsdejai.widgets.filelister;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

/**
 * A dialog that creates a number of dialogs for selecting files to
 * be opened or saved.
 *
 * USAGE:
 * Create a FileLister object, using one of the four constructors:
 *    FileLister fl = new FileLister();
 *    FileLister fl = FileLister(fileSystemView)
 *    FileLister fl = FileLister(currentDirectory)
 *    FileLister fl = FileLister(currentDirectory, fileSystemView)
 * Show a dialog:
 *    int retVal = fl.showDialog(owner, dialogType)
 * If retVal == APPROVE_OPTION, then call:
 *      File f = fl.getSelectionFile(), if dialoType is OPEN_FILE_DIALOG
 *      File [] f = fl.getSelectionFiles(), if dialoType is OPEN_FILES_DIALOG
 *
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 17/01/2012
 */
public class FileLister extends javax.swing.JPanel
{

   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   // ********************************
   // ***** Dialog Return Values *****
   // ********************************
   /** Return value if cancel is chosen. */
   public static final int CANCEL_OPTION = 1;
   /** Return value if approve (yes, ok) is chosen. */
   public static final int APPROVE_OPTION = 0;
   /** Return value if an error occurred. */
   public static final int ERROR_OPTION = -1;

   private int returnValue = ERROR_OPTION;   
   /* ******************************************************** */


   /* *** FileListerDialog Properties >>>>>>>>>>>>>>>>>>>>>>> */
   /** Says that a different object is being used to find available drives
    * on the system.  */
   public static final String FILE_SYSTEM_VIEW_CHANGED_PROPERTY = "FileSystemViewChanged";   
   /** Identifies a change in the list of predefined file filters
    *  the user can choose from. */
   public static final String CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY = "ChoosableFileFilterChangedProperty";
   /** User changed the kind of files to display. */
   public static final String FILE_FILTER_CHANGED_PROPERTY = "fileFilterChanged";     
   /** Says that a different object is being used to retrieve file
    * information. */
   public static final String FILE_VIEW_CHANGED_PROPERTY = "fileViewChanged";   
   /** Identifies change in user's single-file selection. */
   public static final String SELECTED_FILE_CHANGED_PROPERTY = "SelectedFileChangedProperty";
   /** Identifies user's directory change. */
   public static final String DIRECTORY_CHANGED_PROPERTY = "directoryChanged";
   /** Identifies change in user's multiple-file selection. */
   public static final String SELECTED_FILES_CHANGED_PROPERTY = "SelectedFilesChangedProperty";   
   // FileListerDialog Properties <<<<<<<<<<<<<<<<<<<<<<<<<<< */

   // *** Dialog Types >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> */
   /** Type value indicating that the <code>FileListerDialog</code> supports an
    * "Open" file operation for selecting one file. */
   public static final int OPEN_FILE_DIALOG = 0;
   /** Type value indicating that the <code>FileListerDialog</code> supports an
    * "Open" file operation for selecting multiple files. */
   public static final int OPEN_FILES_DIALOG = 1;
   /** Type value indicating that the <code>FileListerDialog</code> supports a
    * "Save" file operation. */
   public static final int SAVE_DIALOG = 2;   

   private int dialogType = OPEN_FILE_DIALOG;
   // Dialog Types <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< */

   // *** Filtering >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> */
   private boolean isShowHiddenFilesEnabled = true;

   List<FileNameExtensionFilter> filters = new ArrayList<FileNameExtensionFilter>(5);
   /** The current file filter */
   FileNameExtensionFilter selectedFileFilter;

   FileNameExtensionFilter acceptAllFileFilter =
           FileNameExtensionFilter.getAcceptAllFilter();
   // Filtering <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< */

   // *** File selection Mode >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> */
   /** Instruction to display only files. */
   public static final int FILES_ONLY = 0;
   /** Instruction to display only directories. */
   public static final int DIRECTORIES_ONLY = 1;
   /** Instruction to display both files and directories. */
   public static final int FILES_AND_DIRECTORIES = 2;

   private int fileSelectionMode = FILES_ONLY;
   // File selection Mode <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< */

   // *** File Preview Component >>>>>>>>>>>>>>>>>>>>>>>>>>>>> */
   // If isFilePreviewSupported is false preview is not possible at all
   private boolean isFilePreviewSupported = true;
   // If isFilePreviewEnabled is false the user can enable preview by using
   // a checkbox in the dialog
   // Note: dialog gets a better minimum size if set isFilePreviewEnabled = false,
   //       before calling showDialog()
   private boolean isFilePreviewEnabled = false;
   // File Preview Component <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< */

   // *** Single or Multi-selection Mode >>>>>>>>>>>>>>>>>>>>> */
   /** The selection mode for files displayed in the file list:
    * <code>ListSelectionModel.SINGLE_SELECTION</code>
    * <code>ListSelectionModel.SINGLE_INTERVAL_SELECTION </code>
    * <code>ListSelectionModel.MULTIPLE_INTERVAL_SELECTION </code>
    */
   private int fileListSelectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

   private File selectedFile = null; // single-selection mode
   private File[] selectedFiles; // Multi selection mode
   // Single or Multi-selection Mode <<<<<<<<<<<<<<<<<<<<<<<<< */


   private File currentDirectory = new File(System.getProperty("user.home"));

   private FileSystemView fileSystemView;
   private FileView fileView;

   private JDialog dialog;
   

   /**
     * Constructs a <code>FileListerDialog</code> pointing to the user's
     * default directory. */
    public FileLister() {
	this((File) null, (FileSystemView) null, true);
    }

    /**
     * Constructs a <code>FileListerDialog</code> using the given <code>File</code>
     * as the path. Passing in a <code>null</code> file
     * causes the file lister to point to the user's default directory.     *
     * @param currentDirectory  a <code>File</code> object specifying
     *				the path to a file or directory
     */
    public FileLister(File currentDirectory) {
	this(currentDirectory, (FileSystemView) null, true);
    }

    /**
     * Constructs a <code>FileListerDialog</code> using the given
     * <code>FileSystemView</code>.
     */
    public FileLister(FileSystemView fsv) {
	this( (File) null, fsv, true);
    }


    /**
     * Constructs a <code>FileListerDialog</code> using the given current directory
     * and <code>FileSystemView</code>.
     */
   @SuppressWarnings("OverridableMethodCallInConstructor")
   public FileLister(File currentDirectory, FileSystemView fsv) {
      this( currentDirectory, fsv, true);
   }

   public FileLister(boolean isFilePreviewSupported) {
      this((File) null, (FileSystemView) null, isFilePreviewSupported);
   }

   public FileLister(File currentDirectory, FileSystemView fsv,
           boolean isFilePreviewSupported)
   {
      setup(fsv);
      setCurrentDirectory(currentDirectory);
      this.isFilePreviewSupported = isFilePreviewSupported;

      initGUIElements();

      addDefaultFilters();

   }

   private void addDefaultFilters()
   {
      addAcceptAllFileFilter();
/*
      String[] supportedExtensions = plsdejai.util.ImageIconUtils.getSupportedExtensions();
      FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Images",
              supportedExtensions);
      addChoosableFileFilter(imageFilter);

      imageFilter =new FileNameExtensionFilter("JAR files", "jar");
      fileList.getCellRenderer().setSupportedExtensions(new String[]{"jar"});

      addChoosableFileFilter(imageFilter);

   // fileList.getCellRenderer().setSupportedExtensions(supportedExtensions);
 */
   }

   private String okButtonText = "Ok";
   private String cancelButtonText = "Cancel";
   private String selectDirectoryButtonText = "Select directory";   
   private String openAllFilesButtonText = "Open all files";

   public void setOkButtonText(String s) {
      okButtonText = s;
      okButton.setText(s);
   }
   public void setCancelButtonText(String s) {
      cancelButtonText = s;
      cancelButton.setText(s);
   }

   public void setSelectDirectoryButtonText(String s) {
      selectDirectoryButtonText = s;
      selectDirectoryButton.setText(s);
   }
   public void setOpenAllFilesButtonText(String s) {
      openAllFilesButtonText = s;
      selectAllFilesButton.setText(s);
   }

   private String defaultSaveAsTypesExtension = "bmp";

   public void setDefaultSaveAsTypesExtention(String s) {
      defaultSaveAsTypesExtension = s;
      
      ListModel model = saveAsTypesCombo.getModel();
      for (int i = 0, len = model.getSize(); i < len; ++i){
         if ( ((String) model.getElementAt(i)).equalsIgnoreCase(defaultSaveAsTypesExtension) ){
            saveAsTypesCombo.setSelectedItem(defaultSaveAsTypesExtension);
            break;
         }

      }


   }

   // private String[] saveAsTypesExtentions = plsdejai.util.ImageIconUtils.getSupportedExtensions();
   public void setSaveAsTypesExtensions(String [] extensions)
   {
      // saveAsTypesExtentions = extensions;
      DefaultComboBoxModel model = (DefaultComboBoxModel) saveAsTypesCombo.getModel();

      if (model.getSize() != 0)
         model.removeAllElements();

      for (String ext : extensions) {
         saveAsTypesCombo.addItem(ext);
      }
   }
     

   public void addAcceptAllFileFilter()
   {
      addChoosableFileFilter(acceptAllFileFilter);
   }

   public void removeAcceptAllFileFilter()
   {
      this.removeChoosableFileFilter(acceptAllFileFilter);
   }

   public void setShowHiddenFilesEnabled(boolean showHiddenFiles)
   {
      if (isShowHiddenFilesEnabled != showHiddenFiles){
         isShowHiddenFilesEnabled = showHiddenFiles;
         fileList.setShowHiddenFilesEnabled(showHiddenFiles);
         fileList.updateList();
      }
   }
   public boolean getShowHiddenFilesEnabled() { return isShowHiddenFilesEnabled;}


   public void setFilePreviewSupported (boolean isSupported)
   {      

      isFilePreviewSupported = isSupported;

      togglePreviewPane.setVisible(isSupported);
      if (isSupported && toggleButton.isSelected())
         setFilePreviewEnabled(true);
      else
         setFilePreviewEnabled(false);
   }

   public boolean getFilePreviewSupported () { return isFilePreviewSupported; }

   public void setFilePreviewEnabled (boolean enabled)
   {
      isFilePreviewEnabled = enabled;

      if (toggleButton.isSelected() != enabled)
         toggleButton.setSelected(enabled);

      if (preview.getParent() == null) {
         if (enabled && isFilePreviewSupported){
            int selectedIndex = fileList.getSelectedIndex();
            fileList.clearSelection();            
            add(preview, BorderLayout.EAST);
            fileList.setSelectedIndex(selectedIndex);
         }
            
      } else if (!enabled || !isFilePreviewSupported)
         remove(preview);

      if (dialog != null)
         dialog.pack();
      
   }
   public boolean getFilePreviewEnabled (){ return isFilePreviewEnabled; }

   FileList fileList;
   FileList directoryList;
   JPanel togglePreviewPane;
   JCheckBox toggleButton;
   JButton parentButton;
   JPanel saveAsTypesPanel;
   JComboBox saveAsTypesCombo;
   JComboBox showTypesCombo;
   JPanel fileNamePanel;
   JTextField fileNameField;
   JButton selectDirectoryButton;
   JButton okButton;
   JButton cancelButton;
   
   JButton selectAllFilesButton;   
   JPanel batchFilterInfoPanel;
   JTextField batchFilterDirField;
   JComponent preview;

   public static final String BATCH_TARGET_SUBDIRECTORY = "filtered-files";

   private void initGUIElements()
   {
      addPropertyChangeListeners();
      setLayout(new BorderLayout());

      // *** A. Set the List of Directories & files displayed in the middle of the dialog
      CustomFileView fv = new CustomFileView(this.fileSystemView);
      String[] extensions = plsdejai.util.ImageIconUtils.getSupportedExtensions();
      for( String ext : extensions)
         fv.putIcon(ext, fv.createImageIcon("images/image-x-generic.png", "image"));

      fv.putIcon("folder", fv.createImageIcon("images/folder.png", "folder"));
      fv.putIcon("root", fv.createImageIcon("images/folder.png", "root"));
      fv.putIcon("drive", fv.createImageIcon("images/drive-harddisk.png", "drive"));
      fv.putIcon("non-image", fv.createImageIcon("images/text.png", "non-image"));

      setFileView(fv);

      JPanel fileListPanel = new JPanel();
      fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.LINE_AXIS));
      add(fileListPanel, BorderLayout.CENTER);
      fileListPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));

      //     A.1 The directory list in the current directory
      //         It, also, helps to go down the filesystem hierarchy

      directoryList = new FileList(this, FileList.DIRECTORIES_ONLY);
      JScrollPane directoryScroller = directoryList.getScrollableList();
      fileListPanel.add(directoryScroller);
      directoryScroller.setBorder(BorderFactory.createTitledBorder("Directory List"));
      directoryList.setBackground(this.getBackground());
      directoryList.addFocusListener(new FocusAdapter()
      {
         public void focusGained(FocusEvent evt)
         {
            if (! evt.isTemporary()){
               fileList.clearSelection();

               selectDirectoryButton.setEnabled(true); // Enable "Select Directory" button
               
               if (dialogType == FileLister.OPEN_FILES_DIALOG){
                  setSelectedFiles(null); // That is the default all files is active now                  
               }
               
            }            
            
         }
      });


      directoryList.addListSelectionListener(new ListSelectionListener()
      {
         public void valueChanged(ListSelectionEvent evt)
         {
            if(evt.getValueIsAdjusting() == true)
               return;

            if (directoryList.getSelectedValue() == null)
               selectDirectoryButton.setEnabled(false); 
            else
               selectDirectoryButton.setEnabled(true);
         }
      });

      directoryList.addKeyListener(new KeyAdapter(){
         public void keyTyped(KeyEvent evt)
         {            
            if ( evt.getKeyChar() == KeyEvent.VK_ENTER)
               selectDirectoryButton.doClick();
         }
      });

      //     A.2 The file only list in the current directory
      fileList = new FileList(this, FileList.FILES_ONLY);
      JScrollPane fileScroller = fileList.getScrollableList();
      fileListPanel.add(fileScroller);
      fileScroller.setBorder(BorderFactory.createTitledBorder("File List"));
      fileList.setBackground(this.getBackground());
      fileList.addFocusListener(new FocusAdapter()
      {
         public void focusGained(FocusEvent evt)
         {
            if (! evt.isTemporary())
               directoryList.clearSelection();
            
         }
      });

      fileList.addListSelectionListener(new ListSelectionListener()
      {
         public void valueChanged(ListSelectionEvent evt)
         {
            if(evt.getValueIsAdjusting() == true)
               return;            

            /* If dialogType is OPEN_FILES_DIALOG, there is no need to select a file
            if (dialogType == FileLister.OPEN_FILES_DIALOG)
               return;
             *
             */
            if (dialogType == FileLister.OPEN_FILES_DIALOG) {
               Object [] objects = fileList.getSelectedValues();
               int len = objects.length;
               File [] files = new File[len];

               for (int i = 0; i < len ; ++i)
                  files[i] = (File) objects[i];

               FileLister.this.setSelectedFiles( files);


            } else { // (dialogType != FileLister.OPEN_FILES_DIALOG)
               setSelectedFile((File) fileList.getSelectedValue() );
            }
         }
      });


      // *** B. Set the buttons in the north of the dialog
      JPanel north = new JPanel();
      north.setLayout(new BoxLayout(north, BoxLayout.PAGE_AXIS));
      add(north, BorderLayout.NORTH);
      // *** B.1 Set the buttons for manipulating directories
      JPanel northUp = new JPanel();
      northUp.setLayout(new BoxLayout(northUp, BoxLayout.LINE_AXIS));
      northUp.setBorder(BorderFactory.createEmptyBorder(10, 2, 5, 2));
      north.add(northUp);

      //   B.1.a The directory parent list:
      //         It specifies the current directory, whose contents are displayed
      //         in the middle panel, fileList and directoryList
      //         Also, it helps to go up the filesystem hierarchy
      northUp.add(new JLabel("Look in:"));
      northUp.add(Box.createHorizontalStrut(5));

      FileComboBox directoryParentList = new FileComboBox(this);
      northUp.add(directoryParentList);
      northUp.add(Box.createHorizontalStrut(5));

      //   B.1.b Go to the parent directory Button
      parentButton =new JButton("Parent");
      northUp.add(parentButton);
      northUp.add(Box.createHorizontalStrut(2));
      parentButton.addActionListener(
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt)
                 {
                    File currentDir = getCurrentDirectory();
                    
                    if (currentDir != null && ! fileSystemView.isRoot(currentDir)
                           // This condition is crucial for the Windows platform
                           && ! fileSystemView.isFileSystemRoot(currentDir) )
                       FileLister.this.setCurrentDirectory(
                               currentDir.getParentFile());
                    
                 }
              });

      //   B.1.c Go to the Home directory Button
      JButton homeButton =new JButton("Home");
      northUp.add(homeButton);
      northUp.add(Box.createHorizontalStrut(2));
      homeButton.addActionListener(
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt)
                 {
                    FileLister.this.setCurrentDirectory(
                            fileSystemView.getHomeDirectory());
                 }
              });

      //   B.1.d Create New Folder Button
      JButton newFolderButton =new JButton("New Folder");
      northUp.add(newFolderButton);
      northUp.add(Box.createHorizontalStrut(2));
      newFolderButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt)
         {            
            String folderName =
                    JOptionPane.showInputDialog(FileLister.this, "Type the name of the new folder",
                   "Create New Folder", JOptionPane.PLAIN_MESSAGE);

            FileLister.this.setCurrentDirectory(createNewFolder(folderName));

         }
      });

      // *** B.2 File Preview toggle button option


      togglePreviewPane = new JPanel();
      togglePreviewPane.setLayout(new BoxLayout(togglePreviewPane, BoxLayout.LINE_AXIS));
      north.add(togglePreviewPane);
      togglePreviewPane.add(Box.createHorizontalGlue());
      JLabel toggleLabel = new JLabel("Preview");
      togglePreviewPane.add(toggleLabel);
      togglePreviewPane.add(Box.createHorizontalStrut(5));
      toggleButton = new JCheckBox();
      togglePreviewPane.add(toggleButton);
      toggleButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            if (toggleButton.isSelected())
               FileLister.this.setFilePreviewEnabled(true);
            else
               FileLister.this.setFilePreviewEnabled(false);
         }
      });

      // Set the preview of an image
      preview = new FilePreviewComponent(this);
      preview.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(
              BevelBorder.LOWERED), "Preview"));

      if (isFilePreviewEnabled) {
         add(preview, BorderLayout.EAST);
         toggleButton.setSelected(true);
         FileLister.this.setFilePreviewEnabled(true);
      } else {
         toggleButton.setSelected(false);
         FileLister.this.setFilePreviewEnabled(false);
      }

      if (!isFilePreviewSupported)
         setFilePreviewSupported(isFilePreviewSupported);
      
      

      // *** C. Set the buttons in the south of the dialog
      JPanel south = new JPanel();
      south.setLayout(new BoxLayout(south, BoxLayout.PAGE_AXIS));
      south.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
      add(south, BorderLayout.SOUTH);      

      // *** C.1

      JPanel typesPanel = new JPanel();
      BoxLayout southMiddleLayout = new BoxLayout(typesPanel, BoxLayout.LINE_AXIS);
      typesPanel.setLayout(southMiddleLayout);
      typesPanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));
      south.add(typesPanel);


      // *** C.2 Label and Combo Box for choosing the extension type to save to
      //         Visible only for Save
      
      saveAsTypesPanel = new JPanel();
      saveAsTypesPanel.setLayout(new BoxLayout(saveAsTypesPanel, BoxLayout.LINE_AXIS));
      typesPanel.add(saveAsTypesPanel);

      JLabel saveAsTypesLabel = new JLabel("Save as type:");
      saveAsTypesPanel.add(saveAsTypesLabel);
      saveAsTypesPanel.add(Box.createHorizontalStrut(5));

      saveAsTypesCombo = new JComboBox();
      saveAsTypesPanel.add(saveAsTypesCombo);

     
      if (saveAsTypesCombo.getModel().getSize() == 0){
         setSaveAsTypesExtensions(plsdejai.util.ImageIconUtils.getSupportedExtensions());
         this.setDefaultSaveAsTypesExtention("bmp");
      }

      saveAsTypesCombo.addActionListener(new ActionListener()
      {

         public void actionPerformed(ActionEvent evt)
         {
            String extension = (String) saveAsTypesCombo.getSelectedItem();
            String text = fileNameField.getText();
            int index = text.lastIndexOf('.');
            try {
               if (index >= 0) { // index != -1
                  index++;
                  fileNameField.getDocument().remove(index, 
                          fileNameField.getDocument().getLength() - index);
                  
                  fileNameField.getDocument().
                          insertString(fileNameField.getDocument().getLength(), extension, null);
               } else {
                  fileNameField.getDocument().
                          insertString(fileNameField.getDocument().getLength(), "." + extension, null);
                  //or fileNameField.setText("." + extension);
               }

            } catch (javax.swing.text.BadLocationException e) { }
            
            fileNameField.requestFocusInWindow();
         }
      });



      // *** C.3 Label and Combo Box for choosing the file filter for the displayed
      //         files in the file list
      typesPanel.add(Box.createHorizontalGlue());

      JPanel showTypesPanel = new JPanel();
      showTypesPanel.setLayout(new BoxLayout(showTypesPanel, BoxLayout.LINE_AXIS) );
      typesPanel.add(showTypesPanel);

      showTypesPanel.add(new JLabel("Show Files of Type:"));
      showTypesPanel.add(Box.createHorizontalStrut(5));

      showTypesCombo = new JComboBox();
      showTypesPanel.add(showTypesCombo);
      // Note: if none filter is added then all files are shown

      showTypesCombo.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {            
            FileLister.this.setFileFilter(
                    FileLister.this.filters.get( showTypesCombo.getSelectedIndex() ));
         }
      });

      // *** C.4 Label and Field for writing the filename to Open from or Save to
      //         Set Visibility off only for dialog type OPEN_FILES_DIALOG
     
      fileNamePanel = new JPanel();
      fileNamePanel.setLayout(new BoxLayout(fileNamePanel, BoxLayout.LINE_AXIS));
      fileNamePanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));
      south.add(fileNamePanel);

      fileNamePanel.add(new JLabel("Filename:"));
      fileNamePanel.add(Box.createHorizontalStrut(5));

      fileNameField = new JTextField(20);
      fileNamePanel.add(fileNameField);
      fileNamePanel.add(Box.createHorizontalGlue());
      fileNameField.addFocusListener(new FocusAdapter()
      {

         public void focusGained(FocusEvent evt)
         {
            if (!evt.isTemporary()) {
               fileList.clearSelection();
               directoryList.clearSelection();

               fileNameField.setSelectionStart(0);

               fileNameField.setSelectionColor(Color.orange);
               String text = fileNameField.getText();
               int index = text.lastIndexOf('.');

               if (index >= 0)
                  fileNameField.setSelectionEnd(index);
               else
                  fileNameField.setSelectionEnd(text.length());

            }
         }
      });

      fileNameField.getDocument().addDocumentListener(new DocumentListener()
      {

         public void changedUpdate(DocumentEvent evt)
         {
         }

         public void insertUpdate(DocumentEvent evt)
         {
            if (okButton != null && okButton.isVisible() && !okButton.isEnabled())
               okButton.setEnabled(true);
         }

         public void removeUpdate(DocumentEvent evt)
         {
               if (okButton != null && okButton.isVisible())
                  if (evt.getDocument().getLength() <= 0)
                     okButton.setEnabled(false);
                  else if (!okButton.isEnabled())
                     okButton.setEnabled(true);
         }
      });
      

      // *** C.5 Action buttons
      JPanel actionButtonsPanel = new JPanel();
      actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.LINE_AXIS));
      actionButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));
      south.add(actionButtonsPanel);

      actionButtonsPanel.add(Box.createHorizontalStrut(5));

      //  C.5.a  Select Directory button.
      //         It is the default button if the dialog type is OPEN_FILES_DIALOG

      selectDirectoryButton = new JButton();
      selectDirectoryButton.setText(selectDirectoryButtonText);
      actionButtonsPanel.add(selectDirectoryButton);
      selectDirectoryButton.setEnabled(false);
      selectDirectoryButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt)
         {
            if (directoryList.isFocusOwner()
                    || selectDirectoryButton.isFocusOwner()) {
               // Manipulate user actions (select + ENTER) on the directory list

               File f = null;
               if (!directoryList.isSelectionEmpty()) {
                  f = (File) directoryList.getSelectedValue();

                  if (f != null /* && f.isDirectory() */)
                     setCurrentDirectory(f);
               }

            }
         }
      });


      actionButtonsPanel.add(Box.createHorizontalGlue());

      //     C.2.d.ii Button Ok ={Open File or Save File}
      //         It is visible and the default button for all dialog types,
      //            except for OPEN_FILES_DIALOG
      
      okButton = new JButton(okButtonText);
      okButton.setEnabled(false);
      actionButtonsPanel.add(okButton);
      okButton.addActionListener(new ActionListener()
      {

         public void actionPerformed(ActionEvent evt)
         {            
            activateOkButton();
         }
      });

      actionButtonsPanel.add(Box.createHorizontalStrut(10));
      
      //  C.5.b Action button for processing all of the selected files in the current directory
      //        Visible only for dialog type OPEN_FILES_DIALOG.      

      selectAllFilesButton = new JButton();
      selectAllFilesButton.setText(openAllFilesButtonText);
      actionButtonsPanel.add(selectAllFilesButton);
      selectAllFilesButton.addActionListener(new ActionListener()
      {

         public void actionPerformed(ActionEvent evt)
         {
            // Select All files in the the fileList
            ListModel listModel = fileList.getModel();
            int len = listModel.getSize();
            File[] files = new File[len];
            for (int i = 0; i < len; ++i) {
               files[i] = (File) listModel.getElementAt(i);
            }

            setSelectedFiles(files);

            activateOkButton(); // delegate to OPEN_FILES_DIALOG code segment
         }
      });
   
      actionButtonsPanel.add(Box.createHorizontalStrut(5));
      

      // C.5.c Cancel button
      cancelButton = new JButton(cancelButtonText);
      actionButtonsPanel.add(cancelButton);
      cancelButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt)
         {
            returnValue = FileLister.CANCEL_OPTION;
            dialog.dispose();
         }
      });

      //  C.5.d  Label and text field for informing the user about the directory
      //         where the processed files will be saved
      //         Visible only for dialog type OPEN_FILES_DIALOG.

      
      batchFilterInfoPanel = new JPanel();
      batchFilterInfoPanel.setLayout(new BoxLayout(batchFilterInfoPanel, BoxLayout.LINE_AXIS));
      batchFilterInfoPanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
      south.add(batchFilterInfoPanel);

      JPanel targetDirInfoPanel = new JPanel();
      targetDirInfoPanel.setLayout(new BoxLayout(targetDirInfoPanel, BoxLayout.LINE_AXIS));
      targetDirInfoPanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));
      batchFilterInfoPanel.add(targetDirInfoPanel);

      JLabel targetDirLabel = new JLabel();
      targetDirLabel.setText("Filtered files will be saved in:");
      targetDirInfoPanel.add(targetDirLabel);
      targetDirInfoPanel.add(Box.createHorizontalStrut(5));
      batchFilterDirField = new JTextField();
      targetDirInfoPanel.add(batchFilterDirField);

      batchFilterDirField.setEditable(false);
      batchFilterDirField.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

      File currentDir = getCurrentDirectory();
      if (currentDir == null)
         batchFilterDirField.setText("");
      else
         batchFilterDirField.setText((new File(currentDir, "Filtered-files")).getAbsolutePath());     
      
   }

   private boolean isBatchFilterInfoPanelSupported = true;
   public void setBatchFilterInfoPanelSupported(boolean isSupported)
   {
      isBatchFilterInfoPanelSupported = isSupported;
   }


   /*
    * @returns the created file, or null if directory creation failed
    */
   private File createNewFolder(String folderName)
   {      

      File path = null;
      if (folderName != null)
         path = new File(folderName);

      if (path != null) {

         if (!path.isAbsolute())
            path = new File(currentDirectory, folderName);

         if (path.exists()) {
            JOptionPane.showMessageDialog(null,
                    "A folder with the name '" + path + "' already exists",
                    "Folder exists", JOptionPane.INFORMATION_MESSAGE);
            
         } else {
            try {
               if (!path.mkdir()){
                  JOptionPane.showMessageDialog(null,
                          "System failed to create a folder with the name '"
                          + path + "'",
                          "System error", JOptionPane.ERROR_MESSAGE);
                  //retVal = false;
                  }
              

            } catch (SecurityException e) {
               JOptionPane.showMessageDialog(null,
                       "System failed to create a folder with the name '"
                       + path + "'" + "due to security restrictions",
                       "Security System error", JOptionPane.ERROR_MESSAGE);
               path = null;
            }
            
         }
      }

      return path;
   }

   private void addPropertyChangeListeners()
   {
      addPropertyChangeListener(new PropertyChangeListener() {

         public void propertyChange(PropertyChangeEvent evt)
         {
            String propertyName = evt.getPropertyName();

            if (propertyName == FileLister.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY) {

               
               FileNameExtensionFilter[] oldFilters = (FileNameExtensionFilter[]) evt.getOldValue();
               FileNameExtensionFilter[] newFilters = (FileNameExtensionFilter[]) evt.getNewValue();

               int oldFiltersLen = oldFilters.length;
               int newFiltersLen = newFilters.length;

               if  (oldFiltersLen < newFiltersLen){ // a new Filter has been added
                  FileNameExtensionFilter filter = newFilters[newFiltersLen - 1];

                  showTypesCombo.addItem(filter.getDescription());

               } else if (oldFiltersLen > newFiltersLen) { // a Filter has been removed
                  // search the filter that is removed                  
                  int index = 0;
                  for (; index < newFiltersLen; ++index) {
                     if (oldFilters[index] != newFilters[index])
                        break;
                  }

                 showTypesCombo.removeItem(oldFilters[index].getDescription());

               }

            } else if (propertyName == FileLister.FILE_FILTER_CHANGED_PROPERTY) {

               FileNameExtensionFilter filter = (FileNameExtensionFilter) evt.getNewValue();
               if (filter != null) {
                  String desc = filter.getDescription();
                  for (int i = 0, len = showTypesCombo.getItemCount(); i < len; ++i) {
                     if (desc.equals((String) showTypesCombo.getItemAt(i))) {
                        showTypesCombo.setSelectedIndex(i);                        
                        break;
                     }
                  }
               }

            }  else if (propertyName == FileLister.DIRECTORY_CHANGED_PROPERTY) {

               if (batchFilterInfoPanel != null && batchFilterInfoPanel.isVisible()) {
                  File currentDir = (File) evt.getNewValue();

                  if (currentDir == null)
                     batchFilterDirField.setText("");
                  else
                     batchFilterDirField.setText(
                             (new File(currentDir, BATCH_TARGET_SUBDIRECTORY).getAbsolutePath()));
               }
               
               // The following line automatically executes also : okButton.setEnabled(false)
               if (fileNameField != null && fileNameField.isVisible())
                  if (dialogType == SAVE_DIALOG)
                     fileNameField.setText( "." +
                             (String)FileLister.this.saveAsTypesCombo.getSelectedItem());
                  else
                     fileNameField.setText("");

            } else if (propertyName == FileLister.SELECTED_FILE_CHANGED_PROPERTY) {
               // Only for FileLister.OPEN_FILES_DIALOG I use multi-selection and not single selection
               // okButton and fileNameField are guaranteed to be non-null
               if ( dialogType == FileLister.OPEN_FILES_DIALOG)
                  return;

               File f = (File) evt.getNewValue();
               if (f == null || f.isDirectory())
                  return;

               File parent = f.getParentFile();
               if ( parent != null &&  ! parent.equals(getCurrentDirectory()))
                  fileNameField.setText( f.getAbsolutePath());
               else
                  fileNameField.setText( f.getName());

               okButton.setEnabled(true);
               
            }
            
            else  if (propertyName == SELECTED_FILES_CHANGED_PROPERTY) {
               // Only for FileLister.OPEN_FILES_DIALOG I use multi-selection
               if ( dialogType != FileLister.OPEN_FILES_DIALOG)
                  return;
               
               if (evt.getNewValue() == null)
                  okButton.setEnabled(false);
               else
                  okButton.setEnabled(true);
            }
                

         }
      });
      

   }


   public void activateOkButton()
   {
      // Safety code: Logically this condition should be never met
      // i.e. if dialogType == FileLister.OPEN_FILES_DIALOG
      if ( okButton == null || ! okButton.isVisible() ) 
         return;

      if (FileLister.this.dialogType == OPEN_FILES_DIALOG){
         ; // Currently OPEB_FILES_DIALOG executes the apply batch filter function
      } else if (directoryList.isFocusOwner()  ) {
         // Manipulate user actions (select + ENTER) on the directory list

         File f = null;
         if (!directoryList.isSelectionEmpty()) {
            f = (File) directoryList.getSelectedValue();

            if (f != null /* && f.isDirectory() */ )
               setCurrentDirectory(f);
         }
         return;

      } else if (fileList.isFocusOwner()) {
         // Manipulate user actions (select + ENTER) on the file list
         File f = null;
         if (!fileList.isSelectionEmpty()) {
            f = (File) fileList.getSelectedValue();

            // if a user selects + ENTER or double clicks a non-supported file, do nothing
            if (f != null && ! getFileFilter().isSupportedExtension(
                    FileNameExtensionFilter.getExtension(f)))
               return;

         }

         // When the "Ok" button is pressed, I have confidence that the fileNameField
         // keeps the last specified value
      } else if (fileNameField != null  && fileNameField.getText().length() > 0 ) {         
         setSelectedFile(new File (fileNameField.getText()) );

      } else {
           return;
      }
         

      switch (FileLister.this.dialogType) {
         // The first three error conditions, should not ever happen at runtime
         // because we remove the ability for the user to specify a filename
         // in a text field.
         // We add them for debugging reasons
         case OPEN_FILE_DIALOG:
            if(this.selectedFile == null ){
               returnValue = FileLister.ERROR_OPTION;
               
            } else if (this.selectedFile.isDirectory()) {

               returnValue = FileLister.ERROR_OPTION;               
               
            } else if (!this.selectedFile.isFile()) {

               returnValue = FileLister.ERROR_OPTION;               
               
            } else {

               returnValue = FileLister.APPROVE_OPTION;
            }
            break;

         case OPEN_FILES_DIALOG:
            // Dialog type OPEN_FILES_DIALOG does not use single-selection            
            setSelectedFile(null);
            
            File [] files = this.getSelectedFiles();
      
            if(files == null ){
               returnValue = FileLister.ERROR_OPTION;

            } else if (batchFilterInfoPanel.isVisible()) {
               if (files.length != 0) {
                  File targetDir = new File(getCurrentDirectory(), BATCH_TARGET_SUBDIRECTORY);

                  if (!targetDir.exists()
                          && // On failure createNewFolder takes care of the information messages
                          // createNewFolder also sets targetDir as the currentDirectory
                          createNewFolder(targetDir.getAbsolutePath()) == null) {
                     JOptionPane.showMessageDialog(dialog,
                             "The system could not create the directory '"
                             + targetDir.getPath() + "'\n",
                             "File System Error", JOptionPane.ERROR_MESSAGE);
                     returnValue = ERROR_OPTION;

                  } else{
                     returnValue = APPROVE_OPTION;
                     setCurrentDirectory(targetDir);
                  }

               }
               
            } else {
               returnValue = APPROVE_OPTION;
            }

            setSelectedFiles(files);
           
            break;

         case SAVE_DIALOG:
            // The first error condition, should not ever happen at runtime
            // We add it for debugging reasons

            if(this.selectedFile == null ){

               returnValue = FileLister.ERROR_OPTION;

            } else if ( this.selectedFile.isDirectory()
                    || ! this.selectedFile.getParentFile().isDirectory()) {
               // This action just changes the current directory
               // through setSelectedFile()
               
               returnValue = FileLister.ERROR_OPTION;
            } else {
               // If not a legal image file extension is provided, then
               // add the one selected in the save as types combo box

               if (! getFileFilter().isSupportedExtension(
                       FileNameExtensionFilter.getExtension(this.selectedFile)))
                  this.selectedFile = new File(this.selectedFile.getPath() +
                          "." + (String) saveAsTypesCombo.getSelectedItem());

               if (this.selectedFile.isFile()) {

                  int retVal = JOptionPane.showConfirmDialog(dialog,
                          "File '" + this.selectedFile.getPath() + "' already exists.\n"
                          + "Do you really want to overwrite it?",
                          "Overwrite existing file", JOptionPane.YES_NO_OPTION,
                          JOptionPane.WARNING_MESSAGE);

                  switch (retVal) {

                     case JOptionPane.YES_OPTION:
                        returnValue = FileLister.APPROVE_OPTION;
                        break;

                     case JOptionPane.NO_OPTION:
                        returnValue = FileLister.CANCEL_OPTION;
                        break;

                     default:
                        returnValue = FileLister.ERROR_OPTION;
                        break;
                  }

               } else
                  returnValue = FileLister.APPROVE_OPTION;

            }
            
            break;

         default:
            throw new IllegalArgumentException("FileLister: Uknown dialog type");
      }

      if (returnValue == APPROVE_OPTION)
         dialog.dispose();

   }

   public <T extends JFrame> int showDialog( T owner) { return showDialog(owner, this.dialogType, null); }

   public <T extends JFrame> int showDialog( T owner, int dialogType) { return showDialog(owner, dialogType, null); }

   public <T extends JFrame> int showDialog( T owner, int dialogType, String title)
   {
      // Note the visible GUI Elements depends on the default dialogType
      setDialogType( dialogType );      

      this.dialog = new JDialog(owner, java.awt.Dialog.ModalityType.APPLICATION_MODAL);

      switch (this.dialogType) {
         case OPEN_FILE_DIALOG:
            if (title == null)
               dialog.setTitle("Open File");
            else
               dialog.setTitle(title);
            
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fileList.updateList();

            directoryList.updateList();

            okButton.setVisible(true);
            okButton.setText("Open File");
            dialog.getRootPane().setDefaultButton(okButton);

            saveAsTypesPanel.setVisible(false);

            fileNamePanel.setVisible(false);

            selectAllFilesButton.setVisible(false);
            batchFilterInfoPanel.setVisible(false);
            break;

         case OPEN_FILES_DIALOG:
            if (title == null)
               dialog.setTitle("Open Files");
            else
               dialog.setTitle(title);

            fileList.setSelectionMode(fileListSelectionMode);
            fileList.updateList();

            directoryList.updateList();

            dialog.getRootPane().setDefaultButton(selectDirectoryButton);

            okButton.setVisible(true);

            saveAsTypesPanel.setVisible(false);

            fileNamePanel.setVisible(false);

            selectAllFilesButton.setVisible(true);
            batchFilterInfoPanel.setVisible( isBatchFilterInfoPanelSupported);
            break;

         case SAVE_DIALOG:
            if (title == null)
               dialog.setTitle("Save File");
            else
               dialog.setTitle(title);

            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fileList.updateList();

            directoryList.updateList();

            okButton.setVisible(true);
            okButton.setText("Save");
            dialog.getRootPane().setDefaultButton(okButton);

            saveAsTypesPanel.setVisible(true);
            fileNameField.setText("." + defaultSaveAsTypesExtension);

            fileNamePanel.setVisible(true);

            selectAllFilesButton.setVisible(false);
            batchFilterInfoPanel.setVisible(false);
            break;

         default:
            throw new IllegalArgumentException("FileListerDialog: Uknown dialog type");
      }
      dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      dialog.setResizable(true);

      dialog.setContentPane(this);      

      dialog.pack();

      Rectangle owner_bounds = owner.getBounds();
      Dimension size = getSize();
      dialog.setLocation(new Point(owner_bounds.x
              + (owner_bounds.width - size.width) / 2,
              (owner_bounds.y + owner_bounds.height - size.width) / 2));
      dialog.setMinimumSize(size);

      dialog.setVisible(true);
      
      return returnValue;
   }



   protected void setup(FileSystemView view)
   {
      if(view == null) {
            view = FileSystemView.getFileSystemView();
        }
        setFileSystemView(view);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);

   }

   /** Sets the type of this dialog. */
   private void setDialogType(int dialogType) {
	if(this.dialogType == dialogType) {
	    return;
	}
	
	this.dialogType = dialogType;
    }

   /**
     * Sets the current file filter. The file filter is used by the
     * file lister to filter out files from the user's view. */
   public void setFileFilter(FileNameExtensionFilter filter) {
	FileNameExtensionFilter oldValue = selectedFileFilter;
	selectedFileFilter = filter;
        
	if (filter != null) {
	    if ( ( fileSelectionMode == ListSelectionModel.SINGLE_INTERVAL_SELECTION ||
                   fileSelectionMode == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION )
                   && selectedFiles != null && selectedFiles.length > 0) {
		List<File> fList = new ArrayList<File>(20);
		boolean failed = false;
		for (int i = 0; i < selectedFiles.length; i++) {
		    if (filter.accept(selectedFiles[i])) {
			fList.add(selectedFiles[i]);
		    } else {
			failed = true;
		    }
		}
		if (failed) {
		    setSelectedFiles((fList.isEmpty() ) ? null :
                       (File[])fList.toArray());
		}
	    } else if (selectedFile != null && !filter.accept(selectedFile)) {
		setSelectedFile(null);
	    }
        }        
	firePropertyChange(FILE_FILTER_CHANGED_PROPERTY, oldValue, filter);
        

    }

   /** Returns the currently selected file filter. */
   public FileNameExtensionFilter getFileFilter() { return this.selectedFileFilter; }
   

   /** Gets the list of user choosable file filters. */
   public FileNameExtensionFilter[] getChoosableFileFilters() {	
	return filters.toArray( new FileNameExtensionFilter[filters.size()]) ;
    }
   
   /** Adds a filter to the list of user choosable file filters.*/
   public boolean addChoosableFileFilter(FileNameExtensionFilter filter) {      
      boolean retVal = false;
      if(filter != null  && !filters.contains(filter)) {
	    FileNameExtensionFilter[] oldValue = getChoosableFileFilters();
	    retVal = filters.add(filter);
	    firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue, getChoosableFileFilters());
	}

	setFileFilter(filter);
        return retVal;
   }

   /** Removes a filter from the list of user choosable file filters. Returns
     * true if the file filter was removed.
     */

   public boolean removeChoosableFileFilter(FileNameExtensionFilter filter) {
      boolean retVal = false;
      if (filter != null && filters.contains(filter)) {
         FileNameExtensionFilter[] oldValue = getChoosableFileFilters();
         retVal =filters.remove(filter);
         firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue, getChoosableFileFilters());
      }
      if(getFileFilter() == filter )
         setFileFilter(null);

      return retVal;
   }


   /**
     * Sets the selected file. If the file's parent directory is
     * not the current directory, changes the current directory
     * to be the file's parent directory. */
   public void setSelectedFile(File file) {
	File oldValue = selectedFile;
	selectedFile = file;
        
	if(selectedFile != null) {
	    if (file.isAbsolute()){

               if (selectedFile.isDirectory()) {
                  setCurrentDirectory(selectedFile);
                  return;
               } else if (!getFileSystemView().isParent(getCurrentDirectory(), selectedFile))
                  setCurrentDirectory(selectedFile.getParentFile());


	    }
            else {
               selectedFile = new File(getCurrentDirectory(), file.getPath() );
            }
	}
        
	firePropertyChange(SELECTED_FILE_CHANGED_PROPERTY, oldValue, selectedFile);
    }

    /** @return the selected file */
    public File getSelectedFile() { return selectedFile; }

    /** Sets the list of selected files if the file lister is
     *  set to allow multiple selection. */
    public void setSelectedFiles(File[] selectedFiles) {       
	File[] oldValue = this.selectedFiles;
	if (selectedFiles != null && selectedFiles.length == 0) {
	    selectedFiles = null;
	}
	this.selectedFiles = selectedFiles;
	setSelectedFile((selectedFiles != null) ? selectedFiles[0] : null);
	firePropertyChange(SELECTED_FILES_CHANGED_PROPERTY, oldValue, this.selectedFiles);
    }
    
    /** Returns a list of selected files if the file lister is
     *  set to allow multiple selection. */
    public File[] getSelectedFiles() {
	if(selectedFiles == null) {
	    return new File[0];
	} else {
	    return selectedFiles.clone();
	}
    }
   

   /** Sets the current directory. Passing in <code>null</code> sets the
     * file lister to point to the user's default directory. */
   public void setCurrentDirectory(File dir)
   {
      File oldValue = currentDirectory;

      if (dir != null && dir.isFile())
         dir = dir.getParentFile();

      if (dir == null || ! dir.isDirectory())
         dir = null;

      if (dir == null)
         dir = getFileSystemView().getDefaultDirectory();

      if (currentDirectory != null)
         /* Verify the toString of object */
         if (this.currentDirectory.equals(dir))
            return;

      currentDirectory = dir;

      // Disable the parent button if there is no parent for the current directory
      if (parentButton != null)
         parentButton.setEnabled(dir.getParent() != null);

      firePropertyChange(DIRECTORY_CHANGED_PROPERTY, oldValue, currentDirectory);
   }

   /** @return the current directory */
   public File getCurrentDirectory() { return this.currentDirectory; }

   /** Changes the directory to be set to the parent of the
     * current directory. */
   public void changeToParentDirectory() {
	selectedFile = null;
	File oldValue = getCurrentDirectory();
	setCurrentDirectory(getFileSystemView().getParentDirectory(oldValue));
    }


   /** Sets the file view to used to retrieve UI information, such as
     * the icon that represents a file or the type description of a file. */
   public void setFileView(FileView fileView) {
	FileView oldValue = this.fileView;
	this.fileView = fileView;
	firePropertyChange(FILE_VIEW_CHANGED_PROPERTY, oldValue, fileView);
    }
   /** Returns the current file view. */
   public FileView getFileView() { return fileView; }

   // ******************************
   // *****FileView delegation *****
   // ******************************
   /** Returns the filename. */
   public String getName(File f) {
	String filename = null;
	if(f != null) {
	    if(getFileView() != null) {
		filename = getFileView().getName(f);
	    }
        }
	return filename;
   }

   /** Returns the file description. */
   public String getDescription(File f) {
	String description = null;
	if(f != null) {
	    if(getFileView() != null) {
		description = getFileView().getDescription(f);
	    }
        }
	return description;
    }

   /** Returns the file type. */
   public String getTypeDescription(File f) {
	String typeDescription = null;
	if(f != null) {
	    if(getFileView() != null) {
		typeDescription = getFileView().getTypeDescription(f);
	    }
        }
	return typeDescription;
    }

   /** Returns the icon for this file or type of file, depending on the system */
   public Icon getIcon(File f) {
	Icon icon = null;
	if (f != null) {
	    if(getFileView() != null) {
		icon = getFileView().getIcon(f);
	    }
	}
	return icon;
    }

   /** @return true if the file/directory can be traversed, otherwise false */
   public boolean isTraversable(File f) {
	Boolean traversable = null;
	if (f != null) {
	    if (getFileView() != null) {
		traversable = getFileView().isTraversable(f);
	    }
	    if (traversable == null) {
		traversable = getFileSystemView().isTraversable(f);
	    }
	}
	return (traversable != null && traversable.booleanValue());
    }

   /**  @return true if the file should be displayed, otherwise false */
   public boolean accept(File f) {
	boolean shown = true;
	if(f != null && selectedFileFilter != null) {
	    shown = selectedFileFilter.accept(f);
	}
	return shown;
    }

   /**
     * Sets the file system view that the <code>FileLister</code> uses for
     * accessing and creating file system resources, such as finding
     * the floppy drive and getting a list of root drives.
     */
   public void setFileSystemView(FileSystemView fsv) {
	FileSystemView oldValue = fileSystemView;
	fileSystemView = fsv;
	firePropertyChange(FILE_SYSTEM_VIEW_CHANGED_PROPERTY, oldValue, fileSystemView);
    }
   /** Returns the file system view. */
   public FileSystemView getFileSystemView() { return fileSystemView; }   


   /** Sets the file lister to allow multiple file selections. */
   public void setFileListSelectionMode(int mode) {
	if ( mode == ListSelectionModel.SINGLE_SELECTION ||
             mode == ListSelectionModel.SINGLE_INTERVAL_SELECTION ||
             mode == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
           fileListSelectionMode = mode;
    }

   /** @return true if multiple files can be selected */
   public int getFileListSelectionMode() { return fileListSelectionMode; }

   
}
