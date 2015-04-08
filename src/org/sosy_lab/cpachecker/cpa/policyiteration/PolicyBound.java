package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.rationals.Rational;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;

import com.google.common.base.Objects;

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
   * Whether the bound can change with changing initial conditions.
   * // todo: what things on initial specifically.
   * // how about doing that without an SMT solver?
   */
  private final boolean dependsOnInitial;

  private static final Map<Triple<PolicyAbstractedState, BooleanFormula, PolicyAbstractedState>, Integer>
      serializationMap = new HashMap<>();
  private static final UniqueIdGenerator pathCounter = new UniqueIdGenerator();

  private PolicyBound(PathFormula pFormula, Rational pBound,
      PolicyAbstractedState pPredecessor,
      boolean pDependsOnInitial) {
    formula = pFormula;
    bound = pBound;
    predecessor = pPredecessor;
    dependsOnInitial = pDependsOnInitial;
  }

  public static PolicyBound of(PathFormula pFormula, Rational bound,
      PolicyAbstractedState pUpdatedFrom,  boolean dependsOnInitial) {
    return new PolicyBound(pFormula, bound, pUpdatedFrom,  dependsOnInitial);
  }

  public PolicyBound updateValue(Rational newValue) {
    return new PolicyBound(formula, newValue, predecessor,
        dependsOnInitial);
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
    return predecessor.getLatestVersion();
  }

  public PathFormula getFormula() {
    return formula;
  }

  public Rational getBound() {
    return bound;
  }

  public PathFormula getStartPathFormula() {
    return getPredecessor().getPathFormula();
  }

  public boolean dependsOnInitial() {
    return dependsOnInitial;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(predecessor, bound, formula);
  }

  @Override
  public String toString() {
    return String.format("%s (from: %s)", bound, predecessor);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (other.getClass() != this.getClass()) return false;
    PolicyBound o = (PolicyBound) other;
    return
        predecessor.equals(o.predecessor)
            && bound.equals(o.bound)
            && formula.equals(o.formula);
  }
}

