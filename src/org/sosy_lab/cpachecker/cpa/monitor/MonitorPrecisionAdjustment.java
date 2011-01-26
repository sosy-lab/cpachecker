package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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

    UnmodifiableReachedSet elements = new UnmodifiableReachedSetView(
        pElements,  MonitorElement.getUnwrapFunction(), Functions.<Precision>identity());

    AbstractElement oldElement = element.getWrappedElement();
    
    totalTimeOfPrecAdj.start();
    Triple<AbstractElement, Precision, Action> unwrappedResult = wrappedPrecAdjustment.prec(oldElement, oldPrecision, elements);
    long totalTimeOfExecution = totalTimeOfPrecAdj.stop();
    // add total execution time to the total time of the previous element
    long updatedTotalTime = totalTimeOfExecution + element.getTotalTimeOnPath();
    
    AbstractElement newElement = unwrappedResult.getFirst();
    Precision newPrecision = unwrappedResult.getSecond();
    Action action = unwrappedResult.getThird();

    if ((oldElement == newElement) && (oldPrecision == newPrecision)) {
      // nothing has changed
      return new Triple<AbstractElement, Precision, Action>(pElement, oldPrecision, action);
    }
      // no. of nodes and no. of branches on the path does not change, just update the
      // set the adjusted wrapped element and update the time
    MonitorElement resultElement = 
      new MonitorElement(newElement, element.getNoOfNodesOnPath(), element.getNoOfBranchesOnPath(), updatedTotalTime);

    return new Triple<AbstractElement, Precision, Action>(resultElement, newPrecision, action);
  }
}
