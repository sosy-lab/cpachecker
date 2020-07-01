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
package org.sosy_lab.cpachecker.cpa.slab;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils.buildPathFormula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGLogger;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class SLABRefiner implements Refiner, StatisticsProvider {

  private final ARGBasedRefiner refiner;
  private SLABCPA slabCpa;
  private Solver solver;
  private ARGLogger argLogger;
  private boolean initialSliceDone = false;

  public SLABRefiner(ARGBasedRefiner pRefiner, SLABCPA pSlabCpa, Configuration config)
      throws InvalidConfigurationException {
    refiner = pRefiner;
    slabCpa = pSlabCpa;
    argLogger = new ARGLogger(config, slabCpa.getLogger());
    solver = slabCpa.getPredicateCpa().getSolver();

  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    SLABCPA argCpa = CPAs.retrieveCPA(pCpa, SLABCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(SlicingAbstractionsRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    RefinementStrategy strategy =
        new SlicingAbstractionsStrategy(predicateCpa, predicateCpa.getConfiguration());

    PredicateCPARefinerFactory factory = new PredicateCPARefinerFactory(pCpa);
    ARGBasedRefiner refiner =  factory.create(strategy);
    return new SLABRefiner(refiner, argCpa, predicateCpa.getConfiguration());
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    CounterexampleInfo counterexample = null;

    if (!initialSliceDone) {
      // get rid of states where the state formula is already unsatisfiable. This is just an
      // optimization and in accordance with the SLAB paper:
      removeInfeasibleStates(pReached);
      argLogger.log("in refinement after removeInfeasibleStates", pReached.asCollection());

      sliceEdges(from(pReached).transform(x -> (SLARGState) x).toList());
      argLogger.log("in refinement after sliceEdges", pReached.asCollection());

      initialSliceDone = true;
    }

    // TODO: Refactor CPAchecker to only use one kind of "Optional"!
    com.google.common.base.Optional<AbstractState> optionalTargetState;
    while (true) {

      optionalTargetState = from(pReached).firstMatch(x -> ((SLARGState) x).isTarget());
      if (optionalTargetState.isPresent()) {
        AbstractState targetState = optionalTargetState.get();
        ARGPath errorPath = ARGUtils.getShortestPathTo((ARGState) targetState);
        ARGReachedSet reached = new ARGReachedSet(pReached, slabCpa);
        assert errorPath != null;
        counterexample = refiner.performRefinementForPath(reached, errorPath);
        argLogger.log("in refinement after sliceEdges", pReached.asCollection());
        if (!counterexample.isSpurious()) {
          ((ARGState) targetState).addCounterexampleInformation(counterexample);
          return false;
        }
      } else {
        break;
      }
    }
    argLogger.log("after successful refinement", pReached.asCollection());
    return true;
  }

  private void sliceEdges(List<SLARGState> allStates) throws InterruptedException, CPAException {
    for (SLARGState parent : allStates) {
      List<SLARGState> toSlice = new ArrayList<>();
      for (ARGState argChild : parent.getChildren()) {
        SLARGState child = (SLARGState) argChild;
        boolean infeasible = checkEdge(parent, child);
        if (infeasible) {
          toSlice.add(child);
        }
      }
      for (SLARGState child : toSlice) {
        child.removeParent(parent);
      }
    }
  }

  /*
   * check edge whether it is infeasible. This is only needed in the beginning of the first refinement pass
   */
  private boolean checkEdge(SLARGState startState, SLARGState endState)
      throws InterruptedException, CPAException {
    assert startState.getChildren().contains(endState);
    EdgeSet edgeSet = startState.getEdgeSetToChild(endState);

    // Optimization (from SLAB paper): remove all edges going out of target states:
    if (startState.isTarget()) {
      edgeSet.clear();
      return true;
    }
    boolean infeasible = true;
    Iterator<CFAEdge> it = edgeSet.iterator();
    while (it.hasNext()) {
      CFAEdge cfaEdge = it.next();
      edgeSet.select(cfaEdge);
      if (isInfeasibleEdge(startState, endState)) {
        it.remove();
      } else {
        infeasible = false;
      }
    }

    return infeasible;
  }

  private boolean isInfeasibleEdge(SLARGState startState, SLARGState endState)
      throws InterruptedException, CPAException {

    SSAMap startSSAMap = SSAMap.emptySSAMap().withDefault(1);
    PointerTargetSet startPts = PointerTargetSet.emptyPointerTargetSet();
    PathFormulaManager pfmgr = slabCpa.getPredicateCpa().getPathFormulaManager();

    BooleanFormula formula =
        buildPathFormula(startState, endState, startSSAMap, startPts, solver, pfmgr, true)
            .getFormula();

    boolean infeasible = false;
    try (ProverEnvironment thmProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      thmProver.push(formula);
      if (thmProver.isUnsat()) {
        infeasible = true;
      } else {
        infeasible = false;
      }
    } catch (SolverException  e){
         throw new CPAException("Solver Failure", e);
    }
    return infeasible;
  }

  private void removeInfeasibleStates(ReachedSet pReached)
      throws InterruptedException, CPAException {
    List<SLARGState> toRemove = new ArrayList<>();
    for (AbstractState state : pReached) {
      SLARGState slabState = AbstractStates.extractStateByType(state, SLARGState.class);
      BooleanFormula stateFormula =
          PredicateAbstractState.getPredicateState(slabState).getAbstractionFormula().asFormula();
      try (ProverEnvironment thmProver =
          solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        thmProver.push(stateFormula);
        if (thmProver.isUnsat()) {
          slabState.removeFromARG();
          toRemove.add(slabState);
        }
      } catch (SolverException e) {
        throw new CPAException("Solver Failure", e);
      }
    }
    pReached.removeAll(toRemove);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (refiner instanceof StatisticsProvider) {
      ((StatisticsProvider) refiner).collectStatistics(pStatsCollection);
    }
  }
}
