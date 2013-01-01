package plsdejai.widgets.event;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

/**
 * An implementation of MouseListener and MouseMotionListener that gives better
 * L&F than the default implementation, especially for selecting items by dragging.
 *
 * USAGE: ( new JListMouseListeners(list) ).addMouseListeners()
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 19/12/2011
 */
public class JListMouseListeners
        extends JComponent // in order to call the protected method firePropertyChange(String, Object,Object)
        implements MouseListener, MouseMotionListener
{

   private JList list;
   private ListSelectionModel selectionModel;

   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   public JListMouseListeners(JList list) {
      this.list = list;
      selectionModel = list.getSelectionModel();
   }

   public void addMouseListeners()
   {
      // Remove default MouseListeners
      MouseListener[] mouseListeners = list.getMouseListeners();
      int i = 0;
      for (int len = mouseListeners.length; i < len; ++i)
         list.removeMouseListener(mouseListeners[i]);

      // Remove default MouseMotionListeners
      MouseMotionListener[] mouseMotionListeners = list.getMouseMotionListeners();
      i = 0;
      for (int len = mouseMotionListeners.length; i < len; ++i)
         list.removeMouseMotionListener(mouseMotionListeners[i]);

      list.addMouseListener(this);
      list.addMouseMotionListener(this);
   }

   
   private boolean isDragged = false;

   public static String LIST_SELECTED_INDEX_CHANGED_PROPERTY = "listSelectedIndexChanged";

   public void mousePressed(MouseEvent e)
   {
      isDragged = false;
      int index = list.locationToIndex(e.getPoint());

      int anchor = list.getAnchorSelectionIndex();
      int lead = list.getLeadSelectionIndex();

      switch ( list.getSelectionMode()){

         case ListSelectionModel.SINGLE_INTERVAL_SELECTION:
            
            if (anchor == -1 || lead == -1)
               selectionModel.setSelectionInterval(index, index);
            else if (e.isShiftDown())
               selectionModel.addSelectionInterval(index, index);
            //else if (e.isControlDown())
              // selectionModel.addSelectionInterval(index, index);
            else
               selectionModel.setSelectionInterval(index, index);
            break;

         case ListSelectionModel.MULTIPLE_INTERVAL_SELECTION:
       
            if (anchor == -1 || lead == -1)
               selectionModel.setSelectionInterval(index, index);

            else if (e.isShiftDown())
               if (list.isSelectedIndex(index))
                  selectionModel.removeSelectionInterval(index, index);
               else{
                  selectionModel.addSelectionInterval(anchor, index);
                  firePropertyChange(LIST_SELECTED_INDEX_CHANGED_PROPERTY, null,
                          list.getModel().getElementAt(index));
               }
                  

            else if (e.isControlDown())
               if (list.isSelectedIndex(index))
                  selectionModel.removeSelectionInterval(index, index);
               else{
                  selectionModel.addSelectionInterval(index, index);
                  firePropertyChange(LIST_SELECTED_INDEX_CHANGED_PROPERTY, null,
                          list.getModel().getElementAt(index));
               }
                  
            else
               selectionModel.setSelectionInterval(index, index);
            
            break;

         default: //  case : ListSelectionModel.SINGLE_SELECTION:
            
            selectionModel.setSelectionInterval(index, index );
            break;

      }
      
   }


   public void mouseDragged(MouseEvent e)
   {
      int index = list.locationToIndex(e.getPoint());     


      if (list.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION)
         selectionModel.setSelectionInterval(index, index );

      else {

         int anchor = list.getAnchorSelectionIndex();
         //int lead = list.getLeadSelectionIndex();

         if (isDragged) {

            if (index != anchor) // I assume that anchor == lead, for a drag operation
               if (list.isSelectedIndex(index))
                  selectionModel.removeSelectionInterval(index, index);
               else{
                  selectionModel.addSelectionInterval(index, index);
                  firePropertyChange(LIST_SELECTED_INDEX_CHANGED_PROPERTY, null,
                          list.getModel().getElementAt(index));
               }

         } else isDragged = true;
      }

   }


   public void mouseClicked(MouseEvent e) { list.requestFocusInWindow(); }
   public void mouseReleased(MouseEvent e) { }
   public void mouseEntered(MouseEvent e)  { }
   public void mouseExited(MouseEvent e) { }
   public void mouseMoved(MouseEvent e) { }

}
