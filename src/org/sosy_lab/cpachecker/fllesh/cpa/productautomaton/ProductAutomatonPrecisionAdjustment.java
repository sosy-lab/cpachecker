package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
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
  public Pair<AbstractElement, Precision> prec(AbstractElement pElement,
      Precision pPrecision, UnmodifiableReachedSet pElements) {
    
    if (pElement instanceof ProductAutomatonAcceptingElement) {
      return new Pair<AbstractElement, Precision>(pElement, SingletonPrecision.getBreakInstance());
    }
    else {
      return new Pair<AbstractElement, Precision>(pElement, pPrecision);
    }
  }

}
