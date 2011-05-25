package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;

public interface Reducer {

  AbstractElement getVariableReducedElement(AbstractElement expandedElement, CachedSubtree context, CFANode callNode);
  
  AbstractElement getVariableExpandedElement(AbstractElement rootElement, CachedSubtree rootContext, AbstractElement reducedElement);
  
  boolean isEqual(AbstractElement reducedTargetElement, AbstractElement candidateElement);

  AbstractElementHash getHashCodeForElement(AbstractElement elementKey, Precision precisionKey, CachedSubtree context, CachedSubtreeManager csmgr);
}
