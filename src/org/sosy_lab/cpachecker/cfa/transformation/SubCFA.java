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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class SubCFA {
  private CFA cfa;
  private CFANode originalCFAEntryNode;
  private CFANode originalCFAExitNode;
  private CFANode subCFAEntryNode;
  private CFANode subCFAExitNode;
  private ProgramTransformationEnum programTransformationEnum;
  private ProgramTransformationBehaviour programTransformationBehaviour;
  private ImmutableSet<CFANode> allNodes;
  private ImmutableSet<CFAEdge> allEdges;

  public SubCFA(
      CFA pCfa,
      CFANode pOriginalCFAEntryNode,
      CFANode pOriginalCFAExitNode,
      CFANode pSubCFAEntryNode,
      CFANode pSubCFAExitNode,
      ProgramTransformationEnum pProgramTransformationEnum,
      ProgramTransformationBehaviour pProgramTransformationBehaviour,
      ImmutableSet<CFANode> pAllNodes,
      ImmutableSet<CFAEdge> pAllEdges) {
    cfa = pCfa;
    originalCFAEntryNode = pOriginalCFAEntryNode;
    originalCFAExitNode = pOriginalCFAExitNode;
    subCFAEntryNode = pSubCFAEntryNode;
    subCFAExitNode = pSubCFAExitNode;
    programTransformationEnum = pProgramTransformationEnum;
    programTransformationBehaviour = pProgramTransformationBehaviour;
    allNodes = pAllNodes;
    allEdges = pAllEdges;
  }

  public SubCFA(CFA pCFA, CFANode pEntryNode, CFANode pExitNode, ProgramTransformationEnum pTransformation) {
    SubCFA afterTransformation = null;
    ProgramTransformation selectedTransformation;

    switch (pTransformation) {
      case TAIL_RECURSION_ELIMINATION:
        selectedTransformation = new TREProgramTransformation();
        afterTransformation = selectedTransformation.transform(pCFA, pEntryNode, pExitNode);
        break;
      default:
        break;
    }

    if (null != afterTransformation) {
      cfa = afterTransformation.cfa;
      originalCFAEntryNode = afterTransformation.originalCFAEntryNode;
      originalCFAExitNode = afterTransformation.originalCFAExitNode;
      subCFAEntryNode = afterTransformation.subCFAEntryNode;
      subCFAExitNode = afterTransformation.subCFAExitNode;
      programTransformationEnum = afterTransformation.programTransformationEnum;
      programTransformationBehaviour = afterTransformation.programTransformationBehaviour;
      allNodes = afterTransformation.allNodes;
      allEdges = afterTransformation.allEdges;
    }
  }


  public CFA insertSubCFA(CFA pCFA) {
    return pCFA;
  }
}
