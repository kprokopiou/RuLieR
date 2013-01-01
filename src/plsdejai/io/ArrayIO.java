package plsdejai.io;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A utility that loads and saves array objects, that contain elements
 * of int or double
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 26/12/2011
 */
public class ArrayIO
{

   public void save(String filename, int[][] iArr )
   {
      ObjectOutputStream out = null;
      try {
         out = new ObjectOutputStream(
                 new GZIPOutputStream(
                 new FileOutputStream(filename)));

         out.writeObject(iArr);
         

      } catch (EOFException e) {

      } catch (IOException e) {

      } finally {
         try {
            if (out != null) {
               out.flush();
               out.close();
            }
         } catch (Exception e){}
      }


   }

   public void save(String filename, double[][] dArr )
   {
      ObjectOutputStream out = null;
      try {
         out = new ObjectOutputStream(
                 new GZIPOutputStream(
                 new FileOutputStream(filename)));

         out.writeObject(dArr);

      } catch (EOFException e) {
         System.out.println(e);
      } catch (IOException e) {
         System.out.println(e);
      } finally {
         try {
            if (out != null) {
               out.flush();
               out.close();
            }
         } catch (Exception e){}
      }


   }

   public int[][] loadInt(String filename)
   {
      ObjectInputStream in = null;
      try {
         in = new ObjectInputStream(
                 new GZIPInputStream(
                 new FileInputStream(filename)));

         int[][] iArr = (int[][]) in.readObject();
         return iArr;

      } catch (EOFException e) {
         // System.out.println("EOF while reading '" +filename  +"'");
      } catch (IOException e) {
         System.out.println(e);
      } catch (ClassNotFoundException e) {
         System.out.println(e);
      } finally {         
         try {
            if (in != null) {
               in.close();
            }
         } catch (Exception e){}
      }

      return null;

   }

   public double[][] loadDouble( String filename )
   {
      ObjectInputStream in = null;
      try {
        in = new ObjectInputStream(
                new GZIPInputStream(
                new FileInputStream(filename)));

        double[][] dArr = (double[][]) in.readObject();        
        return dArr;

      } catch (EOFException e) {
         // System.out.println("Saving to '" +filename  +"' : EOF ");
      } catch (IOException e) {
         System.out.println(e);
      } catch (ClassNotFoundException e) {
         System.out.println(e);
      } finally {
         try {
            if (in != null) {
               in.close();
            }
         } catch (Exception e){}
      }

      return null;

   }
   
}
