package plsdejai.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import plsdejai.Main;
import plsdejai.StandardBinaryOp;

/**
 * This dialog is called in order to remove one or more filters from the
 * filter list in the Main window
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 11/01/2012
 */
public class FilterRemoveDialog extends JDialog
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;

   private JButton okCommand;
   private JButton cancelCommand;

   private Main owner;
   
   // a list of the filters added by the user
   JList filterList;

    public FilterRemoveDialog (Main owner){
      super(owner, "Remove Filter", Dialog.ModalityType.APPLICATION_MODAL);

      if (owner.getAddedFilters().isEmpty()){
         this.dispose();
         return;
      }

      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      setResizable(false);
      this.owner = owner;

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
      Container contentPane = getContentPane();
      contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

      contentPane.add(createFilterListPanel());
      contentPane.add(Box.createVerticalStrut(10));
      contentPane.add(createCommandPanel());
      contentPane.add(Box.createVerticalStrut(10));

      this.setContentPane(contentPane);
   }

   private JPanel createFilterListPanel()
   {
      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createEmptyBorder(20, 4, 10, 4));
      panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

      DefaultListModel listModel = new DefaultListModel();
      filterList = new JList(listModel);

      List <StandardBinaryOp> filters = owner.getAddedFilters();

      for (Iterator<StandardBinaryOp> it = filters.iterator(); it.hasNext();)
         listModel.addElement(it.next());

      ( new plsdejai.widgets.event.JListMouseListeners(filterList) ).addMouseListeners();
     
      filterList.getSelectionModel().addListSelectionListener(
              new ListSelectionListener(){
         public void valueChanged(ListSelectionEvent e)
         {
            int [] indices = filterList.getSelectedIndices();
            if (indices != null && indices.length != 0 )
               okCommand.setEnabled(true);
            else
               okCommand.setEnabled(false);

         }

      });
      filterList.setCellRenderer(new FilterListCellRenderer());

      JScrollPane listScroller = new JScrollPane (filterList);
      listScroller.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "List of Added Filters"));
      filterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

      filterList.setBackground(filterList.getParent().getBackground());
      filterList.clearSelection();
      panel.add( listScroller );


      filterList.setMinimumSize(new Dimension(250, 260));

      panel.add(Box.createHorizontalGlue());

      return panel;
   }

   private JPanel createCommandPanel()
   {
      JPanel commandPanel = new JPanel();
      commandPanel.setBorder(BorderFactory.createEmptyBorder(20, 4, 10, 4));
      commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.LINE_AXIS));

      commandPanel.add(Box.createHorizontalStrut(10));

      okCommand = new JButton("Remove");
      okCommand.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            int [] indices = filterList.getSelectedIndices();
            int len = indices.length;
            if (indices != null && len != 0){

               DefaultListModel listModel = (DefaultListModel) filterList.getModel();
               for (int  i = len - 1; i >= 0; --i){ // Crucial because after a removal, indexes go down.                  
                  listModel.removeElementAt(i);                  
               }
            }

            owner.removeAddedFilters( indices);

            if (filterList.getModel().getSize() == 0)
               okCommand.setEnabled(false);

         }
      });

      okCommand.setEnabled(false);
      commandPanel.add(okCommand);

      commandPanel.add(Box.createHorizontalStrut(20));

      cancelCommand = new JButton("Cancel");
      cancelCommand.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e)
         {
            FilterRemoveDialog.this.dispose();
         }
      });
      commandPanel.add(cancelCommand);
      commandPanel.add(Box.createGlue());

      return commandPanel;
   }


   class FilterListCellRenderer extends JLabel implements ListCellRenderer
   {
      // Every Serializable class should define an ID
      public static final long serialVersionUID = 42L;

      public FilterListCellRenderer()
      {
         setOpaque(true);
      }

      public Dimension getPreferredSize()
      {
         int maxW = (int) Math.max( 200, super.getPreferredSize().getWidth());
         int maxH = (int) super.getPreferredSize().getHeight();
         return new Dimension(maxW, maxH) ;
      }


      public Component getListCellRendererComponent(JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus)
      {

         StandardBinaryOp filter = (StandardBinaryOp) value;


         setText(filter.getName());

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

        return this;
      }
   }

}
