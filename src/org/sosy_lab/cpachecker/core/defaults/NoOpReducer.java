package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

public class NoOpReducer implements Reducer {

  private static final NoOpReducer instance = new NoOpReducer();
  
  public static Reducer getInstance() {
    return instance;
  }
  
  @Override
  public AbstractElement getVariableReducedElement(AbstractElement pExpandedElement, Block pContext, CFANode pCallNode) {
    return pExpandedElement;
  }

  @Override
  public AbstractElement getVariableExpandedElement(AbstractElement pRootElement, Block pRootContext, AbstractElement pReducedElement) {
    return pReducedElement;
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement, AbstractElement pCandidateElement) {
    return pReducedTargetElement.equals(pCandidateElement);
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey, Precision pPrecisionKey, Block pContext, BlockPartitioning pPartitioning) {
    return pElementKey;
  }

}
