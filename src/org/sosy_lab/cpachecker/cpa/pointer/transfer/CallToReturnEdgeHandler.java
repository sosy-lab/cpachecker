// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;

/**
 * Handles edges skipping function bodies, e.g., for external functions without a CFA. It preserves
 * the current pointer state because no explicit points-to modifications are modeled.
 */
public final class CallToReturnEdgeHandler implements TransferRelationEdgeHandler<AbstractCFAEdge> {
  @Override
  public PointerAnalysisState handleEdge(PointerAnalysisState pState, AbstractCFAEdge pCfaEdge) {
    return pState;
  }
}
