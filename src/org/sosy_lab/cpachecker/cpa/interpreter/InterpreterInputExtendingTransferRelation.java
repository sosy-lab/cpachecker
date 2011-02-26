package org.sosy_lab.cpachecker.cpa.interpreter;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.MissingInputException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class InterpreterInputExtendingTransferRelation extends
    InterpreterTransferRelation {

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    Collection<? extends AbstractElement> lSuccessors = null;
    
    boolean lRedo = false;
    
    do {
      try {
        lSuccessors = super.getAbstractSuccessors(pElement, pPrecision, pCfaEdge);
        
        lRedo = false;
      }
      catch (MissingInputException e) {
        // extend input
        InterpreterElement lElement = (InterpreterElement)pElement;
        
        int[] lInputs = new int[lElement.getInputs().length + 1];
        
        for (int lIndex = 0; lIndex < lInputs.length - 1; lIndex++) {
          lInputs[lIndex] = lElement.getInputs()[lIndex];
        }
        
        long lMaxInt = Integer.MAX_VALUE;
        long lMinInt = Integer.MIN_VALUE;
        
        long lRange = lMaxInt - lMinInt;
        
        long lRandomValue = (long)(Math.random() * lRange) + lMinInt;
        
        lInputs[lInputs.length - 1] = (int)lRandomValue;
        
        System.out.println("Extending input with " + lInputs[lInputs.length - 1]);
        
        InterpreterElement lTmpElement = new InterpreterElement(lElement.getConstantsMap(), lElement.getPreviousElement(), lElement.getInputIndex(), lInputs);
        
        pElement = lTmpElement;
        
        // TODO make configurable
        int lThreshold = 20;
        if (lInputs.length > lThreshold) {
          throw e;
        }
        
        lRedo = true;
      }
    }
    while (lRedo);
    
    return lSuccessors;
  }

}
