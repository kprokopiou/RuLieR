package plsdejai.widgets;

import java.awt.Dimension;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

/**
 *
 * @author konsprok
 */
public class JListFactory {
   private static final ListModel listModel = new DefaultListModel();

   public static JScrollPane createScrollableList(String [] data)
   {
      JScrollPane listScroller = null;
      JList list = new JList(data);

      list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
      list.setVisibleRowCount(-1);

      listScroller = new JScrollPane(list);
      listScroller.setPreferredSize(new Dimension(250, 80));

      return listScroller;
   }

}
