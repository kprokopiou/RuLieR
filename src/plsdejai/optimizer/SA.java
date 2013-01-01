package plsdejai.optimizer;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Random;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import plsdejai.Parameter;
import plsdejai.StandardBinaryOp;
import plsdejai.filter.linearsubspace.LSubspaceOfCentralMoments;
import plsdejai.widgets.LSubspaceParameterToolbar;
import plsdejai.filter.linearsubspace.LSubspaceOfCentralMomentsOp;
import plsdejai.widgets.NumericTextField;


/**
 * Simulated Annealing
 * The usage of this class is straightforward
 * First we create an instance of the class with:
 * - SA sa = new SA(args);
 * Optionally we set the initial state with:
 * - sa.setInitialState(args)
 * Then we start the the Simulated Annealing procedure with:
 * - sa.start()
 * At the end we retrieve the optimized parameters with:
 * - Parameters [] params = sa.getOptimizedParameters();
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 23/02/2012
 */
public class SA
{   

   /* ***************************** Constants **************************** */
   // The number of neighbours to check around a given parameter value
   private static final int NB_MAX = 3;
   // The minimum range to start with, for integer parameters
   private static final int MIN_INTEGER_RANGE = 5;
   // Initial temperature. You can change this value by calling the setInitialState()
   private static final double T_start = 100;
   // Maximum number of iterations
   private static final int TIME = 45;
   // The maximum best energy that is considered satisfactory
   private static final Energy E_MIN = new Energy(1.0, 1.0);
   /* ******************************************************************** */

   
   // The objective-function of the Simulated Annealing process
   private StandardBinaryOp op;

   // The image data that are needed to calculate the energy of the states.
   private Raster [] rasterGT;
   private Raster [] rasterSynth;
   // The number of images couples that are used as the training dataset
   public int datasetSize;

   /* **************** Variables that depend on the Parameterization ***** */
   // The number of dimensions
   public int size;
   // The name of the dimensions
   public String[] name;
   // The description of the field
   public String[] desc;
   // One of the types NumericTextField.INTEGER or NumericTextField.DOUBLE
   public int[] type;
   // The lower limit of the allowable values
   public Number[] aBound;
   // The upper limit of the allowable values
   public Number[] bBound;   
   // The initial range to be searched for, for each parameter.
   private double[] initRange;

    // The initial state
   private State initState;
   // State with the best (lowest) energy value of all the tested states
   private State S_BEST;
   /* ******************************************************************** */

   /**
    * The SA class implements the Simulated Annealing algorithm.    *
    * 
    * @param op : the algorithm that will applied to the input image
    * @param initState : A State is depicted by a Parameter [] array, so
    *                    all a set of states forms a eucledian space of n dimensions    
    * @param groundTruthFile : The ground truth, rule-lines only, image file.
    * @param synthFile   The synthetic image file, produced by the groundTruthFile
    *                   and  text-only file. If null is specified a
    *                   synthetic image is created by using the ground truth and
    *                   text-only image
    */
   public SA(StandardBinaryOp op, File [] groundTruthFiles, File [] synthFiles)
   {

      this.op = op;         
    
      if (!isValidParameters(op.getParameters())){         
         System.err.println("Invalid parameters argument");
      }
         

      else if (!createRasters(groundTruthFiles, synthFiles))
         System.err.println("Invalid image file arguments");

      else {

         init();
      }       
   }
  
   private void init()
   {
      Parameter[] param = op.getParameters();
      
      size = param.length;

      name = new String[size];
      desc = new String[size];
      type = new int[size];
      aBound = new Number[size];
      bBound = new Number[size];
      initRange = new double[size];

      Number[] value = new Number[size];


      for (int i = 0; i < size; ++i) {
         name[i] = param[i].name;
         desc[i] = param[i].desc;
         type[i] = param[i].type;
         aBound[i] = param[i].min;
         bBound[i] = param[i].max;

         double range = bBound[i].intValue() - aBound[i].intValue();

         initRange[i] = range / Math.pow(NB_MAX, 3);
         if (type[i] == NumericTextField.INTEGER){
            ++range;
            if (MIN_INTEGER_RANGE > initRange[i])
               initRange[i] = MIN_INTEGER_RANGE;
            else
               initRange[i] = (int) initRange[i];
         }
            

         value[i] = param[i].value;

         if (value[i] == null)
            value[i] = aBound[i];

      }
      
      setInitialState(value, T_start);
     
   }

   /**
    * Sets the initial state
    * @param value an array of values of the parameters
    * @param temp the temperature of the initial state,
    *             if null the default T_start is used
    */
   public void setInitialState(Number value[], double temp)
   {
      initState = new State(value, temp);
   }

   /**
    * @return the initial state
    */
   public State getInitialState()
   {
      return initState;
   }

   
   /**
    * Create the raster data from the file arguments
    * @param gt : The ground truth, rule-lines only, image file.
    * @param synth : The synthetic image file, produced by the groundTruthFile
    *                and  text-only file. If null is specified a synthetic
    *                image is created by using the ground truth and text-only image
    * @return true if valid rasters for all image files are created, otherwise false
    *              Note: either of textOnly or synth can be null, but not both
    */
   private boolean createRasters(File [] groundTruthFiles, File [] synthFiles)
   {
      if (groundTruthFiles == null || synthFiles == null)
         return false;
      
      datasetSize = groundTruthFiles.length;
      if (datasetSize != synthFiles.length)
         return false;

      rasterGT = new Raster[datasetSize];
      rasterSynth = new Raster[datasetSize];

      for (int i = 0; i < datasetSize; ++i) {

         if (groundTruthFiles[i] == null || !groundTruthFiles[i].isFile()
                 || synthFiles[i] == null || !synthFiles[i].isFile())
            return false;

         // Load and create the images from the specified filenames
         
         BufferedImage bi = null;
         File f = groundTruthFiles[i];
         int width = 0;
         int height = 0;

         for (int j = 0; j < 2; ++j) {

            WritableRaster raster = null;

            if (f != null) {
               bi = StandardBinaryOp.change2BinaryColorModel(
                       plsdejai.io.ImageIO.fileLoad(f));
               if (bi == null) {
                  JOptionPane.showMessageDialog(null,
                       "'" + f.getName() + "' is not a binary image",
                       "Invalid image",
                       JOptionPane.INFORMATION_MESSAGE);
                  
                  return false;
               }

               raster = bi.getRaster();

            } else {
               JOptionPane.showMessageDialog(null,
                       "null image filename",
                       "Invalid image",
                       JOptionPane.INFORMATION_MESSAGE);
               return false;

            }

            if (j == 0) {
               rasterGT[i] = raster;
               width = rasterGT[i].getWidth();
               height = rasterGT[i].getHeight();

               f = synthFiles[i]; // use this value in the next iteration

            } else {
               rasterSynth[i] = raster;

               if (width != raster.getWidth() || height != raster.getHeight()) {
                  JOptionPane.showMessageDialog(null,
                       "Images '" + groundTruthFiles[i].getName() +"' and \n'"+
                       synthFiles[i].getName() + "' have not the same size",
                       "Invalid image",
                       JOptionPane.INFORMATION_MESSAGE);
                  return false;
               }


            }
         }
      }
      
      return true;
   }
   
   /**
    * Checks if a state is valid, after any necessary corrections are done
    * A valid state must have for each dimension, the minimum and maximum value set.
    * If the value is null, it is set to the value of min.
    * @param params
    * @return true if state is valid, otherwise false.
    */
   private boolean isValidParameters(Parameter[] params)
   {
      for (Parameter param : params) {
         // We should always set the limits
         if (param.min == null || param.max == null)
            return false;

         if (param.value == null)
            param.value = param.min;

      }
      return true;
   }

   private SwingWorker task;

   /**
    * The Simulated Annealing algorithm.
    * @param initState : the initial state for starting the algorithm
    * @return false if the SA object is not initialized properly
    */
   public boolean start( SwingWorker task)
   {
      this.task = task;
      op.setTask(task);


      // Check if the parameters are set
      if (size == 0) {
         JOptionPane.showMessageDialog(null, "The SA object has not been initialized properly",
                 "SA object is invalid", JOptionPane.ERROR_MESSAGE);
         return false;
      }

      // Initial states and their respective energies      
      State s = initState;
      Energy e = s.energy;

      // keeps the states with the optimum energy
      S_BEST = new State(s.value, s.temp);
      
      State sNew = null;
      Energy eNew = null;

      Random random = new Random();
      
      Number[] newValues = new Number[size];
      for (int i = 0; i < size; ++i) {
         newValues[i] = s.value[i];
      }

      // While time left or there is improvement,
      // and the optimum (minimum energy) is not achieved
      for (int t = 0; t < TIME && e.compareTo(E_MIN) > 0; ++t) {

         double temp = getTemperature(t);

         boolean[] aBoundReached = new boolean[size];
         boolean[] bBoundReached = new boolean[size];

         for (int i = 0; i < size; ++i) {
            aBoundReached[i] = false;
            bBoundReached[i] = false;
         }
               

         // Pick some neighbour and compute its energy, using the VNS algorithm
         int dim = 0;
         boolean foundLocalBest = false;

         for (int k = 1; dim < size; ++k) {

            for (; dim < size
                    && !(aBoundReached[dim] && bBoundReached[dim]); ++dim) {

               if (task != null && task.isCancelled())
                  return false;


               // Find the range around the current value. The new neighbourhood
               // will be [value - halfRange, value + halfRange]
               double halfRange = (initRange[dim] * k * k * k) / 2;

               // Find the left bound of the new neighbourhood
               double newABound = aBound[dim].doubleValue();
               if (!aBoundReached[dim]) {
                  newABound = s.value[dim].doubleValue() - halfRange;
                  if (newABound < aBound[dim].doubleValue()) {
                     newABound = aBound[dim].doubleValue();
                     aBoundReached[dim] = true;
                  }
               }

               // Find the right bound of the new neighbourhood
               double newBBound = bBound[dim].doubleValue();
               if (!bBoundReached[dim]) {
                  newBBound = s.value[dim].doubleValue() + halfRange;
                  if (newBBound > bBound[dim].doubleValue()) {
                     newBBound = bBound[dim].doubleValue();
                     bBoundReached[dim] = true;
                  }
               }

               // Generate a new value at random from the neighbourhood
               double newValue = newABound + random.nextDouble() * (newBBound - newABound);


               if (type[dim] == NumericTextField.INTEGER)
                  newValues[dim] = Integer.valueOf((int) newValue);
               else
                  newValues[dim] = Double.valueOf(newValue);

               // Create a new state and compare it with the original state
               sNew = new State(newValues, temp);

               eNew = sNew.energy;


               double diffOfEnergy = eNew.minus(e);
               if (diffOfEnergy <= 0) {

                  s = sNew;
                  e = eNew;

                  if (eNew.compareTo(S_BEST.energy) < 0) {
                     S_BEST = new State(sNew.value, sNew.temp);

                  }

                  foundLocalBest = true;
                  continue;

               } else {
     
                  double probability = sNew.temp == 0 ? 0 :
                     (sNew.temp / T_start) * Math.exp( -diffOfEnergy / sNew.temp);
     
                  if ( probability > random.nextDouble()) {
                     s = sNew;
                     e = eNew;

                     foundLocalBest = true;
                     continue;

                  }
               }

            } // end for

            if (foundLocalBest)
               break;

            // Check if there is at least one parameter that has
            // search in the entire neighbourhood
            dim = 0;
            for (; dim < size; ++dim) {
               // select the next parameter not fully explored yet               
               if (!(aBoundReached[dim] && bBoundReached[dim]))
                  break;
            }
            // if dim = size, the loop stops

         } // end for



      } // for (t < TIME_MAX ...

      return true;


   }

   /**
    * <code>a</code> is a constant, maybe 1, 2 or 4. It depends on the positions
    * of the relative minima. Large values of <code>a</code> will spend more
    * iterations at lower temperature.
    */
   private static final double a = 2;

   /**
    * A non-increasing function: N -> (0, infinity), called the "cooling schedule"
    * The temperature schedule defines how the temperature in SA is decreased
    * All schedules start with a temperature T_start which is greater than 0
    * @param t
    * @return
    */
   private static double getTemperature(int t)
   {
      double temp = T_start * Math.pow(1 - Math.min(1, (double) t / (TIME - 1)), a);
      return temp;
   }

  
   /**
    *
    * @return the optimum parameters
    */
   public Parameter[] getOptimumParameters()
   {
      if (S_BEST == null)
         return null;

      Parameter[] params = new Parameter[size];

      for (int i = 0; i < size; ++i){
         params[i] = new Parameter();

         params[i].name = name[i];
         params[i].desc = desc[i];
         params[i].type = type[i];
         params[i].min = aBound[i];
         params[i].max = bBound[i];
         params[i].value = S_BEST.value[i];
      }

      return params;
   }

   /**
    * @return the optimum energy
    */
   public Energy getOptimumEnergy() { return S_BEST.energy; }

   /**
    *
    * A class that keeps useful attributes and methods for manipulating the state.
    * Each time a state is created, or its values change the energy is recalculated
    * @author konsprok
    */
   public class State
   {
      // The current parameter values associated with the state

      public Number[] value;
      // The temperature associated with the state
      public double temp;
      // The energy associated with this state
      public Energy energy;

      /**
       * Constructs a new state with the given values and temperature
       * @param val
       * @param temp
       */
      public State(Number[] val, double temp)
      {
         this.value = new Number[size];
         setState(val, temp);
      }


      /**
       * Sets the whole state. By calling this function we can use the same
       * variables to regenerate a state, and of course recalculate the energy.
       * @param val
       * @param temp
       */
      private void setState(Number[] val, double temp)
      {
         // Temperature must have values greater or equal to 0
         if (temp < 0)
            this.temp = T_start;
         else
            this.temp = temp;

         if(val.length != size)
            throw new IllegalArgumentException("State values size is illegal.");

         for (int i = 0; i < size; ++i) {
            if (val[i].doubleValue() >= aBound[i].doubleValue()
                    && val[i].doubleValue() <= bBound[i].doubleValue())
               this.value[i] = val[i];
            else
               this.value[i] = aBound[i];
         }
         calcEnergy();
      }

      /**
       * After a state is created, if one of the public fields are
       * changed we should call calcEnergy()
       * @return the energy class calculated for the state argument.
       */
      public Energy calcEnergy()
      {
         for (int i = 0; i < size; ++i) {
            op.setParameterValue(name[i], value[i]);
         }

         if (op instanceof LSubspaceOfCentralMomentsOp){

	    LSubspaceOfCentralMoments model = ((LSubspaceOfCentralMomentsOp)op).getModel();
            ((LSubspaceParameterToolbar) op.getToolbar()).createNewSubspace();       
            
            for (int i = 0; i < datasetSize; ++i) {
               int w = rasterGT[i].getWidth();
               int h = rasterGT[i].getHeight();
               BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
               bi.getRaster().setPixels(0, 0, w, h,
                       rasterGT[i].getPixels(0, 0, w, h, (int[]) null));               

               model.addTrainingData(bi, task);
            }
            
         }

         double precision = 0;
         double recall = 0;
         double f1 = 0;
         Energy e;
         for (int i = 0; i < datasetSize; ++i) {
            e = Energy.calcEnergy(rasterGT[i], rasterSynth[i],
                 op.filter(rasterSynth[i], null)); // The output of the filtering operation

            System.gc();

            if (task != null && task.isCancelled())
               break;
            
            precision += e.getPrecision();
            recall += e.getRecall();
            f1 += e.getF1();
         }
     
         energy = new Energy(precision / datasetSize, recall / datasetSize, f1 / datasetSize);

         return energy;
      }

      /**
       *
       * @return a string representation of the state
       */
      public String toString()
      {
         DecimalFormat df = new DecimalFormat("0.0###############");
         StringBuilder s = new StringBuilder("State: Parameters[");
         for (int i = 0; i < size; ++i) {
            if (type[i] == NumericTextField.DOUBLE)
               s.append(name[i]).append("=").append(df.format(value[i])).append(", ");
            else
               s.append(name[i]).append("=").append(value[i]).append(", ");
         }
         int len = s.length();
         s.delete(len - 2, len).append("], ");
         s.append("temp=").append(df.format(temp));

         return s.toString();
      }
   }   
   

   

}
