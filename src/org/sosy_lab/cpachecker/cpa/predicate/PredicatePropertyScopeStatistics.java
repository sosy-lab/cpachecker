/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.predicate;


import static org.sosy_lab.cpachecker.cpa.predicate.PredicatePropertyScopeUtil.asNonTrueAbstractionState;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePropertyScopeUtil.FormulaVariableResult;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PredicatePropertyScopeStatistics extends AbstractStatistics {

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfMgr;
  private final BlockOperator blk;
  private final RegionManager regionManager;
  private final AbstractionManager abstractionManager;
  private final PredicateAbstractionManager predicateManager;
  private final PredicateAbstractDomain domain;
  private final MergeOperator merge;
  private final PredicateTransferRelation transfer;
  private final PredicatePrecisionAdjustment predPrec;

  public PredicatePropertyScopeStatistics(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      Solver pSolver,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfMgr,
      BlockOperator pBlk,
      RegionManager pRegionManager,
      AbstractionManager pAbstractionManager,
      PredicateAbstractionManager pPredicateManager,
      PredicateAbstractDomain pDomain,
      MergeOperator pMerge,
      PredicateTransferRelation pTransfer,
      PredicatePrecisionAdjustment pPredPrec) {

    config = pConfig;
    logger = pLogger;
    cfa = pCfa;
    solver = pSolver;
    fmgr = pFmgr;
    pfMgr = pPfMgr;
    blk = pBlk;
    regionManager = pRegionManager;
    abstractionManager = pAbstractionManager;
    predicateManager = pPredicateManager;
    domain = pDomain;
    merge = pMerge;
    transfer = pTransfer;
    predPrec = pPredPrec;
  }

  private static Optional<String> computeNewEntryFunction(ReachedSet reached) {
    List<String> longestPrefix = null;
    for (AbstractState absSt : reached) {
      CallstackState csSt = extractStateByType(absSt, CallstackState.class);
      if (asNonTrueAbstractionState(absSt).isPresent()) {
        if (longestPrefix == null) {
          longestPrefix = getStack(csSt);
        } else {
          longestPrefix = longestPrefixOf(longestPrefix, getStack(csSt));
        }
      }
    }

    return longestPrefix == null ? Optional.empty()
                                 : Optional.of(longestPrefix.get(longestPrefix.size() - 1));
  }

  private static Optional<Long> computeDepthOfHighestNonTrueAbstractionInCallstack(ReachedSet
                                                                                       reached) {
    long depth = Long.MAX_VALUE;
    for (AbstractState absSt : reached) {
      CallstackState csSt = extractStateByType(absSt, CallstackState.class);
      if (asNonTrueAbstractionState(absSt).isPresent()) {
        depth = Math.min(csSt.getDepth(), depth);
      }
    }

    return depth != Long.MAX_VALUE ? Optional.of(depth) : Optional.empty();
  }

  private static <T> List<T> longestPrefixOf(List<T> list1, List<T> list2) {
    int minlen = Math.min(list1.size(), list2.size());
    ArrayList<T> newList = Lists.newArrayList();
    for (int i = 0; i < minlen; i++) {
      T elem = list1.get(i);
      if (elem.equals(list2.get(i))) {
        newList.add(elem);
      } else {
        return newList;
      }
    }
    return newList;
  }

  private static List<String> getStack(CallstackState pState) {
    final List<String> stack = new ArrayList<>();
    CallstackState state = pState;
    while (state != null) {
      stack.add(state.getCurrentFunction());
      state = state.getPreviousState();
    }
    return Lists.reverse(stack);
  }

  private static Set<FormulaVariableResult> getGlobalVariablesInAbstractionFormulas(
      ReachedSet reached, FormulaManagerView fmgr) {
    return reached.asCollection().stream()
        .map(pAS -> PredicatePropertyScopeUtil.formulaVariableSplitStream(pAS, fmgr)
            .filter(pResult -> pResult.function == null).map(pResult -> pResult)
        ).flatMap(pStringStream -> pStringStream).distinct().collect(Collectors.toSet());
  }
  private static long countNonTrueAbstractionStates(ReachedSet pReached) {
    return pReached.asCollection().stream()
        .filter(as -> asNonTrueAbstractionState(as).isPresent())
        .count();
  }

  private static double avgGlobalRatioInAbsFormulaAtoms(ReachedSet reached,
                                                        FormulaManagerView fmgr) {
    long globalAtoms = 0;
    long atomCount = 0;
    for (AbstractState st : reached) {
      Optional<PredicateAbstractState> oPredState =
          asNonTrueAbstractionState(st);
      if (oPredState.isPresent()) {
        AbstractionFormula absFormula = oPredState.get().getAbstractionFormula();
        ImmutableSet<BooleanFormula> atoms =
            fmgr.extractAtoms(absFormula.asInstantiatedFormula(), false);

        for (BooleanFormula atom : atoms) {
          atomCount += 1;
          boolean globalInAtom = fmgr.extractVariableNames(atom).stream()
              .map(PredicatePropertyScopeUtil::splitFormulaVariable)
              .anyMatch(fvar -> fvar.function == null);
          if (globalInAtom) {
            globalAtoms += 1;
          }
        }
      }
    }
    return globalAtoms / (double) atomCount;
  }

  private static Set<String> collectFunctionsWithNonTrueAbsState(ReachedSet pReachedSet) {
    return pReachedSet.asCollection().stream()
        .filter(st -> asNonTrueAbstractionState(st).isPresent())
        .map(st -> extractStateByType(st, CallstackState.class).getCurrentFunction())
        .collect(Collectors.toSet());
  }

  @Override
  public void printStatistics(
      PrintStream pOut, Result pResult, ReachedSet pReached) {

    Set<String> functionsInScope = collectFunctionsWithNonTrueAbsState(pReached);
    

    addKeyValueStatistic("Functions with non-true abstraction", functionsInScope);

    addKeyValueStatistic("Non-true abstraction function count", functionsInScope.size());

    String newEntry = computeNewEntryFunction(pReached).orElse("<unknown>");
    addKeyValueStatistic("New entry Function Candidate", newEntry);

    Set<FormulaVariableResult> globalVariablesInAbstractionFormulas =
        getGlobalVariablesInAbstractionFormulas(pReached, fmgr);
    addKeyValueStatistic("Number of global variables in abstraction formulas",
        globalVariablesInAbstractionFormulas.size());

    addKeyValueStatistic("Number of non-true abstraction states",
        countNonTrueAbstractionStates(pReached));

    Optional<Long> highestStack = computeDepthOfHighestNonTrueAbstractionInCallstack(pReached);
    String highestStackKey = "Highest point in callstack with non-true abstraction formula";
    if (highestStack.isPresent()) {
      addKeyValueStatistic(highestStackKey, highestStack.get());
    } else {
      addKeyValueStatistic(highestStackKey, "<unknown>");
    }

    double globalRatAtoms = avgGlobalRatioInAbsFormulaAtoms(pReached, fmgr);
    addKeyValueStatistic("Average ratio of formula atoms with global variable",
        Double.isNaN(globalRatAtoms) ? "<unknown>" : globalRatAtoms);


     super.printStatistics(pOut, pResult, pReached);
  }

  @Override
  public String getName() {
    return "Predicate Property Scope";
  }
}
