package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa.UnmodifiableSSAMap;

public class MergedElement extends NonabstractionElement {

  /**
   * The abstract element that was merged into this.
   * Used for fast coverage checks.
   */
  private final NonabstractionElement mMergesInto;
  
  public MergedElement(AbstractionElement pAbstractionElement,
      PathFormula<UnmodifiableSSAMap> pPathFormula, int pSizeSinceAbstraction,
      NonabstractionElement pMergesInto) {
    super(pAbstractionElement, pPathFormula, pSizeSinceAbstraction);
    mMergesInto = pMergesInto;
  }
  
  public NonabstractionElement getMergesInto() {
    return mMergesInto;
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
      MergedElement lOther = (MergedElement)pOther;
      
      return mAbstractionElement.equals(lOther.mAbstractionElement) && mSizeSinceAbstraction == lOther.mSizeSinceAbstraction && mPathFormula.equals(lOther.mPathFormula) && mMergesInto.equals(lOther.mMergesInto);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return super.hashCode() + mMergesInto.hashCode() * 21;
  }

}
