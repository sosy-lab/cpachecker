package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
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
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement pElement,
      Precision pPrecision, UnmodifiableReachedSet pElements) {
    
    CompoundElement lElement = (CompoundElement)pElement;
    CompositePrecision lPrecision = (CompositePrecision)pPrecision;
    
    List<AbstractElement> lOutElements = new ArrayList<AbstractElement>();
    List<Precision> lOutPrecisions = new ArrayList<Precision>();

    boolean lModified = false;
    Action action = Action.CONTINUE;

    for (int i = 0; i < mDimension; ++i) {
      UnmodifiableReachedSet slice =
        new UnmodifiableReachedSetView(pElements, elementProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractElement oldElement = lElement.getSubelement(i);
      Precision oldPrecision = lPrecision.get(i);
      Triple<AbstractElement,Precision,Action> out = precisionAdjustment.prec(oldElement, oldPrecision, slice);
      
      AbstractElement newElement = out.getFirst();
      Precision newPrecision = out.getSecond();
      if (out.getThird() == Action.BREAK) {
        action = Action.BREAK;
      }
      
      if ((newElement != oldElement) || (newPrecision != oldPrecision)) {
        lModified = true;
      }
      
      lOutElements.add(newElement);
      lOutPrecisions.add(newPrecision);
    }

    if (lModified) {
      return new Triple<AbstractElement, Precision, Action>(new CompoundElement(lOutElements), new CompositePrecision(lOutPrecisions), action);
    } else {
      return new Triple<AbstractElement, Precision, Action>(pElement, pPrecision, action);
    }
  }

}
