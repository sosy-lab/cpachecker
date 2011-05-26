package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;


public interface Reducer {

  AbstractElement getVariableReducedElement(AbstractElement expandedElement, Block context, CFANode callNode);
  
  AbstractElement getVariableExpandedElement(AbstractElement rootElement, Block rootContext, AbstractElement reducedElement);
  
  boolean isEqual(AbstractElement reducedTargetElement, AbstractElement candidateElement);

  AbstractElementHash getHashCodeForElement(AbstractElement elementKey, Precision precisionKey, Block context, BlockPartitioning partitioning);
}
