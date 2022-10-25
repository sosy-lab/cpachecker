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
 * The creation of some CFA edges depends on other edges (e.g, we need to know the {@link
 * FunctionSummaryEdge} to create a {@link FunctionCallEdge}). {@link CfaEdgeProvider}
 * implementations resolve those dependencies.
 *
 * <p>CFA edge transformers use {@link CfaEdgeProvider} the following way: Given the edges the
 * original edge depends on, return the corresponding transformed edges.
 *
 * <p>A {@link CfaEdgeProvider} must always return the same edge for a specific given edge.
 */
@FunctionalInterface
public interface CfaEdgeProvider {

  /**
   * A {@link CfaEdgeProvider} that throws an {@link UnsupportedOperationException} when it's used.
   *
   * <p>This dummy implementation can be used where a {@link CfaEdgeProvider} is required, but it's
   * guaranteed that it's never used (e.g., if during edge transformation only edges are transformed
   * that don't require other transformed edges for their construction).
   */
  public static final CfaEdgeProvider UNSUPPORTED =
      edge -> {
        throw new UnsupportedOperationException("CFA edge provider is unsupported");
      };

  /**
   * Returns the substitute for the specified CFA edge.
   *
   * <p>This method always returns the same edge for a specific given edge.
   *
   * @param pEdge the CFA edge to get the substitute for
   * @return the substitute for the specified CFA edge (must not return {@code null})
   * @throws NullPointerException if {@code pEdge == null}
   */
  CFAEdge get(CFAEdge pEdge);
}
