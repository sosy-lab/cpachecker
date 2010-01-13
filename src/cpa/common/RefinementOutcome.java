package cpa.common;

import java.util.Collection;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;

public class RefinementOutcome {

  private final boolean refinementPerformed;
  private final Precision newPrecision;
  private final Path errorPath;
  private final Collection<? extends AbstractElementWithLocation> toUnreach;
  private final Collection<? extends AbstractElementWithLocation> toWaitlist;
  
  public RefinementOutcome(Path pErrorPath) {
    refinementPerformed = false;
    newPrecision = null;
    toUnreach = null;
    toWaitlist = null;
    errorPath = pErrorPath;
  }

  public RefinementOutcome(Precision pNewPrecision, Collection<? extends AbstractElementWithLocation> pToUnreach,
      Collection<? extends AbstractElementWithLocation> pToWaitlist, Path pErrorPath) {
    refinementPerformed = true;
    newPrecision = pNewPrecision;
    toUnreach = pToUnreach;
    toWaitlist = pToWaitlist;
    errorPath = pErrorPath;
  }

  public boolean refinementPerformed() {
    return refinementPerformed;
  }

  public Precision getNewPrecision() {
    return newPrecision;
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
