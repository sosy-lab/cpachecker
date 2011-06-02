package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ProgressPrecisionAdjustment implements PrecisionAdjustment {

  /*
   * (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet)
   *
   * This method does not depend on pElements.
   */
  @Override
  public Triple<AbstractElement, Precision, Action> prec(
      AbstractElement pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) throws CPAException {
    ProgressElement lElement = (ProgressElement)pElement;
    ProgressPrecision lPrecision = (ProgressPrecision)pPrecision;

    if (lPrecision.isProgress(lElement.getTransition())) {
      GuardedEdgeAutomatonElement lWrappedElement = lElement.getWrappedElement();

      if (!(lWrappedElement instanceof GuardedEdgeAutomatonStateElement)) {
        throw new RuntimeException();
      }

      GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lWrappedElement;

      Precision lAdjustedPrecision = lPrecision.remove(lElement.getTransition());

      return new Triple<AbstractElement, Precision, Action>(new AlternationElement(lStateElement), lAdjustedPrecision, Action.BREAK);
    }
    else {
      return new Triple<AbstractElement, Precision, Action>(lElement.getWrappedElement(), pPrecision, Action.CONTINUE);
    }
  }

}
