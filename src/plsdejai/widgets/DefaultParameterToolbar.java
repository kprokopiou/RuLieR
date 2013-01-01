package plsdejai.widgets;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

/**
 * This class overrides the abstract methods of AbstractParameterToolbar
 * In addition to setParameterValue and getParemeterValue,
 * it also adds getters and setters for the minimum and maximum value
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 03/02/2012
 */
public class DefaultParameterToolbar extends AbstractDefaultParameterToolbar
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   JPanel valueToolbar = new JPanel();
   List <JPanel> valueToolbarPanes  = new ArrayList<JPanel>(2);
   JPanel infoToolbar = new JPanel();
   List <JPanel> infoToolbarPanes  = new ArrayList<JPanel>(2);

   public DefaultParameterToolbar( String title )
   {
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

      setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), title ));

      valueToolbar.setLayout(new BoxLayout(valueToolbar, BoxLayout.PAGE_AXIS));
      add(valueToolbar);

      add(Box.createVerticalStrut(4));

      infoToolbar.setLayout(new BoxLayout(infoToolbar, BoxLayout.PAGE_AXIS));
      infoToolbar.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
              "Current parameter values"));
      // Add an Element in order to show the whole title
      JPanel space = new JPanel();
      space.setLayout(new BoxLayout(space, BoxLayout.LINE_AXIS));      
      space.add(Box.createHorizontalStrut(200));
      infoToolbar.add(space);
      add(infoToolbar);

      
      JPanel p = new JPanel();
      addSetValuesButton(p);
      valueToolbar.add(p);       
   }
   
   JButton setNewValues;

   protected final void addSetValuesButton(Container container)
   {
      setNewValues = createButton("Set new values", container);
      setNewValues.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            JButton button = (JButton) e.getSource();

            if (!button.isEnabled())
               return;

            for (Iterator<String> it = fields.keySet().iterator(); it.hasNext();) {
               String key = it.next();
               setParameterValue(key, fields.get(key).valueFld.getValue());
            }
         }
      });
   }

   /**
    * Check if all user input are non-null. If not synchronizes GUI
    */

   protected void checkUserInputValidity()
   {
      if (isUserInputValid()){         
         setNewValues.setEnabled(true);
      } else {         
         setNewValues.setEnabled(false);
      }
   }

   /* ************* Methods that implement the AbstractDefaultParameterToolbar ***** */

   // The number of columns for the numeric text field
   private static final int FIELD_COL = 8;
   /**
    *
    * @param name
    * @param desc
    * @param type: NumericTextField.INTEGER or NumericTextField.DOUBLE
    * @param min
    * @param max
    */
   public final void createParameter(String name, String desc, int type,
           Number min, Number max, Number value)
   {
      // For every 3 parameters create a new toolbar
      if (fields.size() % 3 == 0){
         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

         valueToolbarPanes.add(panel);
         valueToolbar.add(panel, valueToolbarPanes.size() - 1);

         panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
         infoToolbarPanes.add(panel);
         infoToolbar.add(panel);
      }

      ParameterField fld = new ParameterField();
      if (desc == null  || desc.length() == 0)
         desc = name;
      if (desc.length() > MAX_FIELD_NAME_LEN)
         desc = desc.substring(0, MAX_FIELD_NAME_LEN);
      fld.desc = desc;
      fld.valueFld = new NumericTextField(FIELD_COL, type, min, max);
      fld.infoFld = new JTextField(FIELD_COL);

      fields.put(name, fld);

      JPanel valueBar = valueToolbarPanes.get(valueToolbarPanes.size() - 1);
      JPanel infoBar = infoToolbarPanes.get(infoToolbarPanes.size() - 1);

      createLabeledNumericTextFieldPane(fld, valueBar);

      createInfoLabeledTextFieldPane(fld.desc, fld.infoFld, infoBar);

      setParameterValue(name, value);

      fld.valueFld.addPropertyChangeListener(this);
   }
   

}
