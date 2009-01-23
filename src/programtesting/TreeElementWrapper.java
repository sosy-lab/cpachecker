/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting;

/**
 *
 * @author holzera
 */
public class TreeElementWrapper<TreeElement> {
  TreeElement mElement;
  
  public TreeElementWrapper(TreeElement lElement) {
    assert(lElement != null);
    
    mElement = lElement;
  }
  
  public TreeElement getWrappedElement() {
    return mElement;
  }
  
  @Override
  public boolean equals(Object o) {
    return (mElement == o);
  }
  
  @Override
  public int hashCode() {
    return mElement.hashCode();
  }
  
  @Override
  public String toString() {
    return mElement.toString();
  }
}
