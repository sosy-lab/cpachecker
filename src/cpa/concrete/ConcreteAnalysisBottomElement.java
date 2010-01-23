/**
 * 
 */
package cpa.concrete;

/**
 * @author holzera
 *
 * Bottom element of the concrete analysis lattice.
 */
public class ConcreteAnalysisBottomElement implements ConcreteAnalysisDomainElement {
  private final static ConcreteAnalysisBottomElement mInstance = new ConcreteAnalysisBottomElement();
  
  public static ConcreteAnalysisBottomElement getInstance() {
    return mInstance;
  }
  
  private ConcreteAnalysisBottomElement() {
    
  }
  
  @Override
  public String toString() {
    return "<ConcreteAnalysis BOTTOM>";
  }

  @Override
  public boolean isError() {
    return false;
  }
}
