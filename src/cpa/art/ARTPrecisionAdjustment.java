package cpa.art;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import common.Pair;

import cpa.common.UnmodifiableReachedElements;
import cpa.common.UnmodifiableReachedElementsView;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;

public class ARTPrecisionAdjustment implements PrecisionAdjustment {
  
  private final PrecisionAdjustment wrappedPrecAdjustment;
  
  public ARTPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }
  
  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement pElement,
      Precision pPrecision, UnmodifiableReachedElements pElements) {
    
    Preconditions.checkArgument(pElement instanceof ARTElement);
    ARTElement element = (ARTElement)pElement;
    
    UnmodifiableReachedElements elements = new UnmodifiableReachedElementsView(
        pElements,  ARTElement.getUnwrapFunction(), Functions.<Precision>identity());
    
    Pair<AbstractElement, Precision> unwrappedResult = wrappedPrecAdjustment.prec(element.getWrappedElement(), pPrecision, elements);
    
    ARTElement resultElement;
    if (element.getWrappedElement().equals(unwrappedResult.getFirst())) {
      resultElement = element;
    } else {
      resultElement = new ARTElement(unwrappedResult.getFirst(), null);
      
      for (ARTElement parent : element.getParents()) {
        resultElement.addParent(parent);
      }
      for (ARTElement child : element.getChildren()) {
        resultElement.addParent(child);
      }
      
      // first copy list of covered elements, then remove element from ART, then set elements covered by new element
      ImmutableList<ARTElement> coveredElements = ImmutableList.copyOf(element.getCoveredByThis());
      element.removeFromART();
      
      for (ARTElement covered : coveredElements) {
        covered.setCovered(resultElement);
      }
    }
    
    return new Pair<AbstractElement, Precision>(resultElement, unwrappedResult.getSecond());
  }
}