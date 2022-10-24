// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
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

/** {@link CfaNodeTransformer} for CFA nodes that are contained in C program CFAs. */
public final class CCfaNodeTransformer implements CfaNodeTransformer {

  /**
   * A {@link CfaNodeTransformer} that creates new CFA nodes that resemble the given nodes as
   * closely as possible.
   */
  public static final CCfaNodeTransformer CLONER = new CCfaNodeTransformer(ImmutableList.of());

  private final ImmutableList<CCfaNodeAstSubstitution> nodeAstSubstitutions;

  private CCfaNodeTransformer(ImmutableList<CCfaNodeAstSubstitution> pNodeAstSubstitutions) {
    nodeAstSubstitutions = pNodeAstSubstitutions;
  }

  /**
   * Creates a new {@link CCfaNodeTransformer} that performs the specified AST node substitutions on
   * all nodes.
   *
   * @param pNodeAstSubstitution the first AST node substitution to apply
   * @param pNodeAstSubstitutions all other AST node substitutions to apply in the order they are
   *     specified
   * @return a new {@link CCfaNodeTransformer} that performs the specified AST node substitutions on
   *     all nodes
   * @throws NullPointerException if any parameter is {@code null} or if {@code
   *     pNodeAstSubstitutions} has an element that is {@code null}
   */
  public static CCfaNodeTransformer withSubstitutions(
      CCfaNodeAstSubstitution pNodeAstSubstitution,
      CCfaNodeAstSubstitution... pNodeAstSubstitutions) {

    ImmutableList.Builder<CCfaNodeAstSubstitution> substitutionsBuilder =
        ImmutableList.builderWithExpectedSize(pNodeAstSubstitutions.length + 1);
    substitutionsBuilder.add(pNodeAstSubstitution);
    substitutionsBuilder.addAll(Arrays.asList(pNodeAstSubstitutions));

    return new CCfaNodeTransformer(substitutionsBuilder.build());
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
