package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public abstract class SlicingState implements AbstractState {

  /**
   * Cast to subclass. Syntax sugar.
   */
  public SlicingIntermediateState asIntermediate() {
    return (SlicingIntermediateState) this;
  }

  public SlicingAbstractedState asAbstracted() {
    return (SlicingAbstractedState) this;
  }

  public abstract boolean isAbstracted();
}
