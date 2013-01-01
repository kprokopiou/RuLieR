package plsdejai.widgets;

import javax.swing.JPanel;
import plsdejai.Parameter;

/**
 * Defines an abstract class, which is responsible for creating the toolbar
 * that is a part of a <code>StandardBinaryOp</code> object
 *
 * @author Prokopiou Konstantinos
 * @version 1.0, 28/01/2012
 */
public abstract class AbstractParameterToolbar extends JPanel
{
   /**
    * This method is responsible for creating a component of the toolbar
    * relating to a parameter
    * @param name the name of a parameter (maximum 40 characters)
    * @param desc the description of a parameter
    * @param type the type of the parameter (Double or Integer)
    * @param min the minimum value that can be given to a parameter
    * @param max the maximum value that can be given to a parameter
    * @param value the initial value of a parameter
    */
   public abstract void createParameter(String name, String desc, int type,
           Number min, Number max, Number value);

   /**
    * This method is responsible for setting the value of a parameter
    * @param name the name of a parameter
    * @param value the new value of a parameter
    */
   public abstract void setParameterValue(String name, Number value);

   /**
    * This method is responsible for getting the value of a parameter
    * @param name the name of the parameter
    * @return the current value of the parameter
    */
   public abstract Number getParameterValue(String name);

   /**
    * @return the whole set of the parameters of the
    * <code>AbstractParameterToolbar</code> object
    */
   public abstract Parameter[] getParameters();

}
