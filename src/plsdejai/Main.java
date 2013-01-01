package plsdejai;

import plsdejai.optimizer.SA;
import plsdejai.filter.LowerProfileOfZeroTriadsOp;
import java.awt.image.ImagingOpException;
import java.awt.image.BufferedImage;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.Iterator;
import javax.swing.JComboBox;
import plsdejai.widgets.FilterRemoveDialog;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import plsdejai.widgets.FilterListComboBox;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextField;
import java.beans.PropertyChangeListener;
import plsdejai.widgets.JMenuFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import plsdejai.filter.DirectionalLocalProfileOp;

import plsdejai.filter.linearsubspace.LSubspaceOfCentralMomentsOp;
import plsdejai.optimizer.Energy;
import plsdejai.util.Logger;
import plsdejai.util.Task;
import plsdejai.widgets.AbstractDefaultParameterToolbar;
import plsdejai.widgets.AbstractParameterToolbar;
import plsdejai.widgets.ButtonFactory;
import plsdejai.widgets.DefaultParameterToolbar;
import plsdejai.widgets.LSubspaceParameterToolbar;
import plsdejai.widgets.LogDialog;
import plsdejai.widgets.ParameterLister;


/*
 * Program: RuLieR : Rule-Line eRaser
 * This is the main class having the GUI elements for user interaction
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 15/02/2012
 */
public class Main extends JFrame
        implements ActionListener, ItemListener, PropertyChangeListener
{

   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   /* ************* Global variables ******************* */
   // The superclass of all classes implementing filters on binary images
   public static final Class<?> FILTER_SUPERCLASS = plsdejai.StandardBinaryOp.class;
   /* ************************************************** */

   private final String programName = "Rule-Line eRaser (RuLieR): "; // Main Frame title

   /* *** The three regions (sub-frames) that are show inside the Main frame */
   // A. Toolbars
   // A.1 The Container of all toolbars
   private JPanel toolbarPane;
   // A.2 The Toolbar that allows to choose a filter, add or remove a filter
   //     and apply filter to the currently opened image or for batch processing
   private JPanel filterListToolBar;
   private JButton applyCurrentFilter;
   private JButton applyBatchFilter;
   // A.3.a The Toolbar that it is specific to the chosen filter
   private JPanel currentFilterToolbar;


   // A.3.b Get feedback after implementing a filter
      JComboBox feedbackCombo;
      JButton feedBackSave;

   // B. The canvas
   private Canvas canvas;

   // C. The status bar
   private JTextField statusField;
   /* ************************************************** */

   /* *** Operation for opening, saving or closing a file, and also
    *     for loading and applying filters, are delegated to an object of FileOperationsUI
    */
   private FileOperationsUI fileUI = new FileOperationsUI(this);
   /* ************************************************** */

   /* ************* menu items of the File menu ******** */
   private JMenu fileMenu;

   private JMenuItem fileOpen;
   private JMenuItem reloadImage;
   private JMenuItem fileSave;
   private JMenuItem fileSaveAs;
   private JMenuItem fileClose;
   private JMenuItem fileQuit;
   /* ************************************************** */

   /* ************ The StandardBinaryOp ************************** */
   /* It is crucial to note that filtersMenu contains in the following order
    *    - filterMenuItems.size() menu items.
    *    - a separator
    *    - the filterAdd button
    * You must take care of this arrangment when you add or remove filters.
    */
   private JMenu filtersMenu;

   private StandardBinaryOp currentFilter;

   // filterListCombo keeps track of the total, default and added filters
   // The total list of filters is included in the combo box model
   // the default filters list is returned by the getDefaultFilters()
   // the user added filters list is returned by getAddedFilters()
   private FilterListComboBox filterListCombo;

   private JMenuItem filterAdd;
   private JMenuItem filterRemove;

   //private static final int INDEX_OF_FIRST_FILTER_MENU_ITEM = 2;
   private List<JMenuItem> filterMenuItems = new ArrayList<JMenuItem>();
   private ButtonGroup filterButtonGroup;

   // The owner of LogDialog objects
   JFrame dialogOwner;

   // Variables used by the "Optimize Parameters" toolbar
   private JButton addGTImagesButton = null;
   private JButton addSynthImagesButton = null;
   private JButton optimizeButton = null;
   private File[] groundTruthImageFiles;
   private File [] syntheticImageFiles;
   private File feedbackLogFile;
   
   /* ************************************************** */

   private Main() { super(); initGUI();}

   public Canvas getCanvas(){ return this.canvas; }

   public String getName () { return this.programName; }

   private void initGUI()
   {
      /* *** Setup Look-And-Feel ************************** */
      //JFrame.setDefaultLookAndFeelDecorated(false); // Default: false - Use native Look-And-Feel
      /* setDefaultLookAndFeelDecorated(true) has the same effect on a single JFrame by doing the following:
         JFrame frame = new JFrame();
         frame.setUndecorated(true); // Note: The window manager must support undecorated windows.
         frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
       */
      /* ************************************************** */

      setTitle(programName +  "(empty)");
      
      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      addWindowListener( new WindowAdapter(){
         public void windowClosing(WindowEvent e)
         {              
            if (canvas.isContentModified()) {
               int retVal = JOptionPane.showConfirmDialog(null,
                       "Do you want to save changes to current image before exit?",
                       "Exit", JOptionPane.YES_NO_OPTION,
                       JOptionPane.WARNING_MESSAGE);

               switch (retVal) {

                  case JOptionPane.YES_OPTION:
                     fileUI.doOperation(FileOperationsUI.SAVE_FILE);
                     break;

                  default:
                     // Do nothing
                     break;
               }
            }

            saveFilterParameters();
            System.exit(0);
         }
      });


      /* A Create Menu Bar ****************************** */
      /* A.1 Create Menu Bar */
      JMenuBar menuBar = new JMenuBar();
      menuBar.setOpaque(true);
      menuBar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 4));
      menuBar.setBackground(Color.GRAY);
      menuBar.setForeground(Color.blue);
      menuBar.setPreferredSize(new Dimension(300, 40));
      setJMenuBar(menuBar);

      /* A.2 Create Main Menus */

      fileMenu = JMenuFactory.createJMenu("File", menuBar);      
      menuBar.add(Box.createRigidArea(new Dimension(8, 40)));
      filtersMenu = JMenuFactory.createJMenu("Filters", menuBar);

      /* A.3 Create Menu Items */

      /* A.3.(i) Create Menu Items for the File menu */
      fileOpen = ButtonFactory.createJMenuItem("Open", fileMenu, true, this);      
      fileMenu.addSeparator();
      reloadImage = ButtonFactory.createJMenuItem("Reload", fileMenu, false, this);
      fileMenu.addSeparator();
      fileSave = ButtonFactory.createJMenuItem("Save", fileMenu, false, this);
      fileSaveAs = ButtonFactory.createJMenuItem("Save As", fileMenu, false, this);
      fileMenu.addSeparator();
      fileClose = ButtonFactory.createJMenuItem("Close", fileMenu, false, this);
      fileMenu.addSeparator();
      fileQuit = ButtonFactory.createJMenuItem("Quit", fileMenu, true, this);

      /* A.3.(ii) Create Menu Items for the View menu */
      //viewZoom = JMenuItemFactory.createJMenuItem("Zoom ...", viewMenu, false, this);

      /* A.3.(iii) Create Menu Items for the Filters menu
       *           and also the filter instances
       */
      filterButtonGroup = new ButtonGroup();

      filterAdd = ButtonFactory.createJMenuItem("Add Filter ...", filtersMenu, true, this);

      filterRemove = ButtonFactory.createJMenuItem("Remove Filter ...", filtersMenu, false,
              new ActionListener(){
                 public void actionPerformed(ActionEvent evt){
                    
                    new FilterRemoveDialog(Main.this);
                 }
              });

      filtersMenu.addSeparator();
      /* ************************************************** */

      /* B. Create Content Pane ************************** */
      Container pane = this.getContentPane();

      toolbarPane =  new JPanel();
      toolbarPane.setOpaque(true);
      toolbarPane.setLayout(new BoxLayout(toolbarPane, BoxLayout.PAGE_AXIS));
      pane.add(toolbarPane, BorderLayout.PAGE_START);

      /* B.1 Control Panel containing controls for selecting,
       * adding, removing and applying filters */
      filterListToolBar = new JPanel();
      filterListToolBar.setLayout(new BoxLayout(filterListToolBar, BoxLayout.LINE_AXIS));
      filterListToolBar.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Filters"));
      toolbarPane.add(filterListToolBar);

      JLabel labelForFilterListCombo = new JLabel("Select filter ");
      filterListToolBar.add(labelForFilterListCombo);
      labelForFilterListCombo.setLabelFor(filterListCombo);

      filterListCombo = new FilterListComboBox();
      filterListCombo.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {
            setCurrentFilter( filterListCombo.getSelectedIndex() );
         }
      });
      filterListToolBar.add(filterListCombo);
      filterListToolBar.add(Box.createHorizontalStrut(5));

      applyCurrentFilter = new JButton("Filter current image");
      applyCurrentFilter.setActionCommand("Filter");
      // applyCurrentFilter.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.green));
      
      applyCurrentFilter.setEnabled(false);

      filterListToolBar.add(applyCurrentFilter);
      filterListToolBar.add(Box.createHorizontalStrut(5));
      applyCurrentFilter.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {            
            if (applyCurrentFilter.getActionCommand().equals("Filter")) {

               applyCurrentFilter.setForeground(Color.red);
               applyCurrentFilter.setText("Cancel");
               applyCurrentFilter.setActionCommand("Cancel");               
               fileOpen.setEnabled(false);
               fileClose.setEnabled(false);
               reloadImage.setEnabled(false);
               boolean fileSaveEnabledStatus = fileSave.isEnabled();
               fileSave.setEnabled(false);               
               fileSaveAs.setEnabled(false);

               Task task = new Task<Void, Void>("Filter current image", 
                       Boolean.valueOf(fileSaveEnabledStatus))
               {
                  long startTime;
                  public Void doInBackground()
                  {
                      canvas.setProcessTime(" ");

                      startTime = System.currentTimeMillis();
                     
                     BinaryImageComponent currentContent = canvas.getCurrentContent();

                     BufferedImage bi = null;

                     StandardBinaryOp op = getCurrentFilter(true);
                     
                     if (isParametersValid(op)) {
                        
                        try {
                           op.setTask(this);                     
                           bi = op.filter(currentContent.getBufferedImage(), null);

                        } catch (ImagingOpException exc) {
                           bi = null;
                           JOptionPane.showMessageDialog(null,
                                   "We can only filter binary images!", "Filter operation cancelled",
                                   JOptionPane.INFORMATION_MESSAGE);

                           // NullPointerException, IllegalArgumentException or other exception
                        } catch (OutOfMemoryError exc) {
                           bi = null;
                           JOptionPane.showMessageDialog(null,
                                   "Java Virtual Machine cannot allocate more memory!",
                                   "Out of memory error",
                                   JOptionPane.ERROR_MESSAGE);

                        } catch (Exception exc) {

                           bi = null;
                           JOptionPane.showMessageDialog(null,
                                   "Uknown restriction of the filtering operation!",
                                   "Filter operation cancelled",
                                   JOptionPane.WARNING_MESSAGE);
                        }

                        if (! isCancelled() && bi != null) {
                           canvas.displayFilteredImage(bi);
                        }

                     }

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

                     StringBuilder msg = new StringBuilder("");
                     if (hours > 0)
                        msg.append(hours).append("h:").append(min).append("m:");
                     else if (min > 0)
                        msg.append(hours).append(min).append("m:");
                     msg.append(secs).append(".").append(msecs).append("s");

                     canvas.setProcessTime(msg.toString());

                     applyCurrentFilter.setForeground(Color.black);
                     applyCurrentFilter.setText("Filter current image");
                     applyCurrentFilter.setActionCommand("Filter");
                     fileOpen.setEnabled(true);
                     fileClose.setEnabled(true);
                     reloadImage.setEnabled(true);

                     fileSaveAs.setEnabled(true);

                     if (!isCancelled()) // As a consequence the image is modified. So enable save
                        fileSave.setEnabled(true);
                     else
                        fileSave.setEnabled(((Boolean) params[0]).booleanValue());

                     return null;
                  }
               };
               addTask(task);
               task.execute();

            } else { // Cancel
               Main.cancelTask("Filter current image");
            }

         }
      });

      applyBatchFilter = new JButton("Filter a Batch of images ...");
      applyBatchFilter.addActionListener(this);
      applyBatchFilter.setEnabled(true);
      filterListToolBar.add(applyBatchFilter);
      applyBatchFilter.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {            
            fileUI.doOperation(FileOperationsUI.BATCH_FILTER);
         }
      });

      filterListToolBar.add(Box.createHorizontalStrut(5));

      filterListToolBar.add(Box.createHorizontalGlue());

      /* B.2 Control Panel controlling the current filter and the feedback */
      JPanel filterToolbarPanel = new JPanel();
      filterToolbarPanel.setLayout(new BoxLayout(filterToolbarPanel, BoxLayout.PAGE_AXIS));
      filterToolbarPanel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Current Filter" ));
      toolbarPane.add(filterToolbarPanel);

      // B.2.a Control Panel containing controls for manipulating the StandardBinaryOp
      currentFilterToolbar = new JPanel();
      currentFilterToolbar.setOpaque(true);
      //currentFilterToolbar.setBackground(Color.orange);
      currentFilterToolbar.setLayout(new BoxLayout(currentFilterToolbar, BoxLayout.LINE_AXIS));      
      
      filterToolbarPanel.add(currentFilterToolbar);

      filterToolbarPanel.add( Box.createVerticalGlue() );

      // B.2.b The Feedback/Optimizer are added after the currentFilter
      JPanel feedbackToolbar = new JPanel();
      feedbackToolbar.setLayout(new BoxLayout(feedbackToolbar, BoxLayout.LINE_AXIS));


      /* B.2.b.1 Optimizer using Simulated Annealing */
      JPanel optimizePane = new JPanel();
      optimizePane.setLayout(new BoxLayout(optimizePane, BoxLayout.PAGE_AXIS));
      optimizePane.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
              "Simulated Annealing Optimizer"));

      // Adds the training dataset used to search for the optimum parameters
      JPanel optimizePane1 = new JPanel();
      optimizePane1.setLayout(new BoxLayout(optimizePane1, BoxLayout.LINE_AXIS));      

      addGTImagesButton = new JButton("Set ground truth images");
      addGTImagesButton.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {
            fileUI.doOperation(FileOperationsUI.ADD_DATASET_GROUNDTRUTH);
            groundTruthImageFiles = fileUI.getSelectedFiles();
         }
      });

      addSynthImagesButton = new JButton("Set synthetic images ...");
      addSynthImagesButton.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {
            fileUI.doOperation(FileOperationsUI.ADD_DATASET_SYNTHETIC);
            syntheticImageFiles = fileUI.getSelectedFiles();
         }
      });

      optimizeButton = new JButton("Optimize");
      optimizeButton.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {
            if (groundTruthImageFiles == null
                    || groundTruthImageFiles.length == 0){
               JOptionPane.showMessageDialog(null,
                          "Data set of ground truth images is not set",
                          "Dataset is empty",
                          JOptionPane.INFORMATION_MESSAGE);

            } else if( syntheticImageFiles == null
                    || syntheticImageFiles.length == 0){
                JOptionPane.showMessageDialog(null,
                          "Data set of synthetic images is not set",
                          "Dataset is empty",
                          JOptionPane.INFORMATION_MESSAGE);

            } else if (groundTruthImageFiles.length != syntheticImageFiles.length) {
               JOptionPane.showMessageDialog(null,
                       "The number of synthetic and synthetic images is not the same",
                       "Dataset is invalid",
                       JOptionPane.INFORMATION_MESSAGE);

            } else {

               Task task = new Task<Void, Void>("Log Thread")
               {

                  public Void doInBackground()
                  {
                     optimizeButton.setEnabled(false);

                     StandardBinaryOp filter = getCurrentFilter(true);

                     StringBuilder msg = new StringBuilder();
                     msg.append("\n--------------- Starting Simulated Annealing ...\n*** Algorithm: ")
                             .append(filter.getName()).append("\n*** Input images: \n");

                     for (int i = 0, size = groundTruthImageFiles.length; i < size; ++i) {

                        msg.append(i + 1).append(".\tground truth: '").append(groundTruthImageFiles[i]
                                .getName()).append("'\n").append("  \tsynthetic: '")
                                .append(syntheticImageFiles[i].getName()).append("'\n");

                     }

                     this.firePropertyChange(LogDialog.LOG_PROPERTY, null, msg.toString());

                     try {
                        SA sa = new SA(filter, groundTruthImageFiles,
                                syntheticImageFiles);

                        this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                                " Processing ... :");
                        if (sa != null) {

                           if (sa.start(this) && !isCancelled())
                              this.firePropertyChange(LogDialog.LOG_PROPERTY, null, "OK\n");

                           msg.delete(0, msg.length());
                           msg.append("Optimized Parameters:\n");

                           Parameter[] parameters = sa.getOptimumParameters();
                           if (parameters != null && parameters.length > 0) {

                              for (Parameter param : parameters) {
                                 msg.append(">>>").append(param).append("\n");
                              }

                              Energy energy = sa.getOptimumEnergy();
                              if(energy != null){
                                 msg.append(">>>").append("precision=").append(energy.getPrecision())
                                      .append(", recall=").append(energy.getRecall())
                                      .append(", F1=").append(energy.getF1()).append("\n");

                              this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                                      msg.toString());
                              } else
                                 msg.delete(0, msg.length());
                           }
                        }
                     } catch (Exception e) {
                        e.printStackTrace();
                        Main.cancelTask(this.getName());
                     }

                     
                     optimizeButton.setEnabled(true);
                     this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                             "******************* DONE *******************\n");

                     System.gc();

                     return null;
                  }

                  public void done()
                  {                     
                     if (isCancelled())
                        this.firePropertyChange(LogDialog.LOG_PROPERTY, null,
                                "CANCELLED -- \n It can take some time. Please wait ...\n");                     
                  }

               };

               if (dialogOwner == null) {
                  dialogOwner = new JFrame();
                  dialogOwner.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
               }

              new LogDialog(dialogOwner, "Simulated Annealing", task);
            }
         }
      });
      

      optimizePane1.add(addGTImagesButton);
      optimizePane1.add(Box.createHorizontalStrut(4));
      optimizePane1.add(addSynthImagesButton);
      optimizePane1.add(Box.createHorizontalStrut(4));
      optimizePane1.add(optimizeButton);
      optimizePane1.add(Box.createHorizontalGlue());

      optimizePane.add(optimizePane1);
      optimizePane.add(Box.createVerticalGlue());

      feedbackToolbar.add(optimizePane);


      // B.2.b.2 Feedback toolbar
      JPanel feedbackPanel = new JPanel();
      feedbackPanel.setLayout(new BoxLayout(feedbackPanel, BoxLayout.PAGE_AXIS));
      feedbackPanel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Feedback"));

      JPanel feedbackPanel1 = new JPanel();
      feedbackPanel1.setLayout(new BoxLayout(feedbackPanel1, BoxLayout.LINE_AXIS));

      feedbackCombo = new JComboBox(new String[]{
         "[Choose feedback]" ,
         "Poor line detection" ,
         "Poor text/line discrimination",
         "Poor line detection and poor text/line discrimination" ,
         "Yes/Ok"
      });
      feedbackCombo.setEnabled(false);
      feedbackCombo.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {
            if(feedbackCombo.getSelectedIndex() != 0 && canvas.getContentFile() != null)
               feedBackSave.setEnabled(true);
            else
               feedBackSave.setEnabled(false);

         }
      });

      feedbackCombo.setMaximumSize(feedbackCombo.getPreferredSize());
      JLabel labelForFeedback = new JLabel("Were the results ok?");

      JPanel feedbackComboWrap = new JPanel();
      feedbackComboWrap.add(feedbackCombo);
      feedbackComboWrap.setMaximumSize(feedbackComboWrap.getPreferredSize());


      feedBackSave = new JButton("Save feedback");
      feedBackSave.setEnabled(false);
      feedBackSave.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent evt)
         {
            Logger logger = new Logger (feedbackLogFile.getAbsolutePath());
            logger.append("Image: ").append(canvas.getContentFile().getName())
                    .append(", Alg.: ").append(currentFilter.getName()).append(", ");

            Parameter[] params = currentFilter.getParameters();
            
            if (params != null)
               for (int i = 0, len = params.length; i < len; ++i) {                  
                  logger.append(params[i].toString()).append(", ");
               }
            logger.append("Feedback: ").append((String)feedbackCombo.getSelectedItem())
                    .append("\n");
            logger.log();
            logger.close();

            feedbackCombo.setSelectedIndex(0);
         }
      });

      JPanel feedBackSaveWrap = new JPanel();
      feedBackSaveWrap.add(feedBackSave);
      feedBackSaveWrap.setMaximumSize(feedBackSaveWrap.getPreferredSize());
      labelForFeedback.setLabelFor(feedbackCombo);
      

      JPanel feedbackPanel2 = new JPanel();
      feedbackPanel2.setLayout(new BoxLayout(feedbackPanel2, BoxLayout.LINE_AXIS));

      feedbackLogFile = new File (Environment.WORK_DIR, "feedback.txt");

      JLabel logFeedbackLabel = new JLabel("Log file: ");
      JTextField logFeedbackFileFld = new JTextField(40);
      logFeedbackLabel.setLabelFor(logFeedbackFileFld);
      logFeedbackFileFld.setEditable(false);
      logFeedbackFileFld.setBorder(BorderFactory.createEmptyBorder());
      logFeedbackFileFld.setText(feedbackLogFile.getAbsolutePath());

      JPanel feedbackPanel0 = new JPanel();
      feedbackPanel0.setLayout(new BoxLayout(feedbackPanel0, BoxLayout.LINE_AXIS));
      feedbackPanel0.add(labelForFeedback);
      feedbackPanel0.add( Box.createHorizontalGlue() );
      
      feedbackPanel1.add(feedbackComboWrap);
      feedbackPanel1.add(Box.createHorizontalStrut(4));
      feedbackPanel1.add(feedBackSaveWrap);
      feedbackPanel1.add( Box.createHorizontalGlue() );
      //feedbackPanel1.setMaximumSize(feedbackPanel.getPreferredSize());

      feedbackPanel2.add(logFeedbackLabel);
      feedbackPanel2.add(logFeedbackFileFld);
      feedbackPanel2.add( Box.createHorizontalGlue() );

      feedbackPanel.add(feedbackPanel0);
      feedbackPanel.add(feedbackPanel1);
      feedbackPanel.add(feedbackPanel2);
      feedbackToolbar.add(feedbackPanel);

      toolbarPane.add(feedbackToolbar);
      filterToolbarPanel.add( Box.createHorizontalGlue() );

      /* B.3 The canvas for drawing the picture to manipulate */
      setCanvasContent(null); // Sets the canvas

      JPanel canvasPane = new JPanel(new BorderLayout());
      canvasPane.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Canvas" ));
     
      pane.add(canvas, BorderLayout.CENTER);

      /* B.4 The status bar for showing messages */
      statusField = new JTextField("");
      statusField.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
      statusField.setEditable(false);
      add(statusField, BorderLayout.SOUTH);
      
      /* ************************************************** */

      addDefaultFilters();
      loadFilterParameters();

      /* *** Pack, Position and show GUI ****************** */
      this.pack();
      // frame.setLocationRelativeTo(null);

      this.setLocation(new Point(0,0));
      this.setVisible(true);
      
      /* ************************************************** */

   }

   /**
    * Sets the content of the canvas to the image contained in file f.
    * @param f
    */
   public void setCanvasContent(File f) {
      if (canvas == null) {
         canvas = new Canvas(1.0); // zoom = 100%
         canvas.setPreferredSize(calculateCanvasSize());
         canvas.addPropertyChangeListener(this);
      }

      canvas.setZoom(1.0);
      canvas.setContent(f);

      if (f != null)
         setTitle(programName + f.getAbsolutePath());
      else
         setTitle(programName + "(empty)");
      
   }

   /**
    * Calculate the preferred size for canvas, actually its JScrollPane,
    * based on the elements of this frame    *
    */
   private Dimension calculateCanvasSize()
   {
      this.pack(); // In order to take valid measures for the dimensions

      Dimension size = new Dimension();
      GraphicsConfiguration gc = this.getGraphicsConfiguration();

      // Calculate the graphics display size
      Rectangle graphicsBounds = gc.getBounds();

      Dimension rootPaneSize = this.getRootPane().getSize();
      Dimension rootPaneMinusCanvasSize = null;

      if (canvas != null){
         Dimension canvasSize = canvas.getSize();
         rootPaneMinusCanvasSize = new Dimension(
                 rootPaneSize.width - canvasSize.width,
                 rootPaneSize.height - canvasSize.height);
      } else
         rootPaneMinusCanvasSize = rootPaneSize;


      size.width = (int) (0.85* graphicsBounds.width - rootPaneMinusCanvasSize.width);
      size.height = (int)(0.75 * graphicsBounds.height - rootPaneMinusCanvasSize.height);

      return size;
   }


   /**
    * Adds the default Filter Classes
    */
   private void addDefaultFilters()
   {
      // These filters are supported by default.
      List<StandardBinaryOp> defaultFilters = new ArrayList<StandardBinaryOp>();

      defaultFilters.add(new DirectionalLocalProfileOp());
      defaultFilters.add(new LowerProfileOfZeroTriadsOp());
      defaultFilters.add(new LSubspaceOfCentralMomentsOp());

      for (Iterator<StandardBinaryOp> it = defaultFilters.iterator(); it.hasNext();) {

         StandardBinaryOp filter = it.next();

         // Note the sequence of these two statements is crucial
         filterMenuItems.add(ButtonFactory.createJRadioButtonMenuItem(filterButtonGroup,
                 filter.getName(), filtersMenu, true, this));
         filterListCombo.addDefaultItem(filter);
      }
     
      setCurrentFilter(0);
   }
   
   public StandardBinaryOp getCurrentFilter (boolean clone) {
      StandardBinaryOp filter = null;

      if (clone) {
         // Create a new instance of the current filter
            if (StandardBinaryOp.class.isAssignableFrom(Main.this.currentFilter.getClass())){
               filter = currentFilter.clone();
               
            } else {               
               filter = null;
            }
      } else
         filter = this.currentFilter;

      return filter;
   }

   public StandardBinaryOp getCurrentFilter () { return getCurrentFilter(false); }

   /**
    * Adds a new filter
    * @param filter
    */
   public void addFilter(StandardBinaryOp filter)
   {
      if ( filter == null)
         return;

      // Note the sequence of these two statements is crucial
      filterMenuItems.add(ButtonFactory.createJRadioButtonMenuItem(filterButtonGroup,
              filter.getName(), filtersMenu, true, this));
      filterListCombo.addItem(filter);

      filterRemove.setEnabled(true);

      setCurrentFilter(filter);
   }

   /**
    * Saves the current values of the parameters of all filters, default and added
    */
   public void saveFilterParameters()
   {
      File pref = Environment.FILTERS_PREF;
      ObjectOutputStream out = null;
      try {
         out = new ObjectOutputStream(
                 new GZIPOutputStream(
                 new FileOutputStream(pref)));

         for (int j = 0; j < 2; ++j) {

            List<StandardBinaryOp> filters = null;
            if (j == 0)
               filters = filterListCombo.getDefaultFilters();
            else
               filters = filterListCombo.getAddedFilters();

            for (int i = 0, len = filters.size(); i < len; ++i) {
               StandardBinaryOp filter = filters.get(i);
               out.writeObject(filter.getName());
               out.writeObject(filter.getParameters());
            }

         }
      } catch (EOFException e) {

      } catch (IOException e) {

      } finally {
         try {
            if (out != null) {
               out.flush();
               out.close();
            }
         } catch (IOException e) { }
      }


   }

   /**
    * Load Parameters saved in the preference files
    * for the default and added filters
    */
   private void loadFilterParameters()
   {
      File pref = Environment.FILTERS_PREF;
      if (!pref.isFile())
         return;

      ObjectInputStream in = null;
      ObjectInputStream in1 = null;

      List<StandardBinaryOp> filters = filterListCombo.getDefaultFilters();
      try {
         in = new ObjectInputStream(
                 new GZIPInputStream(
                 new FileInputStream(pref)));
         
         for (int j = 0; j < 2; ++j) {

            for (int i = 0, len = filters.size(); i < len; ++i) {
               String name = (String) in.readObject();
               Parameter[] params = (Parameter[]) in.readObject();

               StandardBinaryOp filter = filters.get(i);
               
               if (filter != null && filter.getName().equals(name)
                       && params != null && params.length != 0) {

                  AbstractDefaultParameterToolbar toolbar =
                          (AbstractDefaultParameterToolbar) filter.getToolbar();

                  if (toolbar == null) {
                     toolbar = new DefaultParameterToolbar(filter.getName());
                     filter.setToolbar(toolbar);

                     if (toolbar != null)
                        for (Parameter param : params) {
                           toolbar.createParameter(param.name, param.desc, param.type,
                                   param.min, param.max, param.value);
                        }

                  } else
                     for (Parameter param : params) {
                        toolbar.setMinimumParameterValue(param.name, param.min);
                        toolbar.setMaximumParameterValue(param.name, param.max);

                        filter.setParameterValue(param.name, param.value);
                     }

                  if (toolbar instanceof LSubspaceParameterToolbar) {                     
                     ((LSubspaceParameterToolbar) toolbar).createNewSubspace();
                  }

                  if (j == 1)
                     addFilter(filter);

               }

            }

            filters.clear();

            File addedFilterFilenamesPref = Environment.ADDED_FILTERS_FILENAMES;
            if (!addedFilterFilenamesPref.isFile())
               return;

            in1 = new ObjectInputStream(
                    new GZIPInputStream(
                    new FileInputStream(addedFilterFilenamesPref)));

            try {
               while (true) {
                  StandardBinaryOp filter = fileUI.loadFilterFromJAR((File) in1.readObject());
                  if (filter != null)
                     filters.add(filter);
               }
            } catch (EOFException e) {
               // System.out.println("EOF while reading '" + addedFilterFilenamesPref + "'");
            }

         }

      } catch (EOFException e) {
         // System.out.println("EOF while reading '" + pref + "'");
      } catch (IOException e) {
         System.out.println("Error while reading '" + pref + "'");
      } catch (ClassNotFoundException e) {
         System.out.println("Error while reading '" + pref + "'");
      } finally {
         try {
            if (in != null)
               in.close();
             if (in1 != null)
               in1.close();
         } catch (IOException e) {
         }

      }

   }

   /**
    * Adds the JAR file where the added file is contained to the preference file.
    * @param path
    */
   public void addAddedFilterPath(File path)
   {
      File pref = Environment.ADDED_FILTERS_FILENAMES;

      ObjectOutputStream out = null;
      try {
         out = new ObjectOutputStream(
                 new GZIPOutputStream(
                 new FileOutputStream(pref)));

         out.writeObject(path);

      } catch (EOFException e) {

      } catch (IOException e) {

      } finally {
         try {
            if (out != null) {
               out.flush();
               out.close();
            }
         } catch (IOException e) { }
      }

   }


   /**
    * Removes the JAR file where the added file is contained from the preference file.
    * @param path
    */
   public void removeAddedFilterPath(int [] indices)
   {
      File pref = Environment.ADDED_FILTERS_FILENAMES;

      List <File> files = new ArrayList<File>(5);
      int size = files.size();

      ObjectInputStream in = null;

      try {
         in = new ObjectInputStream(
                 new GZIPInputStream(
                 new FileInputStream(pref)));

         while (true) {
            files.add( (File) in.readObject() );
         }

      } catch (EOFException e) {
         // System.out.println("EOF while reading '" + pref + "'");
      } catch (IOException e) {
         System.out.println("Error while reading '" + pref + "'");
      } catch (ClassNotFoundException e) {
         System.out.println("Error while reading '" + pref + "'");
      } finally {
         try {
            if (in != null)
               in.close();
         } catch (IOException e) {}
      }

      int len = indices.length;
      for (int i = len - 1; i >= 0; --i) {
         files.remove(i);
      }

      ObjectOutputStream out = null;
      try {
         out = new ObjectOutputStream(
                 new GZIPOutputStream(
                 new FileOutputStream(pref)));

         for(Iterator<File> it = files.iterator(); it.hasNext();)
            out.writeObject(it.next());

      } catch (EOFException e) {

      } catch (IOException e) {

      } finally {
         try {
            if (out != null) {
               out.flush();
               out.close();
            }
         } catch (IOException e) { }
      }

   }

   /**
    * Removes the specified filters from the list of added filters
    * @param indices the indices of the added filters in the filter's list
    */
   public void removeAddedFilters(int... indices)
   {      
      int len = indices.length;

      List <StandardBinaryOp> addedFilters = filterListCombo.getAddedFilters();
      
      if (indices != null && len != 0){
         for (int  i = len - 1; i >= 0; --i){
            removeFilter(addedFilters.get(i));
         }

         removeAddedFilterPath(indices);
      }
      
   }

   /**
    * Removes the specified filter from the list of added filter
    * @param filter
    * @return true if the filter is removed, otherwise false
    */
   public boolean removeFilter(StandardBinaryOp filter)
   {
      boolean updateSelection = false;
      boolean retVal = false;

      int index = getFilterIndex(filter);

      if (index != -1) {
         JMenuItem menuItem = filterMenuItems.get(index);
         retVal = filterMenuItems.remove(menuItem);

         if (retVal) {

            if (currentFilter == filter)
               updateSelection = true;

            filtersMenu.remove(menuItem);

            filterListCombo.removeItem(filter);

            if (updateSelection && filterListCombo.getItemCount() != 0)
               setCurrentFilter(0);

         } else retVal = false;
      }
      if (filterListCombo.getAddedFilters().isEmpty())
         filterRemove.setEnabled(false);

      return retVal;
   }

   /**
    * Sets the currently active filter
    * @param index the index in the list of filters
    */
   public void setCurrentFilter(int index)
   {
      // Synchronize selected menu item with the current filter
      if (index == -1)
         return;

      StandardBinaryOp selectedFilter = (StandardBinaryOp) filterListCombo.getItemAt(index);

      if ( currentFilter != selectedFilter && selectedFilter != null){

         // creates a new  toolbar only if it is the filter's toolbar is null
         addNewToolbar(selectedFilter, false);

         currentFilter = selectedFilter;

         filterMenuItems.get(index).setSelected(true);
         filterListCombo.setSelectedIndex(index);

      }

   }

   /**
    * Sets the currently active filter to the specified filter
    * @param filter
    */
   public void setCurrentFilter(StandardBinaryOp filter)
   {
      setCurrentFilter(getFilterIndex(filter));
   }

   public List <StandardBinaryOp> getAddedFilters() { return filterListCombo.getAddedFilters(); }

   /**
    *
    * @param filter
    * @return the index of the filter in the menu items,; or-1 if the filter doesn't exist
    */
   public int getFilterIndex(StandardBinaryOp filter)
   {  
      if (filter != null) {
         int size = filterListCombo.getItemCount();

         int index = size - 1;

         for (; index >= 0; --index)
         {
            if (((StandardBinaryOp) filterListCombo.getItemAt(index)).getName().
                    equals(filter.getName()))
               return index;
         }
      }
      
      return -1;

   }

   /**
    * Valid parameters must have a non-null value
    * @param op the StandardBinaryOp object whose parameter values are checked for validity
    * @return true if all the parameters are valid, otherwise false
    */
   private boolean isParametersValid(StandardBinaryOp op)
   {

      Parameter[] parameters = op.getParameters();

      if (parameters != null)
         for (Parameter p : parameters) {            
            if (p.value == null) {
               JOptionPane.showMessageDialog(null,
                       "Some parameter value is out of range!",
                       "Filter operation cancelled",
                       JOptionPane.WARNING_MESSAGE);

               return false;
            }
         }
      return true;
   }

   /**
    * Replaces the current toolbar with another, or creates a new one
    * @param filter
    * @param forceCreateNew if true then a new toolbar is always create
    *                       if false a new toolbar is created only if the
    *                       <code>filter</code>'s toolbar is <code>null</code>
    */
   private void addNewToolbar(StandardBinaryOp filter, boolean forceCreateNew)
   {      
      AbstractParameterToolbar oldToolbar = null;
      if (currentFilter != null)
         oldToolbar = currentFilter.getToolbar();

      AbstractParameterToolbar toolbar = filter.getToolbar();

      // If the current filter class does not specify a toolbar, create one:
      if (forceCreateNew || toolbar == null) {

         String[] parameterNames = filter.getParameterNames();

         if (parameterNames != null && parameterNames.length != 0) {
            ParameterLister lister = new ParameterLister(parameterNames);

            // Don't care about returnValue
            lister.showDialog(this, "Create Parameter Fields");
            Parameter[] params = lister.getFields();

            if (params != null && params.length != 0) {
               // Create toolbar
               toolbar = new DefaultParameterToolbar(filter.getName());
               for (Parameter param : params) {
                  toolbar.createParameter(param.name, param.desc, param.type,
                          param.min, param.max, param.value);
               }

               filter.setToolbar(toolbar);

            }
         }
      }

      if (oldToolbar != null)
         currentFilterToolbar.remove(oldToolbar);

      if (toolbar != null)
         currentFilterToolbar.add(toolbar, 0);

      canvas.setPreferredSize(calculateCanvasSize());
         //validate();
         //pack();
   }

   /**
    * This method manipulates events relating to the File menu
    * @param e ActionEvent relating the the menu items in the File menu
    */
   public void actionPerformed(java.awt.event.ActionEvent e)
   {
      Object src = e.getSource();

      // Open an image file
      if (src == fileOpen) {
         
         fileUI.doOperation(FileOperationsUI.OPEN_FILE);

      // Save the image file that is displayed on the canvas
      } else if (src == fileSave) {
         fileUI.doOperation(FileOperationsUI.SAVE_FILE);

      // Save the image file that is displayed on the canvas, with a different name
      } else if (src == fileSaveAs) {
         fileUI.doOperation(FileOperationsUI.SAVEAS_FILE);

      // Close the image file that is displayed on the canvas
      } else if (src == fileClose) {
         fileUI.doOperation(FileOperationsUI.CLOSE_FILE);

      // Quit the application
      } else if (src == fileQuit) {
         fileUI.doOperation(FileOperationsUI.QUIT);

      // Add a new filter from a JAR file
      } else if (src == filterAdd) {
         fileUI.doOperation(FileOperationsUI.LOAD_FILTER);

      // Reload the image file that is displayed on the canvas
      } else if (src == reloadImage) {
         canvas.reload();
      }

   }


   public void itemStateChanged( ItemEvent e)
   {
       if (e.getStateChange() == ItemEvent.SELECTED){
          
          if (null != currentFilterToolbar && null != filterMenuItems )
            for (int i = 0; i < filterMenuItems.size(); i++) {
               if (filterMenuItems.get(i) == e.getItemSelectable()) {
                  setCurrentFilter( i );
                  return;
               }
            }
         
      }
   }

   public static final String BATCH_FILTERING_PROPERTY = "batchFiltering";

   public void propertyChange(PropertyChangeEvent e)
   {
      String prop = e.getPropertyName();
      
      if (prop.equals(Canvas.CONTENT_CHANGED_PROPERTY)){
         switch ((Canvas.CONTENT_CHANGED_VALUE) e.getOldValue()){
            case EMPTY:
               if ( ((Canvas.CONTENT_CHANGED_VALUE) e.getNewValue())
                       .equals(Canvas.CONTENT_CHANGED_VALUE.NONEMPTY)) {

                  fileSave.setEnabled(false);
                  fileSaveAs.setEnabled(true);
                  fileClose.setEnabled(true);
                  //viewZoom.setEnabled(true);                  

                  applyCurrentFilter.setEnabled(true);
                  //viewRaster.setEnabled(true);
                  reloadImage.setEnabled(true);

                  feedbackCombo.setEnabled(true);
               }
               break;

            case NONEMPTY:
               if ( ((Canvas.CONTENT_CHANGED_VALUE) e.getNewValue())
                       .equals(Canvas.CONTENT_CHANGED_VALUE.EMPTY) ){

                  fileSave.setEnabled(false);
                  fileSaveAs.setEnabled(false);
                  fileClose.setEnabled(false);

                  // viewZoom.setEnabled(false);                  

                  applyCurrentFilter.setEnabled(false);
                  //viewRaster.setEnabled(false);
                  reloadImage.setEnabled(false);

                  feedbackCombo.setEnabled(false);
               }
               break;
         }

      }
   }

   public void setBatchFilteringEnabled(boolean enable)
   {
      applyBatchFilter.setEnabled(enable);
   }

   
   /* ************** Multi-thread manipulation ************** */
   /** Keeps all the running threads */
   private static List<Task> tasks = new ArrayList<Task>(3);
   
   /**
    * Adds a task to the list of running tasks
    * @param task
    */
   public static void addTask(Task task){
      tasks.add(task);
      task.addPropertyChangeListener(new TaskPropertyChangeListener());
   }

   /**
    * Cancels a task based on its name
    * @param taskName the name of the task to be cancelled
    *        Note: if more than one tasks have the same name then the
    *              one found first is cancelled
    */
   public static void cancelTask(String taskName)
   {
      Task task = null;
      for (Iterator<Task> it = tasks.iterator(); it.hasNext();) {
         task = it.next();
         if (taskName.equals(task.getName()))
            break;
         else
            task = null;

      }
      
      if (task != null){         
         task.cancel(false);
      }
         
   }

   /**
    * The PropertyChangeListener for manipulating the removal of a task
    * after it is done.
    */
   private static class TaskPropertyChangeListener implements PropertyChangeListener
   {

      public void propertyChange(PropertyChangeEvent e)
      {
         String prop = e.getPropertyName();
         if (e.getPropertyName().equals("state")
                 && e.getNewValue() == SwingWorker.StateValue.DONE) {
            
            tasks.remove((Task) e.getSource());
         }
      }
   }
   /* ******************************************************* */

   public static void main(String[] args)
   {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            //Turn off metal's use of bold fonts
            //UIManager.put("swing.boldMetal", Boolean.FALSE);
            new Main();
         }
      });
   }
}
