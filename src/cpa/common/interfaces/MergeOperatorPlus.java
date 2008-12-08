/**
 *
 */
package cpa.common.interfaces;

import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public interface MergeOperatorPlus {
  // TODO I think with Java 1.6 it should be possible to say <AE super AbstractElementWithLocation>
  public AbstractElement merge (AbstractElement element1, AbstractElement element2, Precision precision) throws CPAException;
  public AbstractElementWithLocation merge (AbstractElementWithLocation element1, AbstractElementWithLocation element2, Precision precision) throws CPAException;
}
