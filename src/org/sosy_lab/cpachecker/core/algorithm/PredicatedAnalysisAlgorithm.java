/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.MoreObjects.firstNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoinPredicatedAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeMergeAgreePredicatedAnalysisOperator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;


public class PredicatedAnalysisAlgorithm implements Algorithm, StatisticsProvider{

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final PredicateCPA predCPA;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final List<CAssumeEdge> fakeEdgesFromLastRun = new ArrayList<>();
  private AbstractState initialWrappedState = null;
  private ARGPath pathToFailure = null;
  private boolean repeatedFailure = false;
  private PredicatePrecision oldPrecision = null;
  private Constructor<?extends AbstractState> locConstructor = null;


  public PredicatedAnalysisAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis cpa, CFA pCfa, LogManager logger,
      Configuration config, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    algorithm = pAlgorithm;
    this.cpa = cpa;
    cfa = pCfa;
    this.logger = logger;
    shutdownNotifier = pShutdownNotifier;
    predCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);

    if (!(cpa instanceof ARGCPA)
        || (CPAs.retrieveCPA(cpa, LocationCPA.class) == null && CPAs.retrieveCPA(cpa, LocationCPABackwards.class) == null)
        || CPAs.retrieveCPA(cpa, PredicateCPA.class) == null || CPAs.retrieveCPA(cpa, CompositeCPA.class) == null) { throw new InvalidConfigurationException(
        "Predicated Analysis requires ARG as top CPA and Composite CPA as child. "
            + "Furthermore, it needs Location CPA and Predicate CPA to work.");
    }

    if (CPAs.retrieveCPA(cpa, LocationCPABackwards.class) != null) {
      throw new InvalidConfigurationException("Currently only support forward analyses.");
    }
    if (!(CPAs.retrieveCPA(cpa, CompositeCPA.class).getMergeOperator() instanceof CompositeMergeAgreePredicatedAnalysisOperator)) { throw new InvalidConfigurationException(
        "Composite CPA must be informed about predicated analysis. "
            + "Add cpa.composite.inPredicatedAnalysis=true to your configuration options.");
    }

    if (!(cpa.getMergeOperator() instanceof ARGMergeJoinPredicatedAnalysis)) { throw new InvalidConfigurationException(
        "ARG CPA must be informed about predicated analysis. "
            + "Add cpa.arg.inPredicatedAnalysis=true to your configuration options.");
    }
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // delete fake edges from previous run
    logger.log(Level.FINEST, "Clean up from previous run");
    for (CAssumeEdge edge :fakeEdgesFromLastRun) {
      edge.getPredecessor().removeLeavingEdge(edge);
      edge.getSuccessor().removeEnteringEdge(edge);
    }
    fakeEdgesFromLastRun.clear();

    // activate if cannot use lazy refinement, possibly needed later if further extended TODO use config option
    // restartFromScratchAfterRefinement(pReachedSet);

    // run algorithm
    logger.log(Level.FINEST, "Start analysis.");
    boolean result = false;
    try {
      result = algorithm.run(pReachedSet);
    } catch (PredicatedAnalysisPropertyViolationException e) {
      if(e.getFailureCause()==null){
        throw new CPAException("Error state not known to predicated analysis algorithm. Cannot continue analysis.");
      }
      Precision precision =  pReachedSet.getPrecision(((ARGState)e.getFailureCause()).getParents().iterator().next());
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
      if (e.isMergeViolationCause()) {
        pReachedSet.add(((ARGState) e.getFailureCause()).getMergedWith(), precision);
        ((ARGMergeJoinPredicatedAnalysis)cpa.getMergeOperator()).cleanUp(pReachedSet);
      }

      logger.log(Level.FINEST, "Analysis aborted because error state found");
      ARGState predecessor = (ARGState) pReachedSet.getLastState();
      CFANode node = AbstractStates.extractLocation(predecessor);

      logger.log(Level.FINEST, "Prepare for refinement by CEGAR algorithm");

      // get predicate state
      PredicateAbstractState errorPred = AbstractStates.extractStateByType(predecessor, PredicateAbstractState.class);
      CompositeState comp = AbstractStates.extractStateByType(predecessor, CompositeState.class);

      if (!e.isMergeViolationCause()) {
        if (errorPred.isAbstractionState()) {
          // we must undo the abstraction because we do not want to separate paths at this location but exclude this that
          // thus we require a new abstraction for the previous abstraction state

          PredicateAbstractState prevErrorState =
              AbstractStates.extractStateByType(predecessor.getParents().iterator().next(),
                  PredicateAbstractState.class);

          errorPred = PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(errorPred.getAbstractionFormula()
              .getBlockFormula(), prevErrorState);

          // build new composite state
          ImmutableList.Builder<AbstractState> wrappedStates = ImmutableList.builder();
          for (AbstractState state : comp.getWrappedStates()) {
            if (!(state instanceof PredicateAbstractState)) {
                wrappedStates.add(state);
            } else {
              wrappedStates.add(errorPred);
            }
          }

          comp = new CompositeState(wrappedStates.build());

          assert (predecessor.getChildren().size()==0);
          assert (predecessor.getParents().size()==1);
          assert (predecessor.getCoveredByThis().size()==0);

          ARGState newPred = new ARGState(comp, predecessor.getParents().iterator().next());
          predecessor.removeFromARG();
          pReachedSet.add(newPred, pReachedSet.getPrecision(predecessor));
          pReachedSet.remove(predecessor);
          predecessor = newPred;
        }
      }

      // check if it is the same failure (CFA path) as last time if started from scratch after refinement TODO enable if restract from scratch
      /* ARGPath currentFailurePath = ARGUtils.getOnePathTo(predecessor);
      repeatedFailure = pathToFailure == null || isSamePathInCFA(pathToFailure, currentFailurePath);
      pathToFailure = currentFailurePath;*/

      // create fake edges
      /*try {
        node.getEdgeTo(node);
        throw new CPAException("Predicated Analysis cannot be run with programs whose CFAs have self-loops.");
      } catch (IllegalArgumentException e1) {
        // do nothing we require that the edge does not exist
      }*/

      // create error conditions stored on fake edges
      CFANode predNode = node;
      try {
        for (AutomatonState s : AbstractStates.asIterable(predecessor).filter(AutomatonState.class)) {
          if (s.isTarget()) {
            for (AssumeEdge assume : s.getAsAssumeEdges(node.getFunctionName())) {
              predNode = createFakeEdge(assume.getRawStatement(),  (CExpression) assume.getExpression(), predNode);
            }
          }
        }
      } catch (ClassCastException e2) {
        throw new CPAException("Predicated Analysis requires that the error condition is specified as a CExpression (statement) in the specification (automata).");
      }

      if(fakeEdgesFromLastRun.isEmpty()){
        // create fake edge with assumption true
        createFakeEdge("1", CIntegerLiteralExpression.ONE, predNode);
      }

      // create fake states, one per fake edge, note that states are the same except for predicate state and location state
      if (locConstructor == null) {
        try {
          locConstructor = LocationState.class.getDeclaredConstructor(CFANode.class, boolean.class);
          locConstructor.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e1) {
          throw new CPAException("Cannot prepare for refinement because cannot get constructor for location states.", e1);
        }
      }

      PathFormulaManager pfm = predCPA.getPathFormulaManager();
      PredicateAbstractionManager pam = predCPA.getPredicateManager();
      FormulaManagerView fm = predCPA.getSolver().getFormulaManager();
      PathFormula pf;
      PredicateAbstractState fakePred = errorPred;
      int i=1;
      for(CAssumeEdge assumeEdge:fakeEdgesFromLastRun) {
        errorPred = fakePred;

        // build predicate state, use error condition stored in fake edge
        pf = fakePred.getPathFormula();


        // create path to fake node
        pf = pfm.makeAnd(pf, assumeEdge);

        // if last edge on faked path, build abstraction which is needed for refinement, set to true, we do not know better
        if (i == fakeEdgesFromLastRun.size()) {
          AbstractionFormula abf = pam.makeTrueAbstractionFormula(pf);
          pf = pfm.makeEmptyPathFormula(pf);

          PersistentMap<CFANode, Integer> abstractionLocations = errorPred.getAbstractionLocationsOnPath();
          Integer newLocInstance = firstNonNull(abstractionLocations.get(assumeEdge.getSuccessor()), 0) + 1;
          abstractionLocations = abstractionLocations.putAndCopy(assumeEdge.getSuccessor(), newLocInstance);

          // create fake abstraction predicate state
          fakePred = PredicateAbstractState.mkAbstractionState(fm.getBooleanFormulaManager(), pf, abf,
                  abstractionLocations);
        } else {
          // create fake non abstraction predicate state
          fakePred = PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(pf, fakePred);
        }

        // build composite state
        ImmutableList.Builder<AbstractState> wrappedStates = ImmutableList.builder();
        for (AbstractState state : comp.getWrappedStates()) {
          if (state != errorPred) {
            if (state instanceof LocationState) {
              try {
                wrappedStates.add(locConstructor.newInstance(assumeEdge.getSuccessor(),true));
              } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                  | InvocationTargetException e1) {
                throw new CPAException("Cannot prepare for refinement, cannot build necessary fake states.", e1);
              }
            } else {
              wrappedStates.add(state);
            }
          } else {
            wrappedStates.add(fakePred);
          }
        }


        comp = new CompositeState(wrappedStates.build());

        // build ARG state and add to ARG
        ARGState successor = new ARGState(comp, predecessor);

        // insert into reached set
        pReachedSet.add(successor, pReachedSet.getPrecision(predecessor));
        predecessor = successor;
        i++;
      }

      assert (ARGUtils.checkARG(pReachedSet));

      // return true such that CEGAR works fine
      return true;
    }

    return result;
  }

  private CFANode createFakeEdge(final String pRawAssumeExpr, final CExpression pAssumeExpr, final CFANode pPredecessor) {
    CFANode successor = new CFANode(pPredecessor.getFunctionName());
    CAssumeEdge assumeEdge =
        new CAssumeEdge(pRawAssumeExpr, FileLocation.DUMMY, pPredecessor, successor, pAssumeExpr, true);
    pPredecessor.addLeavingEdge(assumeEdge);
    successor.addEnteringEdge(assumeEdge);
    fakeEdgesFromLastRun.add(assumeEdge);
    return successor;
  }

  @SuppressWarnings("unused")
  private void restartFromScratchAfterRefinement(final ReachedSet pReachedSet) throws RefinementFailedException, InterruptedException {
    // first build initial precision for current run
    logger.log(Level.FINEST, "Construct precision for current run");
    Precision precision =
        buildInitialPrecision(pReachedSet.getPrecisions(), cpa.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()));
    oldPrecision = Precisions.extractPrecisionByType(precision, PredicatePrecision.class);

    // clear reached set for current run
    pReachedSet.clear();

    // initialize reached set
    if (initialWrappedState == null) {
      initialWrappedState = ((ARGState) cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition())).getWrappedState();
    }
    pReachedSet.add(new ARGState(initialWrappedState, null), precision);
  }

  private Precision buildInitialPrecision(Collection<Precision> precisions, Precision initialPrecision)
      throws InterruptedException, RefinementFailedException {
    if (precisions.size()==0) {
      return initialPrecision;
    }

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

    try {
      // assure that refinement fails if same path is encountered twice and precision not refined on that path
      if (repeatedFailure && noNewPredicates(oldPrecision, newPredPrec)) {
        throw new RefinementFailedException(Reason.RepeatedCounterexample, pathToFailure);
      }
    } catch (SolverException e) {
      throw new RefinementFailedException(Reason.InterpolationFailed, pathToFailure, e);
    }

    return Precisions.replaceByType(initialPrecision, newPredPrec, Predicates.instanceOf(PredicatePrecision.class));
  }

  private boolean noNewPredicates(PredicatePrecision oldPrecision, PredicatePrecision newPrecision)
      throws SolverException, InterruptedException {

    // check if global precision changed
    if (isMorePrecise(oldPrecision.getGlobalPredicates(), newPrecision.getGlobalPredicates())) { return false; }
    // get CFA nodes and function names on failure path
    HashSet<String> funNames = new HashSet<>();
    HashSet<CFANode> nodesOnPath = new HashSet<>();

    for (CFAEdge edge : pathToFailure.getInnerEdges()) {
      CFANode current = edge.getSuccessor();
      funNames.add(current.getFunctionName());
      nodesOnPath.add(current);
    }

    // check if precision for one of the functions on path changed
    for (String funName : funNames) {
      if (isMorePrecise(oldPrecision.getFunctionPredicates().get(funName),
          newPrecision.getFunctionPredicates().get(funName))) { return false; }
    }

    // check if precision for one of the CFA nodes on path changed
    for (CFANode node : nodesOnPath) {
      if (isMorePrecise(oldPrecision.getLocalPredicates().get(node),
          newPrecision.getLocalPredicates().get(node))) { return false; }
    }

    return true;
  }

  private boolean isMorePrecise(Set<AbstractionPredicate> lessPrecise,
      Set<AbstractionPredicate> morePrecise)
          throws SolverException, InterruptedException {
    if (lessPrecise != null && morePrecise != null) {
      if (lessPrecise.size() == morePrecise.size() && lessPrecise.equals(morePrecise)) { return false; }

      // build conjunction of predicates
      ArrayList<BooleanFormula> list = new ArrayList<>(Math.max(lessPrecise.size(), morePrecise.size()));
      for (AbstractionPredicate abs : lessPrecise) {
        list.add(abs.getSymbolicAtom());
      }
      final Solver solver = predCPA.getSolver();
      final BooleanFormulaManagerView bfmgr = solver.getFormulaManager().getBooleanFormulaManager();
      BooleanFormula fLess = bfmgr.and(list);

      list.clear();
      for (AbstractionPredicate abs : lessPrecise) {
        list.add(abs.getSymbolicAtom());
      }
      BooleanFormula fMore = bfmgr.and(list);
      fMore = bfmgr.not(fMore);
      fMore = bfmgr.and(fLess, fMore);

      // check if conjunction of less precise does not imply conjunction of more precise
      return solver.isUnsat(fMore);
    }

    return lessPrecise == null && morePrecise != null;
  }

  @SuppressWarnings("unused")
  private boolean isSamePathInCFA(ARGPath path1, ARGPath path2) {
    if (path1.size() != path2.size()) {
      return false;
    }
    // check lists in reverse order for short-circuiting
    List<CFANode> edges1 = Lists.transform(path1.asStatesList().reverse(), AbstractStates.EXTRACT_LOCATION);
    List<CFANode> edges2 = Lists.transform(path2.asStatesList().reverse(), AbstractStates.EXTRACT_LOCATION);
    return edges1.equals(edges2);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }

  }

  @Override
  public boolean reset() {
    fakeEdgesFromLastRun.clear();
    initialWrappedState = null;
    pathToFailure = null;
    repeatedFailure = false;
    oldPrecision = null;

    return algorithm.reset();
  }
}
