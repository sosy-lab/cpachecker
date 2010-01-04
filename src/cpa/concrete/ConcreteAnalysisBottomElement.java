/**
 * 
 */
package cpa.concrete;

/**
 * @author holzera
 *
 */
public class ConcreteAnalysisBottomElement extends ConcreteAnalysisElement {
  private final static ConcreteAnalysisBottomElement mInstance = new ConcreteAnalysisBottomElement();
  
  public static ConcreteAnalysisBottomElement getInstance() {
    return mInstance;
  }
  
  private ConcreteAnalysisBottomElement() {
    
  }
  
  @Override
  public boolean equals(Object pOther) {
    assert(this == mInstance);
    
    return (this == pOther);
  }
  
  @Override
  public int hashCode() {
    return Integer.MIN_VALUE;
  }
  
  @Override
  public String toString() {
    return "<ConcreteAnalysis BOTTOM>";
  }
}
