package plsdejai.util;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * PointList extends ArrayList, so that it can add the Points in a specific
 * order, i.e. by x (col) or y (row) Dimension
 * Also note that it does not adds points that already exist, so it conforms
 * to the set definition.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 09/02/2012
 */
public class PointSortedArrayList extends ArrayList<Point>
{
   // Every Serializable class should define an ID
   public static final long serialVersionUID = 42L;
   
   /** Constant specifying sort order by X (col) and then by Y (row) */
   public final static int XY_ORDER = -1;
   /** Constant specifying sort order by Y (row) and then by X (col) */
   public final static int YX_ORDER = -2;
   /** Variable that keeps the sorting order mode. It can take
    *  either the value of XY_ORDER or YX_ORDER */
   private int sortOrder;

   /**
    * Constructor
    * @param sortOrder the sorting order, either the value of XY_ORDER or YX_ORDER.
    */
   public PointSortedArrayList(int sortOrder) {
      super();
      setSortOrder(sortOrder);
   }

   /**
    * Constructor
    * @param points a collection of points to be added in the list
    * @param sortOrder the sorting order, either the value of XY_ORDER or YX_ORDER.
    */
   public PointSortedArrayList(Collection<Point> points, int sortOrder)
   {
      this(sortOrder);
      addAll(points);
   }

   /**
    * @param initialCapacity The initial capacity of the list
    * @param sortOrder the sorting order, either the value of XY_ORDER or YX_ORDER.
    */
   public PointSortedArrayList(int initialCapacity, int sortOrder)
   {
      super(initialCapacity);
      setSortOrder(sortOrder);
   }


   /**
    * @return the sort order, either the value of XY_ORDER or YX_ORDER.
    */
   public int getSortOrder() { return this.sortOrder; }

   /**
    * Sets the sorting order.
    * @param sortOrder either the value of XY_ORDER or YX_ORDER.
    */
   public final void setSortOrder(int sortOrder)
   {
      if (sortOrder != XY_ORDER && sortOrder != YX_ORDER)
         throw new IllegalArgumentException();

      this.sortOrder = sortOrder;
   }

   /**
    * Adds a point to the list
    * @param p the point to be added to the list
    * @return true if the point was added, and false, if the point wasn't added
    *         for e.g. because already exists.
    * @overrides method <code>add</code> in ArrayList
    */
   public boolean add(Point p)
   {
      int index = 0;

      for (Iterator<Point> it = super.iterator(); it.hasNext();++index) {
         Point pB = it.next();         

         int posA1 = 0,
             posB1 = 0,
             posA2 = 0,
             posB2 = 0;

         if (sortOrder == XY_ORDER) {
            posA1 = p.x;
            posA2 = p.y;
            posB1 = pB.x;
            posB2 = pB.y;
         } else /* sortOrder == YX_ORDER */ {
            posA1 = p.y;
            posA2 = p.x;
            posB1 = pB.y;
            posB2 = pB.x;
         }

         if (posB1 > posA1)
            break;
         else if (posB1 == posA1)
            if (posB2 == posA2)
               return false; // duplicate Point
            else if (posB2 > posA2)
               break;

      }

      super.add(index, p);

      return true;
   }

   /**
    * This method exists with the purpose to invalidate the inherited method
    * from ArrayList
    */
   public void add(int index, Point p)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * Adds a collection o points to the list
    * @param points a collection of points to be added
    * @return true if at least point was added, and false, if no point added
    *         for e.g. because all points in the collection already exists.
    * @overrides method <code>addSll</code> in ArrayList
    */
   public final boolean addAll(Collection<? extends Point> points)
   {
      boolean isCollectionChanged = false;
      for (Iterator<? extends Point> it = points.iterator(); it.hasNext();)
         if ( add(it.next() ) )
            isCollectionChanged = true;

      return isCollectionChanged;
   }
}
