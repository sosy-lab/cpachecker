package cpa.art;

import java.util.Collection;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;

public class ARTPrecisionAdjustment implements PrecisionAdjustment {

  @Override
  public <AE extends AbstractElement> Pair<AE, Precision> prec(AE pElement,
      Precision pPrecision, Collection<Pair<AE, Precision>> pElements) {
    return null;
  }

}
