package cpa.common.interfaces;

import exceptions.CPAException;

public interface MergeOperator
{
  // TODO I think with Java 1.6 it should be possible to say <AE super AbstractElementWithLocation>
  public AbstractElement merge (AbstractElement element1, AbstractElement element2, Precision precision) throws CPAException;
  public AbstractElementWithLocation merge (AbstractElementWithLocation element1, AbstractElementWithLocation element2, Precision precision) throws CPAException;
}
