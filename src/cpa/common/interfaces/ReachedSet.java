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
public interface ReachedSet extends Iterable<Pair<AbstractElementWithLocation, Precision>> {
  public boolean add(Pair<AbstractElementWithLocation, Precision> pPair);
  public boolean addAll(Collection<Pair<AbstractElementWithLocation, Precision>> pToAdd);
  void clear();
  public boolean removeAll(Collection<Pair<AbstractElementWithLocation, Precision>> pToRemove);
  public Collection<AbstractElementWithLocation> getAbstractElementSet();
}
