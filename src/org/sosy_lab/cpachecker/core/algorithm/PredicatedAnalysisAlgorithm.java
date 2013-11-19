/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Objects.firstNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TargetableWithPredicatedAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoinPredicatedAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeMergeAgreePredicatedAnalysisOperator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;


public class PredicatedAnalysisAlgorithm implements Algorithm, StatisticsProvider{

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private CAssumeEdge fakeEdgeFromLastRun = null;
  private AbstractState initialWrappedState = null;


  public PredicatedAnalysisAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis cpa, CFA pCfa, LogManager logger,
      Configuration config, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    algorithm = pAlgorithm;
    this.cpa = cpa;
    cfa = pCfa;
    this.logger = logger;
    shutdownNotifier = pShutdownNotifier;

    if (!(cpa instanceof ARGCPA) || CPAs.retrieveCPA(cpa, LocationCPA.class) == null
        || CPAs.retrieveCPA(cpa, PredicateCPA.class) == null || CPAs.retrieveCPA(cpa, CompositeCPA.class) == null) { throw new InvalidConfigurationException(
        "Predicated Analysis requires ARG as top CPA and Composite CPA as child. "
            + "Furthermore, it needs Location CPA and Predicate CPA to work.");
    }
    if (!(CPAs.retrieveCPA(cpa, CompositeCPA.class).getMergeOperator() instanceof CompositeMergeAgreePredicatedAnalysisOperator)) { throw new InvalidConfigurationException(
        "Composite CPA must be informed about predicated analysis. "
            + "Add cpa.composite.inPredicatedAnalysis=true to your configuration options.");
    }

    if (!(CPAs.retrieveCPA(cpa, CompositeCPA.class).getMergeOperator() instanceof CompositeMergeAgreePredicatedAnalysisOperator)) { throw new InvalidConfigurationException(
        "Composite CPA must be informed about predicated analysis. "
            + "Add cpa.arg.inPredicatedAnalysis=true to your configuration options.");
    }
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // delete fake edge from previous run
    logger.log(Level.FINEST, "Clean up from previous run");
    if(fakeEdgeFromLastRun!=null){
      fakeEdgeFromLastRun.getPredecessor().removeLeavingEdge(fakeEdgeFromLastRun);
      fakeEdgeFromLastRun.getSuccessor().removeEnteringEdge(fakeEdgeFromLastRun);
    }

    // first build initial precision for current run
    logger.log(Level.FINEST, "Construct precision for current run");
    Precision precision =
        buildInitialPrecision(pReachedSet.getPrecisions(), cpa.getInitialPrecision(cfa.getMainFunction()));

    // clear reached set for current run
    pReachedSet.clear();

    // initialize reached set
    if (initialWrappedState == null) {
      initialWrappedState = ((ARGState) cpa.getInitialState(cfa.getMainFunction())).getWrappedState();
    }
    pReachedSet.add(new ARGState(initialWrappedState, null), precision);

    // run algorithm
    logger.log(Level.FINEST, "Start analysis.");
    boolean result = false;
    try{
      result = algorithm.run(pReachedSet);
    }catch(PredicatedAnalysisPropertyViolationException e){
      precision =  pReachedSet.getPrecision(pReachedSet.getLastState());
      if (e.getFailureCause() != null && !pReachedSet.contains(e.getFailureCause())
          && ((ARGState) e.getFailureCause()).getParents().size() != 0) {
        // add element
        pReachedSet.add(e.getFailureCause(), precision);
        // readd parents their may be other siblings in the ARG which are not part of the reached set
        for (ARGState parent : ((ARGState) e.getFailureCause()).getParents()) {
          pReachedSet.reAddToWaitlist(parent);
        }
      }

      // add merged element and clean up
      if(e.isMergeViolationCause()){
        pReachedSet.add(((ARGState) e.getFailureCause()).getMergedWith(), precision);
        ((ARGMergeJoinPredicatedAnalysis)cpa.getMergeOperator()).cleanUp(pReachedSet);
      }

      logger.log(Level.FINEST, "Analysis aborted because error state found");
      ARGState predecessor = (ARGState) pReachedSet.getLastState();
      CFANode node = AbstractStates.extractLocation(predecessor);

      // create fake edge
      logger.log(Level.FINEST, "Prepare for refinement by CEGAR algorithm");
      try{
        node.getEdgeTo(node);
        throw new CPAException("Predicated Analysis cannot be run with programs whose CFAs have self-loops.");
      }catch(IllegalArgumentException e1){
        // do nothing we require that the edge does not exist
      }
      // TODO set null elements correctly so error path export, etc. works correctly
      CAssumeEdge assumeEdge = new CAssumeEdge(null, 0, node, node, null, true);
      fakeEdgeFromLastRun = assumeEdge;
      node.addEnteringEdge(assumeEdge);
      node.addLeavingEdge(assumeEdge);


      // get predicate state
      PredicateCPA predCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
      PredicateAbstractState errorPred = AbstractStates.extractStateByType(predecessor, PredicateAbstractState.class);
      CompositeState comp = AbstractStates.extractStateByType(predecessor, CompositeState.class);

      if (!e.isMergeViolationCause()) {
        if(errorPred.isAbstractionState()){
          // we must undo the abstraction because we do not want to separate paths at this location but exclude this that
          // thus we require a new abstraction for the previous abstraction state

          PredicateAbstractState prevErrorState =
              AbstractStates.extractStateByType(predecessor.getParents().iterator().next(),
                  PredicateAbstractState.class);

          errorPred = PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(errorPred.getAbstractionFormula()
              .getBlockFormula(), errorPred.getViolatedProperty(), prevErrorState);

          // build new composite state
          ImmutableList.Builder<AbstractState> wrappedStates = ImmutableList.builder();
          for (AbstractState state : comp.getWrappedStates()) {
            if (!(state instanceof PredicateAbstractState)) {
                wrappedStates.add(state);
            }else{
              wrappedStates.add(errorPred);
            }
          }

          comp = new CompositeState(wrappedStates.build());

          assert(predecessor.getChildren().size()==0);
          assert(predecessor.getParents().size()==1);
          assert(predecessor.getCoveredByThis().size()==0);

          ARGState newPred = new ARGState(comp, predecessor.getParents().iterator().next());
          predecessor.removeFromARG();
          pReachedSet.add(newPred, pReachedSet.getPrecision(predecessor));
          pReachedSet.remove(predecessor);
          predecessor = newPred;
        }
      }

      // create fake state
      // build predicate state
      PathFormulaManager pfm = predCPA.getPathFormulaManager();
      PredicateAbstractionManager pam = predCPA.getPredicateManager();
      FormulaManagerView fm = predCPA.getFormulaManager();

      // create path to fake node
      PathFormula pf = pfm.makeAnd(errorPred.getPathFormula(),
          ((TargetableWithPredicatedAnalysis) predecessor).getErrorCondition(fm));

      // build abstraction which is needed for refinement, set to true, we do not know better
      AbstractionFormula abf = pam.makeTrueAbstractionFormula(pf);
      pf = pfm.makeEmptyPathFormula(pf);

      PersistentMap<CFANode, Integer> abstractionLocations = errorPred.getAbstractionLocationsOnPath();
      Integer newLocInstance = firstNonNull(abstractionLocations.get(node), 0) + 1;
      abstractionLocations = abstractionLocations.putAndCopy(node, newLocInstance);

      // create fake predicate state
      PredicateAbstractState fakePred =
          PredicateAbstractState.mkAbstractionState(fm.getBooleanFormulaManager(), pf, abf,
              abstractionLocations, errorPred.getViolatedProperty());

      // build composite state
      ImmutableList.Builder<AbstractState> wrappedStates = ImmutableList.builder();
      for (AbstractState state : comp.getWrappedStates()) {
        if (state != errorPred) {
            wrappedStates.add(state);
        } else {
          wrappedStates.add(fakePred);
        }
      }


      comp = new CompositeState(wrappedStates.build());

      // build ARG state and add to ARG
      ARGState successor = new ARGState(comp, predecessor);

      // insert into reached set
      pReachedSet.add(successor, pReachedSet.getPrecision(predecessor));

      assert(ARGUtils.checkARG(pReachedSet));

      // return true such that CEGAR works fine
      return true;
    }

    return result;
  }

  private Precision buildInitialPrecision(Collection<Precision> precisions, Precision initialPrecision) throws InterruptedException {
    if(precisions.size()==0){
      return initialPrecision;
    }
    //TODO assure that refinement fails if same path is encountered twice
    Multimap<Pair<CFANode, Integer>, AbstractionPredicate> locationInstancPreds = HashMultimap.create();
    Multimap<CFANode, AbstractionPredicate> localPreds = HashMultimap.create();
    Multimap<String, AbstractionPredicate> functionPreds = HashMultimap.create();
    Collection<AbstractionPredicate> globalPreds = new HashSet<>();

    Collection<PredicatePrecision> seenPrecisions = new HashSet<>();

    // add initial precision
    PredicatePrecision predPrec = Precisions.extractPrecisionByType(initialPrecision, PredicatePrecision.class);
    locationInstancPreds.putAll(predPrec.getLocationInstancePredicates());
    localPreds.putAll(predPrec.getLocalPredicates());
    functionPreds.putAll(predPrec.getFunctionPredicates());
    globalPreds.addAll(predPrec.getGlobalPredicates());

    seenPrecisions.add(predPrec);

    // add further precision information obtained during refinement
    for (Precision nextPrec : precisions) {
      predPrec = Precisions.extractPrecisionByType(nextPrec, PredicatePrecision.class);

      shutdownNotifier.shutdownIfNecessary();

      if (!seenPrecisions.contains(predPrec)) {
        seenPrecisions.add(predPrec);
        locationInstancPreds.putAll(predPrec.getLocationInstancePredicates());
        localPreds.putAll(predPrec.getLocalPredicates());
        functionPreds.putAll(predPrec.getFunctionPredicates());
        globalPreds.addAll(predPrec.getGlobalPredicates());
      }
    }

    // construct new predicate precision
    PredicatePrecision newPredPrec = new PredicatePrecision(locationInstancPreds, localPreds, functionPreds, globalPreds);

    return Precisions.replaceByType(initialPrecision, newPredPrec, PredicatePrecision.class);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if(algorithm instanceof StatisticsProvider){
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }

  }
}
