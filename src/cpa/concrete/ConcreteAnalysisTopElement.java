/**
 * 
 */
package cpa.concrete;

/**
 * @author holzera
 *
 * Top element of the concrete analysis lattice.
 */
public class ConcreteAnalysisTopElement implements ConcreteAnalysisDomainElement {
  private final static ConcreteAnalysisTopElement mInstance = new ConcreteAnalysisTopElement();
  
  public static ConcreteAnalysisTopElement getInstance() {
    return mInstance;
  }
  
  private ConcreteAnalysisTopElement() {
    
  }
  
  @Override
  public String toString() {
    return "<ConcreteAnalysis TOP>";
  }

  @Override
  public boolean isError() {
    return false;
  }
}
