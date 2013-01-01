package plsdejai.filter.linearsubspace;

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import plsdejai.Parameter;
import plsdejai.StandardBinaryOp;
import plsdejai.widgets.LSubspaceParameterToolbar;
import plsdejai.widgets.NumericTextField;

/**
 * "Wael Abd-Almageed, Jayant Kumar, David Doermann " +
             "\"Page Rule-Line Removal using Linear Subspaces in Monochromatic Handwritten Arabic Documents\" " +
             "10th International Conference on Document Analysis and Recognition, pp. 768-772, 2009.";
 * @author Prokopiou Konstantinos
 * @version 1.0, 09/01/2012
 */
public class LSubspaceOfCentralMomentsOp extends StandardBinaryOp
{

   // Constants for parameter names
   public final static String KEY_HALF_WINDOW = "Window size";
   public final static String KEY_MOMENT_MAX_ORDER = "Maximum moment order";
   public final static String KEY_ERR = "Reconstruction error";
  
   LSubspaceOfCentralMoments model;

   public LSubspaceOfCentralMomentsOp()
   {
      model = new LSubspaceOfCentralMoments();

      toolbar = new LSubspaceParameterToolbar("Linear Subspace Model", model);

      toolbar.createParameter(KEY_HALF_WINDOW, null, NumericTextField.INTEGER,
              Integer.valueOf(0), Integer.valueOf(5),  Integer.valueOf(3));

      toolbar.createParameter(KEY_MOMENT_MAX_ORDER, null, NumericTextField.INTEGER,
              Integer.valueOf(0), Integer.valueOf(6),  Integer.valueOf(4));

      toolbar.createParameter(KEY_ERR, null, NumericTextField.DOUBLE,
              Double.valueOf(0.000001), Double.valueOf(200),  Double.valueOf(50));


      ((LSubspaceParameterToolbar) toolbar).createToolbar1Buttons();

   }


   /** SEE: StandardBinaryOp class */
   public String[] getParameterNames(){
      String [] names = null;
      if (toolbar != null) {
         Parameter [] params = toolbar.getParameters();
         int len = params.length;
         names = new String[len];
         for (int i = 0; i < len; ++i )
            names[i] = params[i].name;
      }

      return names;
   }


   /** SEE: StandardBinaryOp class */
   public String getName() {
      return "Linear Subspace of central moments";
   }

   /** SEE: StandardBinaryOp class */
   public WritableRaster filter(Raster src, WritableRaster dst)
   {

      if (src == null)
         throw new NullPointerException("src image is null");

      if (dst == null)
         dst = createCompatibleDestRaster(src);

      if (src == dst)
         throw new IllegalArgumentException("src image cannot be the "
                 + "same as the dst image");

      if (src.getNumBands() != dst.getNumBands())
         throw new IllegalArgumentException("Number of src bands ("
                 + src.getNumBands()
                 + ") does not match number of "
                 + " dst bands ("
                 + dst.getNumBands() + ")");

      int width = src.getWidth();
      int height = src.getHeight();
      if (width != dst.getWidth() || height != dst.getHeight())
         throw new IllegalArgumentException("src and dst have different dimensions");

      if (! ((LSubspaceParameterToolbar) toolbar).isReady()){;
         return null;
      }

      // Translate raster to 1 (foreground) and 0 (background)
      int[][] iArr = new int[width][height];

      for (int y = 0; y < height; ++y) {
         for (int x = 0; x < width; ++x) {            
            int[] p = src.getPixel(x, y, (int[]) null);
            if (p[0] == FOREGROUND)
               iArr[x][y] = 1;
            else
               iArr[x][y] = 0;

         }
      }

      // Initialize destination raster
      for (int y = 0; y < height; ++y)
         for (int x = 0; x < width; ++x)
            dst.setPixel(x, y, aBACKGROUND);

      for (int y = 0; y < height; ++y) {

         if (task != null && task.isCancelled())
            return null;

         for (int x = 0; x < width; ++x) {
            if (iArr[x][y] == 1){

               double[] v = model.getFeatureVector(iArr, new Point(x, y));

               if (! model.isInSubspace(v)) {
                   
                  dst.setPixel(x, y, aFOREGROUND);
               }

            }

         }
      }
      /* ************************************************************ */

      return dst;
   }

   /**
    * @return the LSubspaceOfCentralMoments model
    */
   public LSubspaceOfCentralMoments getModel() { return model; }

   /**
    * @return a clone of this object
    * @overrides method clone of <code>StandardBinaryOp</code>
    */
   public StandardBinaryOp clone()
   {
      LSubspaceOfCentralMomentsOp op = new LSubspaceOfCentralMomentsOp();

      op.model = new LSubspaceOfCentralMoments(model.getWindowHalfSide(),
              model.getMomentMaxOrder(), model.getError());
      

      LSubspaceParameterToolbar tbar =
              new LSubspaceParameterToolbar(getName(), op.model);
      op.setToolbar(tbar);

      Parameter[] params = getParameters();

      for(Parameter param : params)
         tbar.createParameter(param.name, param.desc, param.type,
                 param.min, param.max, param.value);

      tbar.createToolbar1Buttons();

      op.model.vectorList = new ArrayList<double[]>(model.vectorList);

      return op;
   }

}

