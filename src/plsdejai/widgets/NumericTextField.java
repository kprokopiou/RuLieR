package plsdejai.widgets;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JTextField;
import javax.swing.text.Document;
import plsdejai.StandardBinaryOp;
import plsdejai.widgets.event.DoubleTextFieldDocumentListener;
import plsdejai.widgets.event.IntegerTextFieldDocumentListener;
import plsdejai.widgets.event.NumberTextFieldDocumentListener;
import plsdejai.widgets.event.NumericTextFieldPropertyListener;

/**
 * A JTextField that accepts numeric input, and so it must act accordingly.
 * that is check if values is inside the valid range
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 01/02/2012
 */
public class NumericTextField  extends JTextField
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   // Number Format to which the text field will be traslated.
   public static final int INTEGER = 0;
   public static final int DOUBLE = 1;

   private int type;

   // Keeps the current value of the text field
   // The Document Listener should syncronize the correspondance
   // of the text and this value
   private Number value;

   private Number minimum;
   private Number maximum;

   NumberTextFieldDocumentListener numberTextFieldDocumentListener = null;

   public NumericTextField(int cols, int type) { this(cols, type, null, null); }

   public NumericTextField( int cols, int type, Number min, Number max)
   {
      super(cols);
      setup(type, min, max);
   }
   
   public NumericTextField(String strValue, int cols, int type) {
      this(cols, type, null, null);
      
      setValue(strValue);
   }

   public NumericTextField(String strValue, int cols, int type, Number min, Number max)
   {
      this(cols, type, min, max);
            
      setValue(strValue);
   }

   private void setup(int type, Number min, Number max)
   {
      this.type = type;
      setMinimumValue(min);
      setMaximumValue(max);
      if (type == INTEGER)
         numberTextFieldDocumentListener = new IntegerTextFieldDocumentListener(this);
      else if (type == DOUBLE)
         numberTextFieldDocumentListener = new DoubleTextFieldDocumentListener(this);

     getDocument().addDocumentListener(numberTextFieldDocumentListener );

     addFocusListener(new FocusAdapter()
      {
         public void focusGained(FocusEvent evt)
         {
            if (!evt.isTemporary()){
               NumericTextField.this.setSelectionColor(Color.orange);
               NumericTextField.this.setSelectionStart(0);
               NumericTextField.this.setSelectionEnd(
                       NumericTextField.this.getText().length());
            }
         }
      });
      
   }


   public void addPropertyChangeListener(PropertyChangeListener l)
   {      
      if (l instanceof StandardBinaryOp || l instanceof NumericTextFieldPropertyListener)
         numberTextFieldDocumentListener.addPropertyChangeListener(l);
      else
         super.addPropertyChangeListener(l);

   }

   public void setType(int type)
   {
      if ( this.type != type){
         this.type = type;

         Document doc = getDocument();
         Number newValue = null;
         if(numberTextFieldDocumentListener != null)
            doc.removeDocumentListener(numberTextFieldDocumentListener);

         if (type == INTEGER){            
            numberTextFieldDocumentListener = new IntegerTextFieldDocumentListener(this);
            if (value != null) newValue = Integer.valueOf(value.intValue() );
            if(minimum != null) minimum = Integer.valueOf(minimum.intValue() );
            if(maximum != null) maximum = Integer.valueOf(maximum.intValue() );
         } else if (type == DOUBLE){
            numberTextFieldDocumentListener = new DoubleTextFieldDocumentListener(this);
            if (value != null) newValue = Double.valueOf(value.doubleValue() );
            if(minimum != null) minimum = Double.valueOf(minimum.doubleValue() );
            if(maximum != null)maximum = Double.valueOf(maximum.doubleValue() );
         } else {
            throw new IllegalArgumentException("setType: Argument has the wrong number format");
         }
            
         doc.addDocumentListener(numberTextFieldDocumentListener);         
         setText(String.valueOf(newValue) );
      }

   }

   public int getType(){ return type; }

   public final void setValue(String strValue) { setText(strValue); }

   public final void setValue(Number value)
   {
      setText(String.valueOf(value));

      // Note: NumberTextFieldDocumentListener takes care of:
      //    syncValue( numberTextFieldDocumentListener.valueOf(s));
   }


   
   public Number getValue(){ return value;}

   public void setMinimumValue(Number min){

      Number newMin = null;
      if (min != null) {

         if (type == INTEGER && min instanceof Integer) {
            newMin = Integer.valueOf(min.intValue());

         } else if (type == DOUBLE && min instanceof Double) {
            newMin = Double.valueOf(min.doubleValue());
         } else
            throw new IllegalArgumentException("setMin: Argument has the wrong number format");

      }

      minimum = newMin;

      setText(getText() );

   }

   public Number getMinimumValue(){ return minimum;}

   public void setMaximumValue(Number max){

      Number newMax = null;
      if (max != null) {

         if (type == INTEGER && max instanceof Integer) {
            newMax = Integer.valueOf(max.intValue());

         } else if (type == DOUBLE && max instanceof Double) {
            newMax = Double.valueOf(max.doubleValue());
         } else
            throw new IllegalArgumentException("setMin: Argument has the wrong number format");

      }

      maximum = newMax;
      setText(getText() );
      
   }

   public Number getMaximumValue(){ return maximum;}
   

   /**
    * Note: This method is only intended to be called from NumberTextFieldDocumentListener
    *       in order to synchronize the value with the numeric text
    * @param val
    */
   public void syncValue(Number val){

      Number newValue = null;
      if (val != null) {
         if (type == INTEGER && val instanceof Integer) {
            newValue = Integer.valueOf(val.intValue());

         } else if (type == DOUBLE && val instanceof Double) {
            newValue = Double.valueOf(val.doubleValue());
         } else
            throw new IllegalArgumentException("setValue: Argument has the wrong number format");

      }

      this.value = newValue;
      //setText(String.valueOf(newValue) ); Note this statement causes mutation,
      // because setValue is also called after the text is modified with setText()
   }

}
