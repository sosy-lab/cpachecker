package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Objects;

public class PolicyBound {

  final Location predecessor;
  final PathFormula formula;
  final Rational bound;

  private static final Map<Triple<Location, PathFormula, Location>, Integer> serializationMap = new HashMap<>();
  private static int pathCounter = -1;

  PolicyBound(PathFormula pFormula, Rational pBound, Location pPredecessor) {
    formula = pFormula;
    bound = pBound;
    predecessor = pPredecessor;
  }

  public static PolicyBound of(PathFormula pFormula, Rational bound, Location pUpdatedFrom) {
    return new PolicyBound(pFormula, bound, pUpdatedFrom);
  }

  public int serializePath(Location toLocation) {
    Triple<Location, PathFormula, Location> p = Triple.of(
        predecessor, formula, toLocation);
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
    return String.format("%s (formula: %s)", bound, formula);
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
