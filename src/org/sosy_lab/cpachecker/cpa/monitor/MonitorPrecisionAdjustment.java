package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.cpa.monitor.MonitorElement.TimeoutElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

/**
 * Precision Adjustment for Monitoring.
 * Simply delegates the operation to the wrapped CPA's precision adjustment operator
 * and updates the {@link MonitorElement} based on this computation.
 */
public class MonitorPrecisionAdjustment implements PrecisionAdjustment{

  private final PrecisionAdjustment wrappedPrecAdjustment;

  final Timer totalTimeOfPrecAdj = new Timer();

  public MonitorPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Triple<AbstractElement, Precision, Action> prec(
      AbstractElement pElement, Precision oldPrecision,
      UnmodifiableReachedSet pElements) throws CPAException {

    Preconditions.checkArgument(pElement instanceof MonitorElement);
    MonitorElement element = (MonitorElement)pElement;

    if (element.getWrappedElement() == TimeoutElement.INSTANCE) {
      // we can't call prec() in this case because we don't have an element of the CPA
      return Triple.of(pElement, oldPrecision, Action.CONTINUE);
    }

    UnmodifiableReachedSet elements = new UnmodifiableReachedSetView(
        pElements,  MonitorElement.getUnwrapFunction(), Functions.<Precision>identity());
    // TODO we really would have to filter out all TimeoutElements in this view

    AbstractElement oldElement = element.getWrappedElement();

    totalTimeOfPrecAdj.start();
    Triple<AbstractElement, Precision, Action> unwrappedResult = wrappedPrecAdjustment.prec(oldElement, oldPrecision, elements);
    long totalTimeOfExecution = totalTimeOfPrecAdj.stop();
    // add total execution time to the total time of the previous element
    long updatedTotalTime = totalTimeOfExecution + element.getTotalTimeOnPath();

    Pair<PreventingHeuristicType, Long> preventingCondition = element.getPreventingCondition();
    // TODO we should check for timeLimitForPath here
//    if (preventingCondition != null) {
//      if (timeLimitForPath > 0 && updatedTotalTime > timeLimitForPath) {
//        preventingCondition = Pair.of(PreventingHeuristicType.PATHCOMPTIME, timeLimitForPath);
//      }
//    }

    AbstractElement newElement = unwrappedResult.getFirst();
    Precision newPrecision = unwrappedResult.getSecond();
    Action action = unwrappedResult.getThird();

      // no. of nodes and no. of branches on the path does not change, just update the
      // set the adjusted wrapped element and update the time
    MonitorElement resultElement =
      new MonitorElement(newElement, updatedTotalTime, preventingCondition);

    return Triple.<AbstractElement, Precision, Action>of(resultElement, newPrecision, action);
  }
}
