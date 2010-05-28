package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeMergeOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class CompoundMergeOperator extends CompositeMergeOperator {

  private int[] mIndices;
  
  public CompoundMergeOperator(
      ImmutableList<MergeOperator> pMergeOperators, int[] lIndices) {
    super(pMergeOperators);
    
    mIndices = lIndices;
    
    for (int lIndex = 0; lIndex < mIndices.length; lIndex++) {
      if (lIndex >= pMergeOperators.size()) {
        throw new IllegalArgumentException();
      }
    }
  }

  @Override
  public AbstractElement merge(AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision) throws CPAException {
    CompoundElement lElement1 = (CompoundElement)pElement1;
    CompoundElement lElement2 = (CompoundElement)pElement2;
    
    boolean lMerge = true;
    
    for (int lIndex = 0; lIndex < mIndices.length; lIndex++) {
      if (!lElement1.getSubelement(mIndices[lIndex]).equals(lElement2.getSubelement(mIndices[lIndex]))) {
        lMerge = false;
      }
    }
    
    if (lMerge) {
      return super.merge(pElement1, pElement2, pPrecision);
    }
    else {
      return pElement2;
    }
  }

}
