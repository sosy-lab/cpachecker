/**
 * 
 */
package cpa.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import common.Pair;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.ReachedSet;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class BasicReachedSet implements ReachedSet {
  
  private final HashSet<Pair<AbstractElementWithLocation,Precision>> data;
  private final HashMap<AbstractElementWithLocation,Integer> entryCount;

  public BasicReachedSet() {
    data = new HashSet<Pair<AbstractElementWithLocation,Precision>>();
    entryCount = new HashMap<AbstractElementWithLocation,Integer>();
  }
  
  public boolean add(Pair<AbstractElementWithLocation, Precision> pPair) {
    if (data.add(pPair)) {
      Integer nEntries = entryCount.get(pPair.getFirst());
      if (nEntries != null) {
        ++nEntries;
      } else {
        entryCount.put(pPair.getFirst(), 1);
      }
      return true;
    }
    return false;
  }

  public boolean addAll(
                     Collection<Pair<AbstractElementWithLocation, Precision>> pToAdd) {
    boolean ret = false;
    for (Pair<AbstractElementWithLocation, Precision> pair : pToAdd) {
      if (data.add(pair)) {
        ret = true;
        Integer nEntries = entryCount.get(pair.getFirst());
        if (nEntries != null) {
          ++nEntries;
        } else {
          entryCount.put(pair.getFirst(), 1);
        }
      }
    }
    return ret;
  }

  public void clear() {
    data.clear();
    entryCount.clear();
  }

  public boolean removeAll(
                        Collection<Pair<AbstractElementWithLocation, Precision>> pToRemove) {
    boolean ret = false;
    for (Pair<AbstractElementWithLocation, Precision> pair : pToRemove) {
      if (data.remove(pair)) {
        ret = true;
        Integer nEntries = entryCount.get(pair.getFirst());
        assert (nEntries != null);
        assert (nEntries >= 1);
        if (1 == nEntries) {
          entryCount.remove(pair.getFirst());
        } else {
          --nEntries;
        }
      }
    }
    return ret;
  }

  public Iterator<Pair<AbstractElementWithLocation, Precision>> iterator() {
    return data.iterator();
  }
  
  public Collection<AbstractElementWithLocation> getAbstractElementSet() {
    return entryCount.keySet();
  }
}
