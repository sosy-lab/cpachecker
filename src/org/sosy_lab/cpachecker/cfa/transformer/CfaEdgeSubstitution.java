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
 * A {@code CfaEdgeSubstitution} instance returns the substitute for a specified CFA edge.
 *
 * <p>During CFA edge transformation, new transformed edges are created that require other
 * transformed edges for their construction (e.g., to construct a new {@link FunctionCallEdge}, a
 * {@link FunctionSummaryEdge} is required). Instances of {@code CfaEdgeSubstitution} are used to
 * provide these transformed edges (e.g., they provide the transformed {@code FunctionSummaryEdge}
 * for a given {@code FunctionSummaryEdge}).
 *
 * <p>To implement a {@code CfaEdgeSubstitution}, an implementation for {@link
 * CfaEdgeSubstitution#get(CFAEdge)} must be provided. The implementation must guarantee that a
 * {@code CfaEdgeSubstitution} instance always returns the same substitute for the same given edge.
 */
@FunctionalInterface
public interface CfaEdgeSubstitution {

  /**
   * Returns the substitute for the specified CFA edge.
   *
   * <p>It's guaranteed that a {@code CfaEdgeSubstitution} instance always returns the same
   * substitute for the same given edge.
   *
   * @param pEdge the CFA edge to get the substitute for
   * @return the substitute for the specified CFA edge
   * @throws NullPointerException if {@code pEdge == null}
   */
  CFAEdge get(CFAEdge pEdge);
}
