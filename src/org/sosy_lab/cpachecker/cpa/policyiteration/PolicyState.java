package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

/**
 * Abstract state for policy iteration: bounds on each expression (from the
 * template), for the given control node.
 *
 * Logic-less container class.
 */
public abstract class PolicyState implements AbstractState, Graphable {


  /** Templates tracked. */
  protected final ImmutableSet<Template> templates;

  protected PolicyState(Set<Template> pTemplates) {
    templates = ImmutableSet.copyOf(pTemplates);
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

  public abstract boolean isAbstract();

  public ImmutableSet<Template> getTemplates() {
    return templates;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicyState other = (PolicyState)o;
    return (templates.equals(other.templates));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(templates);
  }
}
