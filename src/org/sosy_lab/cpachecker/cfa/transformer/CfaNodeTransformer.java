// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * A {@link CfaNodeTransformer} returns for a given CFA node (the original node) a new node that may
 * differ from the original node.
 *
 * <p>A {@link CfaNodeTransformer} must always create a new node every time its {@link
 * CfaNodeTransformer#transform(CFANode, CfaNetwork, CfaNodeProvider) transform method} is called.
 */
@FunctionalInterface
public interface CfaNodeTransformer {

  /**
   * Returns a new transformed CFA node for the specified CFA node.
   *
   * <p>Every time this method is called, a new transformed node is created.
   *
   * @param pNode the CFA node to get a new transformed node for
   * @param pCfaNetwork The {@link CfaNetwork} the specified node is a part of. The {@link
   *     CfaNetwork} is used to determine connections between nodes (e.g., the {@link
   *     FunctionExitNode} for a {@link FunctionEntryNode}). If the construction of a transformed
   *     node doesn't depend on other nodes, it's guaranteed that the specified {@link CfaNetwork}
   *     is not used, so a dummy {@link CfaNetwork} can be specified.
   * @param pNodeProvider The creation of some nodes depends on other nodes (e.g, we need to know
   *     the {@link FunctionExitNode} to create a {@link FunctionEntryNode}). The specified {@link
   *     CfaNodeProvider} resolves those dependencies. Given the nodes the specified node depends
   *     on, return the corresponding transformed nodes. If the construction of a transformed node
   *     does not depend on any other nodes, it's guaranteed that the specified provider is not
   *     used, so a dummy provider can be specified.
   * @return a new transformed CFA node for the specified CFA node (must not return {@code null})
   * @throws NullPointerException if any parameter is {@code null}
   */
  CFANode transform(CFANode pNode, CfaNetwork pCfaNetwork, CfaNodeProvider pNodeProvider);
}
