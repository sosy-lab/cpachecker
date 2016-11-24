/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public class PolicyTransferRelation extends SingleEdgeTransferRelation {

  private final PathFormulaManager pfmgr;
  private final StateFormulaConversionManager stateFormulaConversionManager;

  PolicyTransferRelation(
      PathFormulaManager pPfmgr, StateFormulaConversionManager pStateFormulaConversionManager) {
    pfmgr = pPfmgr;
    stateFormulaConversionManager = pStateFormulaConversionManager;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    PolicyState oldState = (PolicyState) state;
    CFANode node = cfaEdge.getSuccessor();
    PolicyIntermediateState iOldState;

    if (oldState.isAbstract()) {
      iOldState =
          stateFormulaConversionManager.abstractStateToIntermediate(oldState.asAbstracted(), false);
    } else {
      iOldState = oldState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), cfaEdge);
    PolicyIntermediateState out =
        PolicyIntermediateState.of(node, outPath, iOldState.getBackpointerState());

    return Collections.singleton(out);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    PolicyIntermediateState pState = ((PolicyState) state).asIntermediate();
    // Collect assumptions.
    FluentIterable<CExpression> assumptions =
        FluentIterable.from(otherStates)
            .filter(AbstractStateWithAssumptions.class)
            .transformAndConcat(AbstractStateWithAssumptions::getAssumptions)
            .filter(CExpression.class);

    if (assumptions.isEmpty()) {

      // No changes required.
      return Collections.singleton(pState);
    }

    PathFormula pf = pState.getPathFormula();
    for (CExpression assumption : assumptions) {
      pf = pfmgr.makeAnd(pf, assumption);
    }

    return Collections.singleton(pState.withPathFormula(pf));
  }
}
