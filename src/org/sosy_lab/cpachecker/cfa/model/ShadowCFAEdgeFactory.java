/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.model;

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * An auxiliary class for creating CFA edges from {@code AAstNode}s.
 */
public enum ShadowCFAEdgeFactory {
  INSTANCE;

  public static class ShadowCFANode extends CFANode {

    //
    //  EnteringEdges(this) == EnteringEdges (OrigianlLocation)
    //  LeavingEdges(this) == ShadowTransitions
    //  TargetLocation(ShadowTransitions) == OriginalLocation
    //

    private final CFANode shadowOnLocation;

    public ShadowCFANode(List<AAstNode> pLeavingShadowCode, CFANode pShadowOnLocation) {
      super(pShadowOnLocation.getFunctionName());

      shadowOnLocation = pShadowOnLocation;

      final MultiEdge codeEdges = ShadowCFAEdgeFactory.INSTANCE.createEdgeForNodeSequence(this, pLeavingShadowCode, pShadowOnLocation);

      addLeavingEdge(codeEdges);
    }

    @Override
    public CFAEdge getEnteringEdge(int pIndex) {
      return shadowOnLocation.getEnteringEdge(pIndex);
    }

    @Override
    public FunctionSummaryEdge getEnteringSummaryEdge() {
      return shadowOnLocation.getEnteringSummaryEdge();
    }

    @Override
    public int getNumEnteringEdges() {
      return shadowOnLocation.getNumEnteringEdges();
    }

    @Override
    public int getReversePostorderId() {
      return shadowOnLocation.getReversePostorderId();
    }

  }

  public CFAEdge createEdgeForNode(CFANode pPredecessor, AAstNode pCode, CFANode pSuccessor) {
    throw new RuntimeException("Implement me");
  }

  /**
   * Given a sequence of operations (declarations, assumes, statements, ...)
   *    produce a sequence of dummy {@code CFANode}s,
   *    and {@code CFAEdge}s (that are not part of the CFA).
   *
   * @param pCode       A sequence [op1, op2, ... opN] of operations (declarations, assumes, statements, ...).
   * @param pSuccessor  The {@code CFANode} lSucc where the last operation should lead to.
   * @return            List of dummy {@code CFAEdge}s, i.e., [(l1', op1, l1''), (l1'', op2, l1'''), ... , (l1', opN, lSucc)].
   */
  public List<CFAEdge> createEdgesForNodeSequence(List<AAstNode> pCode, CFANode pSuccessor) {
    Preconditions.checkNotNull(pCode);
    Preconditions.checkArgument(pCode.size() > 1);
    Preconditions.checkNotNull(pSuccessor);

    LinkedList<CFAEdge> result = Lists.newLinkedList();
    List<CFANode> shadowLocations = Lists.newArrayList();

    //
    //  l0  --op1--> l1
    //

//    Iterator<AAstNode> it = pCode.iterator();
//
//    while (it.hasNext()) {
//      final AAstNode node = it.next();
//
//      if (it.hasNext()) {
//        succLoc = new ShadowCFANode(pLeavingShadowCode, pShadowOnLocation);
//      } else {
//        succLoc = pSuccessor;
//      }
//
//      if (node instanceof CAssignment) {
//
//      } else if (node instanceof CAssignment) {
//
//      } else if (node instanceof CInitializer) {
//
//      } else if (node instanceof CReturnStatement) {
//
//      } else if (node instanceof CRightHandSide) {
//
//      } else if (node instanceof CDeclaration) {
//        final CDeclaration decl = (CDeclaration) node;
//        final CDeclarationEdge edge = new CDeclarationEdge(node.toString(), node.getFileLocation(),
//            pPredecessor, pSuccessor, decl);
//      } else if (node instanceof AStatement) {
//
//      } else {
//        throw new RuntimeException(String.format("Creating edges for interface %s is not yet implemented!", node.getClass().getSimpleName()));
//      }
//
//      CFAEdge edge = null;
//
//    }

    return result;
  }

  public MultiEdge createEdgeForNodeSequence(CFANode pPredecessor,
      List<AAstNode> pCode, CFANode pSuccessor) {

    throw new RuntimeException("Implement me");
  }
}
