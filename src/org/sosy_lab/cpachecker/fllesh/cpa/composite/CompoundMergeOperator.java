package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class CompoundMergeOperator implements MergeOperator {

  private int[] mIndices;
  private MergeOperator[] mMergeOperators;
  
  public CompoundMergeOperator(
      ImmutableList<MergeOperator> pMergeOperators, int[] lIndices) {
    mMergeOperators = new MergeOperator[pMergeOperators.size()];
    
    for (int lIndex = 0; lIndex < pMergeOperators.size(); lIndex++) {
      mMergeOperators[lIndex] = pMergeOperators.get(lIndex);
    }
    
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
    CompositePrecision lPrecision = (CompositePrecision)pPrecision;
    
    boolean lMerge = true;
    
    for (int lIndex = 0; lIndex < mIndices.length; lIndex++) {
      if (!lElement1.getSubelement(mIndices[lIndex]).equals(lElement2.getSubelement(mIndices[lIndex]))) {
        lMerge = false;
      }
    }
    
    if (lMerge) {
      List<AbstractElement> lMergedElements = new ArrayList<AbstractElement>(mMergeOperators.length);
      
      for (int lIndex = 0; lIndex < mMergeOperators.length; lIndex++) {
        MergeOperator lMergeOperator = mMergeOperators[lIndex];
        AbstractElement lSubElement1 = lElement1.getSubelement(lIndex);
        AbstractElement lSubElement2 = lElement2.getSubelement(lIndex);
        Precision lSubprecision = lPrecision.get(lIndex);
        lMergedElements.add(lMergeOperator.merge(lSubElement1, lSubElement2, lSubprecision));
      }
      
      return new CompoundElement(lMergedElements);
    }
    else {
      return pElement2;
    }
  }

}
