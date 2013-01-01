package plsdejai.widgets;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;
import plsdejai.io.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import plsdejai.widgets.filelister.FileNameExtensionFilter;
import plsdejai.Main;
import plsdejai.filter.linearsubspace.LSubspaceOfCentralMoments;
import plsdejai.filter.linearsubspace.LSubspaceOfCentralMomentsOp;
import plsdejai.util.Task;
import plsdejai.widgets.filelister.FileLister;

/**
 * The toolbar that is associated with LSubspaceOfCentralMomentsOp object
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 23/01/2012
 */
public class LSubspaceParameterToolbar extends AbstractDefaultParameterToolbar
        implements PropertyChangeListener
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   LSubspaceOfCentralMoments model;

   JPanel [] toolbars = new JPanel[4];

   JButton createSubspace;

   JButton loadTrainImage;
   JButton loadSubspace;
   JButton clearSubspace;
   JButton saveSubspace;

   JTextField subspaceFieldInfo;

   private FileLister fl = new FileLister();

   public LSubspaceParameterToolbar(String title, LSubspaceOfCentralMoments model)
   {
      this.model = model;

      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), title));

      for (int i = 0; i < 2; ++i ){
         toolbars[i] = new JPanel();
         toolbars[i].setLayout(new BoxLayout(toolbars[i], BoxLayout.LINE_AXIS));         
         //toolbars[i].setMaximumSize(toolbars[i].getPreferredSize());
         add(toolbars[i]);
         add(Box.createHorizontalStrut(4));        
      }
      toolbars[2] = new JPanel();
      toolbars[2].setLayout(new GridLayout(1, 3));
      toolbars[2].setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
              "Current subspace values"));
     // toolbars[2].setMaximumSize(toolbars[2].getPreferredSize());
      add(toolbars[2]);
      add(Box.createHorizontalStrut(4));

      toolbars[3] = new JPanel();
      toolbars[3].setLayout(new GridLayout(1, 1));
      add(toolbars[3]);
      subspaceFieldInfo = new JTextField();
      toolbars[3].add(subspaceFieldInfo);
      subspaceFieldInfo.setBackground(subspaceFieldInfo.getParent().getBackground() );

      setSubspaceFieldInfo();

      add(Box.createVerticalGlue());

      // Add Property Change Listeners
      this.model.addPropertyChangeListener(new PropertyChangeListener(){
         public void propertyChange(PropertyChangeEvent evt)
         {
            String prop = evt.getPropertyName();

            if (prop == LSubspaceOfCentralMoments.SUBSPACE_SIZE_CHANGED_PROPERTY)
               subspaceFieldInfo.setText(LSubspaceParameterToolbar.this.model.toString());

         }
      });
   }

   /**
    * Notifies the LSubspaceOfCentralMomentsOp object if it is ok to execute
    * a filter operation
    * @return true if the subspace is ready to execute a filtering operation
    */
   public boolean isReady()
   {
      return saveSubspace.isEnabled();
   }

   // The number of columns for the numeric text field
   private static final int FIELD_COL = 8;

   public void createParameter(String name, String desc, int type,
           Number min, Number max, Number value)
   {
      ParameterField fld = new ParameterField();
      if (desc == null  || desc.length() == 0)
         desc = name;
      if (desc.length() > MAX_FIELD_NAME_LEN)
         desc = desc.substring(0, MAX_FIELD_NAME_LEN);
      fld.desc = desc;
      fld.valueFld = new NumericTextField(FIELD_COL, type, min, max);
      fld.infoFld = new JTextField(FIELD_COL);

      fields.put(name, fld);

      createLabeledNumericTextFieldPane( fld, toolbars[0]);

      createInfoLabeledTextFieldPane(fld.desc, fld.infoFld, toolbars[2]);

      setParameterValue(name, value);

      if (fields.size() == 3) {
         // Create a new subspace using the values of the parameters
         createNewSubspace();

         createSubspace = createButton("Create subspace", toolbars[0]);
         createSubspace.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent e)
            {
               JButton button = (JButton) e.getSource();

               if (model != null && !model.isEmpty()) {
                  int retVal = JOptionPane.showConfirmDialog(null,
                          "Current subspace will be deleted.\n"
                          + "Do you want to proceed on creating a new subspace?",
                          "Create new subspace", JOptionPane.OK_CANCEL_OPTION);

                  if (retVal != JOptionPane.OK_OPTION)
                     return;
               }
               loadTrainImage.setEnabled(false);
               createSubspace.setEnabled(false);
               loadSubspace.setEnabled(false);
               clearSubspace.setEnabled(false);
               saveSubspace.setEnabled(false);

               createNewSubspace();

               loadTrainImage.setEnabled(true);
               createSubspace.setEnabled(true);
               loadSubspace.setEnabled(true);
               clearSubspace.setEnabled(true);
               saveSubspace.setEnabled(true);

               createSubspace.requestFocusInWindow();

/*
               for (Iterator<String> it = fields.keySet().iterator(); it.hasNext();) {
                  String key = it.next();
                  ParameterField parameterField = fields.get(key);
                  Number v = parameterField.valueFld.getValue();
                  setParameterValue(key, v);
               }
*/
            }
         });

         for (Iterator<String> it = fields.keySet().iterator(); it.hasNext();){
            NumericTextField field = fields.get(it.next()).valueFld;
            field.addPropertyChangeListener(this);
         }
      }

   }

   public void createNewSubspace()
   {
      // Set the parameter values to the values set by the user in the text fields
      for (String name : fields.keySet()) {
         setParameterValue(name, fields.get(name).valueFld.getValue());
      }

      LSubspaceParameterToolbar.this.model.setWindowHalfSide(
              getParameterValue(LSubspaceOfCentralMomentsOp.KEY_HALF_WINDOW).intValue());
      LSubspaceParameterToolbar.this.model.setMomentMaxOrder(
              getParameterValue(LSubspaceOfCentralMomentsOp.KEY_MOMENT_MAX_ORDER).intValue());
      LSubspaceParameterToolbar.this.model.setError(
              getParameterValue(LSubspaceOfCentralMomentsOp.KEY_ERR).doubleValue());

      setSubspaceFieldInfo();
   }

   private void syncParametersToSubspace()
   {
      setParameterValue(LSubspaceOfCentralMomentsOp.KEY_HALF_WINDOW,
              Integer.valueOf(LSubspaceParameterToolbar.this.model.getWindowHalfSide()));
      setParameterValue(LSubspaceOfCentralMomentsOp.KEY_MOMENT_MAX_ORDER,
              Integer.valueOf(LSubspaceParameterToolbar.this.model.getMomentMaxOrder()));
      setParameterValue(LSubspaceOfCentralMomentsOp.KEY_ERR,
              Double.valueOf(LSubspaceParameterToolbar.this.model.getError()));

      setSubspaceFieldInfo();
   }
  
   public void createToolbar1Buttons()
   {
      loadTrainImage = createButton("Add training image", toolbars[1]);
      loadTrainImage.setActionCommand("Add");
      loadTrainImage.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            if (loadTrainImage.getActionCommand().equals("Add")) {
               loadTrainImage.setForeground(Color.red);
               loadTrainImage.setText("Cancel");
               loadTrainImage.setActionCommand("Cancel");

               createSubspace.setEnabled(false);
               loadSubspace.setEnabled(false);
               clearSubspace.setEnabled(false);
               saveSubspace.setEnabled(false);

               Task task = new Task<Void, Void>("Add Training Image")
               {

                  public Void doInBackground()
                  {
                     if (model != null) {
                        File f = openFileDialog(TRAINING_IMAGE_LISTER);

                        if (f != null) {
                           
                           model.addTrainingData(ImageIO.fileLoad(f), this);

                           setSubspaceFieldInfo();
                        }

                     }

                     loadTrainImage.setForeground(Color.black);
                     loadTrainImage.setText("Add training image");
                     loadTrainImage.setActionCommand("Add");

                     createSubspace.setEnabled(true);
                     loadSubspace.setEnabled(true);
                     clearSubspace.setEnabled(true);
                     saveSubspace.setEnabled(true);

                     return null;
                  }
                  
               };
               Main.addTask(task);
               task.execute();
               
            } else {
               Main.cancelTask("Add Training Image");
            }
            
         }
      });

      toolbars[1].add(Box.createHorizontalStrut(5));

      loadSubspace = createButton("Load saved subspace", toolbars[1]);
      loadSubspace.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            if (model != null) {

               if (model != null && !model.isEmpty()) {
                  int retVal = JOptionPane.showConfirmDialog(null, "Current subspace will be deleted.\n"
                          + "Do you want to proceed on creating a new subspace?",
                          "Create new subspace", JOptionPane.OK_CANCEL_OPTION);

                  if (retVal != JOptionPane.OK_OPTION)
                     return;
               }

               loadTrainImage.setEnabled(false);
               createSubspace.setEnabled(false);
               loadSubspace.setEnabled(false);
               clearSubspace.setEnabled(false);
               saveSubspace.setEnabled(false);

               model.loadSubspace(openFileDialog(SUBSPACE_LISTER));

               loadTrainImage.setEnabled(true);
               createSubspace.setEnabled(true);
               loadSubspace.setEnabled(true);
               clearSubspace.setEnabled(true);
               saveSubspace.setEnabled(true);

               // The subspace has taken care of it self
               // Now we only need to set the parameters in the Map structure
               syncParametersToSubspace();
            }

         }
      });
      toolbars[1].add(Box.createHorizontalStrut(5));

      clearSubspace = createButton("Clear subspace data", toolbars[1]);
      clearSubspace.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            if (model != null){
               model.clear();
               setSubspaceFieldInfo();
            }

         }
      });
      toolbars[1].add(Box.createHorizontalStrut(5));

      saveSubspace = createButton("Save subspace", toolbars[1]);
      saveSubspace.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            if (model.isEmpty()){
               JOptionPane.showMessageDialog(null,
                       "Subspace is empty. There is nothing to be saved!\n",
                       "Save subspace", JOptionPane.INFORMATION_MESSAGE);
               return;
            } else if (model != null) {
               File f = saveFileDialog(SUBSPACE_LISTER);

               if (f != null){
                  loadTrainImage.setEnabled(false);
                  createSubspace.setEnabled(false);
                  loadSubspace.setEnabled(false);
                  clearSubspace.setEnabled(false);
                  saveSubspace.setEnabled(false);

                  model.storeSubspace(f);

                  loadTrainImage.setEnabled(true);
                  createSubspace.setEnabled(true);
                  loadSubspace.setEnabled(true);
                  clearSubspace.setEnabled(true);
                  saveSubspace.setEnabled(true);
               }
                  
            }

         }
      });
      toolbars[1].add(Box.createHorizontalStrut(5));
   }

   private void setSubspaceFieldInfo()
   {      
      if (model != null)
         subspaceFieldInfo.setText(model.toString());
      else
         subspaceFieldInfo.setText("Subspace [null]");
      
      subspaceFieldInfo.revalidate();
      subspaceFieldInfo.repaint();
   }

   /**
    * Check if all user input are non-null. If not synchronizes GUI
    */
   protected void checkUserInputValidity()
   {
      if (isUserInputValid()){
         createSubspace.setEnabled(true);
      } else {
         createSubspace.setEnabled(false);
      }
   }

 
   private static final int SUBSPACE_LISTER = 0;
   private static final int TRAINING_IMAGE_LISTER = 1;
  
   private File saveFileDialog(int type)
   {
      File f = null;

      FileNameExtensionFilter filter;
      String title = null;

      //if (type == SUBSPACE_LISTER) {
         filter = new FileNameExtensionFilter("Subspaces", "subspace");
         fl.setFilePreviewSupported(false);
         //fl.setFilePreviewEnabled(true);
         fl.setSaveAsTypesExtensions(new String[]{"subspace"});
         fl.setDefaultSaveAsTypesExtention("subspace");

         title = "Save subspace";

      //}

      fl.setOkButtonText("Save");
      fl.setCancelButtonText("Cancel");

      fl.addChoosableFileFilter(filter);

      
      int retVal = fl.showDialog((JFrame) SwingUtilities.getRoot(this),
              FileLister.SAVE_DIALOG, title);


      // Process the results.
      if (retVal == FileLister.APPROVE_OPTION)
         f = fl.getSelectedFile();

      // Reset the file lister for the next time it's shown.
      fl.setSelectedFile(null);

      return f;
   }

   private File openFileDialog(int type)
   {
      File f = null;

      FileNameExtensionFilter filter;
      String title = null;

      if (type == SUBSPACE_LISTER){
         filter = new FileNameExtensionFilter("Subspaces", "subspace");
         fl.setFilePreviewSupported(false);
         //fl.setFilePreviewEnabled(true);
         title = "Open subspace";

      } else { // (type == TRAINING_IMAGE_LISTER)
         String[] supportedExtensions = plsdejai.util.ImageIconUtils.getSupportedExtensions();
         filter = new FileNameExtensionFilter("Images", supportedExtensions);
         fl.setFilePreviewSupported(true);
         fl.setFilePreviewEnabled(false);

         title = "Open training image";

      }

      fl.setOkButtonText("Open");
      fl.setCancelButtonText("Cancel");

      fl.addChoosableFileFilter(filter);

      fl.setOkButtonText("Open");
      int retVal = fl.showDialog((JFrame) SwingUtilities.getRoot(this),
              FileLister.OPEN_FILE_DIALOG, title);

      // Process the results
      if (retVal == FileLister.APPROVE_OPTION)
         f = fl.getSelectedFile();
      else
         f = null;

      // Reset the file lister for the next time it's shown.
      fl.setSelectedFile(null);

      return f;
   }
}
