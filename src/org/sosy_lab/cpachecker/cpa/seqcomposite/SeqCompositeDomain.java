package org.sosy_lab.cpachecker.cpa.seqcomposite;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SeqCompositeDomain implements AbstractDomain {

  @Override
  public boolean isLessOrEqual(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    // TODO Auto-generated method stub
    return null;
  }

}
