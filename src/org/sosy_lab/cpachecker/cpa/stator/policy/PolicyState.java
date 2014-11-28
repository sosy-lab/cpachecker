package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Abstract state for policy iteration: bounds on each expression (from the
 * template), for the given control node.
 *
 * Logic-less container class.
 */
public final class PolicyState implements AbstractState,
    Iterable<Entry<LinearExpression, PolicyBound>>,
    Graphable {

  private final ImmutableList<AbstractState> otherStates;

  /** All we care about: they are not divided per template. */
  private final ImmutableSet<CFAEdge> incomingEdges;

  private final CFANode node;

  /**
   * Finite bounds for templates.
   */
  private final Optional<ImmutableMap<LinearExpression, PolicyBound>>
      abstraction;

  /** Templates tracked. NOTE: might be better to just resort back to a set. */
  private final ImmutableSet<Template> templates;

  /**
   * Constructor with the abstraction provided.
   */
  private PolicyState(
      Iterable<AbstractState> pOtherStates,
      Set<CFAEdge> pIncomingEdges,
      CFANode pNode,
      Map<LinearExpression, PolicyBound> pPAbstraction,
      Set<Template> pTemplates) {
    otherStates = ImmutableList.copyOf(pOtherStates);
    incomingEdges = ImmutableSet.copyOf(pIncomingEdges);
    node = pNode;
    abstraction = Optional.of(ImmutableMap.copyOf(pPAbstraction));
    templates = ImmutableSet.copyOf(pTemplates);
  }

  /**
   * Constructor without the abstraction.
   */
  private PolicyState(
      Iterable<AbstractState> pOtherStates,
      Set<CFAEdge> pIncomingEdges,
      CFANode pNode,
      Set<Template> pTemplates) {
    otherStates = ImmutableList.copyOf(pOtherStates);
    incomingEdges = ImmutableSet.copyOf(pIncomingEdges);
    node = pNode;
    abstraction = Optional.absent();
    templates = ImmutableSet.copyOf(pTemplates);
  }

  /**
   * Factory methods.
   */
  public static PolicyState ofAbstraction(
      Map<LinearExpression, PolicyBound> data,
      Set<Template> templates,
      CFANode node,
      Set<CFAEdge> pIncomingEdges,
      Iterable<AbstractState> pOtherStates
  ) {
    return new PolicyState(
        pOtherStates, pIncomingEdges, node, data, templates);
  }

  public static PolicyState ofIntermediate(
      Iterable<AbstractState> pOtherStates,
      Set<CFAEdge> pIncomingEdges,
      CFANode node,
      Set<Template> pTemplates
  ) {
    return new PolicyState(
        pOtherStates,
        pIncomingEdges,
        node,
        pTemplates
    );
  }

  public static PolicyState empty(CFANode node) {
    return new PolicyState(
        ImmutableList.<AbstractState>of(), // other states
        ImmutableSet.<CFAEdge>of(), // incoming edges
        node, // node
        ImmutableMap.<LinearExpression, PolicyBound>of(),
        ImmutableSet.<Template>of() // templates
    );
  }

  /** Getters */

  public ImmutableSet<CFAEdge> getIncomingEdges() {
    return incomingEdges;
  }

  public ImmutableList<AbstractState> getOtherStates() {
    return otherStates;
  }

  public boolean isAbstract() {
    return abstraction.isPresent();
  }

  /**
   * @return {@link PolicyBound} for the given {@link LinearExpression}
   * <code>e</code> or an empty optional if it is unbounded.
   */
  public Optional<PolicyBound> getBound(
      LinearExpression e) {
    Preconditions.checkState(abstraction.isPresent(),
        "Template bounds can be obtained only from abstracted states.");
    return Optional.fromNullable(abstraction.get().get(e));
  }

  public ImmutableSet<Template> getTemplates() {
    return templates;
  }

  public CFANode getNode() {
    return node;
  }

  /** Update methods */

  public PolicyState withUpdates(
      Map<LinearExpression, PolicyBound> updates,
      Set<LinearExpression> unbounded,
      Set<Template> newTemplates) {
    Preconditions.checkState(abstraction.isPresent(),
        "Updates can only be applied to the abstracted state");

    ImmutableMap<LinearExpression, PolicyBound> map = abstraction.get();

    ImmutableMap.Builder<LinearExpression, PolicyBound> builder =
        ImmutableMap.builder();

    for (Template template : newTemplates) {
      LinearExpression expr = template.linearExpression;
      if (unbounded.contains(expr)) {
        continue;
      }
      if (updates.containsKey(expr)) {
        builder.put(expr, updates.get(expr));
      } else {
        PolicyBound v = map.get(expr);
        if (v != null) {
          builder.put(expr, map.get(expr));
        }
      }
    }
    return new PolicyState(
        otherStates, incomingEdges, getNode(), builder.build(),
        newTemplates);
  }

  @Override
  public String toDOTLabel() {
    if (abstraction.isPresent()) {
      return (new PolicyDotWriter()).toDOTLabel(abstraction.get());
    } else {
      // NOTE: not much else to visualize for non-abstracted states.
      return "";
    }
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s: %s", node, abstraction);
  }

  @Override
  public Iterator<Entry<LinearExpression, PolicyBound>> iterator() {
    Preconditions.checkState(
        abstraction.isPresent(),
        "Can only iterate through the resolved state"
    );
    return abstraction.get().entrySet().iterator();
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
    return (abstraction.equals(other.abstraction) && node.equals(other.node));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(node, abstraction);
  }
}
