package cpa.common;

import java.util.Collection;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;

public class RefinementOutcome {

  private final boolean refinementPerformed;
  private final Precision newPrecision;
  private final Path errorPath;
  private final Collection<? extends AbstractElement> toUnreach;
  private final Collection<? extends AbstractElement> toWaitlist;
  
  public RefinementOutcome(Path pErrorPath) {
    refinementPerformed = false;
    newPrecision = null;
    toUnreach = null;
    toWaitlist = null;
    errorPath = pErrorPath;
  }

  public RefinementOutcome(Precision pNewPrecision, Collection<? extends AbstractElement> pToUnreach,
      Collection<? extends AbstractElement> pToWaitlist, Path pErrorPath) {
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
  
  public Collection<? extends AbstractElement> getToUnreach() {
    return toUnreach;
  }

  public Collection<? extends AbstractElement> getToWaitlist() {
    return toWaitlist;
  }
  
  public Path getErrorPath() {
    return errorPath;
  }
}
