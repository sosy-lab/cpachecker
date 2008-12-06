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
  public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached, Precision precision) throws CPAException;
}
