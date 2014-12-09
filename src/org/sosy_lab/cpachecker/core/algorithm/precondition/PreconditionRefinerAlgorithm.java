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

import java.util.Collection;
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
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.ReachedSetUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.precondition.segkro.ExtractNewPreds;
import org.sosy_lab.cpachecker.util.precondition.segkro.MinCorePrio;
import org.sosy_lab.cpachecker.util.precondition.segkro.Refine;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

@Options(prefix="precondition")
public class PreconditionRefinerAlgorithm implements Algorithm {

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
  private final FormulaManagerView mgrv;
  private final FormulaManager mgr;
  private final PredicateCPA predcpa;
  private final LogManager logger;
  private final Solver solver;

  private final Refine refiner;
  private final RuleEngine ruleEngine;
  private final PreconditionHelper helper;

  public PreconditionRefinerAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, CFA pCfa,
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
          throws InvalidConfigurationException {

    Preconditions.checkNotNull(pConfig).inject(this);

    logger = Preconditions.checkNotNull(pLogger);
    wrappedAlgorithm = Preconditions.checkNotNull(pAlgorithm);
    reachedSetFactory = new ReachedSetFactory(pConfig, pLogger);

    predcpa = Preconditions.checkNotNull(
        CPAs.retrieveCPA(pCpa, PredicateCPA.class),
        "The CPA must be composed of a predicate analysis in order to provide a precondition!");

    mgrv = predcpa.getFormulaManager();
    mgr = predcpa.getRealFormulaManager();
    solver = predcpa.getSolver();

    helper = new PreconditionHelper(mgrv);
    ruleEngine = new RuleEngine(logger, mgr, mgrv, solver);
    refiner = new Refine(
          pConfig, pLogger, pShutdownNotifier, pCfa,
          new ExtractNewPreds(mgr, mgrv, ruleEngine),
          new MinCorePrio(mgr, mgrv, solver),
          mgr, mgrv);
  }

  private BooleanFormula getPreconditionForViolation(ReachedSet pReachedSet) {
    return helper.getPreconditionFromReached(pReachedSet, PreconditionPartition.VIOLATING);
  }

  private BooleanFormula getPreconditionForValidity(ReachedSet pReachedSet) {
    return helper.getPreconditionFromReached(pReachedSet, PreconditionPartition.VALID);
  }

  private ARGPath getTrace(ReachedSet pReachedSet, PreconditionPartition pFromPartition) {
    ImmutableSet<AbstractState> targetStates = from(pReachedSet)
        .filter(AbstractStates.IS_TARGET_STATE)
        .toSet();

    // get one state from the partition
    ARGState argState = AbstractStates.extractStateByType(
        targetStates.iterator().next(), ARGState.class);

    return ARGUtils.getOnePathTo(argState);
  }

  private ARGPath getValidTrace(ReachedSet pReachedSet) {
    return null;
  }

  private boolean isDisjoint(BooleanFormula pP1, BooleanFormula pP2) {

    return false;
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {

    // Copy the initial set of reached states
    final ReachedSet initialReachedSet = reachedSetFactory.create();
    ReachedSetUtils.addReachedStatesToOtherReached(pReachedSet, initialReachedSet);

    do {
      // Run the CPA algorithm
      final boolean result = wrappedAlgorithm.run(pReachedSet);

      // We use one set of reached states
      //    ... and separate the state space using an automaton!
      final BooleanFormula pcViolation = getPreconditionForViolation(pReachedSet);
      final BooleanFormula pcValid = getPreconditionForValidity(pReachedSet);

      if (isDisjoint(pcViolation, pcValid)) {
        // TODO: Provide the result somehow
        return true && result;
      }

      // Get arbitrary traces...(without disjunctions)
      // ... one to the location that violates the specification
      // ... and one to the location that represents the exit location
      final ARGPath traceViolation = getTrace(pReachedSet, PreconditionPartition.VIOLATING);
      final ARGPath traceValid = getTrace(pReachedSet, PreconditionPartition.VALID);

      // Check the disjointness of the WP for the two traces...
      final BooleanFormula pcViolatingTrace = helper.getPreconditionOfPath(traceViolation);
      final BooleanFormula pcValidTrace = helper.getPreconditionOfPath(traceValid);

      if (!isDisjoint(pcViolatingTrace, pcValidTrace)) {
        logger.log(Level.WARNING, "non-determinism in program."); // This warning is taken 1:1 from the Seghir/Kroening paper
        return false;
      }

      // Refine the precision so that the
      // abstraction on the two traces is disjoint
      Collection<BooleanFormula> newPredicates = refiner.refine(traceViolation, traceValid);

      // Add the predicates to the precision
      // TODO: Location-specific?


      // Restart with the initial set of reached states
      // with the new precision!
      pReachedSet.clear();
      ReachedSetUtils.addReachedStatesToOtherReached(initialReachedSet, pReachedSet);

    } while (true);
  }

}
