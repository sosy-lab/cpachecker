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
import com.google.common.collect.ImmutableSet;

/**
 * Abstract state for policy iteration: bounds on each expression (from the
 * template), for the given control node.
 *
 * Logic-less container class.
 *
 * TODO: might be a good idea to have a separate class for the abstracted value.
 */
public abstract class PolicyState implements AbstractState, Graphable {

  protected final CFANode node;

  /** Templates tracked. */
  protected final ImmutableSet<Template> templates;

  public static final class PolicyAbstractedState extends PolicyState
        implements Iterable<Entry<LinearExpression, PolicyBound>> {
    /**
     * Finite bounds for templates.
     */
    private final ImmutableMap<LinearExpression, PolicyBound> abstraction;
    private final PointerTargetSet pointerTargetSet;

    private PolicyAbstractedState(CFANode pNode,
        Set<Template> pTemplates,
        Map<LinearExpression, PolicyBound> pAbstraction,
        PointerTargetSet pPointerTargetSet) {
      super(pNode, pTemplates);
      abstraction = ImmutableMap.copyOf(pAbstraction);
      pointerTargetSet = pPointerTargetSet;
    }

    public PolicyAbstractedState withUpdates(
        Map<LinearExpression, PolicyBound> updates,
        Set<LinearExpression> unbounded,
        Set<Template> newTemplates) {

      ImmutableMap<LinearExpression, PolicyBound> map = abstraction;

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
    public Optional<PolicyBound> getBound(LinearExpression e) {
      return Optional.fromNullable(abstraction.get(e));
    }

    @Override
    public boolean isAbstract() {
      return true;
    }

    @Override
    public String toDOTLabel() {
      return (new PolicyDotWriter()).toDOTLabel(abstraction);
    }

    @Override
    public String toString() {
      return String.format("%s: %s, %s", node, abstraction, pointerTargetSet);
    }

    @Override
    public Iterator<Entry<LinearExpression, PolicyBound>> iterator() {
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

    private PolicyIntermediateState(CFANode pNode,
        Set<Template> pTemplates, PathFormula pPathFormula) {
      super(pNode, pTemplates);
      pathFormula = pPathFormula;
    }

    public PathFormula getPathFormula() {
      return pathFormula;
    }

    @Override
    public boolean isAbstract() {
      return false;
    }

    @Override
    public String toDOTLabel() {
      return pathFormula.toString();
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
      Map<LinearExpression, PolicyBound> data,
      Set<Template> templates,
      CFANode node,
      PointerTargetSet pPointerTargetSet
  ) {
    return new PolicyAbstractedState(node, templates, data, pPointerTargetSet);
  }

  public static PolicyState ofIntermediate(
      CFANode node,
      Set<Template> pTemplates,
      PathFormula pPathFormula
  ) {
    return new PolicyIntermediateState(
        node,
        pTemplates,
        pPathFormula);
  }

  public PolicyIntermediateState asIntermediate() {
    return (PolicyIntermediateState) this;
  }

  public PolicyAbstractedState asAbstracted() {
    return (PolicyAbstractedState) this;
  }

  public static PolicyState empty(CFANode node) {
    return ofAbstraction(
        ImmutableMap.<LinearExpression, PolicyBound>of(),
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

  /** Update methods */

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
