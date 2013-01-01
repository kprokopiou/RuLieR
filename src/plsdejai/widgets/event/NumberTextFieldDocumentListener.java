package plsdejai.widgets.event;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import plsdejai.widgets.NumericTextField;

/**
 * Takes care of the events produced during user interaction with a NumericTextField object
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 03/02/2012
 */
public abstract class NumberTextFieldDocumentListener
        implements DocumentListener, FocusListener
{

   /*
    * Note: oldValue newValue
    *       non-null   null     the field has now an invalid value
    *        null      non-null the field has now an valid value
    */
   public static final String NUMBER_CHANGED_PROPERTY = "numberChanged";

   private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

   protected NumericTextField field;

   public NumberTextFieldDocumentListener(NumericTextField fld)
   {
      if (fld == null)
         throw new IllegalArgumentException("field argument is null");
      else
         this.field = fld;
   }
   
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

   public void changedUpdate(DocumentEvent evt) {}

   public void insertUpdate(DocumentEvent evt)
   {
      checkDocumentFormat();
   }

   public void removeUpdate(DocumentEvent evt)
   {
      checkDocumentFormat();
   }

   public void focusLost(FocusEvent evt)
   {
      if (!evt.isTemporary()) {
         checkDocumentFormat();
      }
   }

   public void focusGained(FocusEvent evt){ }

   private void checkDocumentFormat()
   {
         String text = field.getText();
         Number oldValue = field.getValue();
         Number newValue = getValidNumber(text);

         field.syncValue(newValue);

         if (newValue != null) {
            field.setBackground(new Color(184, 211, 45) );
         } else {
            field.setBackground(Color.red);
         }

         firePropertyChange(NUMBER_CHANGED_PROPERTY, oldValue, newValue);
   }

   public Number valueOf(String s){ return getValidNumber(s);}

   /**
    *
    * @return a valid Number subclass, or null if the string cannot be translated
    *         to a valid number
    */
   protected abstract Number getValidNumber(String s);
   
}
