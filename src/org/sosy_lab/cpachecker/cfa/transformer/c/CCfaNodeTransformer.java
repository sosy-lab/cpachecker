// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeSubstitution;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeTransformer;

/** {@link CfaNodeTransformer} for CFA nodes that are contained in CFAs of C programs. */
public final class CCfaNodeTransformer implements CfaNodeTransformer {

  public static final CfaNodeTransformer CLONER = new CCfaNodeTransformer(ImmutableList.of());

  private final ImmutableList<CCfaNodeAstSubstitution> nodeAstSubstitutions;

  private CCfaNodeTransformer(ImmutableList<CCfaNodeAstSubstitution> pNodeAstSubstitutions) {
    nodeAstSubstitutions = pNodeAstSubstitutions;
  }

  public static CfaNodeTransformer withSubstitutions(
      List<CCfaNodeAstSubstitution> pNodeAstSubstitutions) {
    return new CCfaNodeTransformer(ImmutableList.copyOf(pNodeAstSubstitutions));
  }

  private CFunctionDeclaration applyNodeAstSubstitutions(
      CFANode pNode, CFunctionDeclaration pFunction) {

    CFunctionDeclaration function = pFunction;
    for (CCfaNodeAstSubstitution nodeAstSubstitution : nodeAstSubstitutions) {
      function = nodeAstSubstitution.apply(pNode, function);
    }

    return function;
  }

  private Optional<CVariableDeclaration> applyNodeAstSubstitutions(
      CFunctionEntryNode pFunctionEntryNode, Optional<CVariableDeclaration> pReturnVariable) {

    Optional<CVariableDeclaration> returnVariable = pReturnVariable;
    for (CCfaNodeAstSubstitution nodeAstSubstitution : nodeAstSubstitutions) {
      returnVariable = nodeAstSubstitution.apply(pFunctionEntryNode, returnVariable);
    }

    return returnVariable;
  }

  private CFunctionDeclaration newFunctionDeclaration(CFANode pOldNode) {
    return applyNodeAstSubstitutions(pOldNode, (CFunctionDeclaration) pOldNode.getFunction());
  }

  private CFALabelNode newCfaLabelNode(CFALabelNode pOldNode) {
    return new CFALabelNode(newFunctionDeclaration(pOldNode), pOldNode.getLabel());
  }

  private CFunctionEntryNode newCFunctionEntryNode(
      CFunctionEntryNode pOldNode, CfaNetwork pCfaNetwork, CfaNodeSubstitution pNodeSubstitution) {

    FunctionExitNode oldExitNode =
        pCfaNetwork.functionExitNode(pOldNode).orElse(pOldNode.getExitNode());
    FunctionExitNode newExitNode = (FunctionExitNode) pNodeSubstitution.get(oldExitNode);

    Optional<CVariableDeclaration> newReturnVariable =
        applyNodeAstSubstitutions(pOldNode, pOldNode.getReturnVariable());

    CFunctionEntryNode newEntryNode =
        new CFunctionEntryNode(
            pOldNode.getFileLocation(),
            newFunctionDeclaration(pOldNode),
            newExitNode,
            newReturnVariable);
    newExitNode.setEntryNode(newEntryNode);

    return newEntryNode;
  }

  private FunctionExitNode newFunctionExitNode(FunctionExitNode pOldNode) {
    return new FunctionExitNode(newFunctionDeclaration(pOldNode));
  }

  private CFATerminationNode newCfaTerminationNode(CFATerminationNode pOldNode) {
    return new CFATerminationNode(newFunctionDeclaration(pOldNode));
  }

  private CFANode newCfaNode(CFANode pOldNode) {
    return new CFANode(newFunctionDeclaration(pOldNode));
  }

  @Override
  public CFANode transform(
      CFANode pOldNode, CfaNetwork pCfaNetwork, CfaNodeSubstitution pNodeSubstitution) {

    if (pOldNode instanceof CFALabelNode) {
      return newCfaLabelNode((CFALabelNode) pOldNode);
    } else if (pOldNode instanceof CFunctionEntryNode) {
      return newCFunctionEntryNode((CFunctionEntryNode) pOldNode, pCfaNetwork, pNodeSubstitution);
    } else if (pOldNode instanceof FunctionExitNode) {
      return newFunctionExitNode((FunctionExitNode) pOldNode);
    } else if (pOldNode instanceof CFATerminationNode) {
      return newCfaTerminationNode((CFATerminationNode) pOldNode);
    } else {
      return newCfaNode(pOldNode);
    }
  }
}
