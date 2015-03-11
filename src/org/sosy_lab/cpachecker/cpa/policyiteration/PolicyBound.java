package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Objects;

/**
 * Policy with a local bound.
 */
public class PolicyBound {

  /**
   * Location of an abstracted state which has caused an update.
   */
  final Location predecessor;

  /**
   * Policy formula. Has to be concave&monotone (no conjunctions in particular).
   */
  final PathFormula formula;

  /**
   * Bound on the policy.
   */
  final Rational bound;

  /**
   * PathFormula which defines the starting {@link SSAMap} and
   * {@link org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet}
   * for {@code formula}.
   */
  final PathFormula startPathFormula;

  /**
   * Whether the bound can change with changing initial conditions.
   */
  final boolean dependsOnInitial;

  private static final Map<Triple<Location, BooleanFormula, Location>, Integer>
      serializationMap = new HashMap<>();
  private static int pathCounter = -1;

  private PolicyBound(PathFormula pFormula, Rational pBound, Location pPredecessor,
      PathFormula pStartPathFormula, boolean pDependsOnInitial) {
    formula = pFormula;
    bound = pBound;
    predecessor = pPredecessor;
    startPathFormula = pStartPathFormula;
    dependsOnInitial = pDependsOnInitial;
  }

  public static PolicyBound of(PathFormula pFormula, Rational bound,
      Location pUpdatedFrom, PathFormula pStartPathFormula,
      boolean dependsOnInitial) {
    return new PolicyBound(pFormula, bound, pUpdatedFrom, pStartPathFormula,
        dependsOnInitial);
  }

  public PolicyBound updateValue(Rational newValue) {
    return new PolicyBound(formula, newValue, predecessor, startPathFormula,
        dependsOnInitial);
  }

  /**
   * @return Unique identifier for value determination.
   *
   * Based on triple (from, to, policy).
   */
  public int serializePolicy(Location toLocation) {
    Triple<Location, BooleanFormula, Location> p = Triple.of(
        predecessor, formula.getFormula(), toLocation);
    Integer serialization = serializationMap.get(p);
    if (serialization == null) {
      serialization = ++pathCounter;
      serializationMap.put(p, serialization);
    }
    return serialization;
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
    return predecessor.equals(o.predecessor) && bound.equals(o.bound) && formula.equals(o.formula);
  }
}
