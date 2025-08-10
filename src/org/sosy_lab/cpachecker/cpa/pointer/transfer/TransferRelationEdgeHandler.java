// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public sealed interface TransferRelationEdgeHandler<T extends CFAEdge>
    permits AssumeEdgeHandler,
        BlankEdgeHandler,
        CallToReturnEdgeHandler,
        DeclarationEdgeHandler,
        FunctionCallEdgeHandler,
        FunctionReturnEdgeHandler,
        ReturnStatementEdgeHandler,
        StatementEdgeHandler {

  PointerAnalysisState handleEdge(PointerAnalysisState pState, T pCfaEdge)
      throws CPATransferException;
}
