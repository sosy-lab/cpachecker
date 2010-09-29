package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public interface SymbPredAbsAbstractElement extends AbstractElement {

  public int getSizeSinceAbstraction();
  public AbstractionElement getAbstractionElement();
  
}
