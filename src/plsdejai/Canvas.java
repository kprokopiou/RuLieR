package plsdejai;

import java.awt.Insets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import plsdejai.widgets.ButtonFactory;

/**
 * The <code>Canvas</code> class is the part of the <code>Main</code> class
 * window, on which an image is shown. It takes care of basic image manipulations
 * such such as changing the canvas size and the zooming operations.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 18/02/2012
 */
public class Canvas extends JPanel
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   /** Constant that is used by the property change listener to denote that
    *  the content of the canvas has changed */
   public static final String CONTENT_CHANGED_PROPERTY = "CONTENT_CHANGED_PROPERTY";
   /** The values accepted by the <code>CONTENT_CHANGED_PROPERTY</code> */
   public static enum CONTENT_CHANGED_VALUE
   {
      NONEMPTY, // Content is changed from null to non-null
      EMPTY // Content is changed from non-null to null"
   }

   /**
    * The content image of the canvas, is the image at the time when the
    *   image file is opened.
    * The content is always not null, even when no file is opened
    *   a default BinaryImageComponent content is shown
    */
   private BinaryImageComponent content;

   /** The file where the image shown on the canvas is stored.
    *  If contentFile is null, then the canvas method <code>isEmpty</code>
    *  returns true, and a default BinaryImageComponent content is shown
    */
   private File contentFile;
   /**
    * A container component that contains the image shown on the canvas
    * and permits scrolling operations
    */
   private JScrollPane scrollPane;

   /** The current zoom value of the image shown on the canvas */
   private double zoom = 1.0;

   /** An object of the class <code>UndoUtil</code> that permits
    *  undo/redo operations during successive filtering operations */
   private UndoUtil<BinaryImageComponent> undo;

   /**
    * Constructor
    * @param zoom the current zoom value of the image shown on the canvas
    */
   public Canvas(double zoom)
   {
      this.undo = new UndoUtil<BinaryImageComponent>();
      this.setZoom(zoom);

      this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
      this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      this.setOpaque(true);

      //Set up the scroll pane.
      scrollPane = new JScrollPane();
      //pictureScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      //pictureScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      scrollPane.setPreferredSize(new Dimension(this.getPreferredSize()));
      scrollPane.setViewportBorder(
              BorderFactory.createLineBorder(Color.black));

      // Set up the toolbar that is shown at the top of the canvas
      setCanvasToolbar();      

      this.add(scrollPane);

      undo.addPropertyChangeListener(new PropertyChangeListener()
      {
         public void propertyChange(PropertyChangeEvent evt)
         {
            String prop = evt.getPropertyName();
            if (prop == UndoUtil.UNDO_LIST_SIZE_CHANGED_PROPERTY){
               Integer newSize = (Integer) evt.getNewValue();
               undoButton.setEnabled(newSize.intValue() != 0);

            } else if (prop == UndoUtil.REDO_LIST_SIZE_CHANGED_PROPERTY){
               Integer newSize = (Integer) evt.getNewValue();
               redoButton.setEnabled(newSize.intValue() != 0);

            }
         }
      });
   }

   /**
    * Constructor
    * @param f the file containing the image to be the content of the canvas
    * @param zoom the current zoom value of the image shown on the canvas
    */
   public Canvas(File f, double zoom)
   {
      this(zoom);
      setContent(f);        
   }

    public Canvas(File f) { this(f, 1.0); }

   /**
    * Sets the current zoom value of the image shown on the canvas
    * @param zoom the current zoom value of the image shown on the canvas
    *      Note: setting a new zoom value, automatically displays the scaled image
    */
   public final void setZoom(double zoom) { this.zoom = zoom; }

   /**
    * @return the current zoom value of the image shown on the canvas
    */
   public double getZoom() { return zoom; }

   /**
    * @return the Dimension of the area to display an image
    *         If a BinaryImageComponent is greater than this area scrollbars will show
    */
   public Dimension getViewSize()
   {
      Dimension size = scrollPane.getSize();
      Insets ins = scrollPane.getInsets();

      int insWidth = ins.left + ins.right + 3;
      int insHeight = ins.bottom + ins.top + 3;

      return new Dimension( size.width - insWidth, size.height - insHeight );
   }
   

   /**
    * Sets the current content of the canvas using the image that is opened from
    * a file
    * @param f the file that contains the image
    */
   public final void setContent(File f)
   {      
      // If f = null, then an empty BinaryImageComponent is created
      BinaryImageComponent newContent = new BinaryImageComponent(f, zoom/*, columnView.getIncrement() */);

      if(null == newContent.getBufferedImage()){
         if (f != null){
            f = null;
            newContent = new BinaryImageComponent( (File) null, zoom);
         }

         scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      } else {
         scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
         scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      }      

      if(null == this.contentFile && null != f) {
         firePropertyChange(CONTENT_CHANGED_PROPERTY,
                 CONTENT_CHANGED_VALUE.EMPTY, CONTENT_CHANGED_VALUE.NONEMPTY);

      } else if (null == f && null != this.contentFile) {
            firePropertyChange(CONTENT_CHANGED_PROPERTY,
                 CONTENT_CHANGED_VALUE.NONEMPTY, CONTENT_CHANGED_VALUE.EMPTY);
      }      

      this.content = newContent;

      this.contentFile = f;

      undo.clear();

      if (f == null)
         scrollPane.setColumnHeaderView(null);
      else{

         imageSizeLabel.setText("Image size = [" + this.getContent().getBufferedImage().getWidth()
                 + " x " +  this.getContent().getBufferedImage().getHeight() +"]");

         scrollPane.setColumnHeaderView(canvasToolbar);
      }
         
      applyBinaryImageComponent(newContent, true, false);
      
   }

   /**
    * Throws away any products that was produced as the result of filtering,
    * and displays the content (i.e. the image at the time it was opened)
    */
   public void reload()
   {      
      if (getCurrentContent() != content)
         applyBinaryImageComponent(getContent(), true, false);

      undo.clear();

      // Omitting this line the reloading shows the image in the current zoom
      zoom(1.0);

   }

   /**
    * Adds a new image to the undo list, and displays it. Content is unchanged
    * The most useful combinations are:
    *   isViewPort isUndoable
    *   true        true           display an image that is the result of a filtering operation
    *   true        false          display a scaled image of the current image
    * @param bic the image component that is requested to be displayed on the canvas
    * @param isViewPort if true the image is displayed on the canvas
    * @param isUndoable it true the image is added to the list of undoable images
    */
   private void applyBinaryImageComponent(BinaryImageComponent bic,
           boolean isViewPort, boolean isUndoable)
   {
      if (bic == null) {
         bic = new BinaryImageComponent( (File) null, zoom/*, columnView.getIncrement() */);
      }
      

      if (isViewPort) {         
         scrollPane.setViewportView(bic);
         
         bic.revalidate();
         bic.repaint();         

         System.gc();
      }

      if (isUndoable)
         undo.add(bic);

   }

   /**
    * Sets the current zoom of the canvas and also diplays the current image
    * using the new zoom value
    * @param zoom the new zoom value of the canvas
    */
   public void zoom(double zoom)
   {      
      setZoom(zoom);
      BinaryImageComponent ri = getCurrentContent();
      ri.setAffineTransform(zoom);

      ri.revalidate();
      ri.repaint();
   }

   /**
    * A convenience method for displaying an image on the canvas. It delegates
    * the operation to method <code>applyBinaryImageComponent</code>
    * @param bi  the image that is requested to be displayed on the canvas
    */
   public void displayFilteredImage(BufferedImage bi )
   {
      applyBinaryImageComponent(new BinaryImageComponent(bi, zoom), true, true);
   }

   /**
    * @return true if the image shown on the canvas is modified by some
    * filtering operation
    */
   public boolean isContentModified() { return isUndoAllowed(); }

   /**
    * @return true if the undo list is not empty, otherwise false
    */
   public boolean isUndoAllowed()
   {
      if (undo.hasMoreUndo())
         return true;
      return false;
   }

   /**
    * @return true if the redo list is not empty, otherwise false
    */
   public boolean isRedoAllowed()
   {
      if (undo.hasMoreRedo())
         return true;
      return false;
   }

   /**
    * @return true if the canvas content is empty, otherwise false
    */
   public boolean isEmpty()
   {
      if (contentFile == null)
         return true;
      return false;
   }

   /**
    * @return the file where the image shown on the canvas is stored.
    */
   public File getContentFile() { return contentFile; }

   /**
    * @return the image component at the time the image file is opened
    */
   public BinaryImageComponent getContent() { return this.content; }

   /**
    * @return the image component that is currently shown on the canvas
    */
   public BinaryImageComponent getCurrentContent()
   { // This is the most recent form of the image, which is the
      // last element in the undo list
      if (undo.hasMoreUndo())
         return undo.getLastUndoElement();
      else
         return this.content;
   }

   /**
    * Saves an image to a file
    * @param content the image component to be saved
    * @param f the file where the image will be saved
    * @param replaceFile : if true replace file, if exists, without confirmation
    *                      if false, ask for confirmation
    */
   public void save(BinaryImageComponent content, File f, boolean replaceFile)
   {
      if (content == null) {
         JOptionPane.showMessageDialog(null, "Error! The content is empty\n"
                 + "Nothing is written to '" + f + "'"
                 + "Code Error!!!!" + JOptionPane.ERROR_MESSAGE);
         return;
      }

      try {
         plsdejai.io.ImageIO.fileStore(content.getBufferedImage(), f, replaceFile);
      } catch (Exception e) {
         JOptionPane.showMessageDialog(Canvas.this, "Error! Could not save image"
                 + " to '" + f.toString() + "'");
         return;
      }

      // The save action sets the new content and drops any undo elements
      this.content = content;
      undo.clear();
   }   

   // The canvas toolbar buttons
   private JPanel canvasToolbar;
   private JButton undoButton;
   private JButton redoButton;
   private JLabel imageSizeLabel;
   private JTextField proccessTimeFld;

   /**
    * Sets up the buttons displayed on the top of the canvas, and are
    * used by the user to execute the basic operations of zooming, undo and redo
    */
   private void setCanvasToolbar()
   { 
      canvasToolbar = new JPanel();
      canvasToolbar.setLayout(new BoxLayout(canvasToolbar, BoxLayout.LINE_AXIS));

      JPanel zoomToolbar = new JPanel();
      zoomToolbar.setLayout(new BoxLayout(zoomToolbar, BoxLayout.LINE_AXIS));
      zoomToolbar.setBorder(BorderFactory.createEmptyBorder());
      canvasToolbar.add(zoomToolbar);

      zoomToolbar.add(Box.createHorizontalStrut(6));

      imageSizeLabel = new JLabel();
      zoomToolbar.add(imageSizeLabel);
      zoomToolbar.add(Box.createHorizontalStrut(4));

      String path = "plsdejai/resources/images/";
      JButton zoomIn = ButtonFactory.createButton(
              null, path + "zoomin_24.png", zoomToolbar);
      zoomIn.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            zoom(getZoom() * 1.25);
         }
      });
      
      JButton zoomOut = ButtonFactory.createButton(
              null, path + "zoomout_24.png", zoomToolbar);
      zoomOut.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            zoom(getZoom() * 0.8);
         }
      });
      zoomToolbar.add(Box.createHorizontalStrut(4));

      JButton zoomFitWidth = ButtonFactory.createButton(
              null, path + "zoomfitwidth_24.png", zoomToolbar);
      zoomFitWidth.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            Dimension viewSize = getViewSize();

            Rectangle imgBounds = getCurrentContent().getBufferedImage().getRaster().getBounds();
            viewSize = getViewSize();

            int correction = 15;

            imgBounds = getCurrentContent().getBufferedImage().getRaster().getBounds();
            double zoomValue =  (double) ( viewSize.width - correction) / imgBounds.getWidth();
            zoom(zoomValue);
         }
      });
      zoomToolbar.add(Box.createHorizontalStrut(4));

      JButton zoomFitPage = ButtonFactory.createButton(
              null, path + "zoomfitpage_24.png", zoomToolbar);
      zoomFitPage.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            Dimension viewSize = getViewSize();

            Rectangle imgBounds = getCurrentContent().getBufferedImage().getRaster().getBounds();
            viewSize = getViewSize();

            int correction = 15;

            imgBounds = getCurrentContent().getBufferedImage().getRaster().getBounds();
            double zoomValue = Math.min(
                    (double) ( viewSize.width - correction) / imgBounds.getWidth(),
                    (double) (viewSize.height - correction) / imgBounds.getHeight());
            zoom(zoomValue);
         }
      });
      zoomToolbar.add(Box.createHorizontalStrut(4));

      JButton zoomCustom = ButtonFactory.createButton(
              "Zoom ...", null, zoomToolbar);
      zoomCustom.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            new  plsdejai.widgets.ZoomDialog(
                    (Main) javax.swing.SwingUtilities.getWindowAncestor(Canvas.this));
         }
      });
      zoomToolbar.add(Box.createHorizontalStrut(6));

      maxPreferredHeight = 0;
      maxPreferredWidth = 0;
      calcBestFitSize(zoomIn);
      //calcBestFitSize(zoomOut);
      //calcBestFitSize(zoomFitWidth);
      //calcBestFitSize(zoomFitPage);
      calcBestFitSize(zoomCustom);
      Dimension d = new Dimension(maxPreferredWidth, maxPreferredHeight);
      zoomCustom.setMinimumSize(d);
      zoomCustom.setMaximumSize(d);      

      JPanel undoToolbar = new JPanel();
      undoToolbar.setLayout(new BoxLayout(undoToolbar, BoxLayout.LINE_AXIS));
      canvasToolbar.add(undoToolbar);

      undoToolbar.add(Box.createHorizontalStrut(6));

      undoButton = ButtonFactory.createButton(
              null, path + "edit-undo-8.png", undoToolbar);
      undoButton.setEnabled(isUndoAllowed());
      undoButton.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {
            undo.undo();
            applyBinaryImageComponent(getCurrentContent(), true, false);
         }
      });

      undoToolbar.add(Box.createHorizontalStrut(4));

      redoButton = ButtonFactory.createButton(
              null, path + "edit-redo-8.png", undoToolbar);
      redoButton.setEnabled(isRedoAllowed());
      redoButton.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {
            undo.redo();
            applyBinaryImageComponent(getCurrentContent(), true, false);
         }
      });

      undoToolbar.add(Box.createHorizontalStrut(6));

      JPanel processTimeToolbar = new JPanel();
      processTimeToolbar.setLayout(new BoxLayout(processTimeToolbar, BoxLayout.LINE_AXIS));
      JLabel proccessTimeLabel = new JLabel("Process time: ");
      proccessTimeFld = new JTextField(6);
      proccessTimeFld.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      proccessTimeFld.setEditable(false);

      processTimeToolbar.add(proccessTimeLabel);
      processTimeToolbar.add(proccessTimeFld);
      canvasToolbar.add(processTimeToolbar);

      canvasToolbar.add(Box.createHorizontalGlue());
   }

   /**
    * Sets the process time in the process time field
    * @param msg the process time
    */
   public void setProcessTime(String msg)
   {
      proccessTimeFld.setText(msg);
   }

   private static int maxPreferredHeight = 0;
   private static int maxPreferredWidth = 0;

   /**
    * Calculates the best fitting size for the buttons of the toolbar
    * shown at the top of the canvas
    * @param button the button for which the best fitting size is calculated
    */
   private void calcBestFitSize (JButton button)
   {
      Dimension size = button.getPreferredSize();
      int w = size.width;
      int h = size.height;

      if (w > maxPreferredWidth)
         maxPreferredWidth = w;

      if (h > maxPreferredHeight)
         maxPreferredHeight = h;
   }


}

/**
 * The <code>UndoUtil</code> class keeps an undo and a redo list:
 *  the undo list keeps Elements that can be undo-ed
 *  the redo list keeps Elements that can be redo-ed
 *
 * The undo() method removes the last element from the undo list
 *    and adds it as the last element in the redo list
 * The redo() method removes the last element from the redo list
 *    and adds it as the last element in the redo list
 * The add() method adds a new element to the undo list
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 18/02/2012
 */
class UndoUtil<Element>
{
   /** An object that adds support for property change events */
   private PropertyChangeSupport change;

   /** A property change value that is fired when the undo list size is changed */
   public static String UNDO_LIST_SIZE_CHANGED_PROPERTY = "UndoListSizeChanged";
   /** A property change value that is fired when the redo list size is changed */
   public static String REDO_LIST_SIZE_CHANGED_PROPERTY = "RedoListSizeChanged";

   /** Default value of the number of modifications that are kept in memory */
   private final int MAX_NUM_OF_ACTIONS = 1;
   // The data structure that keeps the list of undoable elements
   private java.util.ArrayList<Element> undoList =
           new java.util.ArrayList<Element>(MAX_NUM_OF_ACTIONS);
   // The data structure that keeps the list of redoable elements
   private java.util.ArrayList<Element> redoList =
           new java.util.ArrayList<Element>(MAX_NUM_OF_ACTIONS);

   /**
    * Constructor
    */
   public UndoUtil()
   {
      change = new PropertyChangeSupport(this);
   }

   /**
    * A wrapper for the PropertyChangeSupport method <code>addPropertyChangeListener</code>
    * @param l the PropertyChangeListener object
    */
   public void addPropertyChangeListener(PropertyChangeListener l)
   {
      change.addPropertyChangeListener(l);
   }

   /**
    * @return the last element added in the undo list
    */
   public Element getLastUndoElement()
   {
      return undoList.get(undoList.size() - 1);
   }

   /**
    * @return the last element added in the redo list
    */
   public Element getLastRedoElement()
   {
      return redoList.get(redoList.size() - 1);
   }

   /**
    * @return the first element added in the undo list
    */
   public Element getFirstUndoElement()
   {
      return undoList.get(0);
   }

   /**
    * @return the first element added in the redo list
    */
   public Element getFirstRedoElement()
   {
      return redoList.get(0);
   }

   /**
    * @return true if the undo list is not empty, otherwise false
    */
   public boolean hasMoreUndo()
   {
      if (undoList.isEmpty() || MAX_NUM_OF_ACTIONS <= 0)
         return false;
      return true;
   }

   /**
    * @return true if the redo list is not empty, otherwise false
    */
   public boolean hasMoreRedo()
   {
      if (redoList.isEmpty() || MAX_NUM_OF_ACTIONS <= 0)
         return false;
      return true;
   }

   /**
    * @return the undo list
    */
   public java.util.ArrayList<Element> getUndoList()
   {
      return undoList;
   }

   /**
    * @return the redo list
    */
   public java.util.ArrayList<Element> getRedoList()
   {
      return redoList;
   }

   /**
    * Adds a new element in the undo list
    * @param elem the element to be added in the undo list
    */
   public void add(Element elem)
   {
      if (MAX_NUM_OF_ACTIONS <= 0)
         return;

      int oldUndoSize = undoList.size();
      int oldRedoSize = redoList.size();

      if (oldUndoSize + oldRedoSize == MAX_NUM_OF_ACTIONS) {
         // Remove the oldest element, in order to honor MAX_NUM_OF_ACTIONS

         if (oldUndoSize != 0){
            undoList.remove(0);
            if (! undoList.add(elem) )
               change.firePropertyChange(UNDO_LIST_SIZE_CHANGED_PROPERTY,
                       Integer.valueOf(oldUndoSize), Integer.valueOf(oldUndoSize - 1));
         } else {
            redoList.remove(0);
            change.firePropertyChange(REDO_LIST_SIZE_CHANGED_PROPERTY,
                       Integer.valueOf(oldRedoSize), Integer.valueOf(oldRedoSize - 1));

            if (undoList.add(elem) )
               change.firePropertyChange(UNDO_LIST_SIZE_CHANGED_PROPERTY,
                       Integer.valueOf(oldUndoSize), Integer.valueOf(oldUndoSize + 1));
         }

      } else {
         if (undoList.add(elem) )
               change.firePropertyChange(UNDO_LIST_SIZE_CHANGED_PROPERTY,
                       Integer.valueOf(oldUndoSize), Integer.valueOf(oldUndoSize + 1));
      }
      
   }

   /**
    * Removes an element from the undo list to the redo list
    * @return true if the undo operation succeeded, otherwise false
    */
   public boolean undo()
   {
      int oldUndoSize = undoList.size();
      int oldRedoSize = redoList.size();

      boolean retVal = redoList.add(undoList.remove(oldUndoSize - 1));

      if(retVal){
         change.firePropertyChange(UNDO_LIST_SIZE_CHANGED_PROPERTY,
                    oldUndoSize, oldUndoSize - 1);
         change.firePropertyChange(REDO_LIST_SIZE_CHANGED_PROPERTY,
                    oldRedoSize, oldRedoSize + 1);
      }
      return retVal;
   }

   /**
    * Removes an element from the redo list to the undo list
    * @return true if the redo operation succeeded, otherwise false
    */
   public boolean redo()
   {
      int oldUndoSize = undoList.size();
      int oldRedoSize = redoList.size();

      boolean retVal = undoList.add(redoList.remove(oldRedoSize - 1));

      if(retVal){
         change.firePropertyChange(UNDO_LIST_SIZE_CHANGED_PROPERTY,
                    oldUndoSize, oldUndoSize + 1);
         change.firePropertyChange(REDO_LIST_SIZE_CHANGED_PROPERTY,
                    oldRedoSize, oldRedoSize - 1);
      }
      return retVal;
   }

   /**
    * // Remove all elements, and empty the undo and redo lists
    */
   public void clear()
   { 
      int oldUndoSize = undoList.size();
      int oldRedoSize = redoList.size();
      undoList.clear();
      redoList.clear();

      if (oldUndoSize != 0)
         change.firePropertyChange(UNDO_LIST_SIZE_CHANGED_PROPERTY,
                    oldUndoSize, 0);
      if (oldRedoSize != 0)
         change.firePropertyChange(REDO_LIST_SIZE_CHANGED_PROPERTY,
                    oldRedoSize, 0);
      System.gc();
   }
}
