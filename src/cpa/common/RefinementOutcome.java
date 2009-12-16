package cpa.common;

import java.util.Collection;

import cpa.common.interfaces.AbstractElementWithLocation;

public class RefinementOutcome {

  private final boolean refinementPerformed;
  private final Collection<? extends AbstractElementWithLocation> toUnreach;
  private final Collection<? extends AbstractElementWithLocation> toWaitlist;
  
  public RefinementOutcome() {
    refinementPerformed = false;
    toUnreach = null;
    toWaitlist = null;
  }

  public RefinementOutcome(boolean pB, Collection<? extends AbstractElementWithLocation> pToUnreach,
      Collection<? extends AbstractElementWithLocation> pToWaitlist) {
    refinementPerformed = pB;
    toUnreach = pToUnreach;
    toWaitlist = pToWaitlist;
  }

  public boolean refinementPerformed() {
    return refinementPerformed;
  }

  public Collection<? extends AbstractElementWithLocation> getToUnreach() {
    return toUnreach;
  }

  public Collection<? extends AbstractElementWithLocation> getToWaitlist() {
    return toWaitlist;
  }
}
