package plsdejai.filter;

import plsdejai.Parameter;
import plsdejai.StandardBinaryOp;
import plsdejai.widgets.DefaultParameterToolbar;
import plsdejai.widgets.NumericTextField;

import plsdejai.util.PointSortedArrayList;
import java.util.HashMap;
import java.awt.Point;
import java.util.Map;
import java.util.TreeMap;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import plsdejai.util.DynamicArray2D;

/**
 * This class implements the method described in:
 * >   Z. Shi, S. Setlur and V. Govindaraju “Removing Rule-lines From Binary
 * >   Handwritten Arabic Document Images Using Directional Local Profile”,
 * >   Intl. Conf. Pattern Recognition, pp.1916–1919, 2010.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 08/02/2012
 */
public class DirectionalLocalProfileOp extends StandardBinaryOp
{
   /* *************************** CONSTANTS ******************************* */
   /** Labels in labels array are sequential starting from 0. The special value
    *  UNLABELED = -1 means no label, i.e. it corresponds to a background pixel */
   private final static int UNLABELED = -1;

   /** An optimization constant, that specifies the initial size of Array Lists */
   private final static int CAPACITY = 100;

   // *** Constants for parameter names:
   private final static String KEY_K1 = "K1";
   private final static String KEY_K2 = "K2";
   private final static String KEY_K3 = "K3";

   private final static String KEY_MAX_NUM_OF_SKIPPED_BACKGROUND_PIXELS = "Skipped Background Pixels";
   private final static String KEY_HALF_WINDOW = "Window size";

   /* ********************************************************** */

   /** Constructor */
   public DirectionalLocalProfileOp()
   {
      setupToolbar(0, 0, 0.000001, 0.000001, 0.000001);
   }

   /**
    * Creates an DefaultParameterToolbar object, and initializes the values
    * of the parameters
    */
   private void setupToolbar(int skippedBGPixels, int n,
           double k1, double k2, double k3)
   {

      toolbar = new DefaultParameterToolbar(getName());

      toolbar.createParameter(KEY_MAX_NUM_OF_SKIPPED_BACKGROUND_PIXELS, null, NumericTextField.INTEGER,
              Integer.valueOf(0), Integer.valueOf(50), Integer.valueOf(skippedBGPixels));
      toolbar.createParameter(KEY_HALF_WINDOW, null, NumericTextField.INTEGER,
              Integer.valueOf(0), Integer.valueOf(5), Integer.valueOf(n));
      
      toolbar.createParameter(KEY_K1, null, NumericTextField.DOUBLE,
              Double.valueOf(0.000001), Double.valueOf(100), Double.valueOf(k1));
      toolbar.createParameter(KEY_K2, null, NumericTextField.DOUBLE,
              Double.valueOf(0.000001), Double.valueOf(100), Double.valueOf(k2));
      toolbar.createParameter(KEY_K3, null, NumericTextField.DOUBLE,
              Double.valueOf(0.000001), Double.valueOf(100), Double.valueOf(k3));

   }

   

   /**
    * Implementation of the abstract <code>getName</code> method in StandardBinaryOp
    * @return a String, which is a descriptive name for the class
    *         The returned string should have at most than 40 characters
    * @see <code>getName</code> method in StandardBinaryOp
    */
   public final String getName() { return "Directional Local Profile"; }

   /**
    * Implementation of the abstract <code>getParameterNames</code> method in StandardBinaryOp
    * @return an array of String of the parameters that are used to adapt the algorithm
    * @see <code>getParameterNames</code> method in StandardBinaryOp
    */
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

   /**
    * Implementation of the <code>filter</code> method in StandardBinaryOp
    * @param src The <code>Raster</code> to be filtered
    * @param dst The <code>Raster</code> in which to store the results
    *            if null, new <code>Raster</code> will be created
    * @return a <code>WritableRaster</code> that represents the result of the filtering operation.
    * @see <code>filter</code> method in StandardBinaryOp
    */
   public WritableRaster filter(Raster src, WritableRaster dst)
   {
      if (src == null)
         throw new NullPointerException("src image is null");

      if (dst == null)
         dst = createCompatibleDestRaster(src);

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

      int width = src.getWidth();
      int height = src.getHeight();
      if (width != dst.getWidth() || height != dst.getHeight()) {
         throw new IllegalArgumentException("src and dst have different dimensions");
      }

      dst.setPixels(0, 0, width, height,
              src.getPixels(0, 0, width, height, (int[]) null));
      

      for (int j = 0; j < 2; ++j) {
         if(task != null && task.isCancelled())
            return null;

         // Executes the fuzzy runlength, in order to get the connectivity map
         double[][] dArr = fuzzyRunLength(dst);

         if(task != null && task.isCancelled())
            return null;

         /*
          * Binarize the matrix, by using a local adaptive thresholding algorithm,
          * described in:
          *  E. Giuliano,O. Paitra, and L. Stringa,
          * "Electronic Character Reading System", US Patent No.4047152 dated September 6, 1977
          *
          *  Returns: An array of binary values, of the same size as the input array
          */

         int[][] binaryArr = binarize(dArr);
         if(task != null && task.isCancelled())
            return null;

         /* Connected Component Analysis */
         int[][] labels = cca(binaryArr);
         if(task != null && task.isCancelled())
            return null;

         /* Get Best Fitting Lines */
         List<double[]> bestFitLines = getBestFittingLines(labels);
         if(task != null && task.isCancelled())
            return null;

         /* Remove rule lines */
         removeRuleLines(dst, bestFitLines);

         if(task != null && task.isCancelled())
            return null;

         dst = rotate90(dst, (j == 0) ? false : true);

          System.gc();
      }

      /* ************************************************************ */

      return dst;
   }

   /**
    * Implements the fuzzy runlength algorithm, which is described in:
    * >   Z. Shi, S. Setlur and V. Govindaraju “Removing Rule-lines From Binary
    * >   Handwritten Arabic Document Images Using Directional Local Profile”,
    * >   Intl. Conf. Pattern Recognition, pp.1916–1919, 2010.
    *
    * @param src : The raster of a binary image.
    * @param maxSkippedBackgroundPixels: When the accumulated number of skipped
    *         pixels exceeds this pre-set threshold we stop the runlength tracing
    *
    * @return a two dimensional matrix of the size of the original binary image.
    *         Each entry of the matrix is the fuzzy runlength of the pixel
    *         in its position
    */
   private double[][] fuzzyRunLength(Raster src)
   {
      /* Description of the algorithm by Shi et al.:
       * At each pixel location, we trace a foreground run starting
       * from the pixel along two directions, to its left and right (this is for
       * horizontal runs, otherwise the up and down directions for vertical runs).
       * When the accumulated number of skippped pixels exceeds the pre-set threshold
       * (maxSkippedBackgroundPixels), we stop the tracing and do the same along
       * the other direction. The total number of the traced positions is the
       * length of the run associating to the position where we start the tracing.
       * Intuitively, the fuzzy runlength at a pixel is how much we can connect
       * the current pixel to its neighbouring pixels along horizontal
       * (or vertical position)
       *
       * When the accumulated number of skippped pixels exceeds the pre-set threshold
       * (maxSkippedBackgroundPixels), we stop the tracing and do the same along
       * the other direction. The total number of the traced positions is the
       * length of the run associating to the position where we start the tracing.
       * Intuitively, the fuzzy runlength at a pixel is how much we can connect
       * the current pixel to its neighbouring pixels along horizontal
       * (or vertical position)
       */

      int maxSkippedBackgroundPixels =
              toolbar.getParameterValue(KEY_MAX_NUM_OF_SKIPPED_BACKGROUND_PIXELS).intValue();
      
      int cols = src.getWidth();
      int rows = src.getHeight();     

      /* Output buffer of same size as the input image
       * for holding the fuzzy runlengths */
      double[][] fuzzyRunLength = new double[cols][rows];

      // runLength: keeps the maximum of all runlengths
      // it is used to re-scale the values to the range [0 - 255]
      double maxRunLength = 0;

      /* We generate the horizontal fuzzy runlength by scanning each row of the
       * image twice, from left to right and then from right to left.
       * At each pixel location, we trace a foreground run
       * starting from the pixel along two directions, to its left and right
       * (this is for horizontal runs, otherwise the up and down directions
       * for vertical runs). */
      for (int row = 0; row < rows; ++row) {         
         for (int col = 0; col < cols; ++col) {

            // runLength: keeps the total number of traced pixels in the foreground run
            int runLength = 0;

            // *************** Run length from left to right **************
            int skipped_px = 0;
            int[] pixel = null; // a dummy value
            int c = col;

            while (++c < cols) {
               pixel = src.getPixel(c, row, (int[]) null);

               if (pixel[0] == FOREGROUND)
                  ++runLength;
               else if (pixel[0] == BACKGROUND) {
                  ++skipped_px;

                  if (skipped_px > maxSkippedBackgroundPixels)
                     break;
               }

            }

            // *************** Run length from right to left **************
            skipped_px = 0;
            c = col;
            while (--c >= 0) {
               pixel = src.getPixel(c, row, (int[]) null);

               if (pixel[0] == FOREGROUND)
                  ++runLength;
               else if (pixel[0] == BACKGROUND) {
                  ++skipped_px;

                  if (skipped_px > maxSkippedBackgroundPixels)
                     break;

               } 

            }

            
            fuzzyRunLength[col][row] = (double) runLength;

            if (fuzzyRunLength[col][row] > maxRunLength)
               maxRunLength = fuzzyRunLength[col][row];
         }

      }

      // Normalize values to the range 0 - 255
      if (maxRunLength > 255)
         for (int row = 0; row < rows; row++)
            for (int col = 0; col < cols; col++)
               fuzzyRunLength[col][row] =
                       fuzzyRunLength[col][row] * 256 / (maxRunLength + 1);

      return fuzzyRunLength;
   }

   /**
    * Binarization algorithm described in:
    *   E. Giuliano,O. Paitra, and L. Stringa,
    *   "Electronic Character Reading System",
    *   US Patent No.4047152 dated September 6, 1977
    *
    * @param iArr: an array representing the pixel values of a grayscale image
    */
   private int[][] binarize(double [][] dArr)
   { 
      int[][] binaryArr = new int [dArr.length][dArr[0].length];

      /* ******************** Constants *************************** */
      final double K1 = toolbar.getParameterValue(KEY_K1).doubleValue();
      final double K2 = toolbar.getParameterValue(KEY_K2).doubleValue();
      final double K3 = toolbar.getParameterValue(KEY_K3).doubleValue();
      //@debug:
      //final double K4 = toolbar.getParameterValue(KEY_K4).doubleValue();

      final int windowSide = toolbar.getParameterValue(KEY_HALF_WINDOW).intValue() * 2 + 1;
      final int mesA1 = windowSide * windowSide; // mesA1: the area of zone A1
      /* ********************************************************* */
      double Kii, inverse_mesA1;

      try {         
         inverse_mesA1 =  1.0 / mesA1;
         Kii = K2 / K1;

      } catch (Exception e) {
         throw new ArithmeticException("The first element of 'constants' is zero");
      }

      // Boundaries of the raster
      int minRasterX = 0;
      int minRasterY = 0;
      int maxRasterX = dArr.length - 1;
      int maxRasterY = dArr[0].length - 1;

      // Boundaries of the A1 zone
      int min1X = -1; // dummy value
      int min1Y = -1; // dummy value
      int max1X = -1; // dummy value
      int max1Y = -1; // dummy value

      // Boundaries of the A2 zones
      int min2X = -1; // dummy value
      int min2Y = -1; // dummy value
      int max2X = -1; // dummy value
      int max2Y = -1; // dummy value

      int numOfZones  = 5; // 1 central A1 and 4 diagonal A2 zones

      for (int y = minRasterY; y <= maxRasterY; ++y) { // y: vertical coordinate
         
         for (int x = minRasterX; x <= maxRasterX; ++x) { // x: horizontal coordinate
            double sumA1 = 0, sumA2i = 0;

            /* mesA2i: the area of that _part-of_ zone A2 which is constituted by
             *         pixels having values that satisfy the relationship S >= K_ii */
            int mesA2i = 0;

            // if it is not possible to have a complete 3n x 3n area, set
            // pixel to BACKGROUND
            int extent = (3 * windowSide) / 2;
            if (! (x - extent >= minRasterX &&
                   y - extent >= minRasterY &&
                   x + extent <= maxRasterX &&
                   y + extent <= maxRasterY ) ){

               ///*
               binaryArr[x][y] = BACKGROUND;               

            } else {

               //LOOP:
               for (int zone = 0; zone < numOfZones; zone++) {
                  switch (zone) {
                     case 0: // central zone A1
                        min1X = x - windowSide / 2; // min1X = max(minRasterX, x - n / 2);
                        min1Y = y - windowSide / 2; // min1Y = max(minRasterY, y - n / 2);
                        max1X = x + windowSide / 2; // max1X = min(maxRasterX, x + n / 2);
                        max1Y = y + windowSide / 2; // max1Y = min(maxRasterY, y + n / 2);
                        break;

                     case 1: // diagonal Upper-Left zones A2
                        max2X = min1X - 1;
                        max2Y = min1Y - 1;
                        // if ( max2X < minRasterX || max2Y < minRasterY) continue LOOP;

                        min2X = min1X - windowSide; // min2X = max(minRasterX, min1X - n);
                        min2Y = min1Y - windowSide; // min2Y = max(minRasterY, min1Y - n);
                        break;

                     case 2: // diagonal Upper-Right zones A2
                        min2X = max1X + 1;
                        max2Y = min1Y - 1;
                        // if ( min2X > maxRasterX || max2Y < minRasterY) continue LOOP;

                        max2X = max1X + windowSide; // max2X = min(maxRasterX, max1X + n);
                        min2Y = min1Y - windowSide; // min2Y = max(minRasterY, min1Y - n);
                        break;

                     case 3: // diagonal Lower-Left zones A2
                        max2X = min1X - 1;
                        min2Y = max1Y + 1;
                        // if (max2X < minRasterX || min2Y > maxRasterY) continue LOOP;

                        min2X = min1X - windowSide; // min2X = max(minRasterX, min1X - n);
                        max2Y = max1Y + windowSide; // max2Y = min(maxRasterY, max1Y + n);
                        break;

                     case 4: // diagonal Lower-Right zones A2
                        min2X = max1X + 1;
                        min2Y = max1Y + 1;
                        // if (min2X > maxRasterX || min2Y > maxRasterY) continue LOOP;

                        max2X = max1X + windowSide; // max2X = min(maxRasterX, max1X + n);
                        max2Y = max1Y + windowSide; // max2Y = min(maxRasterY, max1Y + n);
                        break;

                     default:

                        break;
                  }

                  if (zone == 0) // Central zone A1
                     for (int x1 = min1X; x1 <= max1X; x1++) {
                        for (int y1 = min1Y; y1 <= max1Y; y1++) {
                           sumA1 += dArr[x1][y1];
                        }

                  } else { // Diagonal zones A2

                     for ( int x2 = min2X; x2 <= max2X; x2++)
                        for ( int y2 = min2Y; y2 <= max2Y; y2++){
                           if ( dArr[x2][y2] >= Kii ){
                              ++mesA2i;
                              sumA2i += dArr[x2][y2];
                           }

                        }
                  }

               }

               // Response parameter R(x,y):
               double R = K1 * ( inverse_mesA1 * sumA1 -
                       ((mesA2i > 0) ? K3 / mesA2i * sumA2i : 0) );


               // Calculate Binary Value from R(x,y) and S(x,y)
               if (R > 0 && dArr[x][y] >= Kii){
                  binaryArr[x][y] = FOREGROUND;                  
               }

               else
                  binaryArr[x][y] = BACKGROUND;

            }
         }
      }     

      return binaryArr;
   }

   /**
    * Connected-Component Analysis
    * The algorithm is described in:
    *   E.R. Davies “Machine Vision: Theory, Algorithms, Practicalities”,
    *   3rd Edition, ELSEVIER, p.164 – 167.
    * @param data: An array of int of [cols x rows] values representing the
    *              pixel values of a binary image raster
    * @returns labels: An array of the same dimensions as data. The labels array is
    *             filled with the label values in the positions corresponding to
    *             the data array. Labels are a sequence starting from 0.
    *             The value UNLABELED (-1) is put in the place of background pixels
    */
    public int [][] cca(int[][] data)
   {
      int cols = data.length;
      int rows = data[0].length;

      // Labels must have values equal or greater than 0
      int label = UNLABELED;

      // A dynamic table, that is used to hold the label coexistence data
      DynamicArray2D<Integer> coexist = new DynamicArray2D<Integer>();

      // Labels is a structure with dimensions of data, initialized with the value of UNLABELED
      int labels[][] = new int[cols][rows];
      for (int i = 0; i < cols; i++) {
         Arrays.fill(labels[i], UNLABELED);
      }

      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < cols; col++) {

            if (data[col][row] != BACKGROUND) { // For each non-background data

               int minLabel = Integer.MAX_VALUE;

               // If neighbours are unlabeled, then neighbourLabels is empty
               List<Integer> neighbourLabels = new ArrayList<Integer>(4);

               // Check each of the NE, N, NW and W neighbours if they are labeled
               // If a neighbour is labeled then set the coexistance value
               // and update the max label value
               int[] neighbours = new int[4];
               neighbours[0] = (row > 0 && col < cols - 1)
                       ? labels[col + 1][row - 1] : UNLABELED;
               neighbours[1] = (row > 0) ? labels[col][row - 1] : UNLABELED;
               neighbours[2] = (row > 0 && col > 0)
                       ? labels[col - 1][row - 1] : UNLABELED;
               neighbours[3] = (col > 0) ? labels[col - 1][row] : UNLABELED;

               for (int i = 0; i < neighbours.length; i++) {
                  if (neighbours[i] != UNLABELED) {
                     minLabel = Math.min(neighbours[i], minLabel);

                     neighbourLabels.add(new Integer(neighbours[i]));
                  }
               }

               // if neighbours are unlabeled create and set a new label
               // for the current data
               if (neighbourLabels.isEmpty()) {

                  labels[col][row] = ++label; // First label is set to 0
                  coexist.setLength(label);
                  coexist.setValue(label, label, label);

               } else {
                  // Set the min label of the neighbours for the current data
                  labels[col][row] = minLabel;

                  // Update the coexist table
                  for (Iterator<Integer> it = neighbourLabels.iterator(); it.hasNext();) {
                     Integer elem = it.next();
                     coexist.setValue(labels[col][row],elem.intValue(), minLabel);
                     coexist.setValue(elem.intValue(),labels[col][row], minLabel);
                  }

               }
            }

         }

      }
      
      System.gc();

      /* The next step is to minimize the entries along the individual rows of
       * the coexist table; next we minimize along the individual columns; and
       * then we minimize along rows again. */
      int length = coexist.getLength();
      Integer value = null;
      for (int count = 0; count < 3; count++) {

         for (int r = 0; r < length; ++r) {
            int minLabel = Integer.MAX_VALUE;

            // find minimum label
            for (int c = 0; c < length; ++c) {
               if (count != 1) // minimize the entries along the individual rows
                  value = coexist.getValue(r, c);
               else  // minimize the entries along the individual columns
                  value = coexist.getValue(c, r);

               if (value != null) {
                  minLabel = Math.min(value.intValue(), minLabel);
               }
            }


            // set values to minimum label
            for (int c = 0; c < length; ++c) {
               if (count != 1) // minimize the entries along the individual rows
                  value = coexist.getValue(r, c);
               else  // minimize the entries along the individual columns
                  value = coexist.getValue(c, r);

               if (value != null && value.intValue() != minLabel)
                  if (count != 1) // minimize the entries along the individual rows
                     coexist.setValue(r, c, minLabel);
                  else  // minimize the entries along the individual columns
                     coexist.setValue(c, r, minLabel);
            }
         }

      }

      // Normalize the labels table to the minimum label values

      // Maps each (key) label to the minimum label number (value)
      Map<Integer, Integer> coexistMap = new TreeMap<Integer, Integer>();

      // Scan each row in coexistMap, until a non-null value is found.
      // Then set a Map entry, which maps the row number to the found value.
      // (Note: All non-null values have the same value.)

      for (int r = 0, c = 0; r < length; ++r) { // length = coexist.getLength(); is defined above

         for (c = 0; c < length; ++c){
            value = coexist.getValue(r, c);
            if (value != null) {
               coexistMap.put(r, value);

              break; // No need to continue, all non-null values have the same value
            }

         }

      }

      System.gc();
      
     // Renumber the min coexist labels, in sequential order
      SortedSet<Integer> uniqueLabels = new TreeSet<Integer>(coexistMap.values());
      // Now map to the unique labels to the sequential labels
      Map<Integer,Integer> uniqueLabels2sequentialLabels = new HashMap<Integer,Integer>(uniqueLabels.size());
      int seq = 0;
      for(Iterator<Integer> it = uniqueLabels.iterator(); it.hasNext(); ++seq )
         uniqueLabels2sequentialLabels.put(it.next(), seq);

      for (int row = 0; row < rows; row++)
         for (int col = 0; col < cols; col++)
            if (labels[col][row] != UNLABELED) {


               labels[col][row] = uniqueLabels2sequentialLabels.get(
                       Integer.valueOf(coexistMap.get(labels[col][row]))).intValue();

            }

      System.gc();
      
      return labels;
   }

   /**
    * Returns the list of valid line patterns, calculated from each
    * of the connected components returned by method <code>cca</code>
    * @param labels an array in which each pixel that belongs to
    *        the same connected component have the same value
    * @return a list of valid line patterns
    */
   private List<double[]> getBestFittingLines(int[][] labels)
   {
      /*
       * - We estimate an average line width (thickness) using the line
       *   pattern(s) corresponding to the line;
       * - Using the average line width, thick areas in the line patterns are iden-
       *   tified. The pixels in the identified area are marked out.
       * - Using the unmarked pixels in the thin areas of the the line patterns
       *   to estimate a best fitting line using linear regression method.
       * - Then finally, the rule-line is re-constructed using the estimated best fitting line.
       */

      int cols = labels.length;
      int rows = labels[0].length;

      java.util.List<LinePattern> linePatterns =
              new java.util.ArrayList<LinePattern>(CAPACITY);

      for (int col = 0; col < cols; col++) {
         for (int row = 0; row < rows; row++) {
            if (labels[col][row] != UNLABELED) {

               // Create LinePattern objects up to the label value of the array
               // element labels[col][row]. The index in the ArrayList equals the label value
               for (int i = linePatterns.size(); i <= labels[col][row]; i++) {

                  linePatterns.add(new LinePattern(SortOrder.YX_ORDER));
               }


               linePatterns.get(labels[col][row]).addPoint(new java.awt.Point(col, row));
            }
         }
      }

      List<double[]> bestFitLines = new ArrayList<double[]>(linePatterns.size());
      

      // Group near-by connected components together, by measuring their
      // vertical (for horizontal runlength) overlaps.
      for (int startIndex = 0; startIndex < linePatterns.size(); ++startIndex) {

         // Use this pattern, to test for ovelaps with the patterns following this pattern
         LinePattern currentPattern = linePatterns.get(startIndex);
         int min = 0,
             max = 0;

         PointSortedArrayList points = currentPattern.getPoints();

         min = points.get(0).y;
         max = points.get(points.size() - 1).y;

         for (int i = 0, size = linePatterns.size(); i < size; ++i) {

            if (i == startIndex)
               continue;


            PointSortedArrayList points1 = linePatterns.get(i).getPoints();

            int min1 = points1.get(0).y,
                max1 = points1.get(points1.size() - 1).y;


            if ((min1 >= min && min1 <= max)
                    || (max1 <= max && max1 >= min)) { // Overlap Condition
               points.addAll(points1);
               linePatterns.remove(i--);
               --size;
            }

         }

         System.gc();

      }

         bestFitLines.clear();
         for (Iterator<LinePattern> it = linePatterns.iterator(); it.hasNext();)
         {
            LinePattern pattern = it.next();

            double line[] = pattern.getBestFittingLine();

            if (isValidLine(line))
               bestFitLines.add(line);
         }


      return bestFitLines;

   }

   /**
    * In place conversion or raster
    * @param raster
    * @param lines
    */
   private void removeRuleLines(WritableRaster raster, List <double[]> lines)
   {
      /*
       * To be efficient only those vertical runs touching the re-
       * constructed rule-lines are traced. If a vertical run is en-
       * tirely covered inside a re-constructed rule-line, the run
       * is removed from the original document image.
       */

      /* For each re-constructed line
         1. for each column, x,
         1.1 Calculate the minimum and maximum y (row) values, minY and maxY, respectively.
         1.2 if pixel (x, minY-1) = FOREGROUND,
         1.2.a  execute a downwards vertical runlength from minY to maxY
                     until a BACKGROUND pixel,p, is found
         1.2.b  if no such BACKGROUND pixel is found, goto step 1
         1.2.c  REMOVE_VERTICALRUN(p1) // p1 is the pixel that follows p, in the downwards direction
         1.2.d  goto step 1
         1.3 else -- if pixel (x, minY-1) = BACKGROUND
         1.3.a  REMOVE_VERTICALRUN( pixel (x, minY) )
         1.3.b  goto step 1

       REMOVE_VERTICALRUN(p)
         1. Execute a downwards vertical runlength starting from p.y to maxY
               until a FOREGROUND pixel, pF, is found
         2. if no such FOREGROUND pixel is found, return
         3. Continue the vertical scan from pF to maxY, until a BACKGROUND pixel, pB, is found.
         4. if pB.y > maxY
                  set pixels (x,pF.y) to (x,maxY) to BACKGROUND
                  return
         5. else
                  set pixels (x,pF.y) to (x,pB - 1) to BACKGROUND
                  p.y = pF.y
                  goto step 1

       */
      
      for (Iterator<double[]> it = lines.iterator(); it.hasNext();) {
         double[] line = it.next();

         LOOP:
         for (int col = 0, colLen = raster.getWidth();  col < colLen; col++) {

            double thickness = line[2]/2.0 + 1; 
                        // beta1 * x    +  beta0   - thickness/2
            double dMinY = line[0] * col + line[1] - thickness;
            // Normalize double to integer
            int minY = (int) dMinY; minY =(minY < dMinY) ? minY + 1 : minY;

                        // beta1 * x    +  beta0   + thickness/2
            int maxY = (int) (line[0] * col + line[1] + thickness );

            minY = Math.max( minY, 0);
            maxY = Math.min( maxY, raster.getHeight() - 1);

            if( minY >= maxY) continue;

            int maxRow = maxY;

            // Check if there is a FOREGROUND pixel just above the re-constructed line
            if (minY != 0 && raster.getPixel(col, minY - 1, (int[]) null)[0] == FOREGROUND) {
               // Keep the vertical span that begins outside the reconstructed line

               int row = minY;
               for (; row <= maxRow; row++) {

                  if (raster.getPixel(col, row, (int[]) null)[0] == BACKGROUND)
                     removeVerticalRun(raster, new Point(col, row + 1), maxRow);
               }

               if (row > maxRow)
                  continue LOOP;

            } else {
               removeVerticalRun(raster, new Point(col, minY), maxRow);
            }
         }
      }

   }

   private void removeVerticalRun(WritableRaster raster, Point p, int maxRow)
   {
      int rows = raster.getHeight();

      int[] aBACKGROUND = new int[raster.getSampleModel().getNumBands()];
      Arrays.fill(aBACKGROUND, BACKGROUND);

      // Candidate points for removal
      List <Point> points = new ArrayList<Point>(15);


      int col = p.x;
      int row = p.y;
      for (; row <= maxRow; row++) {
         if (raster.getPixel(col, row, (int[]) null)[0] == FOREGROUND) {

            points.add( new Point (col, row) );

            int r = row + 1;
            for (; r < rows &&
                    raster.getPixel(col, r, (int[]) null)[0] == FOREGROUND; r++) {
               if ( r > maxRow){ // a FOREGROUND pixel is just outside the reconstructed line

                  points.clear(); // do not keep points for removal
                  break;

               } else{
                  points.add(new Point(col, r));
               }

            }

            // remove points
            for(Iterator<Point> it = points.iterator(); it.hasNext();){
               Point p1 = it.next();

               raster.setPixel(p1.x, p1.y, aBACKGROUND);
            }
            points.clear();

            row = r;
         }
      }

   }



   /**
    * Specifies the constants that determine the sorting order,
    * in the LinPattern class.
    */
   public static enum SortOrder { XY_ORDER, YX_ORDER}

   /**
    * This class constructs a Line pattern, from all the points that
    * belong to the same connected component.
    */
   private class LinePattern
   {
      /** Constant specifying that the average will be calculated per column */
      public final static int AVG_PER_COLUMN = 0;
      /** Constant specifying that the average will be calculated per row */
      public final static int AVG_PER_ROW= 1;
      /** Variable that keeps how the average will be calculated. It can take
       *  either the value of AVG_PER_COLUMN or AVG_PER_ROW */
      private int avgOrder = AVG_PER_COLUMN;

      /** Keeps all points added to the pattern in sorted order */
      private PointSortedArrayList points;

      /* Keeps the widths values for each key: col (if avgOrder = AVG_PER_COLUMN) or
       *                                       row (if avgOrder = AVG_PER_ROW) */
      private Map<Integer,Integer> widths = new HashMap<Integer,Integer>(CAPACITY);

      /** Keeps the thickness of the line */
      private Double thickness;

      /**
       * Constructor
       * @param sortOrder: XY_ORDER sort points by X (col) and then by Y (row)
       *                   YX_ORDER sort by Y (row) and then by X (col)
       */
      public LinePattern(SortOrder sortOrder)
      {
         if (sortOrder ==  SortOrder.YX_ORDER)
            points = new PointSortedArrayList(CAPACITY, PointSortedArrayList.YX_ORDER);
         else
            points = new PointSortedArrayList(CAPACITY, PointSortedArrayList.XY_ORDER);
      }

      /**
       * Clear operations in order to release memory
       */
      public void clear()
      {
         points.clear(); points = null;
         widths.clear(); widths = null;
         thickness = null;
      }

      /**
       *
       * @return either the value of AVG_PER_COLUMN or AVG_PER_ROW
       */
      public int getAvgOrder() { return this.avgOrder;}

      /**
       * Sets the average order
       * @param avgOrder either the value of AVG_PER_COLUMN or AVG_PER_ROW
       */
      public void setAvgOrder(int avgOrder) {
         if (avgOrder != AVG_PER_COLUMN && avgOrder != AVG_PER_ROW)
            throw new IllegalArgumentException();

         this.avgOrder = avgOrder;
      }

      /**
       * Adds a point to the points list
       * @param p the point to be added to the list
       * @return true if the point is added, or false if not
       */
      public boolean addPoint(Point p){ return points.add(p); }

      /**
       * Estimate an average line width (thickness) using the line pattern
       * For each column (x-dimension) we calculate the number of Points; this is
       * the width for the given x-dimension.
       * @return the average width or null if there are no points in the LinePattern
       */
      public Double calculateAvgWidth()
      {
         if (points.isEmpty()){

            this.thickness = null;
            return null;
         }

         double avg = 0;
         if (! widths.isEmpty())
            widths.clear();

         int l = -1; // -1 is a dummy value

         for (Iterator<Point> it = points.iterator(); it.hasNext();) {
            Point p = it.next();

            if (avgOrder == AVG_PER_COLUMN)
               l = p.x;
            else  /* avgOrder == AVG_PER_ROW */
               l = p.y;

            if (!widths.containsKey(Integer.valueOf(l)))
               widths.put(l, Integer.valueOf(1));
            else
               widths.put(l, widths.get(l).intValue() + 1);

         }

         for (Iterator<Integer> it = widths.keySet().iterator(); it.hasNext();) {
            avg += widths.get(it.next()).intValue();
         }

         this.thickness = Double.valueOf(avg / widths.size());
         return thickness;
      }

      /**
       * @return a sorted list of all the points added to this pattern
       */
      public PointSortedArrayList getPoints() { return points; }

      /**
       * @return a list of the points that are in a col with width greater than the avegage
       */
      public PointSortedArrayList getPointsOfThickAreas()
      {
         PointSortedArrayList pointsOfThickAreas = null;

         calculateAvgWidth();

         if ( this.thickness == null)
            return null;

         double avg = this.thickness.doubleValue();
         pointsOfThickAreas = new PointSortedArrayList(CAPACITY, PointSortedArrayList.XY_ORDER);

         for (Iterator<Point> it = points.iterator(); it.hasNext();){
            Point p = it.next();

            if(widths.get(p.x) > avg)
               pointsOfThickAreas.add(p);
         }

         return pointsOfThickAreas;
      }

      /*
       * @return a List of the points that are in a col with width equal or less than the avegage
       */
      public PointSortedArrayList getPointsOfThinAreas()
      {
         PointSortedArrayList pointsOfThinAreas = null;

         calculateAvgWidth();

         if ( this.thickness == null)
            return null;

         double avg = this.thickness.doubleValue();
         pointsOfThinAreas = new PointSortedArrayList(CAPACITY,  PointSortedArrayList.XY_ORDER);


         for (Iterator<Point> it = points.iterator(); it.hasNext();) {
            Point p = it.next();

            if (widths.get(p.x) <= avg)
               pointsOfThinAreas.add(p);
         }

         return pointsOfThinAreas;
      }

      /**
       * Number of processed points: a statistical measure of how many points
       *   of thin areas are used to construct the line
       * @return : Array[beta0, beta1, thickness, minimum x-coordinate, maximum x-coordinate, number-of-processed-points]
       *                if the PointSortedArrayList is sorded in XY_ORDER (vertical runlength)
       *           Array[beta0, beta1, thickness, minimum y-coordinate, maximum y-coordinate, number-of-processed-points]
       *                if the PointSortedArrayList is sorded in YX_ORDER (horizontal runlength)
       *           an empty array, if getPointsOfThinAreas() returns null
       */
      public double[] getBestFittingLine()
      {
         PointSortedArrayList pointsOfThinAreas = getPointsOfThinAreas();

         if (pointsOfThinAreas == null)
            return new double[0];

         /*
          *  Reads in a sequence of pairs of real numbers and computes the
          *  best fit (least squares) line y  = ax + b through the set of points.
          *  Note: the two-pass formula is preferred for stability.
          */
         int n = pointsOfThinAreas.size();

         // first pass: read in data, compute xbar and ybar
         double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
         for (Iterator<Point> it = pointsOfThinAreas.iterator(); it.hasNext();) {
            Point p = it.next();
            int x = p.x;
            int y = p.y;
            sumx += x;
            sumx2 += x * x;
            sumy += y;
         }

         double xbar = sumx / n;
         double ybar = sumy / n;

         // second pass: compute summary statistics
         double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
         for (Iterator<Point> it = pointsOfThinAreas.iterator(); it.hasNext();) {
            Point p = it.next();
            int x = p.x;
            int y = p.y;
            double xMinusXbar = x - xbar;
            double yMinusYbar = y - ybar;
            xxbar += xMinusXbar * xMinusXbar;
            yybar += yMinusYbar * yMinusYbar;
            xybar += xMinusXbar * yMinusYbar;
         }
         double beta1 = xybar / xxbar;
         double beta0 = ybar - beta1 * xbar;


         
         // Calculate the fittest line rectangle for xLeft to xRight coordinates
         double fittestLine[] = new double[6];
         fittestLine[0] = beta1;
         fittestLine[1] = beta0;
         fittestLine[2] = this.thickness;
         if (points.getSortOrder() == PointSortedArrayList.XY_ORDER){
            fittestLine[3] = (double) pointsOfThinAreas.get(0).x; // left limit
            fittestLine[4] = (double) pointsOfThinAreas.get(pointsOfThinAreas.size() - 1).x; // right limit

         } else { // (points.getSortOrder() == PointSortedArrayList.YX_ORDER)
            fittestLine[3] = (double) pointsOfThinAreas.get(0).y; // upper limit
            fittestLine[4] = (double) pointsOfThinAreas.get(pointsOfThinAreas.size() - 1).y; // lower limit
         }
         fittestLine[5] = (double) pointsOfThinAreas.size(); // Number of points used to construct this line

         return fittestLine;
      }

   }

   

   /**
    * @param line the line, represented by an array of type
    *        Array[beta0, beta1, thickness, minimum x-coordinate,
    *        maximum x-coordinate, number-of-processed-points]
    * @return true if <code>line</code> is a valid line, otherwise false
    */
   private boolean isValidLine(double [] line)
   {
      // A low limit for the minimum number of points used to construct this line
      // in order to be accepted as a valid line
      final int minSampleSize = 15;

      if (line.length == 0 ||
              Double.valueOf(line[0]).isNaN() || // beta1 is NaN
              Double.valueOf(line[1]).isNaN() || // beta0 is NaN
              line[5] < minSampleSize ||         // Sample size is too small
              line[2] < 1             ||         // thickness threshold
              line[0] > 5 || line[0] < -5        // slope
         )  {

         return false;
      }

      return true;
   }

   /**
    * @return a clone of this object
    * @overrides method clone of <code>StandardBinaryOp</code>
    */
   public StandardBinaryOp clone()
   {
      DirectionalLocalProfileOp op = new DirectionalLocalProfileOp();

      DefaultParameterToolbar tbar = new DefaultParameterToolbar(getName());
      op.setToolbar(tbar);

      Parameter[] params = getParameters();

      for(Parameter param : params)
         tbar.createParameter(param.name, param.desc, param.type,
                 param.min, param.max, param.value);

      return op;
   }

  
}
