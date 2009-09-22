package cpa.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import cmdline.CPAMain;

import common.LocationMappedReachedSet;
import common.Pair;

import cpa.art.ARTDomain;
import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;

public class ReachedElements {
  
//  private Collection<AbstractElementWithLocation> reachedSet;
  private Collection<Pair<AbstractElementWithLocation,Precision>> reached;
  private AbstractElementWithLocation lastElement;
  private AbstractElementWithLocation firstElement;
  ConfigurableProgramAnalysis cpa;
  
  public ReachedElements(
      ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;
    reached = createReachedSet(cpa);
//    // TODO Remove this hack
//    if (reached instanceof LocationMappedReachedSet) {
//      reachedSet = new LocationMappedReachedSetProjectionWrapper((LocationMappedReachedSet)reached);
//    }
//    else {
//      reachedSet = new ProjectionWrapper(reached);
//    }
    
  }
  
  private Collection<Pair<AbstractElementWithLocation,Precision>> createReachedSet(
      ConfigurableProgramAnalysis cpa) {
    if(CPAMain.cpaConfig.getBooleanValue("cpa.useSpecializedReachedSet")){
      return new LocationMappedReachedSet();
    }
    return new HashSet<Pair<AbstractElementWithLocation,Precision>>();
  }

  public void add(Pair<AbstractElementWithLocation, Precision> pPair) {
    if(reached.size() == 0){
      firstElement = pPair.getFirst();
    }
    reached.add(pPair);
    lastElement = pPair.getFirst();
  }

  public Collection<Pair<AbstractElementWithLocation, Precision>> getReached() {
    return reached;
  }

  public AbstractElementWithLocation getLastElement() {
    return lastElement;
  }

  public Collection<AbstractElementWithLocation> getReachedWithElements(){
    ArrayList<AbstractElementWithLocation> simpleReached = 
      new ArrayList<AbstractElementWithLocation>();
    
    for(Pair<AbstractElementWithLocation,Precision> p:reached){
      AbstractElementWithLocation absEl = p.getFirst();
      simpleReached.add(absEl);
    }
    
    return simpleReached;
    
  }
  
  public Object size() {
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
  
  public void setLastElementToFalse(){
    AbstractDomain domain = this.cpa.getAbstractDomain();
    lastElement = (AbstractElementWithLocation)domain.getBottomElement(); 
  }

  public void buildNewReachedSet(
      Collection<Pair<AbstractElementWithLocation, Precision>> pNewreached) {
    reached.clear();
    reached.addAll(pNewreached);
    lastElement = null;
  }

  public AbstractElementWithLocation getFirstElement() {
    return firstElement;
  }
}
