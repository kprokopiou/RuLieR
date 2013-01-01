package plsdejai.widgets;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import plsdejai.Parameter;
import plsdejai.widgets.event.NumberTextFieldDocumentListener;
import plsdejai.widgets.event.NumericTextFieldPropertyListener;

/**
 * This class subclasses the abstract class AbstractParameterToolbar, by specifying:
 * - Implements the interface AbstractParameterToolbar by overriding the abstract methods:
 *   -> setParameters
 *   -> getParameters
 *   -> setParameterValue
 *   -> getParameterValue
 * - It does not implement the abstract method of AbstractParameterToolbar
 *   -> createParameter //abstract
 * - Enhances the interface of AbstractParameterToolbar by declaring the methods: 
 *   -> checkUserInputValidity // abstract
 *   -> setMinimumParameterValue
 *   -> getMinimumParameterValue
 *   -> setMaximumParameterValue
 *   -> getMaximumParameterValue
 * - Declares useful methods for constructing the GUI elements:
 *   -> 
 * - Declares methods for manipulating invalid user input
 *   -> isUserInputValid
 *   -> propertyChange
 *   -> checkUserInputValidity // abstract
 * 
 * @author Prokopiou Konstantinos
 * @version 1.0, 30/01/2012
 */
public abstract class AbstractDefaultParameterToolbar extends AbstractParameterToolbar
        implements PropertyChangeListener, NumericTextFieldPropertyListener
{
   /** Constant that defines the maximum length of the name of a field */
   public static final int MAX_FIELD_NAME_LEN = 40;

   /** An inner class that defines simply the structure of a parameter field */
   public class ParameterField
   {
      /** The description of the field */
      public String desc;
      /** The currently active value of the parameter */
      public Number value;
      /** This field takes care of all the user interaction relating to setting
       * the new value, and checking if this value is inside bounds.
       * It also keeps the type of the parameter */
      public NumericTextField valueFld;
      /** A text field that supplies information about the bounds of the parameter */
      public JTextField boundsInfoFld;
      /** The text field that displays the currently set parameter value*/
      public JTextField infoFld;
   }

   /** A structure that maps parameter names to values */
   protected Map<String, ParameterField> fields = new HashMap<String, ParameterField>();

   /**
    * Implements method <code>createParameter</code> of <code>AbstractDefaultParameterToolbar</code>
    * @see <code>AbstractDefaultParameterToolbar</code>
    */
   public abstract void createParameter(String name, String desc, int type,
           Number min, Number max, Number value);


   /* **************************************************************** */
   // Methods for checking if user input is valid and manipulate this case
   /* **************************************************************** */
   /**
    * Check if all user input are non-null. If not synchronizes GUI
    */
   protected abstract void checkUserInputValidity();

   protected boolean isUserInputValid()
   {
      for (Iterator<String> it = fields.keySet().iterator(); it.hasNext();) {
         String key = it.next();
         if(fields.get(key).valueFld.getValue() == null)
            return false;
      }

      return true;
   }

   public void propertyChange(PropertyChangeEvent evt)
   {
      String prop = evt.getPropertyName();

      if (prop == NumberTextFieldDocumentListener.NUMBER_CHANGED_PROPERTY) {
         checkUserInputValidity();
      }

   }
  

   /**
    * This method allows the caller to get the whole set of the parameters names that
    * determines the behaviour of the filtering algorithm and the
    * current values for each parameter
    * Returns the whole information about the parameters used in the
    * algorithm, in order to be used in the Simulated Annealing algorithm
    * @return
    */
   public Parameter[] getParameters()
   {
      Parameter [] params = new Parameter[fields.size()];

      int index = 0;
      for (String key : fields.keySet()) {
         params[index] = new Parameter();

         ParameterField fld = fields.get(key);

         params[index].name = key;
         params[index].desc = fld.desc;
         params[index].type = fld.valueFld.getType();
         params[index].min = fld.valueFld.getMinimumValue();
         params[index].max = fld.valueFld.getMaximumValue();
         params[index].value = fld.value;
         ++index;
      }
      return params;
   }

   /**
    * Set the currently active parameter values
    * @param name
    * @param value
    */
   public void setParameterValue(String name, Number value)
   {
      ParameterField fld = fields.get(name);
      if (fld == null)
         return;

      NumericTextField valueFld = fld.valueFld;
      if (valueFld == null)
         return;

      // The following lines suffice, and obsolete the code that follows this segment
      if (value != null) {
         String s = String.valueOf(value);
         valueFld.setText(s);
         fld.infoFld.setText(s);

      } else {
         valueFld.setText("");
         fld.infoFld.setText("");
      }

      fld.value = valueFld.getValue();

   }

   public Number getParameterValue(String name)
   {
      return fields.get(name).value;
   }

   /* *** Methods that enhance the AbstractParameterToolbar functionality  ** */
   public void setMinimumParameterValue(String name, Number min)
   {
      ParameterField field = fields.get(name);
      ( field.valueFld).setMinimumValue(min);
      setBoundsInfoText(field);
   }

   public Number getMinimumParameterValue(String name)
   {
      ParameterField field = fields.get(name);
      return ( field.valueFld).getMinimumValue();
   }

   public void setMaximumParameterValue(String name, Number max)
   {
      ParameterField field = fields.get(name);
      ( field.valueFld).setMaximumValue(max);
      setBoundsInfoText(field);
   }

   public Number getMaximumParameterValue(String name)
   {
      ParameterField field = fields.get(name);
      return ( field.valueFld).getMaximumValue();
   }

   /* ************* Methods for constructing the GUI *********************** */   

   protected JPanel createLabeledNumericTextFieldPane(
           ParameterField parameterField, Container container)
   {
      String labelText = parameterField.desc;
      NumericTextField field = parameterField.valueFld;

      JPanel pane = new JPanel();
      pane.setOpaque(true);
      pane.setEnabled(true);
      pane.setLayout(new GridLayout(2, 1));
      pane.setBorder(BorderFactory.createLineBorder(Color.gray, 1));

      JPanel north = new JPanel();

      JLabel label = new JLabel(labelText + ": ");
      label.setMaximumSize(label.getPreferredSize());
      north.add(label);
      label.setLabelFor(field);

      field.setBorder(BorderFactory.createEmptyBorder());

      field.setMinimumSize(field.getPreferredSize());
      north.add(field);

      north.setMaximumSize(north.getPreferredSize());

      pane.add(north);

      parameterField.boundsInfoFld = new JTextField();
      setBoundsInfoText(parameterField);

      JPanel south = new JPanel();
      south.add(parameterField.boundsInfoFld);

      pane.add(south);


      if (container != null)
         container.add(pane);

      pane.setMaximumSize(pane.getPreferredSize());

      return pane;
   }

   protected void setBoundsInfoText(ParameterField parameterField)
   {
      JTextField boundsInfoFld = parameterField.boundsInfoFld;
      NumericTextField numericTextField = parameterField.valueFld;

      boundsInfoFld.setBorder(BorderFactory.createEmptyBorder());
      boundsInfoFld.setEnabled(true);
      boundsInfoFld.setEditable(false);
      StringBuilder text = new StringBuilder();
      if (numericTextField.getType() == NumericTextField.INTEGER) {
         text.append("Integer: [");
         Integer min = (Integer) numericTextField.getMinimumValue();
         Integer max = (Integer) numericTextField.getMaximumValue();
         if (min == null)
            text.append("..., ");
         else
            text.append(min.intValue()).append(", ");
         if (max == null)
            text.append("...]");
         else
            text.append(max.intValue()).append("]");

      } else {
         text.append("Double: [");
         Double min = (Double) numericTextField.getMinimumValue();
         Double max = (Double) numericTextField.getMaximumValue();
         if (min == null)
            text.append("..., ");
         else
            text.append(min.doubleValue()).append(", ");
         if (max == null)
            text.append("...]");
         else
            text.append(max.doubleValue()).append("]");
      }
      boundsInfoFld.setText(text.toString());
   }

   protected JPanel createInfoLabeledTextFieldPane(String labelText,
           JTextField field, Container container)
   {
      JPanel pane = new JPanel();
      pane.setOpaque(true);

      JLabel label = new JLabel(labelText + ": ");
      label.setMaximumSize(label.getPreferredSize());
      pane.add(label);

      label.setLabelFor(field);
      pane.add(field);

      field.setMinimumSize(field.getPreferredSize());
      field.setBackground(field.getParent().getBackground());
      field.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
      field.setEnabled(true);
      field.setEditable(false);

      pane.setMaximumSize(pane.getPreferredSize());
      container.add(pane);

      return pane;
   }

   protected JButton createButton(String text, Container container)
   {
      JButton button = new JButton();
      if (text != null)
         button.setText(text);

      button.setMaximumSize(button.getPreferredSize());

      if (container != null){
         container.add(button);
         //container.add(Box.createHorizontalStrut(7));
      }

      return button;
   }

}
