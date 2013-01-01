package plsdejai;

import java.awt.Graphics;
import java.awt.image.ImagingOpException;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;
import javax.swing.SwingWorker;
import plsdejai.widgets.AbstractParameterToolbar;

/**
 * <p>Class <code>StandardBinaryOp</code> is designed to be the parent of all classes
 * that are used as filters for binary images.</p>
 * <p>It implements Java core interfaces <code>RasterOp</code> and
 * <code>BufferedImageOp</code>, which perform single-input/single-output
 * operations on <code>Raster</code> and <code>BufferedImage</code> objects.
 * <i>Note that the restriction to single-input operations means that the values
 * of destination pixels prior to the operation are not used as input
 * to the filter operation. </i></p>
 * <p>Classes that implement <code>RasterOp</code> and <code>BufferedImageOp</code> must
 * specify whether or not they allow in-place filtering, i.e. if the
 * <code>src</code> and <code>dst</code> arguments of filter() method can be the same.</p>
 * <p><code>StandardBinaryOp</code> implements the <code>RasterOp</code> and
 * <code>BufferedImageOp</code> interfaces in such a way that any subclass of
 * <code>StandardBinaryOp</code> only have to implement or extend only a few methods:
 * <ul type="disc"><li><code>getName()</code>
 * <li>filter(BufferedImage src, BufferedImage dst)</li>
 * <li>filter(Raster src, Writable dst)</li>
 * <li>getParameterNames</li>
 * <li>clone</clone>
 * </ul>
 * Also, a subclass might override getRenderingHints()
 * 
 * @author Prokopiou Konstantinos
 * @version 1.0, 08/12/2011
 */
public abstract class StandardBinaryOp
        implements BufferedImageOp, RasterOp
{
   // The interpretation of the background and foreground pixels
   // in Java indexed binary images
   public final static int BACKGROUND = 1;
   public final static int FOREGROUND = 0;
   public final static int [] aBACKGROUND = {BACKGROUND};
   public final static int [] aFOREGROUND = {FOREGROUND};

   /**
    * The constructor of any subclass must specify a constructor without any arguments.
    * Its role should be to create a GUI of controls and, also, to set
    * the default values for the extra parameters specified in the subclass filter.
    */
   public StandardBinaryOp(){ }

   /**
    * @return a String, which is a descriptive name for the class
    *         The returned string should have at most than 40 characters
    */
   public abstract String getName();
  
   /**
    * Every subclass must return the member variable names
    * that represent parameter names that it uses to configure the algorithm.
    * @return an array of String of the parameters that are used to adapt the algorithm
    */
   public abstract String[] getParameterNames();

   /**
    * An object of AbstractParameterToolbar is used to draw the toolbar
    * and manipulate user input
    * Note: Subclasses should not hide this field, because it is used from
    *       the final classes setToolbar and getToolbar.
    */
   protected AbstractParameterToolbar toolbar;
   /**
    * Sets the toolbar that is used to show the parameter fields
    * and manipulate user input
    * @param toolbar the AbstractParameterToolbar object
    */
   public final void setToolbar(AbstractParameterToolbar toolbar) { this.toolbar = toolbar; }

   /**
    * An object of AbstractParameterToolbar is used to draw the toolbar
    * and manipulate user input
    * @return an AbstractParameterToolbar object
    */
   public final AbstractParameterToolbar getToolbar() { return toolbar; }

   /**
    * Sets the value for the parameter specified by the name argument
    * by delegating the call to an object of AbstractParameterToolbar
    * @param name the name of the parameter
    * @param value the value of the parameter
    */
   public void setParameterValue(String name, Number value)
   {
      if (toolbar != null)
         toolbar.setParameterValue(name, value);
   }

   /**
    * Gets the value for the parameter specified by the name argument
    * by delegating the call to an object of AbstractParameterToolbar
    * @param name the name of the parameter
    * @return the value of the parameter
    */
   public Number getParameterValue(String name) { return toolbar.getParameterValue(name); }  

   /**
    * This method allows the caller to get the whole set of the parameters that
    * determines the behaviour of the filtering algorithm
    * @return the whole information about the parameters used in the algorithm
    */
   public Parameter[] getParameters()
   {
      if (toolbar != null)
         return toolbar.getParameters();
      else
         return null;
   }


   /**
    * Stores the color model of the input image argument of the <code>filter()</code> function
    */
   private ColorModel cm;

   /**
    * Performs a single-input/single-output operation from a source
    * <code>BufferedImage</code> to a destination <code>BufferedImage</code>.
    * <p>Implementation Restrictions:
    * - If the color models for the two images do not match,
    *   a color conversion into the destination color model is performed.
    * - If the destination image is null, a <code>BufferedImage</code> with
    *    an appropriate <code>ColorModel</code> is created.</p>
    *
    * Specified by: <code>filter</code> in interface <code>BufferedImageOp</code>
    *
    * @param src The <code>BufferedImage</code> to be filtered
    * @param dst The <code>BufferedImage</code> in which to store the results
    *            if null, new <code>BufferedImage</code> will be created
    * @return The filtered <code>BufferedImage</code>.
    * @throws <code>IllegalArgumentException</code> If the source and/or destination image is not
    * compatible with the types of images allowed by the class implementing this filter.
    * <p><code>ImagingOpException</code> if <code>src</code> is not a binary image</p>
    * <p><code>NullPointerException</code> if <code>src</code> is null</p>
    */
   public BufferedImage filter(BufferedImage src, BufferedImage dst)
   {
      if (src == null)
         throw new NullPointerException("src image is null");

      src = change2BinaryColorModel(src);

      if (src == null)
         throw new ImagingOpException("Operation requires an binary image");

      /* Do not allow in-place transformation of src */
      if (src == dst)
         throw new IllegalArgumentException("src image cannot be the "
                 + "same as the dst image");
      
      if (dst == null) {         
         dst = createCompatibleDestImage(src, null);
      }
      
      /* Apply Filter:
       * Modify this line in order to apply a custom transform object */
      // Default: delegate to filter(Raster, WritableRaster) method
      Raster r = filter(src.getRaster(), dst.getRaster());
      if (r != null)
         dst.setData(r);
      else if (task != null && task.isCancelled()){
        dst.getRaster().setPixels(0, 0, dst.getWidth(), dst.getHeight(),
                src.getRaster().getPixels(0, 0, dst.getWidth(), dst.getHeight(), (int[]) null));
      } else
         dst = null;
      /* ************************************************************ */

      /* Optional code that may do further standard operations
       *  ...
       */

      return dst;
   }

   /**
    * Performs a single-input/single-output operation from a source
    * <code>Raster</code> to a destination <code>Raster</code>.
    *
    * Specified by: <code>filter</code> in interface <code>RasterOp</code>
    *
    * @param src The <code>Raster</code> to be filtered
    * @param dst The <code>Raster</code> in which to store the results
    *            if null, new <code>Raster</code> will be created
    * @return a <code>WritableRaster</code> that represents the result of the filtering operation.
    * <p>@throws <code>IllegalArgumentException</code> If the source and/or destination
    * <code>Raster</code> is not compatible with the types of images allowed by
    * the class implementing this filter.</p>
    * <p><code>NullPointerException</code> if <code>src</code> is null</p>
    */
   public WritableRaster filter(Raster src, WritableRaster dst)
   {
      if (src == null)
         throw new NullPointerException("src image is null");

      if (dst == null)
         dst = createCompatibleDestRaster(src);

      /* Do not allow in-place transformation of src */
      if (src == dst)
         throw new IllegalArgumentException("src image cannot be the "
                 + "same as the dst image");

      if (src.getNumBands() != dst.getNumBands()) {
         throw new IllegalArgumentException("Number of src bands ("
                 + src.getNumBands()
                 + ") does not match number of "
                 + " dst bands ("
                 + dst.getNumBands() + ")");
      }

      // Test for matching dimensions
      int width = src.getWidth();
      int height = src.getHeight();
      if (width != dst.getWidth() || height != dst.getHeight()) {
         throw new IllegalArgumentException("src and dst have different dimensions");
      }

      /* Apply Filter:
       * Modify this line in order to apply a custom transform object */
      // Default: just clone copy the values of src raster to dst raster
      dst.setPixels(0, 0, width, height,
              src.getPixels(0, 0, width, height, (int[]) null) );
      /* ************************************************************ */
      return dst;
   }


   /**
    * Specified by: <code>filter</code> in interface <code>BufferedImageOp</code>
    *
    * @param src The source <code>BufferedImage</code>
    * @return a <code>Rectangle2D</code> representing the destination image's bounding box.
    * @throws IllegalArgumentException - If the source and/or destination image is not
    * compatible with the types of images allowed by the class implementing this filter.
    */
   public final Rectangle2D getBounds2D(BufferedImage src)
   {
      return getBounds2D(src.getRaster());
   }

   /**
    * Specified by: <code>filter</code> in interface <code>RasterOp</code>
    *
    * @param src The source <code>Raster</code>
    * @return a <code>Rectangle2D</code> representing the destination <code>Raster</code> bounding box
    * @throws IllegalArgumentException - If the source and/or destination <code>Raster</code> is not
    * compatible with the types of rasters allowed by the class implementing this filter.
    */
   public final Rectangle2D getBounds2D(Raster src)
   {
      return new Rectangle(0, 0, src.getWidth(), src.getHeight());
   }


   /**
    * Specified by: <code>filter</code> in interface <code>BufferedImageOp</code>
    *
    * @param src - The <code>BufferedImage</code> to be filtered
    * @param destCM - ColorModel of the destination. If null, the ColorModel of the source is used.
    * @return a zeroed destination image with the correct size and number of bands.
    * <p>IllegalArgumentException - If the source image is not
    * compatible with the types of images allowed by the class implementing this filter.</p>
    */
   public final BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM)
   {
      BufferedImage dst = null;
      Rectangle r = getBounds2D(src).getBounds();

      int w = r.width;
      int h = r.height;

      if (destCM != null)
         cm = destCM;
      else
         cm = src.getColorModel();


      if (destCM == null) // Specify an appropriate color model, if destCM is null
         dst = new BufferedImage(src.getColorModel(),
                 createCompatibleDestRaster(src.getRaster()),
                 src.isAlphaPremultiplied(), null);
      else
         dst = new BufferedImage(destCM,
                 destCM.createCompatibleWritableRaster(w, h),
                 destCM.isAlphaPremultiplied(), null);

      return dst;
   }

   /**
    * Specified by: <code>filter</code> in interface <code>RasterOp</code>
    *
    * @param src The source <code>Raster</code>
    * @return a zeroed destination <code>Raster</code> with the correct size and number of bands.
    * <p>IllegalArgumentException - If the source <code>Raster</code> is not
    * compatible with the types of images allowed by the class implementing this filter.
    * (for e.g., if the transformed width or height is equal to 0.)</p>
    */
   public final WritableRaster createCompatibleDestRaster(Raster src)
   {
      Rectangle2D r = getBounds2D(src);
      WritableRaster dst = null;
      
      if (cm != null)
         dst = cm.createCompatibleWritableRaster(
                 (int) r.getWidth(), (int) r.getHeight());
      else
         dst = src.createCompatibleWritableRaster((int) r.getX(),
                 (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());

      return dst;
   }

   /**
    * Specified by  <code>BufferedImageOp</code> and <code>RasterOp</code>
    *
    * @param srcPt - The <code>Point2D</code> that represents the point in the source image
    * @param dstPt - If non-null, it is used to hold the return value.
    * @return The location of the corresponding destination point given a point in the source image
    */
   public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt)
   { 
      dstPt = srcPt;
      return dstPt;
   }


   /**
    * Specified by  <code>BufferedImageOp</code> and <code>RasterOp</code>
    *
    * @return The <code>RenderingHints</code> object for this.
    *         The default implementation returns null.
    * <code>BufferedImageOp</code> or <code>RasterOp</code>, or null.
    */
   public RenderingHints getRenderingHints() { return null; }

   /** The thread that executes this object */
   protected SwingWorker task;
   /**
    * Specify the thread that executes this object
    * @param task the thread that executes this object
    */
   public void setTask(SwingWorker task) { this.task = task; }

   /**
    * @return a clone of this object
    * @overrides method clone of <code>java.lang.Object</code>
    */
   public StandardBinaryOp clone()
   {
      StandardBinaryOp op = null;

      try {
         op = this.getClass().getConstructor().newInstance();

         op.setToolbar(this.getToolbar());
         
         Parameter[] params = getParameters();

         for (Parameter param : params) {
            op.setParameterValue(param.name, param.value);
         }

      } catch (InstantiationException e) {
         op = null;
      } catch (NoSuchMethodException e) {
         op = null;
      } catch (IllegalAccessException e) {
         op = null;
      } catch (java.lang.reflect.InvocationTargetException e) {
         op = null;
      }

      return op;
   }

   /**
    * Performs a rotation by 90 degrees to an image
    * @param raster the raster of the image to be rotated
    * @param isClockWise: the direction of rotation.
    * @return a raster that results from the rotation by 90 degrees of the input raster
    */
   protected final WritableRaster rotate90(Raster raster, boolean isClockWise)
   {
      int w = raster.getWidth();
      int h = raster.getHeight();

      WritableRaster rotatedRaster = raster.createCompatibleWritableRaster(h, w);

      if(isClockWise)
         for (int y = 0; y < h; ++y)
            for (int x = 0; x < w; ++x)
               rotatedRaster.setPixel(-y + h - 1, x,
                       raster.getPixel(x, y, (int[]) null));
      else
         for (int y = 0; y < h; ++y)
            for (int x = 0; x < w; ++x)
               rotatedRaster.setPixel(y, -x + w - 1,
                       raster.getPixel(x, y, (int[]) null));

      return rotatedRaster;
   }

   /**
    * Checks an image if it is binary
    * @param bi the image that is checked if it is binary
    * @return true if the image is binary, otherwise false
    */
   public static boolean isBinaryImage(BufferedImage bi)
   {
      if (bi != null)
         if (bi.getType() == BufferedImage.TYPE_BYTE_BINARY)
            return true;
         else {

            int w = bi.getWidth();
            int h = bi.getHeight();

            BufferedImage biGrey = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            biGrey.createGraphics().drawImage(bi, 0, 0, null);

            Raster rasterGrey = biGrey.getRaster();


            double totalPixels = (double) w * h;
            int numOfBinaryPixels = 0;
            int numOfAcceptedAsBinaryPixels = 0;

            double hardThreshold = 0.75; // 75 % black and white
            double softThreshold = 0.90; // 90 % accepted pixels

            for (int y = 0; y < h; ++y) {
               for (int x = 0; x < w; ++x) {
                  int[] p = rasterGrey.getPixel(x, y, (int[]) null);
                  if (p[0] == 0 || p[0] == 255) {
                     ++numOfBinaryPixels;
                     ++numOfAcceptedAsBinaryPixels;
                  } else if (p[0] > 200 || p[0] < 5)
                     ++numOfAcceptedAsBinaryPixels;
               }
            }

            if (numOfBinaryPixels / totalPixels > hardThreshold
                    && numOfAcceptedAsBinaryPixels > softThreshold)
               return true;

         }


      return false;

   }

   /**
    * Changes the model of an image to the binary model
    * @param bi the image
    * @return an image that have been converted to the binary model
    * or null if the image is not a binary image
    */
   public static BufferedImage change2BinaryColorModel (BufferedImage bi)
   {
       if (bi == null || ! isBinaryImage(bi))
          return null;         
      
      int w = bi.getWidth();
      int h = bi.getHeight();
      if (bi.getType() != BufferedImage.TYPE_BYTE_BINARY) {
         BufferedImage bi2 =
                 new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
         Graphics big = bi2.getGraphics();
         big.drawImage(bi, 0, 0, null);
         bi = bi2;
      }
      
      return bi;
   }
     
}




