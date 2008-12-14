/**
 * 
 */
package compositeCPA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import common.Pair;

import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.ReachedSet;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CompositePrecisionAdjustment implements PrecisionAdjustment {

  private List<PrecisionAdjustment> precisionAdjustments;
  
  public CompositePrecisionAdjustment (List<PrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.PrecisionAdjustment#prec(cpa.common.interfaces.AbstractElement, cpa.common.interfaces.Precision, java.util.Collection)
   */
  public <AE extends AbstractElement> Pair<AE, Precision> prec(
                                                               AE pElement,
                                                               Precision pPrecision,
                                                               ReachedSet pReached) {
    CompositeElement comp = (CompositeElement) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getElements().size() == prec.getPrecisions().size());
    int dim = comp.getElements().size();
    
    List<AbstractElement> outElements = new ArrayList<AbstractElement>();
    List<Precision> outPrecisions = new ArrayList<Precision>();
    
    for (int i = 0; i < dim; ++i) {
      HashSet<Pair<AbstractElement,Precision>> slice = new HashSet<Pair<AbstractElement,Precision>>();
      for (Pair<AbstractElementWithLocation,Precision> entry : pReached) {
        slice.add(new Pair<AbstractElement,Precision>(((CompositeElement)entry.getFirst()).get(i),
            ((CompositePrecision)entry.getSecond()).get(i)));
      }
      Pair<AbstractElement,Precision> out = precisionAdjustments.get(i).prec(comp.get(i), prec.get(i), slice);
      outElements.add(out.getFirst());
      outPrecisions.add(out.getSecond());
    }
    
    // TODO for now we just take the input call stack, that may be wrong, but how to construct 
    // a proper one in case this _is_ wrong?
    return new Pair<AE,Precision>((AE) new CompositeElement(outElements, comp.getCallStack()),
        new CompositePrecision(outPrecisions));
  }

}
