package plsdejai.filter.linearsubspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import plsdejai.StandardBinaryOp;

   /**
    * <code>LSubspace</code> implements a linear subspace, in which
    * we can add vectors of the same size.
    * The results of adding a vector in the subspace or checking if a vector belongs
    * to the subspace, depends on <code>vSize</code> and <code>error</error>
    *
    * In this subspace implementation, the following rules are keeped:
    * 1. Changing the <code>vSize</code> automatically clears this subspace
    * 2. Changing <code>error</error> to a valid value
    *    keeps the subspace vectors. One should call the <code>clear()</code> method
    *    in order to delete all the existent vectors.
    *
    * @author Prokopiou Konstantinos
    * @version 1.0, 09/01/2012
    */
public abstract class LSubspace
{
   protected List<double[]> vectorList;
   protected int vectorSize;
   private double error;

   public static String SUBSPACE_SIZE_CHANGED_PROPERTY = "subspaceSizeChanged";
   PropertyChangeSupport changeSupport;

   public LSubspace(int vSize, double e)
   {
      vectorList = new ArrayList<double[]>(vectorSize); // capacity = vectorSize
      changeSupport = new PropertyChangeSupport(this);

      setVectorSize(vSize);
      setError(e);
   }
   
   public final void setError(double e)
   {
      if (e < 0.0)
         error = 0.0;
      else
         error = e;
   }

   public final double getError() { return error; }

   public final void setVectorSize(int size)
   {
      if (size < 0)
         vectorSize = 0;
      else
         vectorSize = size;

      // invalidate current subspace
      if (! vectorList.isEmpty())
         vectorList.clear();

   }

   public final int getVectorSize() { return vectorSize; }


   public final List<double[]> getVectors()
   {
      return new ArrayList<double[]>(vectorList);
   }

   public int size() { return vectorList.size(); }

   public boolean isEmpty() { return vectorList.isEmpty(); }

   public void clear()
   {
      int oldValue = vectorList.size();

      vectorList.clear();

      if (oldValue != 0)
         firePropertyChange(SUBSPACE_SIZE_CHANGED_PROPERTY, oldValue, 0);

   }
  
   public abstract double[] getFeatureVector(int [][] iArr, Point p);

   /**
    * Wrapper method
    * @param l
    */
   public void addPropertyChangeListener(PropertyChangeListener l)
   {
      changeSupport.addPropertyChangeListener(l);
   }

   /**
    * Wrapper method
    * @param property
    * @param oldValue
    * @param newValue
    */
   public void firePropertyChange(String property, Number oldValue, Number newValue)
   {
      changeSupport.firePropertyChange(property, oldValue, newValue);
   }

   /**
    * A convenience method for adding a collection of vectors
    * It is implemented by calling iteratively the add(double[]) method
    * @param vectors
    * @return true, if at least one vector has added to to the subspace
    */
   public boolean add(Collection<double[]> vectors)
   {
      boolean isChanged = false;

      int oldValue = vectorList.size();

      for (Iterator<double[]> it = vectors.iterator() ; it.hasNext() /*&&
              vectorList.size() < 5 * vectorSize*/;) {
         if (add(it.next()))
            isChanged = true;
      }

      if (isChanged)
         firePropertyChange(SUBSPACE_SIZE_CHANGED_PROPERTY, oldValue, vectorList.size());

      return isChanged;
   }

   /**
    * @param v : the vector that is to be added in this subspace
    * @return true if the vector is added to subspace; otherwise false.
    */
   public boolean add(double[] v)
   {
      boolean isChanged = false;

      if (v == null)
         return false;

      if (v.length != vectorSize) {

         return false;
      }

      //int oldValue = vectorList.size();


      if (vectorList.isEmpty()) { // Initialize subspace


         if (normalizeVector_ip(v) == null) { // Vector is the 0 vector or has zero size
            return false;
         }

         if (vectorList.add(v))
            isChanged = true;

      } else { // Increment subspace

         double[] vres = getNewSubspaceVector(v);
         if (vres != null) {

            if (vectorList.add(vres))
               isChanged = true;

         } 
      }

      return isChanged;
   }

   /**
    * Transforms a given vector, to a vector suitable to be added to the subspace
    * @param v
    * @return the appropriate vector to be added to the subspace, or <code>null</code>
    *         if the vector cannot be transformed, or if the tranformed vector
    *         already exists in the subspace.
    */   
   private double[] getNewSubspaceVector(double[] v)
   {
      

      if (v == null || v.length == 0)
         return null;

      if (vectorList.isEmpty()){
         return null;
      }

      double p[] = matrixMultiply(vectorList, true, v);

      double r[] = matrixMultiply(vectorList, false, p);

      // Calculate the reconstruction error
      double e = getReconstructionError(v, r);

      // If e is smaller than the threshold this.error, this means that subspace
      // can represent v sufficiently accurately; otherwise the subspace must
      // must be updated
      if (e > error) {

         // ************* The correction ***********
         // Calculate the residual vector
         double[] vres = r;
         for (int i = 0, len = r.length; i < len; ++i) {
            vres[i] = r[i] - v[i];
         }

         // ****************************************

         if (normalizeVector_ip(vres) == null) { // Vector is the 0 vector
            return null;
         }

         return vres;
      }
      
      return null;
   }

   

   /**
    * @param v
    * @return true if vector v belongs to the subspace, or v is null
    */
   public boolean isInSubspace(double[] v)
   {
      // getNewSubspace takes care if v == null
      //return (getNewSubspaceVector(v) == null);

      if (v == null || v.length == 0)
         return false;

      if (vectorList.isEmpty()){

         return false;
      }

      double p[] = matrixMultiply(vectorList, true, v);


      double r[] = matrixMultiply(vectorList, false, p);

      // Calculate the reconstruction error
      double e = getReconstructionError(v, r);

      if (e > error){

         return false;
         
      } else {

         return true;
      }
         

   }

   /**
    * In-place normalization: a vector is divided by its norm
    * @param v : the vector that is to be normalized
    * @return this v in its normalized form, or null if v is the zero vector
    */
   private double[] normalizeVector_ip(double[] v)
   {
      double norm = 0;
      for (int i = 0, len = v.length; i < len; ++i) {
         norm += v[i] * v[i];
      }
      norm = Math.sqrt(norm);


      if (norm == 0) // v was the zero vector or v has zero size
         v = null;
      else {
         for (int i = 0, len = v.length; i < len; ++i) {
            v[i] = v[i] / norm;
         }

      }

      return v;

   }

   /**
    *
    * @param subspace : a List of i vectors of size vSize, representing
    *             the incrementing subspace. It corresponds to a matrix vSize x i
    * @param isTranspose: if true, the Tranpose of <code>subspace</code> is used.
    * @param vec : a vector of size vSize. It corresponds to a matrix vSize x 1
    * @return a i x 1 matrix, corresponding to a vector.
    *         where i is the number of vectors in m1 (i.e. m1.size()) if
    *         isTranspose = true; otherwise the size of a vector, i.e. this.n
    */
   public double[] matrixMultiply(List<double[]> subspace, boolean isTranspose, double[] vec)
   {
      double[] product = null;
      if (isTranspose) {
         product = new double[subspace.size()];
         Arrays.fill(product, 0);
         int row = 0;
         for (Iterator<double[]> it = subspace.iterator(); it.hasNext(); ++row) { // rows
            double v[] = it.next();
            for (int i = 0, len = v.length; i < len; ++i) { // v.length = vSize

               product[row] += v[i] * vec[i];
            }
         }

      } else {
         product = new double[vectorSize];
         Arrays.fill(product, 0);
         int col = 0;
         for (Iterator<double[]> it = subspace.iterator(); it.hasNext(); ++col) { // cols
            double v[] = it.next();
            for (int i = 0, len = v.length; i < len; ++i) { // v.length = vSize

               product[i] += v[i] * vec[col];
            }
         }

      }

      return product;
   }

   /**
    * Calculates the reconstruction error, by using the euclidean distance
    * @param v
    * @param r
    * @return
    */
   private double getReconstructionError(double[] v, double[] r)
   {
      double e = 0;

      int vLen = v.length; // It should be vSize, and also must be vLen = r.length

      // Eucledian distance
      for (int i = 0; i < vLen; ++i) {
         e += Math.pow(r[i] - v[i], 2);
      }
      e = Math.sqrt(e);

      /*
      for (int i = 0; i < vLen; ++i)
      e += Math.abs(r[i] - v[i]);

      // This is a Manhatan distance, but the following makes it non-Manhatan ???
      // Another error of Doermann !!!
      e = Math.sqrt(e);
       */

      return e;
   }

   public abstract boolean isValidRaster(Raster raster);

   /**
    * Specifies whether the point p is an edge point
    * and so must have a special treatment by the algorithm
    * @param p
    * @param raster
    * @return
    */
   protected abstract boolean isEdgePoint(Point p, Raster raster);

   /**
    * Adds vectors to the subspace from a training ground truth image
    * @param bi
    * @param task the thread that calls this method
    */
   public void addTrainingData(BufferedImage bi, SwingWorker task)
   {
      bi = StandardBinaryOp.change2BinaryColorModel(bi);
      if (bi == null){
         JOptionPane.showMessageDialog(null, "We can only extract training data from binary images!",
                 "Invalid image data", JOptionPane.INFORMATION_MESSAGE);
            return;
      }

      Raster raster = bi.getRaster();

      if (! isValidRaster(raster))
         return;

      int width = raster.getWidth();
      int height = raster.getHeight();

      /* 0 -> total number of processed pixels
       * 1 -> number of edge pixels
       * 2 -> number of pixels added to subspace
       * 3 -> number of pixels already belonging to the subspace
       * 4 -> number of pixels rejected because getFeature failed
       *          (because all the pixels in the neighborhood have zero value)       *
       */
      int stats[] = new int[]{0, 0, 0, 0, 0};      


      int oldValue = getVectorSize();

      // Translate raster to 1 (foreground) and 0 (background)
      int iArr[][] = new int[width][height];

      for (int y = 0; y < height; ++y) {

         for (int x = 0; x < width; ++x) {
            if (raster.getPixel(x, y, (int[]) null)[0] == StandardBinaryOp.FOREGROUND)
               iArr[x][y] = 1;
            else
               iArr[x][y] = 0;
         }
      }

      double[] v = null;


      for (int y = 0; y < height; ++y) {

         if (task != null && task.isCancelled()){
            return;
         }

         for (int x = 0; x < width; ++x) {

            if (iArr[x][y] == 1){ // A foreground pixel

               if (!isEdgePoint(new Point(x, y), raster)) { // Non-edge points

                  v = getFeatureVector(iArr, new Point(x, y));

                  // if mean = 0 then there is no foreground pixel in the neighbourhood,
                  //  and no feature vector should be created
                  if (v == null) {
                     // a feature vector could not be calculated
                     // (because all the pixels in the neighbourhood have zero value)

                     continue;
                  }


                  add(v);

               }
               /* ************************************************** */

            }

         }
      }

      int newValue = getVectorSize();
      if (oldValue != newValue)
         firePropertyChange(SUBSPACE_SIZE_CHANGED_PROPERTY, oldValue, newValue);
      
   }


   

   /* ********************************************************** */
   // **** Functions that should be overrided
   /* ********************************************************** */
   /**
    * Note: the methods writeubspaceHeaders, readSubspaceHeaders,
    *       and isValidHeaders should be overriden altogether
    * 
    * This function stores the headers, which are the values
    * that identify the subspace.
    *
    * The two values that should always included is the
    * <code>vectorSize</code> and <code>error</code>.
    *
    * This method should be overriden, in order to add more headers.
    * The proper way to override this method is to always start
    * with super.storeSubspaceHeader()
    */
   protected void writeSubspaceHeaders( ObjectOutputStream out )
           throws EOFException, IOException
   {
      out.writeInt(vectorSize);
      out.writeDouble(error);      
   }

   /**
    * Note: the methods writeSubspaceHeaders, readSubspaceHeaders,
    *       and isValidHeaders should be overriden altogether
    *
    * This function reads the headers, which are the values that identify
    * the subspace, from a file in which a subspace is stored, and sets
    * the corresponding values of this subspace.
    *
    * The two values that should always included and set
    * is the <code>vectorSize</code> and <code>error</code>.
    *
    * By setting the vectorSize, the subspace automatically rejects (clears)
    * all the vectors in the subspace
    *
    * This method should be overriden, in order to add more headers.
    * The proper way to override this method is to always start
    * with super.storeSubspaceHeader()
    */
   protected void readSubspaceHeaders( ObjectInputStream in )
           throws EOFException, IOException
   {
      // this also calls subspace.clear(), so you is better to call it first!
      setVectorSize(in.readInt());

      setError(in.readDouble());

   }

   /**
    * Note: the methods writeSubspaceHeaders, readSubspaceHeaders,
    *       and isValidHeaders should be overriden altogether
    *
    * This function checks if the the headers, which are the values
    * that identify the subspace, that are extracted from a file representing
    * a subspace, are valid.
    * The two values that should always included is the
    * <code>vectorSize</code> and <code>error</code>.
    *
    * This method should be overriden, in order check additional headers.
    * The proper way to override this method is to always start with:
    *    if ( ! super.isValidHeaders(in) )
    *       return false;
    */
   protected boolean isValidHeaders(ObjectInputStream in)
           throws EOFException, IOException
   {
      int size = in.readInt();

      if (size != vectorSize) {
         JOptionPane.showMessageDialog(null,
                 "Failed, because we try to load a subspace with vector size " + size
                 + " \nto the current subspace with vector size " + vectorSize, "Loading failure",
                 JOptionPane.INFORMATION_MESSAGE);
         return false;
      }

      double e = in.readDouble();

      if (e != error) { // @test: change to e - error > 0.0000001
         JOptionPane.showMessageDialog(null,
                 "Failed, because we try to load a subspace with reconstruction error " + e
                 + " \nto the current subspace with reconstruction error " + error, "Loading failure",
                 JOptionPane.INFORMATION_MESSAGE);
         return false;
      }
      
      return true;
   }
   /* ********************************************************** */


   /**
    * Stores the subspace vectors to the specified file
    * @param f
    */
   public void storeSubspace(File f)
   {

      ObjectOutputStream out = null;
      try {
         out = new ObjectOutputStream(
                 new GZIPOutputStream(
                 new FileOutputStream(f)));

         writeSubspaceHeaders(out);

         for (Iterator<double[]> it = vectorList.iterator(); it.hasNext();) {
            out.writeObject(it.next());
         }

      } catch (EOFException e) {

      } catch (IOException e) {

      } catch (NullPointerException e) {

      } finally {
         try {
            if (out != null) {
               out.flush();
               out.close();
            }
         } catch (Exception e) { }
      }

   }

 
      /**
       * Loads the subspace stored in a file, after the current subspace is cleared
       * @param filename
       */
      public void loadSubspace(File f)
      {
         int oldValue = vectorList.size();

         ObjectInputStream in = null;

         try {
            in = new ObjectInputStream(
                    new GZIPInputStream(
                    new FileInputStream(f)));

            // this statement should also clear the subspace
            // by calling the method setVectorSize
            readSubspaceHeaders(in );

            for (;;){
               double []v = (double []) in.readObject();
               vectorList.add(v);
            }

         } catch (EOFException e) {

         } catch (IOException e) {

         } catch (ClassNotFoundException e) {

         } catch (NullPointerException e){

         } finally {
         try {
            if (in != null)
               in.close();
         } catch (Exception e) { }

         }

         int newValue = vectorList.size();
         if (newValue != oldValue)
            firePropertyChange(SUBSPACE_SIZE_CHANGED_PROPERTY, oldValue, vectorList.size());

      }

      /**
       *
       * @return a string representation of the subspace
       */
      public String toString()
      {
         return "Subspace [vector size: " + vectorSize
                 + ", numbers of vectors: " + vectorList.size() + "]";
      }

}

