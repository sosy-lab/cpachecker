package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;

public class PolicyTemplateBound {
  final CFAEdge edge;
  final ExtendedRational bound;

  private PolicyTemplateBound(CFAEdge edge, ExtendedRational bound) {
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

    if (!bound.equals(that.bound)) {
      return false;
    }
    if (!edge.equals(that.edge)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = edge.hashCode();
    result = 31 * result + bound.hashCode();
    return result;
  }
}
