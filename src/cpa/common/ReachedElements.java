package cpa.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

public class ReachedElements implements Iterable<AbstractElementWithLocation> {
  
  private final Set<AbstractElementWithLocation> reached;
  private final Set<AbstractElementWithLocation> unmodifiableReached;
  private final Map<AbstractElement, Precision> precisions;
  private final Collection<Pair<AbstractElementWithLocation, Precision>> reachedWithPrecision;
  private AbstractElementWithLocation lastElement = null;
  private AbstractElementWithLocation firstElement = null;
  private final List<AbstractElementWithLocation> waitlist;
  private final TraversalMethod traversal;
  
  private Function<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>> getPrecisionAsPair = 
    new Function<AbstractElementWithLocation, Pair<AbstractElementWithLocation, Precision>>() {

      @Override
      public Pair<AbstractElementWithLocation, Precision> apply(
                  AbstractElementWithLocation element) {

        return new Pair<AbstractElementWithLocation, Precision>(element, getPrecision(element));
      }
    
  };
 
  public ReachedElements(String traversal) {
    reached = createReachedSet();
    unmodifiableReached = Collections.unmodifiableSet(reached);
    precisions = new HashMap<AbstractElement, Precision>();
    reachedWithPrecision = Collections2.transform(reached, getPrecisionAsPair);
    waitlist = new LinkedList<AbstractElementWithLocation>();
    //set traversal method given in config file; 
    //throws IllegalArgumentException if anything other than dfs, bfs, or topsort is passed
    this.traversal = TraversalMethod.valueOf(traversal.toUpperCase());
  }
  
  //enumerator for traversal methods
  public enum TraversalMethod {
    DFS, BFS, TOPSORT
  }
  
  private Set<AbstractElementWithLocation> createReachedSet() {
    if(CPAMain.cpaConfig.getBooleanValue("cpa.useSpecializedReachedSet")){
      return new LocationMappedReachedSet();
    }
    return new HashSet<AbstractElementWithLocation>();
  }

  public void add(AbstractElementWithLocation element, Precision precision) {
    if (reached.size() == 0) {
      firstElement = element;
    }
    
    reached.add(element);
    waitlist.add(element);
    precisions.put(element, precision);
    lastElement = element;
  }


  public void addAll(Collection<Pair<AbstractElementWithLocation, Precision>> toAdd) {
    for (Pair<AbstractElementWithLocation, Precision> pair : toAdd) {
      add(pair.getFirst(), pair.getSecond());
    }
  }
  
  private void remove(AbstractElementWithLocation element) {
    int hc = element.hashCode();
    if ((firstElement == null) || hc == firstElement.hashCode() && element.equals(firstElement)) {
      firstElement = null;
    }
    
    if ((lastElement == null) || (hc == lastElement.hashCode() && element.equals(lastElement))) {
      lastElement = null;
    }
    waitlist.remove(element);
    reached.remove(element);
    precisions.remove(element);
  }
  
  public void removeAll(Collection<? extends AbstractElementWithLocation> toRemove) {
    for (AbstractElementWithLocation element : toRemove) {
      remove(element);
    }
    assert firstElement != null || reached.isEmpty() : "firstElement may only be removed if the whole reached set is cleared";
  }
  
  public void clear() {
    firstElement = null;
    lastElement = null;
    waitlist.clear();
    reached.clear();
    precisions.clear();
  }
  
  public Set<AbstractElementWithLocation> getReached() {
    return unmodifiableReached;
  }
  
  @Override
  public Iterator<AbstractElementWithLocation> iterator() {
    return unmodifiableReached.iterator();
  }
  
  public Collection<Pair<AbstractElementWithLocation, Precision>> getReachedWithPrecision() {
    return reachedWithPrecision; // this is unmodifiable
  }

  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * elements belonging to a given CFANode. It may even return an empty set if
   * there are no states belonging to the CFANode. Note that it may return up to
   * all abstract states. 
   * 
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   * 
   * The returned set is unmodifiable.
   * 
   * @param loc A CFANode for which the abstract states should be retrieved.
   * @return A subset of the reached set.
   */
  public Set<AbstractElementWithLocation> getReached(CFANode loc) {
    Set<AbstractElementWithLocation> result;
    if (reached instanceof LocationMappedReachedSet) {
      result = ((LocationMappedReachedSet)reached).getReached(loc);
    } else {
      result = reached;
    }
    return Collections.unmodifiableSet(result);
  }
  
  public AbstractElementWithLocation getFirstElement() {
    return firstElement;
  }

  public AbstractElementWithLocation getLastElement() {
    return lastElement;
  }
  
  public TraversalMethod getTraversalMethod() {
    return traversal;
  }
  
  public boolean hasWaitingElement() {
    return !waitlist.isEmpty();
  }
  
  public List<AbstractElementWithLocation> getWaitlist() {
    return Collections.unmodifiableList(waitlist);
  }
  
  public Pair<AbstractElementWithLocation,Precision> popFromWaitlist() {
    AbstractElementWithLocation result = null;

    switch(traversal) {
    case BFS:
      result = waitlist.get(0);
      break;
      
    case DFS:
      result = waitlist.get(waitlist.size()-1);
      break;

    case TOPSORT:
    default:
      for (AbstractElementWithLocation currentElement : waitlist) {
        if ((result == null) 
            || (currentElement.getLocationNode().getTopologicalSortId() >
                      result.getLocationNode().getTopologicalSortId())) {
          result = currentElement;
        }
      }
      break;
    }
    
    waitlist.remove(result);
    return getPrecisionAsPair.apply(result);
  }
  
  /**
   * Returns the precision for an element.
   * @param element The element to look for.
   * @return The precision for the element or null.
   */
  public Precision getPrecision(AbstractElement element) {
    return precisions.get(element);
  }
  
  public int size() {
    return reached.size();
  }
  
  @Override
  public String toString() {   
    return reached.toString();
  }
}
