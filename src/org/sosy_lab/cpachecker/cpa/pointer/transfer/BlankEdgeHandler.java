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
 * Handles no-operation edges such as comments or synthetic transitions. It leaves the pointer state
 * unchanged since they have no semantic effect.
 */
public final class BlankEdgeHandler implements TransferRelationEdgeHandler<AbstractCFAEdge> {
  @Override
  public PointerAnalysisState handleEdge(PointerAnalysisState pState, AbstractCFAEdge pCfaEdge) {
    return pState;
  }
}
