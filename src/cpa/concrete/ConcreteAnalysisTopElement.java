/**
 * 
 */
package cpa.concrete;

/**
 * @author holzera
 *
 */
public class ConcreteAnalysisTopElement extends ConcreteAnalysisElement {
  private final static ConcreteAnalysisTopElement mInstance = new ConcreteAnalysisTopElement();
  
  public static ConcreteAnalysisTopElement getInstance() {
    return mInstance;
  }
  
  private ConcreteAnalysisTopElement() {
    
  }
  
  @Override
  public boolean equals(Object pOther) {
    assert(this == mInstance);
    
    return (this == pOther);
  }
  
  @Override
  public int hashCode() {
    return Integer.MAX_VALUE;
  }
  
  @Override
  public String toString() {
    return "<ConcreteAnalysis TOP>";
  }
}
