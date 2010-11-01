package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

/**
 * Precision Adjustment for Monitoring.
 * Simply delegates the operation to the wrapped CPA's precision adjustment operator
 * and updates the {@link TransferRelationMonitorElement} based on this computation.
 */
public class TransferRelationMonitorPrecisionAdjustment implements PrecisionAdjustment{

  private final PrecisionAdjustment wrappedPrecAdjustment;

  final Timer totalTimeOfPrecAdj = new Timer();
  
  public TransferRelationMonitorPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }
  
  @Override
  public Triple<AbstractElement, Precision, Action> prec(
      AbstractElement pElement, Precision oldPrecision,
      UnmodifiableReachedSet pElements) {
    
    Preconditions.checkArgument(pElement instanceof TransferRelationMonitorElement);
    TransferRelationMonitorElement element = (TransferRelationMonitorElement)pElement;

    UnmodifiableReachedSet elements = new UnmodifiableReachedSetView(
        pElements,  TransferRelationMonitorElement.getUnwrapFunction(), Functions.<Precision>identity());

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
    TransferRelationMonitorElement resultElement = 
      new TransferRelationMonitorElement(newElement, element.getNoOfNodesOnPath(), element.getNoOfBranchesOnPath(), updatedTotalTime);

    return new Triple<AbstractElement, Precision, Action>(resultElement, newPrecision, action);
  }
}
