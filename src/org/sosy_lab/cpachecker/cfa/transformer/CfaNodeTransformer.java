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
 * A {@code CfaNodeTransformer} instance returns a transformed CFA node for a specified CFA node.
 *
 * <p>To implement a {@code CfaNodeTransformer}, an implementation for {@link
 * CfaNodeTransformer#transform(CFANode, CfaNetwork, CfaNodeProvider)} must be provided. The
 * implementation must guarantee that every time the method is called, a new transformed node
 * instance is created.
 */
@FunctionalInterface
public interface CfaNodeTransformer {

  /**
   * Returns a transformed CFA node for the specified CFA node.
   *
   * <p>Every time this method is called, a new transformed node instance is created.
   *
   * @param pNode the CFA node to get a transformed node for
   * @param pCfa The CFA the specified node is a part of. The CFA is used to determine other nodes
   *     that are required for node transformation (e.g., if the {@link FunctionExitNode} of a
   *     {@link FunctionEntryNode} is required, it's determined using the specified CFA). If the
   *     construction of a transformed node does not require other nodes, it's guaranteed that the
   *     specified CFA is not used, so a dummy CFA can be used.
   * @param pNodeProvider If the construction of a transformed node requires other transformed
   *     nodes, the specified node provider is used (e.g., the construction of a transformed {@code
   *     FunctionEntryNode} requires a transformed {@code FunctionExitNode}, so the {@code
   *     FunctionExitNode} of the {@code FunctionEntryNode} is determined using the specified CFA
   *     and the transformed {@code FunctionExitNode} is retrieved using the specified provider). If
   *     the construction of a transformed node does not require other nodes, it's guaranteed that
   *     the specified provider is not used, so a dummy provider can be used.
   * @return a transformed CFA node for the specified CFA node
   * @throws NullPointerException if any parameter is {@code null}
   */
  CFANode transform(CFANode pNode, CfaNetwork pCfa, CfaNodeProvider pNodeProvider);
}
