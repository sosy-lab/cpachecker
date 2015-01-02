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
package org.sosy_lab.cpachecker.core.algorithm.precondition;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.precondition.interfaces.PreconditionWriter;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.partitioning.PartitioningCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.precondition.segkro.ExtractNewPreds;
import org.sosy_lab.cpachecker.util.precondition.segkro.MinCorePrio;
import org.sosy_lab.cpachecker.util.precondition.segkro.Refine;
import org.sosy_lab.cpachecker.util.precondition.segkro.RefineSolverBasedItp;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.PreconditionRefiner;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Verify;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@Options(prefix="precondition")
public class PreconditionRefinerAlgorithm implements Algorithm, StatisticsProvider {

  private static class NoTraceFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public NoTraceFoundException(final String pMessage) {
      super(pMessage);
    }
  }

  private static class PreconditionRefinerStatistics extends AbstractStatistics {
    public int refinements = 0;
    public int tracesToExit = 0;
    public int tracesToError = 0;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      put(pOut, "Number of precondition refinements", refinements);
      put(pOut, "Analyzed traces to the ERROR location", tracesToError);
      put(pOut, "Analyzed traces to the EXIT location", tracesToExit);
    }
  }

  PreconditionRefinerStatistics stats = new PreconditionRefinerStatistics();

  public static enum PreconditionExportType { NONE, SMTLIB }
  @Option(secure=true,
      name="export.type",
      description="(How) should the precondition be exported?")
  private PreconditionExportType exportPreciditionsAs = PreconditionExportType.NONE;

  @Option(secure=true,
      name="export.target",
      description="Where should the precondition be exported to?")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportPreciditionsTo = Paths.get("precondition.txt");

  @Option(secure=true,
      description="Use the refiner that uses a solver-based interpolation mechanism.")
  private boolean solverbasedInterpolation = false;

  private final ReachedSetFactory reachedSetFactory;
  private final Algorithm wrappedAlgorithm;
  private final AbstractionManager amgr;
  private final FormulaManagerView mgrv;
  private final LogManager logger;
  private final Solver solver;
  private final CFA cfa;

  private final PredicateCPA predcpa;
  private final ARGCPA argcpa;

  private final PreconditionRefiner refiner;
  private final RuleEngine ruleEngine;
  private final PreconditionHelper helper;
  private final Optional<PreconditionWriter> writer;

  public PreconditionRefinerAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, CFA pCfa,
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
          throws InvalidConfigurationException {

    Preconditions.checkNotNull(pConfig).inject(this);

    Preconditions.checkNotNull(
        CPAs.retrieveCPA(pCpa, PartitioningCPA.class),
        "The CPA must be composed of a PartitioningCPA in order to provide a precondition!");

    argcpa = Preconditions.checkNotNull(
        CPAs.retrieveCPA(pCpa, ARGCPA.class),
        "The CPA must be composed of an ARG CPA in order to provide a precondition!");

    predcpa = Preconditions.checkNotNull(
        CPAs.retrieveCPA(pCpa, PredicateCPA.class),
        "The CPA must be composed of a predicate analysis in order to provide a precondition!");

    cfa = pCfa;
    logger = Preconditions.checkNotNull(pLogger);
    wrappedAlgorithm = Preconditions.checkNotNull(pAlgorithm);
    reachedSetFactory = new ReachedSetFactory(pConfig, pLogger);

    amgr = predcpa.getAbstractionManager();
    mgrv = predcpa.getSolver().getFormulaManager();
    solver = predcpa.getSolver();

    helper = new PreconditionHelper(mgrv, pConfig, logger, pShutdownNotifier, pCfa);
    ruleEngine = new RuleEngine(logger, solver);

    refiner = createRefiner(pConfig, pShutdownNotifier);

    writer = exportPreciditionsAs == PreconditionExportType.SMTLIB
        ? Optional.<PreconditionWriter>of(new PreconditionToSmtlibWriter(pCfa, pConfig, pLogger, mgrv))
        : Optional.<PreconditionWriter>absent();
  }

  private PreconditionRefiner createRefiner(Configuration pConfig, ShutdownNotifier pShutdown) throws InvalidConfigurationException {
    if (solverbasedInterpolation) {
      return new RefineSolverBasedItp(
          pConfig, logger, pShutdown, cfa,
          solver, amgr,
          new ExtractNewPreds(solver, ruleEngine),
          new MinCorePrio(logger, cfa, solver));
    } else {
      return new Refine(
          pConfig, logger, pShutdown, cfa,
          solver, amgr,
          new ExtractNewPreds(solver, ruleEngine),
          new MinCorePrio(logger, cfa, solver));
    }
  }

  private BooleanFormula getPreconditionForViolation(ReachedSet pReachedSet, CFANode pWpLoc) {
    return helper.getPreconditionFromReached(pReachedSet, PreconditionPartition.VIOLATING, pWpLoc);
  }

  private BooleanFormula getPreconditionForValidity(ReachedSet pReachedSet, CFANode pWpLoc) {
    return helper.getPreconditionFromReached(pReachedSet, PreconditionPartition.VALID, pWpLoc);
  }

  private Collection<ARGPath> getTracesToAbstractionLocation(
      final ReachedSet pReachedSet,
      final Predicate<AbstractState> pPartitionFilterPredicate,
      final CFANode pLoc)
    throws NoTraceFoundException {

    Preconditions.checkNotNull(pPartitionFilterPredicate);
    Preconditions.checkNotNull(pReachedSet);
    Preconditions.checkNotNull(pLoc);

    ImmutableSet<AbstractState> targetStates = from(pReachedSet)
        .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES, toState(PredicateAbstractState.class)))
        .filter(Predicates.compose(equalTo(pLoc), AbstractStates.EXTRACT_LOCATION))
        .filter(pPartitionFilterPredicate)
        .toSet();

    if (targetStates.isEmpty()) {
      throw new NoTraceFoundException("No trace to the target location found!");
    }

    return Collections2.transform(targetStates, new Function<AbstractState, ARGPath>() {
      @Override
      public ARGPath apply(AbstractState pState) {
        // Return a path to the state!
        ARGState arbitraryTargetState = AbstractStates.extractStateByType(pState, ARGState.class);
        return ARGUtils.getOnePathTo(arbitraryTargetState);
      }
    });
  }

  private boolean isDisjoint(BooleanFormula pP1, BooleanFormula pP2) throws SolverException, InterruptedException {
    return solver.isUnsat(mgrv.getBooleanFormulaManager().and(pP1, pP2));
  }

  private CFANode getFirstNodeInEntryFunctionBody() {
    CFANode next = cfa.getMainFunction();
    boolean isEntryFunctionDeclEdge = false;
    do {
      if (next.getNumLeavingEdges() > 1) {
        throw new AssertionError("getFirstNodeInEntryFunctionBody: More than one leaving edge!");
      }

      if (next.getNumLeavingEdges() == 0) {
        next = null;
      } else {
        CFAEdge edge = next.getLeavingEdge(0);
        next = edge.getSuccessor();

        if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
          ADeclarationEdge declEdge = (ADeclarationEdge) edge;
          if (declEdge.getDeclaration() instanceof AFunctionDeclaration) {
            AFunctionDeclaration fnDecl = (AFunctionDeclaration) declEdge.getDeclaration();
            isEntryFunctionDeclEdge = fnDecl.getName().equals(cfa.getMainFunction().getFunctionName());
          }
        }
      }
    } while ((!isEntryFunctionDeclEdge) && (next != null));
    return next;
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {

    // Copy the initial set of reached states
    final ReachedSet initialReachedSet = reachedSetFactory.create();
    ReachedSetUtils.addReachedStatesToOtherReached(pReachedSet, initialReachedSet);

    final CFANode wpLoc = getFirstNodeInEntryFunctionBody();

    boolean precisionFixpointReached = false;
    boolean preconditionFixpointReached = false;

    BooleanFormula lastIterationPcViolation = null;
    BooleanFormula lastIterationPcValid = null;
    PredicatePrecision lastPrecision = null;

    do {
      // Run the CPA algorithm
      wrappedAlgorithm.run(pReachedSet);

      // We use one set of reached states
      //    ... and separate the state space using an automaton!
      final BooleanFormula pcViolation = getPreconditionForViolation(pReachedSet, wpLoc);
      final BooleanFormula pcValid = getPreconditionForValidity(pReachedSet, wpLoc);

      if (lastIterationPcViolation != null
          && lastIterationPcViolation.equals(pcViolation)
          && lastIterationPcValid.equals(pcValid)) {
            preconditionFixpointReached = true;
      }
      lastIterationPcViolation = pcViolation;
      lastIterationPcValid = pcValid;


      if (isDisjoint(pcViolation, pcValid)) {
        logger.log(Level.INFO, "Necessary and sufficient precondition found!");

        // We have found a valid, weakest, precondition
        // -- > write the precondition.
        if (writer.isPresent()) {
          try {
            final BooleanFormula weakestPrecondition = pcValid;
            writer.get().writePrecondition(exportPreciditionsTo, weakestPrecondition);
          } catch (IOException e) {
            logger.log(Level.WARNING, "Writing the precondition failed!", e);
          }
        }
        return true;
      }

      try {
        // Get arbitrary traces...(without disjunctions)
        // ... to the location that violates the specification
        // ... to the location that represents the exit location
        Collection<ARGPath> tracesFromViolation = getTracesToAbstractionLocation(pReachedSet, PreconditionHelper.IS_FROM_VIOLATING_PARTITION, wpLoc);
        Collection<ARGPath> tracesFromValid = getTracesToAbstractionLocation(pReachedSet, PreconditionHelper.IS_FROM_VALID_PARTITION, wpLoc);

        stats.tracesToError += tracesFromViolation.size();
        stats.tracesToExit += tracesFromValid.size();

        PredicatePrecision newPrecision = null;

        for (ARGPath traceVio : tracesFromViolation) {
          for (ARGPath traceVal : tracesFromValid) {

            // Check the disjointness of the WP for the two traces...
            final BooleanFormula pcViolatingTrace = helper.getPreconditionOfPath(traceVio, Optional.of(wpLoc));
            final BooleanFormula pcValidTrace = helper.getPreconditionOfPath(traceVal, Optional.of(wpLoc));

            if (!isDisjoint(pcViolatingTrace, pcValidTrace)) {
              logger.log(Level.WARNING, "non-determinism in program."); // This warning is taken 1:1 from the Seghir/Kroening paper
              return true;
            }

            // Refine the precision so that the
            // abstraction on the two traces is disjoint
            PredicatePrecision newPrecFromTracePair = refiner.refine(traceVio, traceVal, wpLoc);
            if (newPrecision == null) {
              newPrecision = newPrecFromTracePair;
            } else {
              newPrecision = newPrecision.mergeWith(newPrecFromTracePair);
            }

            // TODO: Location-specific precision?
          }
        }

        stats.refinements++;

        if (lastPrecision != null) {
          if (lastPrecision.equals(newPrecision)) {
            precisionFixpointReached = true;
          }
        }
        lastPrecision = newPrecision;

        if (precisionFixpointReached && preconditionFixpointReached) {
          logger.log(Level.WARNING, "Terminated because of a fixpoint in the set of predicates and the precondition!");
          return true;
        }

        // Restart with the initial set of reached states
        // with the new precision!
        Verify.verify(newPrecision != null);
        refinePrecisionForNextIteration(initialReachedSet, pReachedSet, newPrecision);

      } catch (NoTraceFoundException e) {
        logger.log(Level.WARNING, e.getMessage());
        return true;
      }

    } while (true);
  }

  private void refinePrecisionForNextIteration(
      ReachedSet pInitialStates,
      ReachedSet pTo,
      PredicatePrecision pPredPrecision) {

    ARGReachedSet argReachedSetTo = new ARGReachedSet(pTo, argcpa);

    Iterator<AbstractState> rootStatesIterator = pInitialStates.iterator();

    while (rootStatesIterator.hasNext()) {
      AbstractState rootState = rootStatesIterator.next();
      ARGState as = AbstractStates.extractStateByType(rootState, ARGState.class);

      Collection<ARGState> childsToRemove = Lists.newArrayList(as.getChildren());
      for (ARGState childWithSubTreeToRemove: childsToRemove) {
        argReachedSetTo.removeSubtree(childWithSubTreeToRemove, pPredPrecision, Predicates.instanceOf(PredicatePrecision.class));
      }
    }

    // pTo.updatePrecisionGlobally(pPredPrecision, Predicates.instanceOf(PredicatePrecision.class));
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {

    ruleEngine.collectStatistics(pStatsCollection);
    if (wrappedAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) wrappedAlgorithm).collectStatistics(pStatsCollection);
    }

    pStatsCollection.add(stats);
  }

}
