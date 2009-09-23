package cpa.common;

import java.util.Collection;

import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElementWithLocation;

public class RefinementOutcome {

  private boolean refinementPerformed;
  private Collection<ARTElement> toUnreach;
  private Collection<ARTElement> toWaitlist;
  private ARTElement root;
  
  public RefinementOutcome(){
    refinementPerformed = false;
    toUnreach = null;
    toWaitlist = null;
    root = null;
  }

  public RefinementOutcome(boolean pB, Collection<ARTElement> pToUnreach,
      Collection<ARTElement> pToWaitlist, ARTElement pRoot) {
    refinementPerformed = pB;
    toUnreach = pToUnreach;
    toWaitlist = pToWaitlist;
    root = pRoot;
  }

  public boolean refinementPerformed() {
    return refinementPerformed;
  }

  public void setRefinementPerformed(boolean pStopAnalysis) {
    refinementPerformed = pStopAnalysis;
  }

  public Collection<ARTElement> getToUnreach() {
    return toUnreach;
  }

  public void setToUnreach(Collection<ARTElement> pToUnreach) {
    toUnreach = pToUnreach;
  }

  public Collection<ARTElement> getToWaitlist() {
    return toWaitlist;
  }

  public void setToWaitlist(Collection<ARTElement> pToWaitlist) {
    toWaitlist = pToWaitlist;
  }

  public AbstractElementWithLocation getRoot() {
    return root;
  }
  
}
