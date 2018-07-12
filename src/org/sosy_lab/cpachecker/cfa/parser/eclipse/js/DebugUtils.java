/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;

final class DebugUtils {

  public static JSFunctionEntryNode getMain(final CFABuilder pBuilder) {
    return (JSFunctionEntryNode) pBuilder.getParseResult().getFunctions().get("main");
  }

  public static String getLeavingEdgesReport(final CFANode pNode) {
    return getLeavingEdgesReport(pNode, new HashSet<>());
  }

  public static String getLeavingEdgesReport(
      final CFANode pNode, final Set<CFANode> pExcludedNodes) {
    if (pExcludedNodes.contains(pNode)) {
      return "";
    }
    pExcludedNodes.add(pNode);
    final StringBuilder result = new StringBuilder();
    forEachLeavingEdge(
        pNode,
        (edge) ->
            result
                .append(edge.toString())
                .append('\n')
                .append(getLeavingEdgesReport(edge.getSuccessor(), pExcludedNodes)));
    return result.toString();
  }

  public static String getLeavingEdgesDotReport(final CFANode pNode) {
    return "digraph G {\n"
      + getLeavingEdgesDotReport(pNode, new HashSet<>())
        + "}";
  }

  public static String getLeavingEdgesDotReport(
      final CFANode pNode, final Set<CFANode> pExcludedNodes) {
    if (pExcludedNodes.contains(pNode)) {
      return "";
    }
    pExcludedNodes.add(pNode);
    final StringBuilder result = new StringBuilder();
    if (pNode.getNumLeavingEdges() > 0 && pNode.getLeavingEdge(0) instanceof JSAssumeEdge) {
      result.append("  ").append(pNode).append(" [shape=diamond];\n");
    }
    forEachLeavingEdge(
        pNode,
        (edge) ->
            result
                .append("  ")
                .append(pNode)
                .append(" -> ")
                .append(edge.getSuccessor())
                .append(" [ label=\"")
                .append(edge.getDescription())
                .append("\" ];\n")
                .append(getLeavingEdgesDotReport(edge.getSuccessor(), pExcludedNodes)));
    return result.toString();
  }

  public static void forEachLeavingEdge(final CFANode pNode, final Consumer<CFAEdge> pCallback) {
    for (int i = 0; i < pNode.getNumLeavingEdges(); i++) {
      pCallback.accept(pNode.getLeavingEdge(i));
    }
  }
}
