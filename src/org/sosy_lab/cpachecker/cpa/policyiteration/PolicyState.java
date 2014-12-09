package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Abstract state for policy iteration: bounds on each expression (from the
 * template), for the given control node.
 *
 * Logic-less container class.
 */
public abstract class PolicyState implements AbstractState, Graphable {

  protected final CFANode node;

  /** Templates tracked. */
  protected final ImmutableSet<Template> templates;

  public static final class PolicyAbstractedState extends PolicyState
        implements Iterable<Entry<Template, PolicyBound>> {
    /**
     * Finite bounds for templates.
     */
    private final ImmutableMap<Template, PolicyBound> abstraction;
    private final PointerTargetSet pointerTargetSet;

    private PolicyAbstractedState(CFANode pNode,
        Set<Template> pTemplates,
        Map<Template, PolicyBound> pAbstraction,
        PointerTargetSet pPointerTargetSet) {
      super(pNode, pTemplates);
      abstraction = ImmutableMap.copyOf(pAbstraction);
      pointerTargetSet = pPointerTargetSet;
    }

    public PolicyAbstractedState withUpdates(
        Map<Template, PolicyBound> updates,
        Set<Template> unbounded,
        Set<Template> newTemplates) {

      ImmutableMap.Builder<Template, PolicyBound> builder =
          ImmutableMap.builder();

      for (Template template : newTemplates) {
        if (unbounded.contains(template)) {
          continue;
        }
        if (updates.containsKey(template)) {
          builder.put(template, updates.get(template));
        } else {
          PolicyBound v = abstraction.get(template);
          if (v != null) {
            builder.put(template, abstraction.get(template));
          }
        }
      }
      return new PolicyAbstractedState(
          node, newTemplates, builder.build(), pointerTargetSet);
    }

    public PointerTargetSet getPointerTargetSet() {
      return pointerTargetSet;
    }

    /**
     * @return {@link PolicyBound} for the given {@link LinearExpression}
     * <code>e</code> or an empty optional if it is unbounded.
     */
    public Optional<PolicyBound> getBound(Template e) {
      return Optional.fromNullable(abstraction.get(e));
    }

    @Override
    public boolean isAbstract() {
      return true;
    }

    @Override
    public String toDOTLabel() {
      return String.format(
          "%s%n%s%n%s",
          (new PolicyDotWriter()).toDOTLabel(abstraction),
          pointerTargetSet,
          templates
      );
    }

    @Override
    public String toString() {
      return String.format("%s: %s, %s", node, abstraction, pointerTargetSet);
    }

    @Override
    public Iterator<Entry<Template, PolicyBound>> iterator() {
      return abstraction.entrySet().iterator();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(pointerTargetSet, abstraction, super.hashCode());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PolicyAbstractedState other = (PolicyAbstractedState)o;
      return (
          pointerTargetSet.equals(other.pointerTargetSet) &&
          abstraction.equals(other.abstraction) && super.equals(o));
    }
  }

  public static final class PolicyIntermediateState extends PolicyState {
    private final PathFormula pathFormula;
    private final ImmutableMultimap<CFANode, CFANode> trace;

    private PolicyIntermediateState(
        CFANode pNode,
        Set<Template> pTemplates,
        PathFormula pPathFormula,
        Multimap<CFANode, CFANode> pTrace) {
      super(pNode, pTemplates);
      pathFormula = pPathFormula;
      trace = ImmutableMultimap.copyOf(pTrace);
    }

    public PathFormula getPathFormula() {
      return pathFormula;
    }

    public ImmutableMultimap<CFANode, CFANode> getTrace() {
      return trace;
    }

    @Override
    public boolean isAbstract() {
      return false;
    }

    @Override
    public String toDOTLabel() {
      return pathFormula.toString() + "\n" + trace.toString();
    }

    @Override
    public String toString() {
      return String.format("%s: %s", node, pathFormula);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(pathFormula, super.hashCode());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PolicyIntermediateState other = (PolicyIntermediateState)o;
      return (pathFormula.equals(other.pathFormula) && super.equals(o));
    }
  }

  private PolicyState(CFANode pNode, Set<Template> pTemplates) {
    node = pNode;
    templates = ImmutableSet.copyOf(pTemplates);
  }

  /**
   * Factory methods.
   */
  public static PolicyAbstractedState ofAbstraction(
      Map<Template, PolicyBound> data,
      Set<Template> templates,
      CFANode node,
      PointerTargetSet pPointerTargetSet
  ) {
    return new PolicyAbstractedState(node, templates, data, pPointerTargetSet);
  }

  public static PolicyState ofIntermediate(
      CFANode node,
      Set<Template> pTemplates,
      PathFormula pPathFormula,
      Multimap<CFANode, CFANode> pTrace
  ) {
    return new PolicyIntermediateState(
        node,
        pTemplates,
        pPathFormula,
        pTrace);
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

  /**
   * @return Empty abstracted state associated with {@code node}.
   */
  public static PolicyState empty(CFANode node) {
    return ofAbstraction(
        ImmutableMap.<Template, PolicyBound>of(),
        ImmutableSet.<Template>of(), // templates
        node, // node
        PointerTargetSet.emptyPointerTargetSet()
    );
  }

  public abstract boolean isAbstract();

  public ImmutableSet<Template> getTemplates() {
    return templates;
  }

  public CFANode getNode() {
    return node;
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
    return (templates.equals(other.templates) && node.equals(other.node));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(node, templates);
  }
}
