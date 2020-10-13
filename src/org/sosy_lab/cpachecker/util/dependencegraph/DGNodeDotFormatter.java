// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

/** Formats nodes for the GraphViz dot format. */
class DGNodeDotFormatter {

  public String getNodeString(final DGNode pNode) {
    String shape;
    if (pNode.isUnknownPointerNode()) {
      shape = "ellipse";
    } else {
      switch (pNode.getCfaEdge().getEdgeType()) {
        case AssumeEdge:
          shape = "diamond";
          break;
        case FunctionCallEdge:
          shape = "ellipse\", peripheries=\"2";
          break;
        case BlankEdge:
          shape = "box";
          break;
        default:
          shape = "ellipse";
      }
    }

    return format(pNode, shape);
  }

  String getNodeRepresentation(final DGNode pNode) {
    String nodeId = String.valueOf(pNode.hashCode());
    nodeId = nodeId.replaceAll("-", "");
    return "E" + nodeId;
  }

  private String format(final DGNode pNode, final String pShape) {
    return getNodeRepresentation(pNode)
        + " [shape=\""
        + pShape
        + "\","
        + "label=\""
        + escape(pNode.toString())
        + "\""
        + "]";
  }

  private String escape(final String pStatement) {
    return pStatement.replaceAll("\\\"", "\\\\\"");
  }
}
