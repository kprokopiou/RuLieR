package plsdejai.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import plsdejai.StandardBinaryOp;

/**
 * This dialog is responsible for creating the combo box with the filters
 * in the Main window
 * 
 * @author Prokopiou Konstantinos
 * @version 1.0, 7/11/2011
 */
public class FilterListComboBox extends javax.swing.JComboBox
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   // Maximum length of the name of a filter that is allowed to be displayed
   private static final int MAX_NAME_LENGTH = 40;

   // Filters that are supported by default, and normally they can not be removed
   // They are added by instantiation of a FilterListComboBox object
   // and/or by using the addDefaultItem() method
   // and  they are unchanged afterwards
   // removeItem methods force this restriction
   List<StandardBinaryOp> defaultFilters;

   // Filters that are added dynamically by the user, and can be also be removed
   // Of course, addeFilters and defaultFilters make all the items in comboModel
   List<StandardBinaryOp> addedFilters = new ArrayList<StandardBinaryOp>();
   DefaultComboBoxModel comboModel;

   @SuppressWarnings("OverridableMethodCallInConstructor")
   public FilterListComboBox(List<StandardBinaryOp> defaultFilters)
   {
      if (defaultFilters != null) {
         this.defaultFilters = new ArrayList<StandardBinaryOp>(defaultFilters);
      } else {
         this.defaultFilters = new ArrayList<StandardBinaryOp>();
      }
      comboModel = new DefaultComboBoxModel();
      setModel(comboModel); // Specify a List model. Note there is no default List Model

      for (Iterator<StandardBinaryOp> it = this.defaultFilters.iterator(); it.hasNext();) {
         StandardBinaryOp filter = it.next();
         this.addItem(filter);
      }

      this.setMaximumRowCount(8);

      setRenderer(new FileListCellRenderer());
   }


   public FilterListComboBox() { this(null); }

   public List<StandardBinaryOp> getDefaultFilters()
   {
      return new ArrayList<StandardBinaryOp>(defaultFilters);
   }

    public List<StandardBinaryOp> getAddedFilters()
   {
      return new ArrayList<StandardBinaryOp>(addedFilters);
   }

   private void updatePrototypeDisplayValue(StandardBinaryOp filter)
   {
      StandardBinaryOp maxFilter = (StandardBinaryOp) getPrototypeDisplayValue();

      int maxNameLen = 0;

      if (maxFilter != null)
         maxNameLen = maxFilter.getName().length();

      if (filter.getName().length() > maxNameLen)
         maxFilter = filter;


      // Order the ListCellRenderer to use this default dimension
      // for all File elements it renders
      setPrototypeDisplayValue(maxFilter);

   }

   public void addDefaultItem(Object item)
   {
      StandardBinaryOp filter = (StandardBinaryOp) item;

      updatePrototypeDisplayValue(filter);

      defaultFilters.add(filter);
      comboModel.addElement(item);
      /*
       * if (this.isShowing()){
      revalidate();
      repaint();
      }
       */
   }

   public void addItem(Object item)
   {
      StandardBinaryOp filter = (StandardBinaryOp) item;

      updatePrototypeDisplayValue(filter);

      addedFilters.add(filter);
      comboModel.addElement(item);
      /*
       * if (this.isShowing()){
      revalidate();
      repaint();
      }
       */
   }

   public void removeItem(Object item)
   {
      StandardBinaryOp filter = (StandardBinaryOp) item;

      boolean updatePrototype = false;

      // Remove only if the filter belongs to an added, not a default filter
      for (Iterator<StandardBinaryOp> it = addedFilters.iterator(); it.hasNext();) {
         StandardBinaryOp f = it.next();

         if (f == filter ) {
            
            it.remove();
            comboModel.removeElement(item);
            if (item == getPrototypeDisplayValue())
               updatePrototype = true;
         }
      }

      if (updatePrototype)
         for (Iterator<StandardBinaryOp> it = addedFilters.iterator(); it.hasNext();) {
            updatePrototypeDisplayValue(it.next());
         }

   }

   // A poor implementation of the method, because we remove items only from the addedFilters
   public void removeItemAt(int index)
   {
      removeItem ( comboModel.getElementAt(index) );
   }

   public Dimension getMaximumSize()
   {
      return getPreferredSize();
   }

   class FileListCellRenderer extends JLabel implements ListCellRenderer
   {
      // Every Serializable class should define an ID
      public static final long serialVersionUID = 42L;

      public FileListCellRenderer()
      {
         setOpaque(true);
      }

      public Component getListCellRendererComponent(JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus)
      {
         StandardBinaryOp filter = (StandardBinaryOp) value;


         String name = filter.getName();
         setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

         // haircut name:
         if (name.length() > MAX_NAME_LENGTH)
            name = name.substring(0, MAX_NAME_LENGTH);
         setText(name);

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
