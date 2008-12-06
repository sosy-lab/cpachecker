/**
 *
 */
package cpa.common.interfaces;

import java.util.Collection;

import common.Pair;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public interface PrecisionAdjustment {
  public Pair<AbstractElement,Precision> prec (AbstractElement element, Precision precision,
                                            Collection<Pair<AbstractElement,Precision>> elements);
}
