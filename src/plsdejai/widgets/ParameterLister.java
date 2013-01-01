package plsdejai.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import plsdejai.Parameter;



/**
 * USAGE:
 * - Create a ParameterLister object:
 *   ParameterLister lister = new ParameterLister( namesArr);
 * - Show a dialog:
 *   int retVal = lister.showDialog(owner, dialogType)
 * - If retVal == APPROVE_OPTION, then call:
 *   NumericTextFields[] fields = lister.getFields()
 *
 * * @author Prokopiou Konstantinos
 * @version 1.0, 29/01/2012
 */
public class ParameterLister extends javax.swing.JPanel
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   // ********************************
   // ***** Dialog Return Values *****
   // ********************************
   /** Return value if cancel is chosen. */
   public static final int CANCEL_OPTION = 1;
   /** Return value if approve (yes, ok) is chosen. */
   public static final int APPROVE_OPTION = 0;
   /** Return value if an error occurred. */
   public static final int ERROR_OPTION = -1;

   private int returnValue = ERROR_OPTION;

   private JDialog dialog;

   private Parameter [] choosableParameters;

   
   public ParameterLister(Parameter[] params)
   {
      int len = params.length;

      if (params == null || len == 0) {
         JOptionPane.showMessageDialog(null, "No parameter names where specified."
                 + "\nCannot create toolbar.", "Create Parameter Toolbar",
                 JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      choosableParameters = params;

      initGUIElements();
   }

   public ParameterLister(String[] paramNames)
   {
      int len = paramNames.length;

      if (paramNames == null || len == 0) {
         JOptionPane.showMessageDialog(null, "No parameter names where specified."
                 + "\nCannot create toolbar.", "Create Parameter Toolbar",
                 JOptionPane.INFORMATION_MESSAGE);
         return;
      }

      choosableParameters = new Parameter[len];

      for (int i = 0; i < len; ++i) {
         Parameter param = new Parameter();
         param.name = paramNames[i];
         param.desc = param.name;
         param.type = NumericTextField.INTEGER;
         param.max = Integer.valueOf(0);
         param.min = Integer.valueOf(0);
         param.value = Integer.valueOf(0);

         choosableParameters[i] = param;
      }
      
      initGUIElements();
   }

   public int showDialog(JFrame owner, String title)
   {      
      returnValue = APPROVE_OPTION;

      dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);

      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

      dialog.addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            returnValue = CANCEL_OPTION;
            dialog.dispose();
         }
      });

      dialog.setResizable(false);
      dialog.setContentPane(this);

      dialog.pack();

      Rectangle owner_bounds = owner.getBounds();
      Dimension size = getSize();
      dialog.setLocation(new Point(owner_bounds.x
              + (owner_bounds.width - size.width) / 2,
              (owner_bounds.y + owner_bounds.height - size.width) / 2));
      dialog.setVisible(true);

      return returnValue;
   }

   /**
    *
    * @return an array of Parameter that were constructed by the user
    *         or null if no such parameter was constructed
    */
   public Parameter[] getFields()
   {
      Parameter [] fields = null;
      int size = modelOfSelectedParameters.getSize();

      if (size != 0) {
         fields = new Parameter[size];
         for (int i = 0; i < size; ++i)
            fields[i] = (Parameter) modelOfSelectedParameters.get(i);
      }

      return fields;
   }

   private Parameter getChoosableParameter(String name)
   {
      for (int i = 0 , len = choosableParameters.length; i < len ; ++i)
         if (choosableParameters[i].name == name)
            return choosableParameters[i];

      return null;
   }
   
   private javax.swing.JTextField descFld;
   private javax.swing.JComboBox numberTypeCombo;
   private NumericTextField maxFld;
   private NumericTextField minFld;
   private NumericTextField valueFld;

   JTextField errorLogFld;

   private static final int MAX_NUM_OF_PARAMETERS = 5;   

   private static final Border labelTextFieldPaneBorder =
           BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.gray),
            BorderFactory.createEmptyBorder(2,3,2,3));

   private static final Color bgColor = new Color(184, 211, 45);

   private JComboBox nameCombo;
   DefaultComboBoxModel nameComboModel = new DefaultComboBoxModel();

   private javax.swing.JList list;
   private DefaultListModel modelOfSelectedParameters;

   private javax.swing.JButton addButton;
   private javax.swing.JButton removeButton;
   private javax.swing.JButton okButton;

   private Dimension[] dim = new Dimension [6];

   private void initGUIElements()
   {
      setOpaque(true);
      setLayout(new BorderLayout());

      JPanel north = new JPanel();
      north.setLayout(new javax.swing.BoxLayout(north, javax.swing.BoxLayout.PAGE_AXIS));
      north.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Choose Parameters [maximum " +
              ParameterLister.MAX_NUM_OF_PARAMETERS +"]"),
              BorderFactory.createEmptyBorder(5,2,6,2) ) );

      JPanel paramPane = new JPanel();
      paramPane.setLayout(new javax.swing.BoxLayout(paramPane, javax.swing.BoxLayout.LINE_AXIS));
      
      north.add(paramPane);

      JPanel [] panels = new JPanel[6];

      panels[0] = new JPanel();
      panels[0].setBorder(labelTextFieldPaneBorder);
      panels[0].setLayout(new GridLayout(2, 1));
      panels[0].setOpaque(true);

      JLabel nameComboLabel = new JLabel("Parameter name");
      panels[0].add(nameComboLabel);

      nameCombo = new JComboBox();
      nameCombo.setMaximumSize(new Dimension(280, 40));
      nameCombo.setMinimumSize(new Dimension(60, 20));
      nameCombo.setEditable(false);

      for (int i = 0, len = choosableParameters.length; i < len; ++i)
         nameComboModel.addElement(choosableParameters[i].name);
      nameCombo.setModel(nameComboModel);


      // Show the first parameter in the combo of parameters to choose
      if (nameCombo.getModel().getSize() > 0){
          nameCombo.setSelectedIndex(0);
          
          descFld = new JTextField(choosableParameters[0].desc, 20);

      } else {         
         nameCombo.setEnabled(false);

         descFld = new JTextField(20);
      }

      nameCombo.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt)
         {
            syncFields();
         }
      });

      panels[0].add(nameCombo);

      paramPane.add(panels[0]);
      
      
      panels[1] = createLabeledTextFieldPane("Description", descFld, paramPane);
      descFld.addFocusListener(new FocusAdapter()
      {
         public void focusLost(FocusEvent evt)
         {
            if (! evt.isTemporary()){
               String name = (String) nameCombo.getSelectedItem();
               if (name != null)
                  getChoosableParameter(name).desc = descFld.getText();
            }
         }
      });

      panels[2] = new JPanel();
      panels[2].setLayout(new GridLayout(2, 1));
      JLabel numberTypeComboLabel = new JLabel("Number Type");
      panels[2].add(numberTypeComboLabel);
      numberTypeCombo = new JComboBox();
      numberTypeCombo.setRenderer(new NumberTypeComboCellRenderer());
      numberTypeCombo.setModel(
              new DefaultComboBoxModel(new Integer[]{
                 Integer.valueOf(NumericTextField.INTEGER),
                 Integer.valueOf(NumericTextField.DOUBLE)}));

      if (choosableParameters[0].type == NumericTextField.INTEGER
              || choosableParameters.length == 0)
         numberTypeCombo.setSelectedIndex(0);
      else
         numberTypeCombo.setSelectedIndex(1);
      
      numberTypeCombo.setEditable(false);
      numberTypeCombo.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt)
         {
            int index = numberTypeCombo.getSelectedIndex();

            if (index >= 0) {
               int type = NumericTextField.INTEGER;

               if (index == 0)
                  type = NumericTextField.INTEGER;
               else
                  type = NumericTextField.DOUBLE;

               getChoosableParameter((String) nameCombo.getSelectedItem()).type = type;

               minFld.setType(type);
               maxFld.setType(type);
               valueFld.setType(type); // it automatically checks for validity
            }

         }
      });

      
      panels[2].add(numberTypeCombo);

      panels[2].setBorder(labelTextFieldPaneBorder);

      paramPane.add(panels[2]);

      minFld = new NumericTextField(5, NumericTextField.INTEGER, null, null);
      panels[3] = createLabeledNumericTextFieldPane("Minimum", minFld, paramPane);
      minFld.addFocusListener(new FocusAdapter()
      {
         public void focusLost(FocusEvent evt)
         {
            if (!evt.isTemporary()){
               Parameter param =
                       getChoosableParameter((String) nameCombo.getSelectedItem());

               Number value = minFld.getValue();

               param.min = value;
               valueFld.setMinimumValue(value);

               param.value = valueFld.getValue();
            }
               
         }
      });

      maxFld = new NumericTextField(5, NumericTextField.INTEGER, null, null);
      panels[4] = createLabeledNumericTextFieldPane("Maximum", maxFld, paramPane);
      maxFld.addFocusListener(new FocusAdapter()
      {
         public void focusLost(FocusEvent evt)
         {
            if (!evt.isTemporary()){
               Parameter param =
                       getChoosableParameter((String) nameCombo.getSelectedItem());

               Number value = maxFld.getValue();

               param.max = value;
               valueFld.setMaximumValue(value);
              
               param.value = valueFld.getValue();
            }
         }
      });

      valueFld = new NumericTextField(5, NumericTextField.INTEGER, null, null);
      panels[5] = createLabeledNumericTextFieldPane("  Value  ", valueFld, paramPane);
      valueFld.setMinimumValue(minFld.getValue());
      valueFld.setMaximumValue(maxFld.getValue());
      valueFld.addFocusListener(new FocusAdapter()
      {
         public void focusLost(FocusEvent evt)
         {
            if (!evt.isTemporary()){
               getChoosableParameter((String) nameCombo.getSelectedItem()).value =
                       valueFld.getValue();
            }
         }
      });

      JPanel errorLogPanel = new JPanel();
      errorLogPanel.setLayout(new GridLayout(1,1));
      errorLogFld = new JTextField("");
      errorLogFld.setEditable(false);
      errorLogFld.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      errorLogFld.setForeground(Color.red);
      errorLogPanel.add(errorLogFld);
      errorLogPanel.add(errorLogFld);
      north.add(errorLogPanel);

      add(north, BorderLayout.NORTH);

      // find max height
      int maxHeight = panels[0].getPreferredSize().height;
      for (int i = 1, len = panels.length; i < len; ++i){
         int h = panels[i].getPreferredSize().height;
         if (h > maxHeight)
            maxHeight = h;
      }

      for (int i = 0, len = panels.length; i < len; ++i){
         dim[i] = new Dimension( panels[i].getPreferredSize().width, maxHeight);
         panels[i].setMinimumSize(dim[i]);
         panels[i].setPreferredSize(dim[i]);
         panels[i].setMaximumSize(dim[i]);
      }


      JScrollPane scroller = new javax.swing.JScrollPane();
      scroller.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Parameter List"),
              BorderFactory.createEmptyBorder(5,2,6,2) ) );

      list = new JList();
      modelOfSelectedParameters = new DefaultListModel();
      list.setModel(modelOfSelectedParameters );
      list.setCellRenderer(new ParameterListCellRenderer());
      list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
      //list.setVisibleRowCount(-1);

      list.addFocusListener(new FocusAdapter()
      {
         public void focusLost(FocusEvent evt)
         {
            if (! evt.isTemporary()){
               if (evt.getOppositeComponent() != removeButton){
                  list.clearSelection();
                  removeButton.setEnabled(false);
               }

            }

         }

         public void focusGained(FocusEvent evt)
         {
            if (! evt.isTemporary()){
               if(modelOfSelectedParameters.getSize() > 0)
                  removeButton.setEnabled(true);
            }
         }
      });

      int width = 0;
      int height = dim[0].height;
      for (int i = 0, len = dim.length; i < len; ++i)
         width += dim[i].width;

      Insets insets = scroller.getInsets();
      scroller.setPreferredSize(new Dimension(width + insets.left + insets.right,
              height * MAX_NUM_OF_PARAMETERS + insets.top + insets.bottom ));

      scroller.setViewportView(list);

      add(scroller);

      JPanel cmdPane = new javax.swing.JPanel();
      cmdPane.setLayout(new BoxLayout(cmdPane, BoxLayout.LINE_AXIS));

      cmdPane.add(Box.createHorizontalGlue());

      addButton = new javax.swing.JButton("Add");
      addButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {

            String name = (String) nameCombo.getSelectedItem();
            Parameter param = getChoosableParameter(name);
            // Check if name already exists
            // Overprotection against the case, where the caller
            // creates a ParameterList, with an array of parameters
            // that have duplicate names
            for (int i = 0, len = modelOfSelectedParameters.getSize(); i < len; ++i) {
               if (((Parameter) modelOfSelectedParameters.get(i)).name.equals(name)){
                  errorLogFld.setText("Identifier is already specified in the parameter list");
                  return;
               }
            }

            // Check if numeric fields are valid
            if (param.min == null ){
               errorLogFld.setText("Minimum value is an invalid number");

            } else if (param.max == null ){
               errorLogFld.setText("Maximum value is an invalid number");

            } else if (param.value == null ){
               errorLogFld.setText("Value is an invalid number or out of bounds");

            } else {
               
               modelOfSelectedParameters.addElement(param);

               // Remove the name of the parameter from the combo box
               nameCombo.removeItem(name);

               if (modelOfSelectedParameters.getSize() == ParameterLister.MAX_NUM_OF_PARAMETERS
                       || nameCombo.getModel().getSize() == 0) {
                  addButton.setEnabled(false);

                  nameCombo.setEnabled(false);
                  descFld.setEnabled(false);
                  numberTypeCombo.setEnabled(false);
                  maxFld.setEnabled(false);
                  minFld.setEnabled(false);
                  valueFld.setEnabled(false);
               }

            }

            syncFields();

         }
      });


      addButton.addFocusListener(new FocusAdapter()
      {
         public void focusLost(FocusEvent evt)
         {
            if (! evt.isTemporary()){
               errorLogFld.setText(null);
            }

         }
      });
      addButton.setEnabled(true);
      cmdPane.add(addButton);
      cmdPane.add(Box.createHorizontalStrut(3));

      removeButton = new javax.swing.JButton("Remove");
      removeButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {

            Parameter param = (Parameter) modelOfSelectedParameters.
                    getElementAt(list.getSelectedIndex());

            if (modelOfSelectedParameters.getSize() == ParameterLister.MAX_NUM_OF_PARAMETERS ||
                    nameCombo.getModel().getSize() == 0){
               addButton.setEnabled(true);

               nameCombo.setEnabled(true);
               descFld.setEnabled(true);
               numberTypeCombo.setEnabled(true);
               minFld.setEnabled(true);
               maxFld.setEnabled(true);               
               valueFld.setEnabled(true);
            }

            modelOfSelectedParameters.removeElement(param);

            // Add again the identifier name the combo box
            nameCombo.addItem(param.name);
            syncFields();
         }
      });

      removeButton.addFocusListener(new FocusAdapter()
      {
         public void focusLost(FocusEvent evt)
         {
            if (! evt.isTemporary()){

               if (evt.getOppositeComponent() != list)
                  removeButton.setEnabled(false);
            }
         }
      });

      removeButton.setEnabled(false);
      cmdPane.add(removeButton);
      cmdPane.add(Box.createHorizontalStrut(3));

      okButton = new javax.swing.JButton(" OK ");
      okButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            returnValue = APPROVE_OPTION;
            dialog.dispose();
         }
      });
      cmdPane.add(okButton);

      cmdPane.add(Box.createHorizontalGlue());

      add(cmdPane, BorderLayout.SOUTH);      

   }

   /**
    * Every time a new parameter name is set, all fields must be synchronized
    * to the values of the parameter in the choosableParameters that have the
    * specified name
    */
   private void syncFields()
   {
      Parameter param = getChoosableParameter((String) nameCombo.getSelectedItem());
      if (param != null) {
         // Add again the identifier name the combo box
         descFld.setText(param.desc);
         numberTypeCombo.setSelectedIndex(
                 (param.type) == NumericTextField.INTEGER ? 0 : 1);
         minFld.setValue(param.min);
         valueFld.setMinimumValue(param.min);
         maxFld.setValue(param.max);
         valueFld.setMaximumValue(param.max);
         valueFld.setValue(param.value);
      }

   }

   private JPanel createLabeledTextFieldPane(String labelText,
           JTextField field, Container container)
   {
      JPanel pane = new JPanel();
      pane.setLayout(new GridLayout(2, 1));
      pane.setOpaque(true);

      JLabel label = new JLabel(labelText);
      label.setHorizontalAlignment(JLabel.CENTER);
      label.setMaximumSize(label.getPreferredSize());
      label.setLabelFor(field);
      pane.add(label);
      

      field.setMinimumSize(field.getPreferredSize());
      field.setBackground(bgColor);
      field.setBorder(BorderFactory.createEmptyBorder());
      //field.setEnabled(true);
      //field.setEditable(true);
      pane.add(field);

      pane.setMaximumSize(pane.getPreferredSize());

      pane.setBorder(labelTextFieldPaneBorder);
      container.add(pane);

      return pane;
   }

   protected JPanel createLabeledNumericTextFieldPane(String labelText,
           NumericTextField field, Container container)
   {
      JPanel pane = new JPanel();
      pane.setOpaque(true);
      pane.setEnabled(true);
      pane.setLayout(new GridLayout(2, 1));

      JLabel label = new JLabel(labelText);
      label.setHorizontalAlignment(JLabel.CENTER);
      label.setMaximumSize(label.getPreferredSize());
      label.setLabelFor(field);
      pane.add(label);

      field.setMinimumSize(field.getPreferredSize());
      field.setBackground(bgColor);
      field.setBorder(BorderFactory.createEmptyBorder());
      field.setEditable(true);
      field.setText("0");
      
      pane.add(field);

      pane.setMaximumSize(pane.getPreferredSize());

      pane.setBorder(labelTextFieldPaneBorder);
      container.add(pane);
     
      return pane;
   }

   private class NumberTypeComboCellRenderer implements ListCellRenderer
   {

      public Component getListCellRendererComponent(JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus)
      {
         String text = null;

         if (((Integer) value).intValue() == NumericTextField.INTEGER)
            text = "INTEGER";
         else
            text = "DOUBLE";

         JLabel label = new JLabel(text);

         Color background;
         Color foreground;

         // check if this cell represents the current DnD drop location
         JList.DropLocation dropLocation = list.getDropLocation();
         if (dropLocation != null
                 && !dropLocation.isInsert()
                 && dropLocation.getIndex() == index) {

            background = Color.BLUE;
            foreground = Color.WHITE;

            // check if this cell is selected
         } else if (isSelected) {
            background = Color.RED;
            foreground = Color.WHITE;

            // unselected, and not the DnD drop location
         } else {
            if (index % 2 == 0)
               background = Color.YELLOW;
            else
               background = Color.GREEN;

            foreground = Color.BLACK;
         }

         setBackground(background);
         setForeground(foreground);

         return label;
      }

   }

   private class ParameterListCellRenderer implements ListCellRenderer
   {

      public Component getListCellRendererComponent(JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus)
      {
         Parameter param = (Parameter) value;         

         JTextField [] textFields = new JTextField[6];
  
         textFields[0] = new JTextField(param.name);

         textFields[1] = new JTextField(param.desc);

         if (param.type == NumericTextField.INTEGER) {
            textFields[2] = new JTextField("INTEGER" );
            textFields[3] = new JTextField(String.valueOf(param.min.intValue()));
            textFields[4] = new JTextField(String.valueOf(param.max.intValue()));
            textFields[5] = new JTextField(String.valueOf(param.value.intValue()));
         } else {
            textFields[2] = new JTextField("DOUBLE");
            textFields[3] = new JTextField(String.valueOf(param.min.doubleValue()));
            textFields[4] = new JTextField(String.valueOf(param.max.doubleValue()));
            textFields[5] = new JTextField(String.valueOf(param.value.doubleValue()));
         }
        
         JPanel pane = new JPanel();
         pane.setOpaque(true);
         pane.setLayout(new javax.swing.BoxLayout(pane, javax.swing.BoxLayout.LINE_AXIS));

         for (int i = 0, len = textFields.length; i < len; ++i ){

            textFields[i].setEditable(false);
            textFields[i].setBorder(labelTextFieldPaneBorder);

            textFields[i].setPreferredSize(dim[i]);
            textFields[i].setMinimumSize(dim[i]);
            textFields[i].setMaximumSize(dim[i]);

            pane.add(textFields[i]);
         }

         Color background;
         Color foreground;

         // check if this cell represents the current DnD drop location
         JList.DropLocation dropLocation = list.getDropLocation();
         if (dropLocation != null
                 && !dropLocation.isInsert()
                 && dropLocation.getIndex() == index) {

            background = Color.BLUE;
            foreground = Color.WHITE;

            // check if this cell is selected
         } else if (isSelected) {
            background = Color.RED;
            foreground = Color.WHITE;

            // unselected, and not the DnD drop location
         } else {
            if (index % 2 == 0)
               background = Color.YELLOW;
            else
               background = Color.GREEN;

            foreground = Color.BLACK;
         }

         setBackground(background);
         setForeground(foreground);
         for (Component comp :pane.getComponents()){
            comp.setBackground(background);
            comp.setForeground(foreground);
         }


         return pane;
      }

   }

}

   
   
   
