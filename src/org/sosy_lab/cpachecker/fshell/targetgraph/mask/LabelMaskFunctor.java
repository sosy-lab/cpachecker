package org.sosy_lab.cpachecker.fshell.targetgraph.mask;

import org.jgrapht.graph.MaskFunctor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class LabelMaskFunctor implements MaskFunctor<Node, Edge> {

  private String mLabel;

  public LabelMaskFunctor(String pLabel) {
    mLabel = pLabel;
  }

  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    return !matches(pArg0.getSource().getCFANode());
  }

  private boolean matches(CFANode pNode) {
    if (pNode instanceof CFALabelNode) {
      CFALabelNode lLabelNode = (CFALabelNode)pNode;
      return mLabel.equals(lLabelNode.getLabel());
    }

    return false;
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    CFANode lCFANode = pArg0.getCFANode();

    if (matches(lCFANode)) {
      return false;
    }

    for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
      CFAEdge lCFAEdge = lCFANode.getEnteringEdge(lIndex);

      if (matches(lCFAEdge.getPredecessor())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (!pOther.getClass().equals(getClass())) {
      return false;
    }

    LabelMaskFunctor lFunctor = (LabelMaskFunctor)pOther;

    return mLabel.equals(lFunctor.mLabel);
  }

  @Override
  public int hashCode() {
    return mLabel.hashCode() + 21928;
  }

}
