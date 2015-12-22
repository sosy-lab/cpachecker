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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.WeavingLocation;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * An auxiliary class for creating CFA edges from {@code AAstNode}s.
 */
public enum ShadowCFAEdgeFactory {
  INSTANCE;

  public static class ShadowCFANode extends CFANode {

    public ShadowCFANode(String pInFunctionWithName) {
      super(pInFunctionWithName);
    }

  }

  public static class ShadowCodeStartNode extends ShadowCFANode implements WeavingLocation {

    private final CFANode weavedOnLocation;

    public ShadowCodeStartNode(String pInFunctionWithName, CFANode pWeavedOnLocation) {
      super(pInFunctionWithName);
      this.weavedOnLocation = pWeavedOnLocation;
    }

    @Override
    public CFANode getWeavedOnLocation() {
      return weavedOnLocation;
    }

  }

  /**
   * Given a sequence of operations (declarations, assumes, statements, ...)
   *    produce a sequence of dummy {@code CFANode}s,
   *    and {@code CFAEdge}s (that are not part of the CFA).
   *
   * @param pCode       A sequence [op1, op2, ... opN] of operations (declarations, assumes, statements, ...).
   * @param pSuccessorInCfa  The {@code CFANode} lSucc where the last operation should lead to.
   * @return            List of dummy {@code CFAEdge}s, i.e., [(l1', op1, l1''), (l1'', op2, l1'''), ... , (l1', opN, lSucc)].
   */
  public List<CFAEdge> createEdgesForNodeSequence(List<AAstNode> pCode, CFANode pSuccessorInCfa) {
    Preconditions.checkNotNull(pCode);
    Preconditions.checkArgument(pCode.size() > 0);
    Preconditions.checkNotNull(pSuccessorInCfa);

    LinkedList<CFAEdge> result = Lists.newLinkedList();
    Iterator<AAstNode> it = pCode.iterator();

    CFANode predLoc = null;
    CFANode succLoc = null;

    while (it.hasNext()) {
      final AAstNode node = it.next();

      predLoc = (predLoc == null)
        ? new ShadowCodeStartNode(pSuccessorInCfa.getFunctionName(), pSuccessorInCfa)
            : succLoc;

      if (it.hasNext()) {
        succLoc = new ShadowCFANode(pSuccessorInCfa.getFunctionName());
      } else {
        succLoc = pSuccessorInCfa;
      }

      final CFAEdge edge;

      if (node instanceof CStatement) {
        CStatement stmt = (CStatement) node;
        edge = new CStatementEdge(node.toASTString(), stmt, node.getFileLocation(), predLoc, succLoc);

      } else if (node instanceof CInitializer) {
        edge = null;

      } else if (node instanceof CDeclaration) {
        final CDeclaration decl = (CDeclaration) node;
        final CDeclarationEdge declEdge = new CDeclarationEdge(node.toASTString(), node.getFileLocation(),
            predLoc, succLoc, decl);
        edge = declEdge;

      } else if (node instanceof AStatement) {
        edge = null;

      } else {
        throw new RuntimeException(String.format("Creating edges for interface %s is not yet implemented!", node.getClass().getSimpleName()));
      }

      Preconditions.checkState(edge != null, "Each ASTNode has to be reflected in a corresponding CFAEdge! " + node.toString());

      predLoc.addLeavingEdge(edge);

      result.add(edge);
    }

    return result;
  }

}
