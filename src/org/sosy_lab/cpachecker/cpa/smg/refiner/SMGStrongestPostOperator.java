/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGPredicateManager;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;


public class SMGStrongestPostOperator {

  private final SMGTransferRelation transfer;

  private SMGStrongestPostOperator(SMGTransferRelation pTransferRelation) {
    transfer = pTransferRelation;
  }

  public static SMGStrongestPostOperator getSMGStrongestPostOperatorForCEX(LogManager pLogger,
      CFA pCfa, SMGPredicateManager pSMGPredicateManager, BlockOperator pBlockOperator, SMGOptions pOptions) {
    SMGTransferRelation transfer =
        SMGTransferRelation.createTransferRelationForCEX(pLogger, pCfa.getMachineModel(),
            pSMGPredicateManager, pBlockOperator, pOptions);
    return new SMGStrongestPostOperator(transfer);
  }

  public Collection<SMGState> getStrongestPost(
      SMGState pOrigin, Precision pPrecision, CFAEdge pOperation) throws CPAException {
    Collection<SMGState> start = ImmutableList.of(pOrigin);
    return getStrongestPost(start, pPrecision, pOperation);
  }

  public Collection<SMGState> getStrongestPost(
      Collection<SMGState> pStates, Precision pPrecision, CFAEdge pOperation)
      throws CPATransferException {
    List<SMGState> result = new ArrayList<>();
    for (SMGState state : pStates) {
      result.addAll(transfer.getAbstractSuccessorsForEdge(state, pPrecision, pOperation));
    }
    return result;
  }

  public static SMGStrongestPostOperator getSMGStrongestPostOperatorForInterpolation(
      LogManager pLogger, CFA pCfa, SMGPredicateManager pSMGPredicateManager,
      BlockOperator pBlockOperator, SMGOptions pOptions) {

    SMGTransferRelation transferRelation = SMGTransferRelation
        .createTransferRelationForInterpolation(pLogger, pCfa.getMachineModel(),
            pSMGPredicateManager, pBlockOperator, pOptions);
    return new SMGStrongestPostOperator(transferRelation);
  }
}