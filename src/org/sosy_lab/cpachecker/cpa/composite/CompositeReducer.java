package org.sosy_lab.cpachecker.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class CompositeReducer implements Reducer {

  private final List<Reducer> wrappedReducers;
  
  public CompositeReducer(List<Reducer> pWrappedReducers) {
    wrappedReducers = pWrappedReducers;
  }

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext,
      CFANode pLocation) {
    
    List<AbstractElement> result = new ArrayList<AbstractElement>();
    int i = 0;
    for (AbstractElement expandedElement : ((CompositeElement)pExpandedElement).getWrappedElements()) {
      result.add(wrappedReducers.get(i++).getVariableReducedElement(expandedElement, pContext, pLocation));
    }
    return new CompositeElement(result);
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pRootContext,
      AbstractElement pReducedElement) {

    List<AbstractElement> rootElements = ((CompositeElement)pRootElement).getWrappedElements();
    List<AbstractElement> reducedElements = ((CompositeElement)pReducedElement).getWrappedElements();
    
    List<AbstractElement> result = new ArrayList<AbstractElement>();
    int i = 0;
    for (Pair<AbstractElement, AbstractElement> p : Pair.zipList(rootElements, reducedElements)) {
      result.add(wrappedReducers.get(i++).getVariableExpandedElement(p.getFirst(), pRootContext, p.getSecond()));
    }
    return new CompositeElement(result);
  }

  @Override
  public boolean isEqual(AbstractElement pReducedTargetElement,
      AbstractElement pCandidateElement) {
    
    List<AbstractElement> reducedTargetElements = ((CompositeElement)pReducedTargetElement).getWrappedElements();
    List<AbstractElement> candidateElements = ((CompositeElement)pCandidateElement).getWrappedElements();
    
    int i = 0;
    for (Pair<AbstractElement, AbstractElement> p : Pair.zipList(reducedTargetElements, candidateElements)) {
      if (!wrappedReducers.get(i++).isEqual(p.getFirst(), p.getSecond())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey, Precision pPrecisionKey) {
    
    List<AbstractElement> elements = ((CompositeElement)pElementKey).getWrappedElements();
    List<Precision> precisions = ((CompositePrecision)pPrecisionKey).getPrecisions();
    
    List<Object> result = new ArrayList<Object>(elements.size());
    int i = 0;
    for (Pair<AbstractElement, Precision> p : Pair.zipList(elements, precisions)) {
      result.add(wrappedReducers.get(i++).getHashCodeForElement(p.getFirst(), p.getSecond()));
    }
    return result;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    List<Precision> precisions = ((CompositePrecision)pPrecision).getPrecisions();
    List<Precision> result = new ArrayList<Precision>(precisions.size());
    
    int i = 0;
    for (Precision precision : precisions) {
      result.add(wrappedReducers.get(i++).getVariableReducedPrecision(precision, pContext));
    }
    
    return new CompositePrecision(result);    
  }
}
