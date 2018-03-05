/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.dependencegraph;

/** Formats nodes for the GraphViz dot format. */
class DGNodeDotFormatter {

  public String getNodeString(final DGNode pNode) {
    String shape;
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
