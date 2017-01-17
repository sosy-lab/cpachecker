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
package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.FunctionCallCollector;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.FunctionCloner;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Copies all globals and parameters at the function start.
 */
public class SummaryGeneratorHelper {

  private final String postfix;
  private final SetMultimap<String, CIdExpression> assignedVars;

  public SummaryGeneratorHelper(String pPostfix, MutableCFA pMutableCFA) {
    postfix = pPostfix;
    assignedVars = HashMultimap.create();
    populateAssignedVars(pMutableCFA);
  }


  public void copyGlobalsAndParams(MutableCFA pMutableCFA) {
    for (FunctionEntryNode node : pMutableCFA.getAllFunctionHeads()) {
      copyDeclarations(node, pMutableCFA);
    }
  }

  private void copyDeclarations(FunctionEntryNode pNode, MutableCFA pMutableCFA) {
    for (AParameterDeclaration param : pNode.getFunctionParameters()) {

      // todo: do so only for the parameters which were modified.
      CParameterDeclaration cParam = (CParameterDeclaration) param;
      insertCopyingNode(pNode, cParam.asVariableDeclaration(), pMutableCFA);
    }

    for (CIdExpression modifiedVar : assignedVars.get(pNode.getFunctionName())) {
      CSimpleDeclaration decl = modifiedVar.getDeclaration();
      if (decl instanceof CVariableDeclaration) {
        CVariableDeclaration cDecl = (CVariableDeclaration) decl;
        if (cDecl.isGlobal()) {
          insertCopyingNode(pNode, cDecl, pMutableCFA);
        }
      }
    }

  }

  /**
   * Changing
   * <pre>
   *   start -(e)-> rest
   * </pre>
   *
   * to
   * <pre>
   *   start -(declaration copy)-> tmpNode -> -(e)-> rest
   * </pre>
   */
  private void insertCopyingNode(
      CFANode pEntryNode,
      CVariableDeclaration pParam,
      MutableCFA pMutableCFA) {

    FileLocation loc = pParam.getFileLocation();

    // type var__orig = var;
    CVariableDeclaration freshDeclaration =
        new CVariableDeclaration(
            loc,
            pParam.isGlobal(),
            pParam.getCStorageClass(),
            pParam.getType(),
            pParam.getName() + postfix,
            pParam.getOrigName() + postfix,
            pParam.getQualifiedName() + postfix,
            new CInitializerExpression(
                loc,
                new CIdExpression(loc, pParam)
            )
        );

    CFANode tmp = new CFANode(pEntryNode.getFunctionName());
    pMutableCFA.addNode(tmp);

    assert pEntryNode.getNumLeavingEdges() == 1;

    // Leave first edge the same. // todo : introduce a new variable.
    pEntryNode = pEntryNode.getLeavingEdge(0).getSuccessor();
    assert pEntryNode.getNumLeavingEdges() == 1;

    CFAEdge leavingEdge = pEntryNode.getLeavingEdge(0);

    CFANode rest = leavingEdge.getSuccessor();

    pEntryNode.removeLeavingEdge(leavingEdge);
    rest.removeEnteringEdge(leavingEdge);

    CDeclarationEdge declarationEdge = new CDeclarationEdge(
        freshDeclaration.toASTString(),
        loc,
        pEntryNode,
        tmp,
        freshDeclaration);

    pEntryNode.addLeavingEdge(declarationEdge);
    tmp.addEnteringEdge(declarationEdge);

    FunctionCloner cloner = new FunctionCloner(
        pEntryNode.getFunctionName(), pEntryNode.getFunctionName(), true);

    CFAEdge clonedLeavingEdge = cloner.cloneEdge(
        leavingEdge, tmp, rest);

    tmp.addLeavingEdge(clonedLeavingEdge);
    rest.addEnteringEdge(clonedLeavingEdge);
  }

  private void populateAssignedVars(MutableCFA pMutableCFA) {

    // TODO: do not recalculate this information.
    for (FunctionEntryNode node : pMutableCFA.getAllFunctionHeads()) {
      assignedVars.putAll(node.getFunctionName(),
          extractAssignedVariables(pMutableCFA.getFunctionNodes(node.getFunctionName())));
    }

    // Fixpoint loop to account for nesting.
    while (true) {

      boolean changed = false;

      for (FunctionEntryNode node : pMutableCFA.getAllFunctionHeads()) {
        FunctionCallCollector collector = new FunctionCallCollector();
        CFATraversal.dfs().traverseOnce(node, collector);
        for (AStatementEdge edge : collector.getFunctionCalls()) {
          AFunctionCall call = (AFunctionCall) edge.getStatement();

          String calledFunc = call.getFunctionCallExpression().getDeclaration().getName();

          changed |= assignedVars.putAll(node.getFunctionName(),
              assignedVars.get(calledFunc));
        }
      }

      if (!changed) {
        break;
      }
    }
  }

  private Set<CIdExpression> extractAssignedVariables(Collection<CFANode> nodes) {
    Set<CIdExpression> assignedVariables = new HashSet<>();

    for (CFANode node : nodes) {
      for (int i=0; i<node.getNumLeavingEdges(); i++) {
        CFAEdge leavingEdge = node.getLeavingEdge(i);
        if (leavingEdge instanceof AStatementEdge) {
          AStatementEdge edge = (AStatementEdge) leavingEdge;
          if (!(edge.getStatement() instanceof CAssignment)) {
            continue;
          }

          CAssignment assignment = (CAssignment) edge.getStatement();
          assignedVariables.addAll(
              CFAUtils.getIdExpressionsOfExpression(assignment.getLeftHandSide())
                  .toSet());
        }
      }
    }

    return assignedVariables;
  }
}
