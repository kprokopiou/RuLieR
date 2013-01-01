package plsdejai.util;

import java.util.ArrayList;
import java.util.List;


/**
 * A dynamic n x n array of Object values. It is extended automatically,
 * in order to accomodate indexes out of the array, by adding new columns
 * and rows. It always has the same number of rows and columns.
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 08/02/2012
 */
public class DynamicArray2D<T>
{

   /** The dynamic data structure holding the data of the 2D array  */
   private List<List<T>> list;

   /** An optimization constant, that specifies the default initial size of Array Lists */
   private int capacity = 500;

   /** Constructor */
   public DynamicArray2D() {  list = new ArrayList<List<T>>(capacity); }

   /**
    * Constructor
    * @param capacity  specifies the initial size of Array Lists
    */
   public DynamicArray2D(int capacity) {
      setCapacity(capacity);
      list = new ArrayList<List<T>>(capacity);
   }

   /**
    * Set the initial size of Array Lists
    * @param capacity specifies the initial size of Array Lists
    */
   public final void setCapacity(int capacity)
   {
      this.capacity = capacity;
   }

   /**
    * @return the capacity value, which specifies the initial size of Array Lists
    */
   public int getCapacity() { return capacity; }

   /**
    * @return the length of columns and rows in the dynamic array.
    *         Note: All rows and columns have the same length
    */
   public int getLength() { return list.size(); }

   /**
    * @return true if the dynamic array is empty, otherwise false
    */
   public boolean isEmpty() { return list.isEmpty(); }

   /**
    * Sets the length of columns and rows in the dynamic array.
    *         Note: All rows and columns have the same length
    * @param newLength the new row and column length
    */
   public void setLength(int newLength)
   {
      if (capacity < newLength)
         capacity = (int)(newLength * 1.2);

      int currentLength = list.size();

      // Check if it is needed to extend the dymamic array
      if (newLength > currentLength) {
         // Add rows
         for (int r = currentLength; r < newLength; ++r) {

            List<T> row = new ArrayList<T>(capacity);

	    list.add(row);            

            // Fill newly created row with the null value
            for (int c = 0; c < newLength; ++c) {
               row.add((T) null);               
            }
         }

         // Add appropriate cols to the original rows of the array, and set the null value
         for (int r = 0; r < currentLength; ++r) {
            List<T> row = list.get(r);

            for (int c = currentLength; c < newLength; ++c) {
               row.add(c, (T) null);
            }
         }

      } else { 
         return;
      }

   }

   /**
    * Add a value to the cell with coordinates x and y
    * @param x the column
    * @param y the row
    * @param value the value that is added to the array
    * @return <code>value </code>
    */
   public T setValue( int x, int y, T value)
   {
      // The maximum dimension specified
      int newLength = Math.max(x, y) + 1; // note: row/col is zero indexed
      int currentLength = list.size();

      // Check if it is needed to extend the dymamic array
      if (newLength > currentLength)
         setLength(newLength);

      // Now set value and return old value
      return list.get(x).set(y, value);
   }


   /**
    * Get the value of the cell with coordinates x and y
    * @param x the column
    * @param y the row
    * @return the value in column x and row y
    */
   public T getValue( int x, int y )
   {
      return list.get(x).get(y);
   }

   public void clear()
   {
      for(int i = 0; i < list.size(); ++i){
         list.get(i).clear();
      }
      list.clear();
   }
}

