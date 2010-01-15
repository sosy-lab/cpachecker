package cpa.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;
import cpa.common.interfaces.Precision;

public class ReachedElements implements Iterable<AbstractElement> {
  
  private final Map<AbstractElement, Precision> reached;
  private final Set<AbstractElement> unmodifiableReached;
  private final Collection<Pair<AbstractElement, Precision>> reachedWithPrecision;
  private final SetMultimap<CFANode, AbstractElement> locationMappedReached;
  private AbstractElement lastElement = null;
  private AbstractElement firstElement = null;
  private final LinkedList<AbstractElement> waitlist;
  private final TraversalMethod traversal;
  
  private Function<AbstractElement, Pair<AbstractElement, Precision>> getPrecisionAsPair = 
    new Function<AbstractElement, Pair<AbstractElement, Precision>>() {

      @Override
      public Pair<AbstractElement, Precision> apply(
                  AbstractElement element) {

        return new Pair<AbstractElement, Precision>(element, getPrecision(element));
      }
    
  };
 
  public ReachedElements(String traversal) {
    reached = new HashMap<AbstractElement, Precision>();
    unmodifiableReached = Collections.unmodifiableSet(reached.keySet());
    reachedWithPrecision = Collections2.transform(unmodifiableReached, getPrecisionAsPair);
    if (CPAMain.cpaConfig.getBooleanValue("cpa.useSpecializedReachedSet")) {
      locationMappedReached = HashMultimap.create(); 
    } else {
      locationMappedReached = null;
    }
    
    waitlist = new LinkedList<AbstractElement>();
    //set traversal method given in config file; 
    //throws IllegalArgumentException if anything other than dfs, bfs, or topsort is passed
    this.traversal = TraversalMethod.valueOf(traversal.toUpperCase());
  }
  
  //enumerator for traversal methods
  public enum TraversalMethod {
    DFS, BFS, TOPSORT
  }

  private CFANode getLocationFromElement(AbstractElement element) {
    if (element instanceof AbstractWrapperElement) {
      AbstractElementWithLocation locationElement =
        ((AbstractWrapperElement)element).retrieveLocationElement();
      assert locationElement != null;
      return locationElement.getLocationNode();

    } else if (element instanceof AbstractElementWithLocation) {
      return ((AbstractElementWithLocation)element).getLocationNode();
    
    } else {
      return null;
    }
  }
  
  public void add(AbstractElement element, Precision precision) {
    if (reached.size() == 0) {
      firstElement = element;
    }
    
    reached.put(element, precision);
    if (locationMappedReached != null) {
      CFANode location = getLocationFromElement(element);
      assert location != null : "Location information necessary for SpecializedReachedSet";
      locationMappedReached.put(location, element);
    }
    waitlist.add(element);
    lastElement = element;
  }


  public void addAll(Collection<Pair<AbstractElement, Precision>> toAdd) {
    for (Pair<AbstractElement, Precision> pair : toAdd) {
      add(pair.getFirst(), pair.getSecond());
    }
  }
  
  private void remove(AbstractElement element) {
    int hc = element.hashCode();
    if ((firstElement == null) || hc == firstElement.hashCode() && element.equals(firstElement)) {
      firstElement = null;
    }
    
    if ((lastElement == null) || (hc == lastElement.hashCode() && element.equals(lastElement))) {
      lastElement = null;
    }
    waitlist.remove(element);
    reached.remove(element);
    if (locationMappedReached != null) {
      CFANode location = getLocationFromElement(element);
      if (location != null) {
        locationMappedReached.remove(location, element);
      }
    }
  }
  
  public void removeAll(Collection<? extends AbstractElement> toRemove) {
    for (AbstractElement element : toRemove) {
      remove(element);
    }
    assert firstElement != null || reached.isEmpty() : "firstElement may only be removed if the whole reached set is cleared";
  }
  
  public void clear() {
    firstElement = null;
    lastElement = null;
    waitlist.clear();
    reached.clear();
    if (locationMappedReached != null) {
      locationMappedReached.clear();
    }
  }
  
  public Set<AbstractElement> getReached() {
    return unmodifiableReached;
  }
  
  @Override
  public Iterator<AbstractElement> iterator() {
    return unmodifiableReached.iterator();
  }
  
  public Collection<Pair<AbstractElement, Precision>> getReachedWithPrecision() {
    return reachedWithPrecision; // this is unmodifiable
  }
 
  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * elements belonging to the same location as a given element. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states. 
   * 
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   * 
   * The returned set is unmodifiable.
   * 
   * @param element An abstract element for whose location the abstract states should be retrieved.
   * @return A subset of the reached set.
   */
  public Set<AbstractElement> getReached(AbstractElement element) {
    if (locationMappedReached != null) {
      CFANode loc = getLocationFromElement(element);
      return Collections.unmodifiableSet(locationMappedReached.get(loc));
    }
    return unmodifiableReached;
  }
  
  public AbstractElement getFirstElement() {
    return firstElement;
  }

  public AbstractElement getLastElement() {
    return lastElement;
  }
  
  public TraversalMethod getTraversalMethod() {
    return traversal;
  }
  
  public boolean hasWaitingElement() {
    return !waitlist.isEmpty();
  }
  
  public List<AbstractElement> getWaitlist() {
    return Collections.unmodifiableList(waitlist);
  }
  
  public Pair<AbstractElement, Precision> popFromWaitlist() {
    AbstractElement result = null;

    switch(traversal) {
    case BFS:
      result = waitlist.removeFirst();
      break;
      
    case DFS:
      result = waitlist.removeLast();
      break;

    case TOPSORT:
    default:
      int resultTopSortId = Integer.MIN_VALUE;
      for (AbstractElement currentElement : waitlist) {
        if ((result == null) 
            || (getLocationFromElement(currentElement).getTopologicalSortId() >
                resultTopSortId)) {
          result = currentElement;
          resultTopSortId = getLocationFromElement(result).getTopologicalSortId();
        }
      }
      waitlist.remove(result);
      break;
    }
    
    return getPrecisionAsPair.apply(result);
  }
  
  /**
   * Returns the precision for an element.
   * @param element The element to look for.
   * @return The precision for the element or null.
   */
  public Precision getPrecision(AbstractElement element) {
    return reached.get(element);
  }
  
  public int size() {
    return reached.size();
  }
  
  @Override
  public String toString() {   
    return reached.keySet().toString();
  }
}
