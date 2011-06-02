package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.IGuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton.State;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class ProgressElement implements Targetable, AbstractElement, IGuardedEdgeAutomatonStateElement {

  private final GuardedEdgeAutomatonStateElement mAutomatonElement;
  private final NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge mTransition;

  public ProgressElement(GuardedEdgeAutomatonStateElement pAutomatonElement, NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge pTransition) {
    mAutomatonElement = pAutomatonElement;
    mTransition = pTransition;
  }

  public GuardedEdgeAutomatonElement getWrappedElement() {
    return mAutomatonElement;
  }

  public NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge getTransition() {
    return mTransition;
  }

  @Override
  public boolean isFinalState() {
    return mAutomatonElement.isFinalState();
  }

  @Override
  public State getAutomatonState() {
    return mAutomatonElement.getAutomatonState();
  }

  @Override
  public String toString() {
    return "ProgressElement[" + mAutomatonElement.toString() + "]";
  }

  @Override
  public boolean isTarget() {
    return isFinalState();
  }

}
