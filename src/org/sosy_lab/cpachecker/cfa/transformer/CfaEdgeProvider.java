// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * A {@code CfaEdgeProvider} instance returns the substitute for a given CFA edge.
 *
 * <p>During CFA edge transformation, new transformed edges are created that require other
 * transformed edges for their construction (e.g., to construct a new {@link FunctionCallEdge}, a
 * {@link FunctionSummaryEdge} is required). Instances of {@code CfaEdgeProvider} are used to
 * provide these transformed edges (e.g., they provide the transformed {@code FunctionSummaryEdge}
 * for a given {@code FunctionSummaryEdge}).
 *
 * <p>To implement a {@code CfaEdgeProvider}, an implementation for {@link
 * CfaEdgeProvider#get(CFAEdge)} must be provided. The implementation must guarantee that a {@code
 * CfaEdgeProvider} instance always returns the same substitute for the same given edge.
 */
@FunctionalInterface
public interface CfaEdgeProvider {

  /**
   * A {@code CfaEdgeProvider} instance that throws an {@link UnsupportedOperationException} when
   * {@link CfaEdgeProvider#get(CFAEdge)} is called.
   *
   * <p>This can be used where a {@code CfaEdgeProvider} is required, but it's guaranteed that
   * {@link CfaEdgeProvider#get(CFAEdge)} is not going to get called. This makes it explicit that a
   * {@code CfaEdgeProvider} instance is not necessary and never going to be used (e.g., if during
   * edge transformation only edges are transformed that don't require other transformed edges for
   * their construction).
   */
  public static final CfaEdgeProvider UNSUPPORTED =
      edge -> {
        throw new UnsupportedOperationException("CFA edge provider is unsupported");
      };

  /**
   * Returns the substitute for the specified CFA edge.
   *
   * <p>It's guaranteed that a {@code CfaEdgeProvider} instance always returns the same substitute
   * for the same given edge.
   *
   * @param pEdge the CFA edge to get the substitute for
   * @return the substitute for the specified CFA edge
   * @throws NullPointerException if {@code pEdge == null}
   */
  CFAEdge get(CFAEdge pEdge);
}
