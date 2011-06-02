package org.sosy_lab.cpachecker.cpa.cache;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CacheMergeOperator implements MergeOperator {

  private final MergeOperator mCachedMergeOperator;
  private final Map<Precision, Map<AbstractElement, Map<AbstractElement, AbstractElement>>> mCache;

  public CacheMergeOperator(MergeOperator pCachedMergeOperator) {
    mCachedMergeOperator = pCachedMergeOperator;
    mCache = new HashMap<Precision, Map<AbstractElement, Map<AbstractElement, AbstractElement>>>();
  }

  @Override
  public AbstractElement merge(AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision) throws CPAException {

    Map<AbstractElement, Map<AbstractElement, AbstractElement>> lCache1 = mCache.get(pPrecision);

    if (lCache1 == null) {
      lCache1 = new HashMap<AbstractElement, Map<AbstractElement, AbstractElement>>();
      mCache.put(pPrecision, lCache1);
    }

    Map<AbstractElement, AbstractElement> lCache2 = lCache1.get(pElement2);

    if (lCache2 == null) {
      lCache2 = new HashMap<AbstractElement, AbstractElement>();
      lCache1.put(pElement2, lCache2);
    }

    AbstractElement lMergedElement = lCache2.get(pElement1);

    if (lMergedElement == null) {
      lMergedElement = mCachedMergeOperator.merge(pElement1, pElement2, pPrecision);
      lCache2.put(pElement1, lMergedElement);
    }

    return lMergedElement;
  }

}
