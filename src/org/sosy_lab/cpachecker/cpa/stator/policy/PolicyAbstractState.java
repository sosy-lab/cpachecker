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
 * Abstract state for policy iteration: bounds on each expression (from the template),
 * for the given control node.
 *
 * Logic-less container class.
 */
public final class PolicyAbstractState implements AbstractState,
    Iterable<Entry<LinearExpression, PolicyBound>>,
    Graphable {

  /**
   * Other known states.
   * TODO
   */
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
  private final Templates templates;

  /**
   * Constructor with the abstraction provided.
   */
  private PolicyAbstractState(
      Iterable<AbstractState> pOtherStates,
      Set<CFAEdge> pIncomingEdges,
      CFANode pNode,
      Map<LinearExpression, PolicyBound> pPAbstraction,
      Templates pTemplates) {
    otherStates = ImmutableList.copyOf(pOtherStates);
    incomingEdges = ImmutableSet.copyOf(pIncomingEdges);
    node = pNode;
    abstraction = Optional.of(ImmutableMap.copyOf(pPAbstraction));
    templates = pTemplates;
  }

  /**
   * Constructor without the abstraction.
   */
  private PolicyAbstractState(
      Iterable<AbstractState> pOtherStates,
      Set<CFAEdge> pIncomingEdges,
      CFANode pNode,
      Templates pTemplates) {
    otherStates = ImmutableList.copyOf(pOtherStates);
    incomingEdges = ImmutableSet.copyOf(pIncomingEdges);
    node = pNode;
    abstraction = Optional.absent();
    templates = pTemplates;
  }

  /**
   * Factory methods.
   */
  public static PolicyAbstractState ofAbstraction(
      Map<LinearExpression, PolicyBound> data,
      Templates templates,
      CFANode node,
      Set<CFAEdge> pIncomingEdges,
      Iterable<AbstractState> pOtherStates
  ) {
    return new PolicyAbstractState(
        pOtherStates, pIncomingEdges, node, data, templates);
  }

  public static PolicyAbstractState ofIntermediate(
      Iterable<AbstractState> pOtherStates,
      Set<CFAEdge> pIncomingEdges,
      CFANode node,
      Templates pTemplates
  ) {
    return new PolicyAbstractState(
        pOtherStates,
        pIncomingEdges,
        node,
        pTemplates
    );
  }

  public static PolicyAbstractState empty(CFANode node) {
    return new PolicyAbstractState(
        ImmutableList.<AbstractState>of(), // other states
        ImmutableSet.<CFAEdge>of(), // incoming edges
        node, // node
        ImmutableMap.<LinearExpression, PolicyBound>of(),
        Templates.empty() // templates
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
   * {@param e} or an empty optional if it is unbounded.
   */
  public Optional<PolicyBound> getBound(
      LinearExpression e) {
    Preconditions.checkState(abstraction.isPresent(),
        "Template bounds can be obtained only from abstracted states.");
    return Optional.fromNullable(abstraction.get().get(e));
  }

  public Templates getTemplates() {
    return templates;
  }

  public CFANode getNode() {
    return node;
  }

  /** Update methods */

  public PolicyAbstractState withUpdates(
      Map<LinearExpression, PolicyBound> updates,
      Set<LinearExpression> unbounded,
      Templates newTemplates) {
    Preconditions.checkState(abstraction.isPresent(),
        "Updates can only be applied to the abstracted state");

    ImmutableMap<LinearExpression, PolicyBound> map = abstraction.get();

    ImmutableSet<LinearExpression> allTemplates = ImmutableSet.<LinearExpression>
        builder().addAll(updates.keySet()).addAll(map.keySet()).build();

    ImmutableMap.Builder<LinearExpression, PolicyBound> builder =
        ImmutableMap.builder();

    for (LinearExpression template : allTemplates) {
      if (unbounded.contains(template)) {
        continue;
      }
      if (updates.containsKey(template)) {
        builder.put(template, updates.get(template));
      } else {
        builder.put(template, map.get(template));
      }
    }
    return new PolicyAbstractState(
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
    if (this == o)  return true;
    if (o == null || getClass() != o.getClass()) return false;
    PolicyAbstractState other = (PolicyAbstractState)o;
    return (abstraction.equals(other.abstraction) && node.equals(other.node));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(node, abstraction);
  }
}
