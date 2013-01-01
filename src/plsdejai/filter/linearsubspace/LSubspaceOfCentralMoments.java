package plsdejai.filter.linearsubspace;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;


/**
 * A specialization of the LSuspace that calculates feature vectors
 * using central moments
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 09/01/2012
 */
public class LSubspaceOfCentralMoments extends LSubspace
{
   private int k;
   private int windowHalfSide;
   private int windowSide;

   public LSubspaceOfCentralMoments(){ super(0, 0.0); }

   /**
    *
    * @param windowHalfSide : Vectors are calculated for each pixel (x, y),
    *            using a rectangular neighborhood (2n + 1) x (2n + 1) around (x,y)

    * The <code>window</code> parameter is an additional constraint, that does not
    * change the calculations inside the subspace, but it shows how large is
    * the neighbourhood, that another class uses to calculate the vectors,
    * that are provided to the subspace.
    * @param e : Reconstruction error d(r, v). It quantifies the abilility of
    *            the subspace S to represent the feature space, where d(..., ...)
    *            is any normalized distance measure.
    * @param k : we compute central moments of orders from 0 up to k − 1
    *            in both x and y directions. Note: vector-size = k * k + 4
    */
   public LSubspaceOfCentralMoments(int windowHalfSide, int k, double e)
   {
      super(k * k + 4, e);

      setWindowHalfSide( windowHalfSide);
      setMomentMaxOrder(k);      

   }

   /**
    * This constructor adds to the subspace the raster of a training image
    * @param n
    * @param k
    * @param e
    * @param raster: The raster of a binary image
    */
   public LSubspaceOfCentralMoments(int n, int k, double e, BufferedImage bi, SwingWorker task)
   {
      this(n, k, e);
      addTrainingData(bi, task);
   }


   public final void setWindowHalfSide( int windowHalfSide)
   {
      if (windowHalfSide <= 0){
         windowHalfSide = 0;
      }

      this.windowHalfSide = windowHalfSide;
      windowSide = 2 * windowHalfSide + 1;
   }

   public int getWindowHalfSide() { return windowHalfSide; }

   public int getWindowSide() { return windowSide; }

   public final void setMomentMaxOrder(int k)
   {
      if (k < 0) {
         k = 0;
      }

      this.k = k;

      setVectorSize(k * k + 4);

   }

   public int getMomentMaxOrder(){ return k; }


   public boolean isValidRaster(Raster raster)
   {
      // A valid raster should have at least the dimensions of window * window
      if (raster.getWidth() < windowSide || raster.getHeight() < windowSide){
         JOptionPane.showMessageDialog(null, "Raster size is two small. No data added to the subspace",
                 "Invalid image", JOptionPane.INFORMATION_MESSAGE);
         return false;
      }

      return true;
   }

   protected boolean isEdgePoint(Point p, Raster raster)
   {
      int wHalfSide = getWindowHalfSide();

      if (p.x - wHalfSide >= 0 && p.y - wHalfSide >= 0
              && p.x + wHalfSide < raster.getWidth()
              && p.y + wHalfSide < raster.getHeight())
         return false;

      return true;
   }

   protected void writeSubspaceHeaders( ObjectOutputStream out )
           throws EOFException, IOException
   {
      super.writeSubspaceHeaders(out);

      out.writeInt(windowHalfSide);
   }

   protected void readSubspaceHeaders( ObjectInputStream in )
           throws EOFException, IOException
   {
      super.readSubspaceHeaders(in);

      setMomentMaxOrder((int) Math.sqrt(getVectorSize() - 4));

      setWindowHalfSide(in.readInt());
   }

   protected boolean isValidHeaders(ObjectInputStream in)
           throws EOFException, IOException
   {
      if ( ! super.isValidHeaders(in) )
         return false;

      int hWindowSide = in.readInt();

      if (hWindowSide != windowHalfSide) {
         JOptionPane.showMessageDialog(null,
                 "Failed, because we try to load a subspace with window " + hWindowSide
                 + " \nto the current subspace with half window side " + windowHalfSide, "Loading failure",
                 JOptionPane.INFORMATION_MESSAGE);
         return false;
      }

      return true;

   }

   /**
    * For a given pixel (x, y), and a rectangular neighborhood
    * (2n + 1) x (2n + 1) around (x,y), k*k central moments
    * are calculated in both x and y directions, and also,
    * for both horizontal and vertical directions,
    * the standard deviation and kurtosis of the projection profiles are calculated
    * Returns null if the neighbourhood  has all of its pixels 0.
    */
   public final double[] getFeatureVector(int [][] iArr, Point p)
   {

      int maxWidth = iArr.length - 1;
      int maxHeight = iArr[0].length - 1;

      // Calculate neighborhood.
      // windowHalfSide : n; windowSide = 2 * n + 1;
      // If p is not an edge point:
      int x0 = p.x - windowHalfSide;
      int y0 = p.y - windowHalfSide;
      int w = windowSide;
      int h = windowSide;
      /* *** Edge points ********* */
      int diff = p.x - windowHalfSide;
      if (diff < 0){
         x0 = 0;
         w = w + diff; // diff is negative
      }

      diff = p.y - windowHalfSide;
      if (diff < 0) {
         y0 = 0;
         h = h + diff; // diff is negative
      }

      diff = p.x + windowHalfSide - maxWidth;
      if (diff > 0){
         w = w - diff; // diff is positive
      }

      diff = p.y + windowHalfSide - maxHeight;
      if(diff > 0){
         h = h - diff; // diff is positive
      }
      /* ************************* */

      if (w <= 0 || h <= 0){ // For safety only. This could not ever happen

         return null;
      }

      Rectangle neighborhood = new Rectangle(x0, y0, w, h);

      int vSize = getVectorSize();

      double[] v = new double[vSize];

      double vProfile[] = new double[neighborhood.height];
      double hProfile[] = new double[neighborhood.width];

      double yLen = neighborhood.y + neighborhood.height;
      double xLen = neighborhood.x + neighborhood.width;

      // Compute variance & kurtosis for the vertical projection profiles
      Arrays.fill(vProfile, 0);

      for (int y = neighborhood.y, index = 0; y < yLen; ++y, ++index) {
         for (int x = neighborhood.x; x < xLen; ++x) {
            vProfile[index] += iArr[x][y];
         }
      }

      double vProfileMean = 0;
      int len = vProfile.length;
      for (int i = 0; i < len; ++i) {
         vProfileMean += vProfile[i];
      }

      vProfileMean = vProfileMean / len;

      // if vProfileMean = 0 then there is no foreground pixel in the neighbourhood,
      //  and no feature vector should be created
      if (vProfileMean == 0)
         return null;


      double variance = getProfileCentralMoment(vProfile, vProfileMean, 2);

      v[vSize - 4] = Math.sqrt(variance);

      /* Kyrtosis will be defined here as the normalized fourth
       * central moment: k = E(X - mean)^4 / σ^4 */
      v[vSize - 2] = getProfileCentralMoment(vProfile, vProfileMean, 4); // /(variance * variance);

      // Compute variance kyrtosis for the horizontal projection profiles
      Arrays.fill(hProfile, 0);


      for (int x = neighborhood.x, index = 0; x < xLen; ++x, ++index) {
         for (int y = neighborhood.y; y < yLen; ++y) {
            hProfile[index] += iArr[x][y];
         }
      }

      double hProfileMean = 0;
      len = hProfile.length;
      for (int i = 0; i < len; ++i) {
         hProfileMean += hProfile[i];
      }

      hProfileMean = hProfileMean / len;

      variance = getProfileCentralMoment(hProfile, hProfileMean, 2);
      v[vSize - 3] = Math.sqrt(variance);
      v[vSize - 1] = getProfileCentralMoment(hProfile, hProfileMean, 4) ;// /(variance * variance);

      for (int kx = 0, index = 0; kx < k; ++kx) {
         for (int ky = 0; ky < k; ++ky, ++index) {
            v[index] = getCentralMoment(iArr, neighborhood, kx, ky);
         }
      }

      return v;
   }

   private double getRawMoment(int[][] iArr, Rectangle neighborhood, int kx, int ky)
   {
      double moment = 0;

      double yLen = neighborhood.y + neighborhood.height;
      double xLen = neighborhood.x + neighborhood.width;

      for (int y = neighborhood.y; y < yLen; ++y) {
         for (int x = neighborhood.x; x < xLen; ++x) {
            moment += Math.pow(x, kx) * Math.pow(y, ky) * iArr[x][y];
         }
      }

      return moment;
   }

   /**
    * For a given pixel (x, y) and a rectangular neighborhood around (x,y),
    * central moments are calculated in both x and y directions
    * @param iArr
    * @param p
    * @param neighborhood
    * @param kx
    * @param ky
    * @return
    */
   private double getCentralMoment(int[][] iArr,
           Rectangle neighborhood, int kx, int ky)
   {
      double moment = 0;

      int yLen = neighborhood.y + neighborhood.height;
      int xLen = neighborhood.x + neighborhood.width;

      double sumOfLevels = getRawMoment(iArr, neighborhood, 0, 0);
      double meanX = getRawMoment(iArr, neighborhood, 1, 0) / sumOfLevels;
      double meanY = getRawMoment(iArr, neighborhood, 0, 1) / sumOfLevels;

      if (kx == 0 && ky == 0)
         for (int y = neighborhood.y; y < yLen; ++y) {
            for (int x = neighborhood.x; x < xLen; ++x) {
               moment += iArr[x][y];
            }
         }
      else if (kx == 0)
         for (int y = neighborhood.y; y < yLen; ++y) {
            for (int x = neighborhood.x; x < xLen; ++x) {
               moment += Math.pow(y - meanY, ky) * iArr[x][y];
            }
         }
      else if (ky == 0)
         for (int y = neighborhood.y; y < yLen; ++y) {
            for (int x = neighborhood.x; x < xLen; ++x) {
               moment += Math.pow(x - meanX, kx) * iArr[x][y];
            }
         }
      else // kx != 0 && ky != 0
         for (int y = neighborhood.y; y < yLen; ++y) {
            for (int x = neighborhood.x; x < xLen; ++x) {
               moment += Math.pow(x - meanX, kx) * Math.pow(y - meanY, ky) * iArr[x][y];
            }
         }

      int iMoment = (int) moment;
      return ((Math.abs(moment - iMoment) < 0.00000000000001)
              ? (double) iMoment : moment);
   }

   private double getProfileCentralMoment(double[] data, double mean, int k)
   {
      double moment = 0;

      int len = data.length;
      for (int i = 0; i < len; ++i) {
         moment += Math.pow(data[i] - mean, k);
      }
      moment = moment / len;

      int iMoment = (int) moment;
      return ((Math.abs(moment - iMoment) < 0.00000000000001)
              ? (double) iMoment : moment);
   }


}
