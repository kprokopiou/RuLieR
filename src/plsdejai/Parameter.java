package plsdejai;

import java.io.Serializable;
import java.text.DecimalFormat;
import plsdejai.widgets.NumericTextField;
/**
 * A class that defines the structure of the parameter fields
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 18/02/2012
 */
public class Parameter implements Serializable
{
   /** The name of a parameter (maximum 40 characters) */
   public String name;
   /** The description of a parameter */
   public String desc;
   /** The type the type of the parameter (Double or Integer) */
   public int type;
   /** The minimum value that can be given to a parameter */
   public Number min;
   /** The maximum value that can be given to a parameter */
   public Number max;
   /** The initial value of a parameter */
   public Number value;

   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   /**
    * @return a string representation of the name and value of the parameter
    */
   public String toString()
   {
      StringBuilder s = new StringBuilder(name);
      s.append(": ");
      if (type == NumericTextField.INTEGER)
         s.append(value.intValue());
      else {
         s.append( (new DecimalFormat("0.0000")).format(value) );
      }
      return s.toString();
   }
}
