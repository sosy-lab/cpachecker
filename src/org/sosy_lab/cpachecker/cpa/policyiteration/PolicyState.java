package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import com.google.common.collect.ImmutableSet;

/**
 * Abstract state for policy iteration: bounds on each expression (from the
 * template), for the given control node.
 *
 * Logic-less container class.
 */
public abstract class PolicyState implements AbstractState, Graphable {


  /** Templates tracked. */
  private final ImmutableSet<Template> templates;
  private final CFANode node;

  protected PolicyState(
      Set<Template> pTemplates,
      CFANode pNode) {
    templates = ImmutableSet.copyOf(pTemplates);
    node = pNode;
  }

  /**
   * Cast to subclass.
   * Syntax sugar to avoid ugliness.
   */
  public PolicyIntermediateState asIntermediate() {
    return (PolicyIntermediateState) this;
  }

  public PolicyAbstractedState asAbstracted() {
    return (PolicyAbstractedState) this;
  }

  public CFANode getNode() {
    return node;
  }

  public abstract boolean isAbstract();

  public ImmutableSet<Template> getTemplates() {
    return templates;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
