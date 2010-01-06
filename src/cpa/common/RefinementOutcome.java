package cpa.common;

import java.util.Collection;

import cpa.common.interfaces.AbstractElementWithLocation;

public class RefinementOutcome {

  private final boolean refinementPerformed;
  private final Path errorPath;
  private final Collection<? extends AbstractElementWithLocation> toUnreach;
  private final Collection<? extends AbstractElementWithLocation> toWaitlist;
  
  public RefinementOutcome(Path pErrorPath) {
    refinementPerformed = false;
    toUnreach = null;
    toWaitlist = null;
    errorPath = pErrorPath;
  }

  public RefinementOutcome(boolean pB, Collection<? extends AbstractElementWithLocation> pToUnreach,
      Collection<? extends AbstractElementWithLocation> pToWaitlist, Path pErrorPath) {
    refinementPerformed = pB;
    toUnreach = pToUnreach;
    toWaitlist = pToWaitlist;
    errorPath = pErrorPath;
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
  
  public Path getErrorPath() {
    return errorPath;
  }
}
