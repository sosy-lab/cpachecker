package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa.UnmodifiableSSAMap;

public class NonabstractionElement implements SymbPredAbsAbstractElement {

  /**
   * Pointer to the last abstraction element.
   */
  protected final AbstractionElement mAbstractionElement;
  
  /** The path formula for the path from the last abstraction node to this node.
   * it is set to true on a new abstraction location and updated with a new
   * non-abstraction location */
  protected final PathFormula<UnmodifiableSSAMap> mPathFormula;
  
  protected int mSizeSinceAbstraction;
  
  public static int INSTANCES = 0;
  
  public NonabstractionElement(AbstractionElement pAbstractionElement, PathFormula<UnmodifiableSSAMap> pPathFormula, int pSizeSinceAbstraction) {
    mAbstractionElement = pAbstractionElement;
    mPathFormula = pPathFormula;
    mSizeSinceAbstraction = pSizeSinceAbstraction;
    INSTANCES++;
  }
  
  public PathFormula<UnmodifiableSSAMap> getPathFormula() {
    return mPathFormula;
  }
  
  @Override
  public int getSizeSinceAbstraction() {
    return mSizeSinceAbstraction;
  }

  @Override
  public AbstractionElement getAbstractionElement() {
    return mAbstractionElement;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (getClass().equals(pOther.getClass())) {
      NonabstractionElement lOther = (NonabstractionElement)pOther;
      
      return mAbstractionElement.equals(lOther.mAbstractionElement) && mSizeSinceAbstraction == lOther.mSizeSinceAbstraction && mPathFormula.equals(lOther.mPathFormula); 
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mAbstractionElement.hashCode() + mSizeSinceAbstraction;
  }
  
}
