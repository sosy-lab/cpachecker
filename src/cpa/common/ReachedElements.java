package cpa.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import cmdline.CPAMain;

import common.LocationMappedReachedSet;
import common.Pair;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

public class ReachedElements {
  
  private Collection<Pair<AbstractElementWithLocation, Precision>> reached;
  private AbstractElementWithLocation lastElement = null;
  private AbstractElementWithLocation firstElement = null;
  private List<Pair<AbstractElementWithLocation, Precision>> waitlist;
  
  public ReachedElements() {
    reached = createReachedSet();
    waitlist = new LinkedList<Pair<AbstractElementWithLocation, Precision>>();
  }
  
  private Collection<Pair<AbstractElementWithLocation,Precision>> createReachedSet() {
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
//    if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
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
  
  public Collection<Pair<AbstractElementWithLocation, Precision>> getReached() {
    return reached;
  }

  public AbstractElementWithLocation getFirstElement() {
    return firstElement;
  }

  public AbstractElementWithLocation getLastElement() {
    return lastElement;
  }
  
  public boolean hasWaitingElement() {
    return !waitlist.isEmpty();
  }
  
  public Pair<AbstractElementWithLocation,Precision> popFromWaitlist() {
    Pair<AbstractElementWithLocation,Precision> result = null;

    if(CPAMain.cpaConfig.getBooleanValue("analysis.topSort")) {
      for (Pair<AbstractElementWithLocation,Precision> currentElement : waitlist) {
        if ((result == null) 
            || (currentElement.getFirst().getLocationNode().getTopologicalSortId() >
                      result.getFirst().getLocationNode().getTopologicalSortId())) {
          result = currentElement;
        }
      }
    
    } else if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
      result = waitlist.get(0);
      
    } else {
      result = waitlist.get(waitlist.size()-1);
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
