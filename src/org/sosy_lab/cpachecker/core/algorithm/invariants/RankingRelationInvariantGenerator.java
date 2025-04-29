// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.StemAndLoop;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;

public class RankingRelationInvariantGenerator extends AbstractInvariantGenerator
    implements ReachedSetNotEqual {

  private ReachedSet reachedSet;
  private final AggregatedReachedSets aggregatedReachedSets;
  private final CFA cfa;
  private  StemAndLoop stemAndLoop;

  private LassoAnalysisResult synthesizeTerminationArgument;
  private CounterexampleInfo counterexampleInfo;
  private LassoBuilder lassoBuilder;

  public RankingRelationInvariantGenerator(AggregatedReachedSets pAggregatedReachedSets, CFA pCFA) {
    this.aggregatedReachedSets = pAggregatedReachedSets;
    this.cfa = pCFA;
  }

  @Override
  public void setReachedSet(ReachedSet pReachedSet) {
    this.reachedSet = pReachedSet;
  }

  protected Iterable<AbstractState> getReachedStates() {
    if (reachedSet == null) {

      return ImmutableList.of();
    } else {

      return reachedSet.asCollection();
    }
  }


  public void setStemAndLoop(StemAndLoop pStemAndLoop) {
    this.stemAndLoop = pStemAndLoop;
  }
  public void setSynthesizeTerminationArgument(LassoAnalysisResult pSynthesizeTerminationArgument) {
    this.synthesizeTerminationArgument = pSynthesizeTerminationArgument;
  }

  public void setLassoBuilder(LassoBuilder pLassoBuilder) {
    this.lassoBuilder = pLassoBuilder;
  }

  public void setCounterexampleInfo(CounterexampleInfo pCounterexampleInfo) {
    this.counterexampleInfo = pCounterexampleInfo;
  }


  @Override
  protected void startImpl(CFANode pInitialLocation) {}

  @Override
  public boolean isProgramSafe() {
    return false;
  }

  @Override
  public void cancel() {}

  @Override
  public InvariantSupplier getSupplier() throws CPAException, InterruptedException {
    return new InvariantSupplier() {

      @Override
      public BooleanFormula getInvariantFor(
          CFANode node,
          Optional<CallstackStateEqualsWrapper> callstackInformation,
          FormulaManagerView fmgr,
          PathFormulaManager pfmgr,
          PathFormula pContext)
          throws InterruptedException {

        ARGPath argPath = counterexampleInfo.getTargetPath();



        BooleanFormula loopFormula = stemAndLoop.getLoop();


        SSAMap loopInVars = stemAndLoop.getLoopInVars();

        RankingRelation rankingRelation = synthesizeTerminationArgument.getTerminationArgument();
        BooleanFormula loopInvariant = rankingRelation.asFormula();

        BooleanFormula rankingRelationInvariant;
        rankingRelationInvariant = fmgr.getBooleanFormulaManager().and(loopInvariant);
        return rankingRelationInvariant;
      }

    };
  }

  @Override
  public ExpressionTreeSupplier getExpressionTreeSupplier()
      throws CPAException, InterruptedException {
    return new ExpressionTreeInvariantSupplier(aggregatedReachedSets, cfa);
  }


}