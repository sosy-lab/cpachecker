package cpa.art;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class ARTMergeSep implements MergeOperator {

  @Override
  public AbstractElementWithLocation merge(
      AbstractElementWithLocation pElement1,
      AbstractElementWithLocation pElement2, Precision pPrecision)
  throws CPAException {
    return pElement2;
  }

  @Override
  public AbstractElement merge(AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision) throws CPAException {
    assert(false);
    return pElement2;
  }

}
