package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Objects;

public class PolicyBound {

  // We have to track a location now as well, as the node does not
  // identify the callstack position.
  final Location updatedFrom;

  final MultiEdge trace;

  // NOTE: might make more sense to use normal Rational, because we are no longer
  // storing infinities or negative infinities.
  final Rational bound;

  private static final Map<Pair<Location, MultiEdge>, Integer> serializationMap = new HashMap<>();
  private static int pathCounter = -1;

  PolicyBound(MultiEdge pTrace, Rational pBound, Location pUpdatedFrom) {
    trace = pTrace;
    bound = pBound;
    updatedFrom = pUpdatedFrom;
  }

  public static PolicyBound of(MultiEdge edge, Rational bound, Location pUpdatedFrom) {
    return new PolicyBound(edge, bound, pUpdatedFrom);
  }

  public int serializePath() {
    Pair<Location, MultiEdge> p = Pair.of(updatedFrom, trace);
    Integer serialization = serializationMap.get(p);
    if (serialization == null) {
      serialization = ++pathCounter;
      serializationMap.put(p, serialization);
    }
    return serialization;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(updatedFrom, bound, trace);
  }

  @Override
  public String toString() {
    return String.format("%s (edge: %s)", bound, trace);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (other.getClass() != this.getClass()) return false;
    PolicyBound o = (PolicyBound) other;
    // Hm what about the cases where the constraints are equal, but
    // the traces are not?..
    return updatedFrom.equals(o.updatedFrom) && bound.equals(o.bound) && trace.equals(o.trace);
  }
}
