package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

public class TransferRelationMonitorPrecisionAdjustment implements PrecisionAdjustment{

  private final PrecisionAdjustment wrappedPrecAdjustment;

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
    
    Triple<AbstractElement, Precision, Action> unwrappedResult = wrappedPrecAdjustment.prec(oldElement, oldPrecision, elements);

    AbstractElement newElement = unwrappedResult.getFirst();
    Precision newPrecision = unwrappedResult.getSecond();
    Action action = unwrappedResult.getThird();

    if ((oldElement == newElement) && (oldPrecision == newPrecision)) {
      // nothing has changed
      return new Triple<AbstractElement, Precision, Action>(pElement, oldPrecision, action);
    }
      
    TransferRelationMonitorElement resultElement = 
      new TransferRelationMonitorElement(newElement, element.getNoOfNodesOnPath(), element.getNoOfBranchesOnPath(), element.getTotalTimeOnPath());

    return new Triple<AbstractElement, Precision, Action>(resultElement, newPrecision, action);
    
  }

  
  
}
