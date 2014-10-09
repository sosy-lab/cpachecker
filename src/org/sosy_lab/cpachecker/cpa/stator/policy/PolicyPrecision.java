package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class PolicyPrecision implements Precision {
  private final UnmodifiableReachedSet reached;

  public PolicyPrecision(UnmodifiableReachedSet pReached) {
    reached = pReached;
  }

  Collection<AbstractState> getStatesForNode(CFANode node) {
    return reached.getReached(node);
  }

  UnmodifiableReachedSet getReached() {
    return reached;
  }
}
