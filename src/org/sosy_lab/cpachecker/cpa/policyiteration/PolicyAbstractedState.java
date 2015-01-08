package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public final class PolicyAbstractedState extends PolicyState
      implements Iterable<Entry<Template, PolicyBound>> {
  /**
   * Finite bounds for templates.
   */
  private final ImmutableMap<Template, PolicyBound> abstraction;

  /**
   * Non-abstracted section of the formula.
   */
  private final PathFormula formula;

  private PolicyAbstractedState(Location pLocation,
      Set<Template> pTemplates,
      Map<Template, PolicyBound> pAbstraction,
      PathFormula pFormula) {
    super(pLocation, pTemplates);
    abstraction = ImmutableMap.copyOf(pAbstraction);
    formula = pFormula;
  }

  public static PolicyAbstractedState of(
      Map<Template, PolicyBound> data,
      Set<Template> templates,
      Location pLocation,
      PathFormula pFormula
  ) {
    return new PolicyAbstractedState(pLocation, templates, data, pFormula);
  }

  public PolicyAbstractedState withUpdates(
      Map<Template, PolicyBound> updates,
      Set<Template> unbounded,
      Set<Template> newTemplates,
      PathFormula newPathFormula) {

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
        getLocation(), newTemplates, builder.build(),  newPathFormula
    );
  }

  public PathFormula getPathFormula() {
    return formula;
  }

  /**
   * @return {@link PolicyBound} for the given {@link Template}
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
        "%s%n%s%n %n %s",
        (new PolicyDotWriter()).toDOTLabel(abstraction),
        templates,
        formula
    );
  }

  @Override
  public String toString() {
    return String.format("%s: %s", getLocation(), abstraction);
  }

  @Override
  public Iterator<Entry<Template, PolicyBound>> iterator() {
    return abstraction.entrySet().iterator();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(formula, abstraction, super.hashCode());
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
    return (formula.equals(other.formula) &&
        abstraction.equals(other.abstraction) && super.equals(o));
  }
}
