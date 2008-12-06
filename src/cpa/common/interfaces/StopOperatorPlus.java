/**
 *
 */
package cpa.common.interfaces;

import java.util.Collection;

import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public interface StopOperatorPlus {
  public boolean stop (AbstractElement element, Collection<AbstractElement> reached, Precision precision) throws CPAException;
}
