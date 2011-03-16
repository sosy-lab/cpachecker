package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;

public class AlternationElement extends GuardedEdgeAutomatonStateElement
    implements Targetable {
  
  private final GuardedEdgeAutomatonStateElement mWrappedElement;
  
  public AlternationElement(GuardedEdgeAutomatonStateElement pWrappedElement) {
    super(pWrappedElement.getAutomatonState(), pWrappedElement.isFinalState());
    mWrappedElement = pWrappedElement;
  }
  
  public GuardedEdgeAutomatonStateElement getWrappedElement() {
    return mWrappedElement;
  }
  
  @Override
  public Object getPartitionKey() {
    return this;
  }

  @Override
  public boolean isTarget() {
    return true;
  }

}
