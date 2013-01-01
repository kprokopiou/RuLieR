package plsdejai.optimizer;

import java.awt.image.Raster;
import java.text.DecimalFormat;
import plsdejai.StandardBinaryOp;

/**
 * A class that implements the analogy of the energy of a state.
 * Lower energy is optimal.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 23/02/2012
 */
public class Energy implements Comparable<Energy>
{

   private double precision;
   private double recall;
   private double f1;

   /**
    *
    * @param tp True positive pixel: a pixel that exists in
    *           both the detections map and rule line ground truth map
    *           but not in the text only map
    * @param fp False positive pixel: a pixel that exists in
    *           both the detections map and the text only map
    * @param fn False negative pixel: a pixel that exists in
    *           the rule line map but in neither detection map nor the text only map
    */
   public Energy(int tp, int fp, int fn)
   {
      int tpANDfp = (tp + fp);
      int tpANDfn = (tp + fn);

      if (tpANDfp != 0)
         precision = (double) tp / tpANDfp;
      else
         precision = 0;

      if(tpANDfn != 0)
         recall = (double) tp / tpANDfn;
      else
         recall = 0;

      f1 = calcF1();
   }

   public Energy(double precision, double recall)
   {
      this.precision = precision;
      this.recall = recall;

      this.f1 = calcF1();

   }

   public Energy(double precision, double recall, double f1)
   {
      this.precision = precision;
      this.recall = recall;

      this.f1 = f1;

   }

   private double calcF1()
   {
      if (precision == 0 || recall == 0)
         return 0;
      else
         return (2 * precision * recall) / (precision + recall);
   }

   public double getPrecision(){ return precision; }
   public double getRecall(){ return recall; }
   public double getF1(){ return f1; }

   /**
    * Note: we use the minimized version, lower energy is optimal
    * @param e
    * @return the difference between this energy and e
    */
   public double minus(Energy e)
   {
      double diff = 0.0;

      if (Double.isNaN(precision) || Double.isNaN(recall) || Double.isNaN(f1))
         diff = 1.0;
      else if (Double.isNaN(e.precision) || Double.isNaN(e.recall) ||Double.isNaN(e.f1))
         diff = -1.0;
      else
         diff = -(f1 - e.f1);

      return diff;
   }

   /**
    * Note: we use the minimized version, lower energy is optimal
    * @param e
    * @return 1 if this energy is lower than energy e, 0 if both are equal
    *         -1 if this energy is greater than energy e.
    */
   public int compareTo(Energy e)
   {
      double ERROR = 0.0000000001;

      double diff = minus(e);

      if (diff > ERROR)
         return 1;
      else if (diff < -ERROR)
         return -1;
      else
         return 0;

   }

   /**
    * Calculates and returns the energy.
    * @param rasterGT the ground truth image raster
    * @param rasterSynth the synthetic image raster
    * @param rasterOutput the image raster resulting from the filtering operation
    * @return
    */
   public static Energy calcEnergy(Raster rasterGT,
           Raster rasterSynth, Raster rasterOutput)
   {
      return calcEnergy(rasterGT, null, rasterSynth, rasterOutput);
   }
   
   /**
    * Calculates and returns the energy.
    * @param rasterGT the ground truth image raster
    * @param rasterTextOnly the text-only image raster
    * @param rasterSynth the synthetic image raster
    * @param rasterOutput the image raster resulting from the filtering operation
    * @return
    */
   public static Energy calcEnergy(Raster rasterGT, Raster rasterTextOnly,
           Raster rasterSynth, Raster rasterOutput)
   {
      Energy energy = null;

      /* True positive pixel (tp): a pixel that exists in both the
       * detections map and rule line ground truth map but not
       * in the text only map */
      int tp = 0;

      /* False positive pixel (fp): a pixel that exists in both the
       * detections map and the text only map */
      int fp = 0;

      /* False negative pixel (fn): a pixel that exists in the rule
       * line map but in neither detection map nor the text only map */
      int fn = 0;

      int height = rasterSynth.getHeight();
      int width = rasterSynth.getWidth();

      if (rasterOutput != null){

         for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
               int pixelOutput = rasterOutput.getPixel(col, row, (int[]) null)[0];

               int pixelGT = rasterGT.getPixel(col, row, (int[]) null)[0];

               int pixelTextOnly = -1;
               if (rasterTextOnly != null)
                  pixelTextOnly = rasterTextOnly.getPixel(col, row, (int[]) null)[0];

               int pixelSynth = rasterSynth.getPixel(col, row, (int[]) null)[0];

               // detection map: pixels that are detected and removed from
               // the input image
               if (pixelSynth == StandardBinaryOp.FOREGROUND
                       && pixelOutput != StandardBinaryOp.FOREGROUND)
                  if ((rasterTextOnly == null
                          || pixelTextOnly != StandardBinaryOp.FOREGROUND)
                          && pixelGT == StandardBinaryOp.FOREGROUND)
                     ++tp;
                  else // if (pixelTextOnly == StandardBinaryOp.FOREGROUND)
                     ++fp;
               else if (pixelGT == StandardBinaryOp.FOREGROUND
                       && pixelTextOnly != StandardBinaryOp.FOREGROUND)
                  ++fn;

            }
         }


         energy = new Energy(tp, fp, fn);
      }

      return energy;
   }

   public String toString()
   {
      DecimalFormat df = new DecimalFormat("0.0000############");
      StringBuilder s = new StringBuilder("");
      
      s.append("Energy[precision=").append((Double.isNaN(precision)) ? "NaN":df.format(precision)).
              append("; recall=").append((Double.isNaN(recall)) ? "NaN":df.format(recall)).
              append("; F1=").append((Double.isNaN(f1)) ? "NaN":df.format(f1)).append("]");

      return s.toString();
   }

}
