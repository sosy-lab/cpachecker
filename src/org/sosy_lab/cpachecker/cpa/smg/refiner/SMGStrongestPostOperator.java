/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg.SMGPredicateManager;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class SMGStrongestPostOperator {

  private final SMGTransferRelation transfer;

  private SMGStrongestPostOperator(SMGTransferRelation pTransferRelation) {
    transfer = pTransferRelation;
  }

  public static SMGStrongestPostOperator getSMGStrongestPostOperatorForCEX(LogManager pLogger,
      Configuration pConfig, CFA pCfa, SMGPredicateManager pSMGPredicateManager, BlockOperator pBlockOperator) throws InvalidConfigurationException {
    SMGTransferRelation transfer =
        SMGTransferRelation.createTransferRelationForCEX(pConfig, pLogger, pCfa.getMachineModel(), pSMGPredicateManager, pBlockOperator);
    return new SMGStrongestPostOperator(transfer);
  }

  public Collection<SMGState> getStrongestPost(SMGState pOrigin, Precision pPrecision,
      CFAEdge pOperation)
          throws CPAException, InterruptedException {

    Collection<SMGState> start = ImmutableList.of(pOrigin);

    return getStrongestPost(start, pPrecision, pOperation);
  }

  public Collection<SMGState> getStrongestPost(Collection<SMGState> pStates,
      Precision pPrecision,
      CFAEdge pOperation) throws CPATransferException, InterruptedException {

    List<AbstractState> result = new ArrayList<>();

    for (SMGState state : pStates) {
      Collection<? extends AbstractState> successors =
          transfer.getAbstractSuccessorsForEdge(state, pPrecision, pOperation);
      result.addAll(successors);
    }

    return FluentIterable.from(result).transform(new Function<AbstractState, SMGState>() {

      @Override
      public SMGState apply(AbstractState pState) {
        return (SMGState) pState;
      }
    }).toList();
  }

  public static SMGStrongestPostOperator getSMGStrongestPostOperatorForInterpolation(
      LogManager pLogger, Configuration pConfig, CFA pCfa, SMGPredicateManager pSMGPredicateManager,
      BlockOperator pBlockOperator) throws InvalidConfigurationException {

    SMGTransferRelation transferRelation = SMGTransferRelation
        .createTransferRelationForInterpolation(pConfig, pLogger, pCfa.getMachineModel(),
            pSMGPredicateManager, pBlockOperator);
    return new SMGStrongestPostOperator(transferRelation);
  }
}