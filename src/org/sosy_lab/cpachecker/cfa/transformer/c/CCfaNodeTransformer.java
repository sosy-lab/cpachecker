// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeProvider;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeTransformer;

/** {@link CfaNodeTransformer} for CFA nodes of C program CFAs. */
public interface CCfaNodeTransformer extends CfaNodeTransformer {

  /**
   * A {@link CCfaNodeTransformer} that creates new CFA nodes that resemble the given nodes as
   * closely as possible.
   */
  public static final CCfaNodeTransformer CLONER = forSubstitutions();

  /**
   * Creates a new {@link CCfaNodeTransformer} that performs the specified AST node substitutions on
   * all nodes.
   *
   * @param pNodeAstSubstitutions the AST node substitutions to apply in the order they are
   *     specified
   * @return a new {@link CCfaNodeTransformer} that performs the specified AST node substitutions on
   *     all nodes
   * @throws NullPointerException if any parameter is {@code null} or if {@code
   *     pNodeAstSubstitutions} has an element that is {@code null}
   */
  public static CCfaNodeTransformer forSubstitutions(
      CCfaNodeAstSubstitution... pNodeAstSubstitutions) {
    return new CCfaNodeTransformer() {

      private final ImmutableList<CCfaNodeAstSubstitution> nodeAstSubstitutions =
          ImmutableList.copyOf(pNodeAstSubstitutions);

      private CFunctionDeclaration applyNodeAstSubstitutions(
          CFANode pNode, CFunctionDeclaration pFunction) {
        CFunctionDeclaration function = pFunction;
        for (CCfaNodeAstSubstitution nodeAstSubstitution : nodeAstSubstitutions) {
          function = checkNotNull(nodeAstSubstitution.apply(pNode, function));
        }

        return function;
      }

      private Optional<CVariableDeclaration> applyNodeAstSubstitutions(
          CFunctionEntryNode pFunctionEntryNode, Optional<CVariableDeclaration> pReturnVariable) {
        Optional<CVariableDeclaration> returnVariable = pReturnVariable;
        for (CCfaNodeAstSubstitution nodeAstSubstitution : nodeAstSubstitutions) {
          returnVariable =
              checkNotNull(nodeAstSubstitution.apply(pFunctionEntryNode, returnVariable));
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
          CFunctionEntryNode pOldNode, CfaNetwork pCfaNetwork, CfaNodeProvider pNodeProvider) {
        @Nullable FunctionExitNode oldExitNode =
            pCfaNetwork.functionExitNode(pOldNode).orElse(null);
        @Nullable FunctionExitNode newExitNode =
            oldExitNode != null ? (FunctionExitNode) pNodeProvider.get(oldExitNode) : null;

        Optional<CVariableDeclaration> newReturnVariable =
            applyNodeAstSubstitutions(pOldNode, pOldNode.getReturnVariable());

        CFunctionEntryNode newEntryNode =
            new CFunctionEntryNode(
                pOldNode.getFileLocation(),
                newFunctionDeclaration(pOldNode),
                newExitNode,
                newReturnVariable);
        if (newExitNode != null) {
          newExitNode.setEntryNode(newEntryNode);
        }

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
          CFANode pOldNode, CfaNetwork pCfaNetwork, CfaNodeProvider pNodeProvider) {
        if (pOldNode instanceof CFALabelNode) {
          return newCfaLabelNode((CFALabelNode) pOldNode);
        } else if (pOldNode instanceof CFunctionEntryNode) {
          return newCFunctionEntryNode((CFunctionEntryNode) pOldNode, pCfaNetwork, pNodeProvider);
        } else if (pOldNode instanceof FunctionExitNode) {
          return newFunctionExitNode((FunctionExitNode) pOldNode);
        } else if (pOldNode instanceof CFATerminationNode) {
          return newCfaTerminationNode((CFATerminationNode) pOldNode);
        } else {
          return newCfaNode(pOldNode);
        }
      }
    };
  }
}
