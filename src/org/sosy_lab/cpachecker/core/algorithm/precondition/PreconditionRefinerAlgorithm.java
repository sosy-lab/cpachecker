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

import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
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
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.precondition.interfaces.PreconditionWriter;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.partitioning.PartitioningCPA;
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
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

@Options(prefix="precondition")
public class PreconditionRefinerAlgorithm implements Algorithm {

  private static class NoTraceFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public NoTraceFoundException(final String pMessage) {
      super(pMessage);
    }
  }

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

  private final ReachedSetFactory reachedSetFactory;
  private final Algorithm wrappedAlgorithm;
  private final AbstractionManager amgr;
  private final FormulaManagerView mgrv;
  private final FormulaManager mgr;
  private final LogManager logger;
  private final Solver solver;
  private final CFA cfa;

  private final PredicateCPA predcpa;
  private final ARGCPA argcpa;

  private final Refine refiner;
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
    mgrv = predcpa.getFormulaManager();
    mgr = predcpa.getRealFormulaManager();
    solver = predcpa.getSolver();

    helper = new PreconditionHelper(mgrv, pConfig, logger, pShutdownNotifier, pCfa);
    ruleEngine = new RuleEngine(logger, mgr, mgrv, solver);
    refiner = new Refine(
          pConfig, pLogger, pShutdownNotifier, pCfa,
          new ExtractNewPreds(mgr, mgrv, ruleEngine),
          new MinCorePrio(mgr, mgrv, solver),
          mgr, mgrv, amgr);

    writer = exportPreciditionsAs == PreconditionExportType.SMTLIB
        ? Optional.<PreconditionWriter>of(new PreconditionToSmtlibWriter(pCfa, pConfig, pLogger, mgrv))
        : Optional.<PreconditionWriter>absent();
  }

  private BooleanFormula getPreconditionForViolation(ReachedSet pReachedSet) {
    return helper.getPreconditionFromReached(pReachedSet, PreconditionPartition.VIOLATING);
  }

  private BooleanFormula getPreconditionForValidity(ReachedSet pReachedSet) {
    return helper.getPreconditionFromReached(pReachedSet, PreconditionPartition.VALID);
  }

  private ARGPath getTrace(ReachedSet pReachedSet, Predicate<AbstractState> pPartitionFilterPredicate)
      throws NoTraceFoundException {

    ImmutableSet<AbstractState> targetStates = from(pReachedSet)
        .filter(AbstractStates.IS_TARGET_STATE)
        .filter(pPartitionFilterPredicate)
        .toSet();

    if (targetStates.isEmpty()) {
      throw new NoTraceFoundException("No trace to the target location found!");
    }

    ARGState arbitraryTargetState = AbstractStates.extractStateByType(targetStates.iterator().next(), ARGState.class);
    return ARGUtils.getOnePathTo(arbitraryTargetState);
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

    int refinementNumber = 0;

    do {
      // Run the CPA algorithm
      final boolean result = wrappedAlgorithm.run(pReachedSet);

      // We use one set of reached states
      //    ... and separate the state space using an automaton!
      final BooleanFormula pcViolation = getPreconditionForViolation(pReachedSet);
      final BooleanFormula pcValid = getPreconditionForValidity(pReachedSet);

      //if (isDisjoint(pcViolation, pcValid)) {
      if (false) {
        // We have found a valid, weakest, precondition
        // -- > write the precondition.
        if (writer.isPresent()) {
          try {
            writer.get().writePrecondition(exportPreciditionsTo, pcValid);
          } catch (IOException e) {
            logger.log(Level.WARNING, "Writing the precondition failed!", e);
          }
        }

        return true && result;
      }

      CFANode wpLoc = getFirstNodeInEntryFunctionBody();

      // Get arbitrary traces...(without disjunctions)
      // ... one to the location that violates the specification
      // ... and one to the location that represents the exit location
      final ARGPath traceViolation;
      final ARGPath traceValid;
      try {
        traceViolation = getTrace(pReachedSet, PreconditionHelper.IS_FROM_VIOLATING_PARTITION);
        traceValid = getTrace(pReachedSet, PreconditionHelper.IS_FROM_VALID_PARTITION);
      } catch (NoTraceFoundException e) {
        logger.log(Level.WARNING, e.getMessage());
        return false;
      }

      // Check the disjointness of the WP for the two traces...
      final BooleanFormula pcViolatingTrace = helper.getPreconditionOfPath(traceViolation, Optional.of(wpLoc));
      final BooleanFormula pcValidTrace = helper.getPreconditionOfPath(traceValid, Optional.of(wpLoc));

      if (!isDisjoint(pcViolatingTrace, pcValidTrace)) {
        logger.log(Level.WARNING, "non-determinism in program."); // This warning is taken 1:1 from the Seghir/Kroening paper
        return false;
      }

      // Refine the precision so that the
      // abstraction on the two traces is disjoint
      PredicatePrecision newPrecision = refiner.refine(traceViolation, traceValid, Optional.of(wpLoc));

      // Add the predicates to the precision
      // TODO: Location-specific?

      // Restart with the initial set of reached states
      // with the new precision!
      ARGReachedSet argReached = new ARGReachedSet(pReachedSet, argcpa, refinementNumber++);
      refinePrecisionForNextIteration(initialReachedSet, argReached, newPrecision);

    } while (true);
  }

  private void refinePrecisionForNextIteration(
      ReachedSet pInitialStates,
      ARGReachedSet pTo,
      PredicatePrecision pPredPrecision) {

//    pTo.updatePrecision(ae, adaptPrecision(mReached.getPrecision(ae), p, pPrecisionType));
//    pTo.reAddToWaitlist(ae);
//
//    for (AbstractState e: pInitialStates.getWaitlist()) {
//      pTo.add(e, pPrec);
//    }

  }

}
