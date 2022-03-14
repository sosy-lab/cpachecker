// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg.SMGExportDotOption;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGPredicateManager;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelationKind;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class SMGStrongestPostOperator {

  private final SMGTransferRelation transfer;

  SMGStrongestPostOperator(
      LogManager pLogger,
      CFA pCfa,
      SMGPredicateManager pSMGPredicateManager,
      SMGOptions pOptions,
      SMGTransferRelationKind pKind,
      ShutdownNotifier pShutdownNotifier) {
    transfer =
        new SMGTransferRelation(
            pLogger,
            pCfa.getMachineModel(),
            SMGExportDotOption.getNoExportInstance(),
            pKind,
            pSMGPredicateManager,
            pOptions,
            pShutdownNotifier);
  }

  public Collection<SMGState> getStrongestPost(
      Collection<SMGState> pStates, Precision pPrecision, CFAEdge pOperation)
      throws CPATransferException, InterruptedException {
    List<SMGState> result = new ArrayList<>();
    for (SMGState state : pStates) {
      result.addAll(transfer.getAbstractSuccessorsForEdge(state, pPrecision, pOperation));
    }
    return result;
  }
}
