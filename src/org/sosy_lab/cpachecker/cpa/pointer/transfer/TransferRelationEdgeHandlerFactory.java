// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;

public class TransferRelationEdgeHandlerFactory {
  public static TransferRelationEdgeHandler<?> createEdgeHandler(
      CFAEdgeType pEdgeType,
      LogManager logger,
      PointerTransferOptions pOptions,
      AtomicInteger allocationCounter) {
    return switch (pEdgeType) {
      case DeclarationEdge -> new DeclarationEdgeHandler(pOptions);
      case StatementEdge -> new StatementEdgeHandler(logger, pOptions, allocationCounter);
      case CallToReturnEdge -> new CallToReturnEdgeHandler();
      case AssumeEdge -> new AssumeEdgeHandler(pOptions);
      case BlankEdge -> new BlankEdgeHandler();
      case FunctionCallEdge -> new FunctionCallEdgeHandler(pOptions);
      case FunctionReturnEdge -> new FunctionReturnEdgeHandler(logger, pOptions);
      case ReturnStatementEdge -> new ReturnStatementEdgeHandler(pOptions);
    };
  }
}
