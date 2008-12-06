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
  public AbstractElement merge (AbstractElement element1, AbstractElement element2, Precision precision) throws CPAException;
}
