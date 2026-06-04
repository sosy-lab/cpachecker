// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Representation of the result of a successful program transformation.
 */
public record SubCFA(
    CFANode originalCFAEntryNode,
    CFANode originalCFAExitNode,
    CFANode subCFAEntryNode,
    CFANode subCFAExitNode,
    ProgramTransformationEnum programTransformationEnum,
    ProgramTransformationBehaviour programTransformationBehaviour,
    ImmutableSet<CFANode> allNodes,
    ImmutableSet<CFAEdge> allEdges
  ) {

  /**
   * Inserts the CFANodes and CFAEdges into the given MutableCFA.
   *
   * @param pCFA MutableCFA
   */
  public void insertSubCFA(MutableCFA pCFA) {
    // add nodes
    for (CFANode node : allNodes) {
      pCFA.addNode(node);
    }
    // connect entry and exit nodes
    BlankEdge entryEdge =
        new BlankEdge(
            "enter program transformation: " + programTransformationEnum.name(),
            FileLocation.DUMMY,
            originalCFAEntryNode,
            subCFAEntryNode,
            "enter program transformation: " + programTransformationEnum.name());
    originalCFAEntryNode.addLeavingEdge(entryEdge);
    subCFAEntryNode.addEnteringEdge(entryEdge);
    BlankEdge exitEdge = new BlankEdge("exit program transformation: " + programTransformationEnum.name(), FileLocation.DUMMY, subCFAExitNode, originalCFAExitNode, "exit program transformation: " + programTransformationEnum.name());
    originalCFAExitNode.addEnteringEdge(exitEdge);
    subCFAExitNode.addLeavingEdge(exitEdge);
  }
}
