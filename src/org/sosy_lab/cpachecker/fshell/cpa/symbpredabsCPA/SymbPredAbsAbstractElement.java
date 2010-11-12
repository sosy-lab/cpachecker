package org.sosy_lab.cpachecker.fshell.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public interface SymbPredAbsAbstractElement extends AbstractElement {

  public int getSizeSinceAbstraction();
  public AbstractionElement getAbstractionElement();
  
}
