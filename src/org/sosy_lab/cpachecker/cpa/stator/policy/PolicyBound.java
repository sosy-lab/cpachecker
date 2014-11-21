package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.rationals.Rational;

import com.google.common.base.Objects;

public class PolicyBound {
  final CFAEdge trace;

  // NOTE: might make more sense to use normal Rational, because we are no longer
  // storing infinities or negative infinities.
  final Rational bound;

  PolicyBound(CFAEdge trace, Rational bound) {
    this.trace = trace;
    this.bound = bound;
  }

  public static PolicyBound of(CFAEdge edge, Rational bound) {
    return new PolicyBound(edge, bound);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(bound, trace);
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
    return bound.equals(o.bound) && trace.equals(o.trace);
  }
}
