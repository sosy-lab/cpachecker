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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

public class TransferRelationEdgeHandlerFactory {
  public static TransferRelationEdgeHandler<?> createEdgeHandler(
      CFAEdgeType pEdgeType,
      LogManager logger,
      PointerTransferOptions pOptions,
      AtomicInteger allocationCounter,
      MachineModel machineModel) {
    return switch (pEdgeType) {
      case DeclarationEdge -> new DeclarationEdgeHandler(pOptions, machineModel);
      case StatementEdge ->
          new StatementEdgeHandler(logger, pOptions, allocationCounter, machineModel);
      case CallToReturnEdge -> new CallToReturnEdgeHandler();
      case AssumeEdge -> new AssumeEdgeHandler(pOptions, machineModel);
      case BlankEdge -> new BlankEdgeHandler();
      case FunctionCallEdge -> new FunctionCallEdgeHandler(pOptions, machineModel);
      case FunctionReturnEdge -> new FunctionReturnEdgeHandler(logger, pOptions, machineModel);
      case ReturnStatementEdge -> new ReturnStatementEdgeHandler(pOptions, machineModel);
    };
  }
}
