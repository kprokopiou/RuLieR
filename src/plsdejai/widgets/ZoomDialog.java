package plsdejai.widgets;

import java.awt.event.FocusListener;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import plsdejai.Canvas;
import plsdejai.Main;

/**
 * This widget is responsible for showing the dialog with the zoom options.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 03/11/2011
 */
public class ZoomDialog extends javax.swing.JDialog
        implements ActionListener, FocusListener, DocumentListener
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   private JButton okCommand;
   private JButton cancelCommand;

   private Main owner;
   private double selectedZoom;

   private ButtonGroup group;
   private JRadioButton[] radioButtons;
   private JTextField zoomValue;
   private String titles[];


   public ZoomDialog (Main owner){
      super(owner, "Zoom", Dialog.ModalityType.APPLICATION_MODAL);

      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      setResizable(false);
      this.owner = owner;
      selectedZoom = this.owner.getCanvas().getZoom();

      setContentPane();

      pack();
      
      Rectangle owner_bounds = owner.getBounds();
      Dimension size = getSize();
      setLocation(new Point(owner_bounds.x +
              (owner_bounds.width - size.width) / 2,
              (owner_bounds.y + owner_bounds.height - size.width) / 2));
      setVisible(true);
   }

   private void setContentPane()
   {
      JPanel contentPane = new JPanel();
      contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.LINE_AXIS));
      contentPane.setOpaque(true);

      String buttonTitles[] ={"Fit width and height", "Fit width", "100%", "Variable"};


      contentPane.add(new ButtonGroupWidget(buttonTitles));

      contentPane.add(createCommandPanel());

      this.setContentPane(contentPane);      
   }

   private JPanel createCommandPanel()
   {
      JPanel commandPanel = new JPanel();
      commandPanel.setBorder(BorderFactory.createEmptyBorder(20, 4, 10, 4));
      commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.LINE_AXIS));

      commandPanel.add(Box.createHorizontalStrut(10));

      okCommand = new JButton("Ok");
      okCommand.addActionListener(this);
      commandPanel.add(okCommand);

      commandPanel.add(Box.createHorizontalStrut(20));

      cancelCommand = new JButton("Cancel");
      cancelCommand.addActionListener(this);
      commandPanel.add(cancelCommand);
      commandPanel.add(Box.createGlue());
      
      return commandPanel;
   }

   public void actionPerformed(ActionEvent evt)
   {
      Object src = evt.getSource();

      Canvas canvas= this.owner.getCanvas();

      Dimension viewSize = null;
      Rectangle imgBounds = null;     

      int index;
      for (index = 0; index < radioButtons.length; index++)
         if (radioButtons[index] == evt.getSource())
            break;

      int correction = 15;

      if (index < radioButtons.length ){
         switch (index) {
            case 0: // Fit width and height

               zoomValue.setEnabled(false);

               viewSize = canvas.getViewSize();               
               imgBounds = canvas.getCurrentContent().getBufferedImage().getRaster().getBounds();

               selectedZoom = Math.min(
                   (double) ( viewSize.width - correction) / imgBounds.getWidth(),
                   (double) (viewSize.height - correction) / imgBounds.getHeight());
               break;

            case 1: // Fit width
               zoomValue.setEnabled(false);

               viewSize = canvas.getViewSize();
               imgBounds = canvas.getCurrentContent().getBufferedImage().getRaster().getBounds();

               selectedZoom = (double) ( viewSize.width - correction) / imgBounds.getWidth();
               break;

            case 2: // 100%

               zoomValue.setEnabled(false);
               selectedZoom = 1.0;
               break;

            case 3: // Variable zoom
               zoomValue.setEnabled(true);
               zoomValue.requestFocus();
               zoomValue.selectAll();
               break;

            default:
               JOptionPane.showMessageDialog(null,
                       "Index" + index + "in radioButtons[] exceeds limits.",
                       "Code Error!!!", JOptionPane.ERROR_MESSAGE);
               break;
         }
         zoomValue.setText(String.valueOf((int) (100 *selectedZoom)));


      } else if (src == okCommand) {

         canvas.zoom(selectedZoom);
         this.dispose();

      } else if (src == cancelCommand) {
         this.dispose();  
      }

   }

   public void focusLost(java.awt.event.FocusEvent evt)
   {
      if (zoomValue == (JTextField) evt.getSource()){
         Double zoom = getZoomValue();
         if ( zoom != null)
            selectedZoom = zoom.doubleValue() / 100.0; // values are assumed to be %.
         else
            zoomValue.setText(String.valueOf((int) (selectedZoom * 100)));
         
      }

   }

   public void focusGained(java.awt.event.FocusEvent evt){ }

   private Double getZoomValue()
   {
      Double zoom = null;
      char c = 0;
      String text = zoomValue.getText();
      
      if (text.length() != 0)
         c = text.charAt(text.length() - 1);
    
      try {
         if('0' > c || c > '9' )
            throw new java.lang.NumberFormatException();

         zoom = Double.valueOf(text);
         if (zoomValue.getBackground() == Color.red)
            zoomValue.setBackground(Color.WHITE);
      } catch (java.lang.NumberFormatException e) {
         zoom = null;
         zoomValue.setBackground(Color.RED);
      }

      return zoom;
   }

   public void insertUpdate(javax.swing.event.DocumentEvent evt)
   {
      getZoomValue();
   }
   public void removeUpdate(javax.swing.event.DocumentEvent evt)
   {
      getZoomValue();
   }
   public void changedUpdate(javax.swing.event.DocumentEvent evt){ }


   /**
    * This class keeps together a group of radio buttons
    */
   private class ButtonGroupWidget extends JPanel
   {
      // Every Serializable class should define an ID
      public static final long serialVersionUID = 42L;
      
      public ButtonGroupWidget(String titles[])
      {
         ZoomDialog.this.titles = titles;
         ZoomDialog.this.group = new ButtonGroup();
         ZoomDialog.this.radioButtons = new JRadioButton[titles.length];
         createGUI();
      }

      private void createGUI()
      {
         this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
         int lastIndex = radioButtons.length - 1;
         for (int i = 0; i < titles.length; i++) {
            JPanel buttonContainer = new JPanel();

            buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.LINE_AXIS));
            this.add(buttonContainer);

            radioButtons[i] = new JRadioButton(titles[i]);
            
            radioButtons[i].setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

            //radioButtons[i].setPreferredSize(new Dimension(150, 30));

            radioButtons[i].addActionListener(ZoomDialog.this);

            group.add(radioButtons[i]);

            buttonContainer.add(radioButtons[i]);

            if (i == lastIndex) {               
               zoomValue = new JTextField();
               zoomValue.setColumns(4);
               zoomValue.setMaximumSize(zoomValue.getPreferredSize());
               
               zoomValue.addFocusListener(ZoomDialog.this);
               zoomValue.getDocument().addDocumentListener(ZoomDialog.this);

               buttonContainer.add(zoomValue);

               buttonContainer.add(Box.createGlue());
               
               this.add(Box.createVerticalStrut(10));
               this.add(Box.createVerticalGlue());
            }   else {
               buttonContainer.add(Box.createGlue());
            }
         }

      
         double zoom = owner.getCanvas().getZoom();
         if (zoom - 1.0 < 0.01){
            radioButtons[2].setSelected(true);
            zoomValue.setEnabled(false);
         } else {            
            radioButtons[3].setSelected(true);
            zoomValue.setEnabled(true);
            zoomValue.setText(String.valueOf((int) (zoom * 100)));
            zoomValue.requestFocusInWindow();
         }

      }
   }
}

           


      

         
