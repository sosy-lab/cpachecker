package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class ProductAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private static ProductAutomatonPrecisionAdjustment sInstance = new ProductAutomatonPrecisionAdjustment();
  
  public static ProductAutomatonPrecisionAdjustment getInstance() {
    return sInstance;
  }
  
  private ProductAutomatonPrecisionAdjustment() {
    
  }
  
  @Override
  public Triple<AbstractElement, Precision, Action> prec(
      AbstractElement pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) {

    ProductAutomatonElement lElement = (ProductAutomatonElement)pElement;
    
    if (lElement.isFinalState()) {
      return new Triple<AbstractElement, Precision, Action>(pElement, pPrecision, Action.BREAK);
    }
    else {
      return new Triple<AbstractElement, Precision, Action>(pElement, pPrecision, Action.CONTINUE);
    }
  }

}
