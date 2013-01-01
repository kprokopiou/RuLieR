package plsdejai;

import java.awt.Transparency;
import java.awt.GraphicsEnvironment;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import static plsdejai.io.ImageIO.fileLoad;


/**
 * The <code>BinaryImageComponent</code> includes methods for creating
 * and manipulating BufferedImage objects. Its main purpose is to
 * create a component that will become the current content of
 * a <code>Canvas</code> object
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 18/02/2012
 */
public class BinaryImageComponent extends JComponent
        implements Scrollable, MouseMotionListener
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   private BufferedImage bi;
   private AffineTransform affineTransform;

   /**
    * Constructor
    * @param f the file that contains the image
    */
   public BinaryImageComponent(File f) { this(f, 1.0); }

   /**
    * Constructor
    * @param f the file that contains the image
    * @param zoom the zoom value
    */
   public BinaryImageComponent(File f, double zoom)
   {
      RenderedImage ri = null;
      boolean isFileError = false;

      if (f != null) {
         ri = fileLoad(f);

         if (ri == null){
            isFileError = true;

         } else {
            
            if (ri instanceof BufferedImage){
               this.bi = (BufferedImage) ri;

            } else {

               isFileError = true;
            }

         }

      }

      // Manipulates the case that a null BufferedImage is returned
      if (f == null || isFileError) {         

         this.bi = null; // Make sure that a null bi is returned
         repaint();
         
      } else {
         setAffineTransform(zoom);
      }
     
   }

   /**
    * Constructor
    * @param bi the image
    * @param zoom the zoom value
    */
   public BinaryImageComponent(BufferedImage bi, double zoom )
   {
      this.bi = bi;
      setAffineTransform(zoom);
   }
   
   
   {
      //Let the user scroll by dragging to outside the window.
      setAutoscrolls(true); //enable synthetic drag events
      addMouseMotionListener(this); //handle mouse drags
   }

   /**
    * Converts the zoom value to the appropriate AffineTransform object.
    * @param zoom the zoom value
    */
   public final void setAffineTransform (double zoom)
   {      
      this.affineTransform = AffineTransform.getScaleInstance(zoom, zoom);
      revalidate();
      repaint();
   }

   /**
    * @return the BufferedImage object of this BinaryImageComponent object
    */
   public BufferedImage getBufferedImage() { return bi; }

   private int maxUnitIncrement = 1;
   /**
    * Set the maximum number of pixels that the image will be scrolled, each time
    * the arrow on a scroll bar is pressed
    * @param pixels number of pixels to be scrolled
    */
   public void setMaxUnitIncrement(int pixels) { maxUnitIncrement = pixels; }
   
   /* ************ @Override JComponent methods ************** */
   /**
    * @see method <code>getPreferredSize</code> of <code>java.awt.Component</code>
    */
   public Dimension getPreferredSize()
   {
      Dimension d = null;
      if (bi == null)
         d = new Dimension(800, 600);
      else
         d = new Dimension(
                 (int)( (this.bi.getWidth())  * affineTransform.getScaleX() ),
                 (int)( (this.bi.getHeight()) * affineTransform.getScaleY() ) );

      return d;
   }  

   /**
    * @see method <code>paintComponent</code> of <code>javax.swing.JComponent</code>
    */
   public void paintComponent(Graphics g) {
      super.paintComponent(g);

      if ( bi == null)
         return;

      if (getSize().width <= 0 || getSize().height <= 0)
	return;

      Graphics2D g2d = (Graphics2D) g;

      AffineTransform savedTranform = g2d.getTransform();
      RenderingHints savedRenderingHints =g2d.getRenderingHints();
      g2d.transform(affineTransform);
      Map<RenderingHints.Key,Object> hints = new HashMap<RenderingHints.Key,Object>(1);      
      hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);      
      g2d.addRenderingHints(hints);
      
      Rectangle r = g2d.getClipBounds();

      if (bi != null && isShowing())        
         g2d.drawImage(this.bi, r.x, r.y, r.x + r.width,
                 r.y + r.height,
                 r.x, r.y,r.x + r.width, r.y + r.height, this);      

      g2d.setTransform(savedTranform);
      g2d.setRenderingHints(savedRenderingHints);
    }
   /* ********************************************************** */

   /* ************ @Implements MouseMotionListener methods ***** */
   /**
    * @see method <code>mouseMoved</code> of <code>java.awt.event.MouseMotionListener</code>
    */
   public void mouseMoved(MouseEvent e){ }

   /**
    * This method accompanies the <code>setAutoscrolls(true);</code>
    * @see method <code>mouseDragged</code> of <code>java.awt.event.MouseMotionListener</code>
    */
   public void mouseDragged(MouseEvent e)
   {  //The user is dragging us, so scroll!
      scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));
   }
   /* ********************************************************** */

   /* ************ @Implements Scrollable methods ************** */
   /**
    * @see method <code>getPreferredScrollableViewportSize</code> of
    * <code>javax.swing.Scrollable</code>
    */
   public Dimension getPreferredScrollableViewportSize()
   {
      return getPreferredSize();
   }

   /**
    * @see method <code>getScrollableUnitIncrement</code> of
    * <code>javax.swing.Scrollable</code>
    */
   public int getScrollableUnitIncrement(Rectangle visibleRect,
           int orientation, int direction)
   {  //Get the current position.
      int currentPosition = 0;
      if (orientation == SwingConstants.HORIZONTAL)
         currentPosition = visibleRect.x;
      else
         currentPosition = visibleRect.y;

      //Return the number of pixels between currentPosition
      //and the nearest tick mark in the indicated direction.
      if (direction < 0) {
         int newPosition = currentPosition
                 - (currentPosition / maxUnitIncrement)
                 * maxUnitIncrement;
         return (newPosition == 0) ? maxUnitIncrement : newPosition;
      } else
         return ((currentPosition / maxUnitIncrement) + 1)
                 * maxUnitIncrement
                 - currentPosition;
   }

   /**
    * @see method <code>getScrollableBlockIncrement</code> of
    * <code>javax.swing.Scrollable</code>
    */
   public int getScrollableBlockIncrement(Rectangle visibleRect,
           int orientation, int direction)
   {
      if (orientation == SwingConstants.HORIZONTAL)
         return visibleRect.width - maxUnitIncrement;
      else
         return visibleRect.height - maxUnitIncrement;
   }

   /**
    * @see method <code>getScrollableTracksViewportWidth</code> of
    * <code>javax.swing.Scrollable</code>
    */
   public boolean getScrollableTracksViewportWidth()
   {  // Return true if a viewport should always force the height of
      // this Scrollable to match the height of the viewport.
      return false;
   }

   /**
    * @see method <code>getScrollableTracksViewportHeight</code> of
    * <code>javax.swing.Scrollable</code>
    */
   public boolean getScrollableTracksViewportHeight()
   {  // Return true if a viewport should always force the height of
      // this Scrollable to match the height of the viewport.
      return false;
   }
   /* ********************************************************** */
   

}
