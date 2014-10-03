package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;

import com.google.common.base.Preconditions;

public class PolicyTemplateBound {
  final CFAEdge edge;

  // NOTE: might make more sense to use normal Rational, because we are no longer
  // storing infinities or negative infinities.
  final ExtendedRational bound;

  private PolicyTemplateBound(CFAEdge edge, ExtendedRational bound) {
    Preconditions.checkState(
        bound.getType() == ExtendedRational.NumberType.RATIONAL);

    this.edge = edge;
    this.bound = bound;
  }

  public static PolicyTemplateBound of(CFAEdge edge, ExtendedRational bound) {
    return new PolicyTemplateBound(edge, bound);
  }

  @Override
  public String toString() {
    CFANode successor = edge.getSuccessor();
    if (successor.getNumEnteringEdges() == 1) {
      return String.format("%s", bound.toString());
    }
    return String.format("%s (edge: %s->%s)", bound,
        edge.getPredecessor().getNodeNumber(),
        successor.getNodeNumber());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicyTemplateBound that = (PolicyTemplateBound)o;
    return bound.equals(that.bound) && edge.equals(that.edge);
  }

  @Override
  public int hashCode() {
    int result = edge.hashCode();
    result = 31 * result + bound.hashCode();
    return result;
  }
}
