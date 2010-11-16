package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.composite;

import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeStopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class ProductAutomatonStopOperator extends CompositeStopOperator {

  public ProductAutomatonStopOperator(ImmutableList<StopOperator> pStopOperators) {
    super(pStopOperators);
  }
  
  @Override
  public boolean stop(AbstractElement element, AbstractElement reachedElement)
  throws CPAException {
    ProductAutomatonElement compositeElement1 = (ProductAutomatonElement)element;
    ProductAutomatonElement compositeElement2 = (ProductAutomatonElement)reachedElement;

    List<AbstractElement> compositeElements1 = compositeElement1.getElements();
    List<AbstractElement> compositeElements2 = compositeElement2.getElements();

    for (int idx = 0; idx < compositeElements1.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);
      
      AbstractElement absElem1 = compositeElements1.get(idx);
      AbstractElement absElem2 = compositeElements2.get(idx);
      
      if (!stopOp.stop(absElem1, absElem2)){
        return false;
      }
    }
    
    return true;
  }

}
