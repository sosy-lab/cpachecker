// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Representation of the result of a successful program transformation.
 */
public record SubCFA (
    CFANode originalCFAEntryNode,
    CFANode originalCFAExitNode,
    CFANode subCFAEntryNode,
    CFANode subCFAExitNode,
    ProgramTransformationEnum programTransformationEnum,
    ProgramTransformationBehaviour programTransformationBehaviour,
    ImmutableSet<CFANode> allNodes,
    ImmutableSet<CFAEdge> allEdges
  ) {

  public static SubCFA createSubCFA(CFA pCFA, CFANode pEntryNode, CFANode pExitNode, ProgramTransformationEnum pTransformation) {

    return switch (pTransformation) {
      case JUMP_THREADING -> null;
      case TAIL_RECURSION_ELIMINATION ->
        new TailRecursionEliminationProgramTransformation().transform(pCFA, pEntryNode, pExitNode);
      default -> null;
    };
  }

  /**
   * Inserts the CFANodes and CFAEdges plus additional CFAMetadata into the given MutableCFA.
   *
   * @param pCFA MutableCFA
   * @return MutableCFA with the added program transformation
   */
  public MutableCFA insertSubCFA(MutableCFA pCFA) {
    return pCFA;
  }
}
