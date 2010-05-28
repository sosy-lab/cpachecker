package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.UnmodifiableReachedElements;
import org.sosy_lab.cpachecker.core.UnmodifiableReachedElementsView;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

public class CompoundPrecisionAdjustment implements PrecisionAdjustment {

  private final List<PrecisionAdjustment> precisionAdjustments;
  private final ImmutableList<ElementProjectionFunction> elementProjectionFunctions;
  private final ImmutableList<PrecisionProjectionFunction> precisionProjectionFunctions;
  private int mDimension;

  public CompoundPrecisionAdjustment(List<PrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;
    mDimension = precisionAdjustments.size();

    ImmutableList.Builder<ElementProjectionFunction> elementProjectionFunctions = ImmutableList.builder();
    ImmutableList.Builder<PrecisionProjectionFunction> precisionProjectionFunctions = ImmutableList.builder();

    for (int i = 0; i < precisionAdjustments.size(); i++) {
      elementProjectionFunctions.add(new ElementProjectionFunction(i));
      precisionProjectionFunctions.add(new PrecisionProjectionFunction(i));
    }
    this.elementProjectionFunctions = elementProjectionFunctions.build();
    this.precisionProjectionFunctions = precisionProjectionFunctions.build();
  }
  
  private static class ElementProjectionFunction
    implements Function<AbstractElement, AbstractElement>
  {
    private final int dimension;
  
    public ElementProjectionFunction(int d) {
      dimension = d;
    }
  
    @Override
    public AbstractElement apply(AbstractElement from) {
      return ((CompositeElement)from).get(dimension);
    }
  }

  private static class PrecisionProjectionFunction
    implements Function<Precision, Precision>
  {
    private final int dimension;
  
    public PrecisionProjectionFunction(int d) {
      dimension = d;
    }
  
    @Override
    public Precision apply(Precision from) {
      return ((CompositePrecision)from).get(dimension);
    }
  }
  
  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement pElement,
      Precision pPrecision, UnmodifiableReachedElements pElements) {
    
    CompoundElement lElement = (CompoundElement)pElement;
    CompositePrecision lPrecision = (CompositePrecision)pPrecision;
    
    List<AbstractElement> lOutElements = new ArrayList<AbstractElement>();
    List<Precision> lOutPrecisions = new ArrayList<Precision>();

    boolean lModified = false;

    for (int i = 0; i < mDimension; ++i) {
      UnmodifiableReachedElements slice =
        new UnmodifiableReachedElementsView(pElements, elementProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      Pair<AbstractElement,Precision> out = precisionAdjustment.prec(lElement.getSubelement(i), lPrecision.get(i), slice);
      if (out != null) {
        lOutElements.add(out.getFirst());
        lOutPrecisions.add(out.getSecond());
        lModified = true;
      } else {
        lOutElements.add(lElement.getSubelement(i));
        lOutPrecisions.add(lPrecision.get(i));
      }
    }

    if (lModified) {
      return new Pair<AbstractElement, Precision>(new CompoundElement(lOutElements), new CompositePrecision(lOutPrecisions));
    } else {
      return null;
    }
  }

}
