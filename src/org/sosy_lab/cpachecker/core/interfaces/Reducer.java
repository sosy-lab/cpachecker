package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import de.upb.agw.cpachecker.cpa.abm.util.Block;
import de.upb.agw.cpachecker.cpa.abm.util.BlockPartitioning;

public interface Reducer {

  AbstractElement getVariableReducedElement(AbstractElement expandedElement, Block context, CFANode callNode);
  
  AbstractElement getVariableExpandedElement(AbstractElement rootElement, Block rootContext, AbstractElement reducedElement);
  
  boolean isEqual(AbstractElement reducedTargetElement, AbstractElement candidateElement);

  AbstractElementHash getHashCodeForElement(AbstractElement elementKey, Precision precisionKey, Block context, BlockPartitioning partitioning);
}
