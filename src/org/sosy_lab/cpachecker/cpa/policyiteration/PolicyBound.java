package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Policy with a local bound.
 */
public class PolicyBound {

  /**
   * Location of an abstracted state which has caused an update.
   */
  private final PolicyAbstractedState predecessor;

  /**
   * Policy formula. Has to be concave and monotone (no conjunctions in
   * particular).
   */
  private final PathFormula formula;

  /**
   * Bound on the policy.
   */
  private final Rational bound;

  /**
   * Set of incoming templates which may influence this bound.
   */
  private final ImmutableSet<Template> dependencies;

  private int hashCache = 0;

  private static final Map<Triple<PolicyAbstractedState, BooleanFormula, PolicyAbstractedState>, Integer>
      serializationMap = new HashMap<>();
  private static final UniqueIdGenerator pathCounter = new UniqueIdGenerator();

  private PolicyBound(PathFormula pFormula, Rational pBound,
      PolicyAbstractedState pPredecessor,
      Collection<Template> pDependencies) {
    formula = pFormula;
    bound = pBound;
    predecessor = pPredecessor;
    dependencies = ImmutableSet.copyOf(pDependencies);
  }

  public static PolicyBound of(PathFormula pFormula, Rational bound,
      PolicyAbstractedState pUpdatedFrom,
      Collection<Template> pDependencies
  ) {
    return new PolicyBound(pFormula, bound, pUpdatedFrom,
        pDependencies);
  }

  public PolicyBound updateValue(Rational newValue) {
    return new PolicyBound(formula, newValue, predecessor, dependencies);
  }

  public PolicyBound withNoDependencies() {
    return new PolicyBound(formula, bound, predecessor,
        ImmutableSet.of());
  }

  /**
   * @return Unique identifier for value determination.
   *
   * Based on triple (from, to, policy).
   */
  public int serializePolicy(PolicyAbstractedState toState) {
    Triple<PolicyAbstractedState, BooleanFormula, PolicyAbstractedState> p = Triple.of(
        predecessor, formula.getFormula(), toState);
    Integer serialization = serializationMap.get(p);
    if (serialization == null) {
      serialization = pathCounter.getFreshId();
      serializationMap.put(p, serialization);
    }
    return serialization;
  }

  public PolicyAbstractedState getPredecessor() {
    return predecessor;
  }

  public PathFormula getFormula() {
    return formula;
  }

  public Rational getBound() {
    return bound;
  }

  public ImmutableSet<Template> getDependencies() {
    return dependencies;
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hashCode(predecessor, bound, formula);
    }
    return hashCache;
  }

  @Override
  public String toString() {

    // Converting the predecessor to string is very costly.
    return bound.toString();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (other.getClass() != this.getClass()) {
      return false;
    }
    PolicyBound o = (PolicyBound) other;
    return
        predecessor.equals(o.predecessor)
            && bound.equals(o.bound)
            && formula.equals(o.formula);
  }
}

