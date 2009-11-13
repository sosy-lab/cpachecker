package cpa.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFANode;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

public class ReachedElements {
  
  private final Set<Pair<AbstractElementWithLocation, Precision>> reached;
  private AbstractElementWithLocation lastElement = null;
  private AbstractElementWithLocation firstElement = null;
  private final List<Pair<AbstractElementWithLocation, Precision>> waitlist;
  private final TraversalMethod traversal;
  
  public ReachedElements(String traversal) {
    reached = createReachedSet();
    waitlist = new LinkedList<Pair<AbstractElementWithLocation, Precision>>();
    //set traversal method given in config file; 
    //throws IllegalArgumentException if anything other than dfs, bfs, or topsort is passed
      this.traversal = TraversalMethod.valueOf(traversal.toUpperCase());
  }
  
  //enumerator for traversal methods
  public enum TraversalMethod {
    DFS, BFS, TOPSORT
  }
  
  private Set<Pair<AbstractElementWithLocation,Precision>> createReachedSet() {
    if(CPAMain.cpaConfig.getBooleanValue("cpa.useSpecializedReachedSet")){
      return new LocationMappedReachedSet();
    }
    return new HashSet<Pair<AbstractElementWithLocation,Precision>>();
  }

  public void add(Pair<AbstractElementWithLocation, Precision> pair) {
    if (reached.size() == 0) {
      firstElement = pair.getFirst();
    }
    reached.add(pair);
    lastElement = pair.getFirst();
    waitlist.add(pair);
  }


  public boolean addAll(List<Pair<AbstractElementWithLocation, Precision>> toAdd) {
//    if (traversal == traversalMethod.BFS) {
    waitlist.addAll(toAdd);
//    } else {
//      waitlist.addAll(0, toAdd);
//    }
    return reached.addAll(toAdd);

  }
  
  private boolean remove(Pair<AbstractElementWithLocation, Precision> toRemove) {
    int hc = toRemove.hashCode();
    if (hc == firstElement.hashCode() && toRemove.equals(firstElement)) {
      firstElement = null;
    }
    if (hc == lastElement.hashCode() && toRemove.equals(lastElement)) {
      lastElement = null;
    }
    waitlist.remove(toRemove);
    return reached.remove(toRemove);
  }
  
  public void removeAll(Collection<Pair<AbstractElementWithLocation, Precision>> toRemove) {
    for (Pair<AbstractElementWithLocation, Precision> pair : toRemove) {
      remove(pair);
    }
  }
  
  public void clear() {
    firstElement = null;
    lastElement = null;
    waitlist.clear();
    reached.clear();
  }
  
  public Set<Pair<AbstractElementWithLocation, Precision>> getReached() {
    return reached;
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
  public Set<Pair<AbstractElementWithLocation,Precision>> getReached(CFANode loc) {
    Set<Pair<AbstractElementWithLocation,Precision>> result;
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
  
  public Pair<AbstractElementWithLocation,Precision> popFromWaitlist() {
    Pair<AbstractElementWithLocation,Precision> result = null;

    switch(traversal) {
    case TOPSORT: 
      for (Pair<AbstractElementWithLocation,Precision> currentElement : waitlist) {
        if ((result == null) 
            || (currentElement.getFirst().getLocationNode().getTopologicalSortId() >
                      result.getFirst().getLocationNode().getTopologicalSortId())) {
          result = currentElement;
        }
      }
      break;
    
    case BFS:
      result = waitlist.get(0);
      break;
      
    case DFS:
      result = waitlist.get(waitlist.size()-1);
      break;
    }
    
    waitlist.remove(result);
    return result;
  }
  
  public int size() {
    return reached.size();
  }

  public void printStates() {
    for(Pair<AbstractElementWithLocation,Precision> p:reached){
      AbstractElementWithLocation absEl = p.getFirst();
      System.out.println(absEl);
    }
  }
  
  @Override
  public String toString() {   
    return reached.toString();
  }
}
