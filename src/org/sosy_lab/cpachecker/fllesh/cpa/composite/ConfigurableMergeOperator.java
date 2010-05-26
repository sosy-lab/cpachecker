package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeMergeOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class ConfigurableMergeOperator extends CompositeMergeOperator {

  private int[] mIndices;
  
  public ConfigurableMergeOperator(
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
    CompositeElement lElement1 = (CompositeElement)pElement1;
    CompositeElement lElement2 = (CompositeElement)pElement2;
    
    boolean lMerge = true;
    
    for (int lIndex = 0; lIndex < mIndices.length; lIndex++) {
      if (!lElement1.get(mIndices[lIndex]).equals(lElement2.get(mIndices[lIndex]))) {
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
