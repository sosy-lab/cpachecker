package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementHash;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

import de.upb.agw.cpachecker.cpa.abm.util.Block;
import de.upb.agw.cpachecker.cpa.abm.util.BlockPartitioning;

public class AutomatonReducer implements Reducer {

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext,
      CFANode pLocation) {
    return pExpandedElement;
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pRootContext,
      AbstractElement pReducedElement) {
    return pReducedElement;
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement,
      AbstractElement pCandidateElement) {
    return pReducedTargetElement.equals(pCandidateElement);
  }

  @Override
  public AbstractElementHash getHashCodeForElement(AbstractElement pElementKey,
      Precision pPrecisionKey, Block pContext,
      BlockPartitioning pPartitioning) {
    // TODO Auto-generated method stub
    return null;
  }

}
