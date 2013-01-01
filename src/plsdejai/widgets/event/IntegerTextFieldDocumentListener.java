package plsdejai.widgets.event;

import plsdejai.widgets.NumericTextField;

/**
 * A specialization of the NumberTextFieldDocumentListener for NumericTextFields
 * of Integer type
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 03/02/2012
 */
public class IntegerTextFieldDocumentListener extends NumberTextFieldDocumentListener
{
   public IntegerTextFieldDocumentListener(NumericTextField fld){ super(fld); }

   @Override
   protected Number getValidNumber(String s)
   {
      Integer i;
      try {
         i = Integer.valueOf(s);

         Integer min = (Integer) field.getMinimumValue();
         Integer max = (Integer) field.getMaximumValue();

         if ( min != null && i.intValue() < min.intValue()  )
            return null;
         else if ( max != null && i.intValue() > max.intValue()  )
            return null;
         else
            return (Number) i;

      } catch (NumberFormatException e) {
        return null;
      }
   }


}
