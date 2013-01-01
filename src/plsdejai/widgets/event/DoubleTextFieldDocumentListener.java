package plsdejai.widgets.event;

import plsdejai.widgets.NumericTextField;

/**
 * A specialization of the NumberTextFieldDocumentListener for NumericTextFields
 * of Double type
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 03/02/2012
 */
public class DoubleTextFieldDocumentListener extends NumberTextFieldDocumentListener
{
   public DoubleTextFieldDocumentListener(NumericTextField fld){ super(fld); }

   @Override
   protected Number getValidNumber(String s)
   {
      Double d;
      try {
         d = Double.valueOf(s);

         Double min = (Double) field.getMinimumValue();
         Double max = (Double) field.getMaximumValue();

         if ( min != null && d.doubleValue() < min.doubleValue()  )
            return null;
         else if ( max != null && d.doubleValue() > max.doubleValue()  )
            return null;
         else
            return (Number) d;

      } catch (NumberFormatException e) {
        return null;
      }
   }

}
