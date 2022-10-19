// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * A {@code CfaNodeSubstitution} instance returns the substitute for a specified CFA node.
 *
 * <p>During CFA node and edge transformation, new transformed nodes and edges are created that
 * require other transformed nodes and edges for their construction (e.g., to construct a new {@link
 * FunctionEntryNode}, a {@link FunctionExitNode} is required). Instances of {@code
 * CfaNodeSubstitution} are used to provide these transformed nodes (e.g., they provide the
 * transformed {@code FunctionExitNode} for a given {@code FunctionExitNode}).
 *
 * <p>To implement a {@code CfaNodeSubstitution}, an implementation for {@link
 * CfaNodeSubstitution#get(CFANode)} must be provided. The implementation must guarantee that a
 * {@code CfaNodeSubstitution} instance always returns the same substitute for the same given node.
 */
@FunctionalInterface
public interface CfaNodeSubstitution {

  /**
   * A {@code CfaNodeSubstitution} instance that throws an {@link UnsupportedOperationException}
   * when {@link CfaNodeSubstitution#get(CFANode)} is called.
   *
   * <p>This can be used where a {@code CfaNodeSubstitution} is required, but it's guaranteed that
   * {@link CfaNodeSubstitution#get(CFANode)} is not going to get called. This makes it explicit
   * that a {@code CfaNodeSubstitution} instance is not necessary and never going to be used (e.g.,
   * if during node transformation only nodes are transformed that don't require other transformed
   * nodes for their construction).
   */
  public static final CfaNodeSubstitution UNSUPPORTED =
      node -> {
        throw new UnsupportedOperationException("node substitution is unsupported");
      };

  /**
   * Returns the substitute for the specified CFA node.
   *
   * <p>It's guaranteed that a {@code CfaNodeSubstitution} instance always returns the same
   * substitute for the same given node.
   *
   * @param pNode the CFA node to get the substitute for
   * @return the substitute for the specified CFA node
   * @throws NullPointerException if {@code pNode == null}
   */
  CFANode get(CFANode pNode);
}
