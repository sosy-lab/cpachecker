/**
 *
 */
package cpa.common.interfaces;


import common.Pair;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public interface PrecisionAdjustment {
  // TODO I think with Java 1.6 it should be possible to say <AE super AbstractElementWithLocation>
  public <AE extends AbstractElement> Pair<AE,Precision> prec (AE element, Precision precision,
                                            ReachedSet reached);
}
