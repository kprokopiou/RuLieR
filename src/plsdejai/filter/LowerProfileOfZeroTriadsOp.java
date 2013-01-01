package plsdejai.filter;

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import plsdejai.Parameter;
import plsdejai.StandardBinaryOp;
import plsdejai.widgets.DefaultParameterToolbar;
import plsdejai.widgets.NumericTextField;


/**
 * A new method based on the lower profile
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 21/02/2012
 */
public class LowerProfileOfZeroTriadsOp extends StandardBinaryOp
{
   // Constants for parameter names
   private final static String KEY_TOLERANCE = "tolerance";
   private final static String KEY_OFF = "off";
   private final static String KEY_PART = "part";   
   
   /* ***************************************************** */

   public LowerProfileOfZeroTriadsOp()
   {
      toolbar = new DefaultParameterToolbar(getName());

      // Σαν max θα έβλεπα 2*thickness και min 0.
      toolbar.createParameter(KEY_TOLERANCE, null, NumericTextField.INTEGER,
               Integer.valueOf(0), Integer.valueOf( 10), Integer.valueOf(4));

      toolbar.createParameter(KEY_OFF, null, NumericTextField.INTEGER,
              Integer.valueOf(-3), Integer.valueOf( 3), Integer.valueOf(1));

      toolbar.createParameter(KEY_PART, null, NumericTextField.DOUBLE,
              Double.valueOf(1), Double.valueOf( 300 ), Double.valueOf(4));

   }

   /**
    * @return a String, which is a descriptive name for the class
    *         The returned string should have at most than 20 characters
    */
   public final String getName(){return "Lower Profile of Zero Triads";}

   /**
    * @return a String which is the full description of the paper which describes
    *         the filtering method
    */
   public String getDescription(){ return null; }

   /**
    * Every subclass must return the member variable names
    * that represent parameter names that it uses to configure the algorithm.
    * @return an array of String of the parameters that are used to adapt the algorithm
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

      if (width != dst.getWidth() || height != dst.getHeight())
         throw new IllegalArgumentException("src and dst have different dimensions");

      
      dst.setPixels(0, 0, width, height,
              src.getPixels(0, 0, width, height, (int[]) null));

      dst = modelLine(dst);
     
      return dst;

   }


   /**
    *
    * tolerance determines if we can accept a corrected y3,
    * as a valid pixel, in order to delete the vertical strip of pixels. For that
    * we compare: abs(y3 - correctedy3) <= tolerance
    * In any case, tolerance has a meaning if tolerance <= maxRow,
    * if tolerance > maxRow everything is accepted
    * Also, tolerance must be >=0, because 0 means y3 was a black pixel.
    */
   private int tolerance;

   /**
    * off how to extend the calculated thickness of a found rule-line
    */
   private int off;

   private double part;

   private boolean isLineFound = false;

   /**
    *
    * 1. Starting from the bottom, we find the rule-line that is the fittest candidate
    * 2. If there is any such line, we calculate its thickness
    * 3. The thickness is adapted by adding an <code>off</code> parameter.
    * 4. Next in order to want to find the two points of the reconstructed rule line
    *    we scan a region around the rule line found in step 1,
    *    for a horizontal distance of 500 pixels from the left to find the first point,
    *    and then for a horizontal distance of 500 pixels from the right to find the second point.
    *    In detail,
    * 4.a   We get a region (adapted to the boundaries of the image if needed),
    *       around the line found in step 1, that contains:
    *       the rows extending up and down the found rule-line by a value (5 * thickness)
    *       the first (or last) 500 cols if we scan for the left (or the right) point, respectively
    * 4.b. For the region we got in step 4.a, we find the middle point of
    *      the first acceptable  consecutive sequence of pixels
    *      (i.e. constisting of utmost <code>thickness</code> pixels, otherwise
    *	    if no such point is found we return 'null'
    *
    * 4.c If we have two valid points, then we have a first-cut line.
    *     For pixel (x,y) every column of this line:
    * 4.c.1 We find a better coordinate y3,
    *       and also the distance <code>dist</code> from the initial y value.
    * 4.c.2 If the <code>dist</code> is less-equal than a predefined <code>tolerance</code>
    *       then we start the process of pixel deletion for the vertical region around
    *       the adjusted pixel with coordinate y3.
    *       Deletion is done with the following steps:
    * 4.c.2.a We find all the continuous sequence of black pixels to the vertical neighbourhood of y3
    *         by scanning upwards and then downwards from y3, until a white pixel arises. or the image boundaries.
    * 4.c.2.b We set the found pixels to be BACKGROUND
    *
    * @returns the filtered Raster
    */
   private WritableRaster modelLine(WritableRaster src)
   {      
      if (src == null)
         throw new NullPointerException("raster is null");

      Number _off = getParameterValue(KEY_OFF);
      if (_off != null)
         off = _off.intValue();
      else
         throw new NullPointerException("off is null");

      Number _tolerance = getParameterValue(KEY_TOLERANCE);
      if (_tolerance != null)
         tolerance = _tolerance.intValue();
      else
         throw new NullPointerException("tolerance is null");

      Number _part = getParameterValue(KEY_PART);
      if (_part != null)
         part = _part.intValue();
      else
         throw new NullPointerException("part is null");


      int rows = src.getHeight();
      int cols = src.getWidth();

      int minRow = 0;
      int minCol = 0;
      int maxRow = rows - 1;
      int maxCol = cols - 1;

      // This is the image, that is returned by this algorithm
      WritableRaster raster1 = src; // img
      // This is the image, we use for scanning
      WritableRaster raster2 = createCompatibleDestRaster(raster1); // img2
      raster2.setPixels(0, 0, cols, rows,
              src.getPixels(0, 0, cols, rows, (int[]) null));

      int j = 0;   // checks the horizontal/vertical processing of the image

      Point p1 = null;
      Point p2 = null;

      while (j < 2) {
         //isLineFound = true;

         int lowerRowPos = maxRow;
         int offset = 0;


         // <code>(lowerRowPos - offset)</code> is the new bottom edge of the image,
         // that is left to be scanned for the detection of the next rule-line
         while (lowerRowPos - offset >= minRow) { 
            
            if(task != null && task.isCancelled())
               return null;
            // Check for an existent line


            boolean isZoneDeleted = true;

            //Note: returns the first (nearest to the top) row that has the maximum frequency in zero triads
            // Note: this line also sets the flag: isLineFound
            lowerRowPos = isRuled2(raster2, true); 

            if (lowerRowPos <= minRow)
               break;

            // Find the thickness of line, and extended by the value off
            int thickness = Math.abs(findThick(raster2, lowerRowPos) + off);

            // Calculate offset
            offset = 5 * thickness;

            if (isLineFound) {
               // Now we only have to delete an horizontal zone
               // in order to reveal another line if exists

               // Check if minimum boundary exceeds image boundary
               int topRow = lowerRowPos - offset;
               if (topRow < minRow)
                  topRow = minRow;

               // Check if maximum boundary exceeds image boundary
               int bottomRow = lowerRowPos + offset;
               if (bottomRow > maxRow)
                  bottomRow = maxRow;
               // Search for the first point(x1, y1) from the left half, that is
               // in the middle Point of the acceptable  consecutive sequence of pixels
               // (i.e. constisting of utmost <code>thickness</code> pixels, otherwise
               // if no such point is found it returns 'null'
               int zoneWidth = cols / 2;

               int zoneHeight = bottomRow - topRow + 1;
               //note the position is translated in the subraster's coordinates               

               Raster subRaster = raster2.createChild(0, topRow,
                       zoneWidth, zoneHeight, 0, 0, null);

               
               int leftRegionLowerRowPos = isRuled2(subRaster, false);

               if (leftRegionLowerRowPos != -1) {
                  p1 = findxy(subRaster, leftRegionLowerRowPos, thickness, LEFT_TO_RIGHT);

                  if (p1 != null) {
                     // Translate from subraster to raster coordinate system
                     p1.y += topRow;
                     leftRegionLowerRowPos = leftRegionLowerRowPos + topRow;

                  }

               } else // every pixel was white
                  p1 = null;


               // Search for the second point(x2, y2) from the right half
               //note the position is translated in the subraster's coordinates
               subRaster = raster2.createChild(cols - zoneWidth, topRow, zoneWidth,
                       zoneHeight, 0, 0, null);
               int rightRegionLowerRowPos = isRuled2(subRaster, false);

               if (rightRegionLowerRowPos != -1) {
                  p2 = findxy(subRaster, rightRegionLowerRowPos, thickness, RIGHT_TO_LEFT);

                  if (p2 != null) {
                     // Translate from subraster to raster coordinate system                     
                     p2.x += cols - zoneWidth;
                     p2.y += topRow; // y2=y2+a-1; in Java indexes start from 0
                     rightRegionLowerRowPos = rightRegionLowerRowPos + topRow; // limits(i,2)=limit+a-1
                  }

               } else // every pixel was white
                  p2 = null;


               // If we get two valid points, we have a line. We scan the line and for each line pixel p
               // we delete any pixel that exist in the vertical zone [p.y - thickness, p.y + thickness]

               if (p1 != null && p2 != null) {
    
                  // Calculate the 3rd point of the line by using the determinant
                  for (int x3 = minCol; x3 <= maxCol; ++x3) {
                     int y3 = findxy3(p1, p2, x3, minRow, maxRow);

                     if (y3 > maxRow)
                        y3 = maxRow;
                     else if (y3 < minRow)
                        y3 = minRow;

                     int dist = 0;
                     // If the current point is  not black and some of the nearby pixels are black
                     // then y3 is substituded by the nearest to it black pixel in the same column
                     // and also we get the distance <code>dist</code> from the initial y3
                     if (raster1.getPixel(x3, y3, (int[]) null)[0] != FOREGROUND) {
                        // [y3 dist]=correcty3(im,x3,y3,h);
                        int correctedY3 = correcty3(raster1, x3, y3);

                        dist = correctedY3 - y3;
                        if (dist < 0)
                           dist = -dist;

                        y3 = correctedY3;
                     }


                     if (dist <= tolerance) {

                        List<Point> blacks = new ArrayList<Point>(10);

                        // We know that y3 is black, so this is the first point in the list:
                        blacks.add(new Point(x3, y3));

                        for (int row = y3 - 1; row >= minRow
                                && raster1.getPixel(x3, row,
                                (int[]) null)[0] == FOREGROUND; --row) {

                           blacks.add(new Point(x3, row));


                        }
                        // We will need the black point with the min y, below
                        // note: the value is the last element added from the reverse for-loop
                        //       or if the for loop exits immediately, then we have the
                        //       initial point (x3,y3)
                        Point minYBlackPoint = minYBlackPoint = blacks.get(blacks.size() - 1);

                        for (int row = y3 + 1; row <= maxRow
                                && raster1.getPixel(x3, row,
                                (int[]) null)[0] == FOREGROUND; ++row) {

                           blacks.add(new Point(x3, row));


                        }
                       
                        
                        // Delete

                        if (blacks.size() <= thickness

                                || leftRegionLowerRowPos < 9
                                || leftRegionLowerRowPos > maxRow - 10) {
                           
                           for (Iterator<Point> it = blacks.iterator(); it.hasNext();) {
                              Point blackP = it.next();

                              raster1.setPixel(blackP.x, blackP.y, new int[]{BACKGROUND});
                           }
                           
                        }

                        else {
                           isZoneDeleted = false;
                        }

                        
                        int row = minYBlackPoint.y - thickness;

                        if (row < minRow)
                           row = minRow;
                         
                        for (; row <= maxRow; ++row) {
                           raster2.setPixel(x3, row, new int[]{BACKGROUND});
                        }                        


                     } // (dist > tolerance )
                         else {
                        isZoneDeleted = false;
                     } 
                  } // for ( int x3 = minCol; x3 <= maxCol; ++x3)
               } // ( ! ( isLineFound && p1 != null && p2 != null) )
                else {
                  isZoneDeleted = false;
               }
                

            } // if(isLineFound)
            else isZoneDeleted = false;


            raster2.setPixels(0, 0, cols, rows,
                    raster1.getPixels(0, 0, cols, rows, (int[]) null));

            // Delete extended region in order to repeat the process for the next line
            if (! isZoneDeleted)

               for (int row = Math.max(minRow, lowerRowPos - thickness);
                       row <= maxRow; ++row)
                  for (int col = minCol; col <= maxCol; ++col)
                     raster2.setPixel(col, row, new int[]{BACKGROUND});        

   

         } // end of while(lowerRowPos - offset > minRow)

         raster1 = rotate90(raster1, (j == 0) ? true : false);
         raster2 = rotate90(raster2, (j == 0) ? true : false);

         rows = raster1.getHeight();
         cols = raster1.getWidth();

         minRow = 0;
         minCol = 0;
         maxRow = rows - 1;
         maxCol = cols - 1;

         raster2.setPixels(0, 0, cols, rows,
                 raster1.getPixels(0, 0, cols, rows, (int[]) null));

         System.gc();

         ++j;
         

	     }


      return raster1;


   } // end of function ModelLine


   

   /**
    * 1. Starting from the bottom, for each column we find the row of the first foreground pixel
    * 2. For every column of the image we calculate
    *    the vertical distance of the current point in respect to that
    *    of the previous column (note: the distance for the 1st row is left to the default maximum, rows)     *
    * 3. Then we calculate the triads of columns, that all have zero distance. In order to do this:
    *    We have (cols - 2) such triads, and for each triad we keep the sum of the three distances
    * 4. For each of the zero triads, we update the histogram for their common row-coordinate.
    * 5. We find the first (nearest to the top) row of the lower profile,
    *    that has the maximum frequency in the histogram
    *
    *
    * @return lowerRowPos: a value in [minRow,maxRow] if rule line is found, otherwise -1
    * Sets global variable isLineFound
    */
   private int isRuled2(Raster raster, boolean checkIfIsValidLineLength)
   {     

      int rows = raster.getHeight();
      int cols = raster.getWidth();

      int minRow = 0;
      int minCol = 0;
      int maxRow = rows - 1;
      int maxCol = cols - 1;


      // Table where we store the frequency that each row
      // appears as the y-coordinate of the middle pixel in triads that have zero distance
      int[] hist = new int[rows];  // hist=zeros(rows,1);
      Arrays.fill(hist, 0);

      // It keeps for each column the nearest to the bottom edge of the image foreground pixel
      // (i.e. the y-coordinate of the pixel that belongs to the "Lower Profile")
      int[] lowProfile = new int[cols];

      Arrays.fill(lowProfile, minRow);

      // Keeps the vertical distance from the lowest foreground point of the previous column
      // We initialize it to the maximum value, i.e. there is no foreground point in the previous column
      // The value of the first column is constantly equal to the default maximum, <var>rows</var>
      int[] dist = new int[cols]; // dist=height*ones(1,width)
      Arrays.fill(dist, rows);


      // 1. Starting from the bottom, for each column we find the row of the first foreground pixel

      // 2. For every column of the image we  calculate the vertical distance
      //    of the current point in respect to that
      //    of the previous adjacent point in the profile

      // Locate profile for the 1st column (index 0)

   
      int col = 0;
      for (int row = maxRow; row >= minRow; --row) {
         if (raster.getPixel(col, row, (int[]) null)[0] == FOREGROUND) {
            lowProfile[col] = row;
            break;
         }
      }

      for (++col; col <= maxCol; ++col) { // Locate profile for the rest of the columns


         for (int row = maxRow; row >= minRow; --row) {

            if (raster.getPixel(col, row, (int[]) null)[0] == FOREGROUND) {

               lowProfile[col] = row;

               // Distance from the point of the previous column
               dist[col] = Math.abs(lowProfile[col] - lowProfile[col - 1]);

               break;
            }
         }

      }

      // 3. We calculate the triads of columns, that all have zero distance.
      int sumDistLen = dist.length - 2;
      int[] sumDist = new int[sumDistLen];

      for (int i = 0; i < sumDistLen; ++i)
         sumDist[i] = dist[i] + dist[i + 1] + dist[i + 2];


      // 4. For each of the zero triads, we update the histogram.
      int numOfZeroTriads = 0;
      for (int i = minRow; i < sumDistLen; ++i) {
         if (sumDist[i] == 0) {
            ++numOfZeroTriads;
            // hist is the histogram of the positions of zero differences
            // note: because the two edges of a triad overlap with the neighbouring triads
            //       we use the value of the middle point of the triad as index to <var>hist</var>
            ++hist[lowProfile[i + 1]];
         }
      }


      // 5. We find the row that has the maximum frequency in the histogram

      int peak = 0;
      int lowerRowPos = -1;  
      for (int row = 0; row <= maxRow; ++row) {
         // We return the first (nearest to the top) row, if we have many identical maximum values
         // In order to return the nearest to the bottom row, change the next line to
         if (hist[row] > peak) {
            peak = hist[row];
            lowerRowPos = row;
         }

      }
      

      // condition for a line to exist is to be greater than cols / part
      if (checkIfIsValidLineLength){

         if (part != 0 && numOfZeroTriads > cols / part)
            isLineFound = true;
         else
            isLineFound = false;
      }    

      return lowerRowPos;

   } // end of function isRuled2

   /**
    * 1. Using the given lower row, <code>lowpos</code>, we scan this line
    *    for all the pixels that are black, and are followed in the next line
    *    by a white pixel (or <code>lowpos</code> is the last line).
    * 2. For each of the pixels in <code>lowpos</code>, found in step 1
    *    we count the thickness, which is defined, as the sequence of
    *    consecutive pixels, scanning upwards, that are all black.
    * 3. We return the most frequent thickness we calculated in the step 2
    *
    * @param lowerRowPos: the nearest to the bottom line that constitutes a rule-line
    *
    * @returns the most frequent thickness of a line (if more than one thicknesses
    * have the maximum value, then the smaller of them is returned)
    */
   private int findThick(Raster raster, int lowerRowPos)
   {
      int rows = raster.getHeight();
      int cols = raster.getWidth();

      int minRow = 0;
      int minCol = 0;
      int maxRow = rows - 1;
      int maxCol = cols - 1;


      // Keeps the frequencies of the various each possible thickness values
      // i.e. each index of the array is the thickness value and the value is the frequency
      int[] freqOfThickness = new int[rows + 1]; // A thickness can vary from 0 to maxRow.
      Arrays.fill(freqOfThickness, 0); 

      for (int col = minCol; col <= maxCol; ++col) {

         // If the current pixel is black and the next, if exists, is white
         if (raster.getPixel(col, lowerRowPos, (int[]) null)[0] == FOREGROUND
                 && // Because we scan the total row, the following condition
                 // ensures that only the width of the pixels that constitute
                 // the lower profile are used for counting the thickness.
                 (lowerRowPos == maxRow
                 || raster.getPixel(col, lowerRowPos + 1, (int[]) null)[0] == BACKGROUND)) {

            // begin counting
            int thickness = 1; // count this pixel
            int row = lowerRowPos - thickness;
            while (row >= minRow && raster.getPixel(col, row, (int[]) null)[0] == FOREGROUND) {
               ++thickness;
               --row;
            }

            ++freqOfThickness[thickness];
         }
      }


      // Note: if there are more than one most frequent thickness values
      //       then the smaller value is returned.
      int mostFreqThickness = 0;
      int maxFrequency = 0;      
      for (int thickness = minRow, len = freqOfThickness.length;
              thickness < len; ++thickness) {
         
         if (maxFrequency < freqOfThickness[thickness]) {
            maxFrequency = freqOfThickness[thickness];
            mostFreqThickness = thickness;
         }

      }

      return mostFreqThickness;
   } // end of function findThick


   private static final int LEFT_TO_RIGHT = 0;
   private static final int RIGHT_TO_LEFT = 1;

   /**
    * 1. We find the lower row position, that belongs to a rule line
    *    That will be the lower limit to all the operations that follow
    * 2. Starting from the row limit found in step 1,
    *    we search a vertical strip of columns, from minCol to half width
    *    for a black pixel that is either already in the last row or is followed
    *    in the next row,  by a white pixel.
    *    and then run upwards until we meet a white pixel, or the minRow boundary
    *    In other words, we calculate the number of consecutive pixels in a column
    *    starting from pixels in the limit row, and going upwards.
    *   2.a if we find a consecutive sequence that is less than thickness, then
    *       we save the middle point coordinates, else
    *   2.b we continue scanning the next column.   
    *
    * @param raster: modelLine specifies a subraster for columns 0 to 499 and also a range of rows
    * @param lowerRowPos : the row with the highest frequency of zero-triads in the lower profile
    *                      in the given raster
    * @param thickness
    * @returns:
    *          - the middle Point of the first acceptable consecutive sequence of pixels,
    *            (i.e. constisting of at most <code>thickness</code> pixels, otherwise
    *            if no such point is found we return 'null' // i.e no need for the flagy
    *
    */
   private Point findxy(Raster raster, int lowerRowPos, int thickness, int direction)
   {
      int minRow = 0;
      int minCol = 0;
      int maxRow = raster.getHeight() - 1;
      int maxCol = raster.getWidth() - 1;


      Point p = null;

      if (lowerRowPos >= minRow)
         switch (direction) {
            case LEFT_TO_RIGHT:

               for (int col = minCol; col <= maxCol; ++col) { 
                  p = getLinePoint(raster, col, lowerRowPos, thickness);
                  if (p != null)
                     break;
               }

               break;

            case RIGHT_TO_LEFT:

               for (int col = maxCol; col >= minCol; --col) {
                  p = getLinePoint(raster, col, lowerRowPos, thickness);
                  if (p != null)
                     break;
               }

               break;

            default:
               throw new IllegalArgumentException("Direction argument is invalid.");

         }


      return p;
   } // end of function findxy

   private Point getLinePoint(Raster raster, int x, int y, int thickness)
   {

      int minRow = 0;
      int minCol = 0;
      int maxRow = raster.getHeight() - 1;
      int maxCol = raster.getWidth() - 1;

      Point p = null;

      int kk = 0; // number of consecutive black pixels
      if (raster.getPixel(x, y, (int[]) null)[0] == FOREGROUND 
              && (y == maxRow
              || raster.getPixel(x, y + 1, (int[]) null)[0] == BACKGROUND)) {

         kk = 1;

         int row = y - kk;
         while (row > minRow 
                 
                 && raster.getPixel(x, row, (int[]) null)[0] == FOREGROUND) {
            ++kk;
            --row;
         }


         // We accept if the number of consecutive black pixels is less or equal to thickness
         if (kk <= thickness) {

            y = y - (((kk + 1) / 2)); 

            p = new Point(x, y);

         } // else p = null;
      } // else p = null;

      return p;
   }


   /**
    * For the given column x3, we scan the image to find the nearest black pixel
    * in two directions in relation to a given row y3.
    * The upward scan starts from y3 to the top
    * while the downward scan starts from y3 to a given distance h
    * In the end the function returns:
    * @returns y3 : the black pixel y-coordinate (in the same column)
    *               that is nearest to the given row y3, or the original y3
    *               if there is no black pixel in the neighbourhood
    */
   public int correcty3(Raster raster, int x3, int y3)
   {
      int minRow = 0;
      int maxRow = raster.getHeight() - 1;

      // We set it to a value greater than tolerance
      int notToleratedDist = tolerance + 1;


      int ymin_limit = y3 - notToleratedDist;
      if (ymin_limit < minRow)
         ymin_limit = minRow;

      int ymin = ymin_limit;

      // Finds the nearest (to y3) black, starting from the top

      for (int row = y3; row >= ymin_limit; --row) {

         if (raster.getPixel(x3, row, (int[]) null)[0] == FOREGROUND) {
            ymin = row; 
            break;
         }
      }

      int ymax_limit = y3 + notToleratedDist;
      if (ymax_limit > maxRow)
         ymax_limit = maxRow;

      int ymax = ymax_limit; //int ymax = hx2;
      // Finds the nearest (to y3) black, towards the bottom of the image

      for (int row = y3; row <= ymax_limit; ++row) {
 
         if (raster.getPixel(x3, row, (int[]) null)[0] == FOREGROUND) {

            ymax = row;
            break;
         }
      }

      // It keeps the one that is nearest to the row y3

      if (y3 - ymin < ymax - y3)
         y3 = ymin;
      else
         y3 = ymax;

      return y3;

   } //end of correcty3


   /**
    * @return a clone of this object
    * @overrides method clone of <code>StandardBinaryOp</code>
    */
   public StandardBinaryOp clone()
   {
      LowerProfileOfZeroTriadsOp op = new LowerProfileOfZeroTriadsOp();

      DefaultParameterToolbar tbar = new DefaultParameterToolbar(getName());
      op.setToolbar(tbar);

      Parameter[] params = getParameters();

      for(Parameter param : params)
         tbar.createParameter(param.name, param.desc, param.type,
                 param.min, param.max, param.value);

      return op;
   }

    /**    
    * Calculate the 3rd point of the line, using the determinant
    * @param p1: the 1st point that belongs to the line
    * @param p2: the 2nd point that belongs to the line
    * @param x3: the x-coordinate, of the 3rd point
    * @param minY: the minimum y coordinate
    * @param maxY: the maximum y coordinate
    */
 
   private int findxy3(Point p1, Point p2, int x3, int minY, int maxY)
   {
   // Calculate the determinant

   double y3 =  ( (-p1.y) * det(new Point(1,1), new Point(p2.x,x3)) +
   p2.y  * det(new Point(1,1), new Point(p1.x,x3)) ) / (double)
   det(new Point(1,1), new Point(p1.x, p2.x));

   int y3_int = (int) (2 * y3 + 1) / 2;

   // Correction to be inside image boundaries
   if(y3_int > maxY)
   y3_int = maxY;
   if(y3_int < minY)
   y3_int = minY;

   return y3_int;
   } // end of function findxy3
   
   /** 
    * Calculate the determinant
    */   
   private int det(Point p1, Point p2)
   {
   return (p1.x * p2.y) - (p2.x * p1.y);
   }
    

}
