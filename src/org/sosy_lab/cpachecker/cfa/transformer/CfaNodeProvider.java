// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * The creation of some CFA nodes/edges depends on other nodes (e.g., to create a new CFA edge, we
 * need its endpoints). {@link CfaNodeProvider} implementations resolve those dependencies.
 *
 * <p>CFA node/edge transformers use {@link CfaNodeProvider} the following way: given the nodes the
 * original node/edge depends on, return the corresponding transformed nodes.
 *
 * <p>A {@link CfaNodeProvider} must always return the same node for a specific given node.
 */
@FunctionalInterface
public interface CfaNodeProvider {

  /**
   * A {@link CfaNodeProvider} that throws an {@link UnsupportedOperationException} when it's used.
   *
   * <p>This dummy implementation can be used where a {@link CfaNodeProvider} is required, but it's
   * guaranteed that it's never used (e.g., if during node transformation only nodes are transformed
   * that don't require other transformed nodes for their construction).
   */
  public static final CfaNodeProvider UNSUPPORTED =
      node -> {
        throw new UnsupportedOperationException("CFA node provider is unsupported");
      };

  /**
   * Returns the substitute for the specified CFA node.
   *
   * <p>This method always returns the same node for a specific given node.
   *
   * @param pNode the CFA node to get the substitute for
   * @return the substitute for the specified CFA node
   * @throws NullPointerException if {@code pNode == null}
   */
  CFANode get(CFANode pNode);
}
