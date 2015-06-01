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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Options(prefix="bmc")
public class BMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(secure=true, description="Check reachability of target states after analysis "
      + "(classical BMC). The alternative is to check the reachability "
      + "as soon as the target states are discovered, which is done if "
      + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  @Option(secure=true, description="dump counterexample formula to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate dumpCounterexampleFormula = PathTemplate.ofFormatString("ErrorPath.%d.smt2");

  private final ConfigurableProgramAnalysis cpa;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final MachineModel machineModel;

  private final Configuration config;
  private final CFA cfa;

  public BMCAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCFA)
                      throws InvalidConfigurationException, CPAException {
    super(pAlgorithm, pCPA, pConfig, pLogger, pReachedSetFactory, pShutdownNotifier, pCFA,
        new BMCStatistics(),
        false /* no invariant generator */);
    pConfig.inject(this);

    cpa = pCPA;
    config = pConfig;
    cfa = pCFA;

    PredicateCPA predCpa = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();
    machineModel = predCpa.getMachineModel();
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    try {
      return super.run(reachedSet);
    } finally {
      invariantGenerator.cancel();
    }
  }

  @Override
  protected CandidateGenerator getCandidateInvariants(CFA cfa,
      Collection<CFANode> targetLocations) {
    if (targetLocations.isEmpty()) {
      return CandidateGenerator.EMPTY_GENERATOR;
    } else {
      return new StaticCandidateProvider(Sets.<CandidateInvariant>newHashSet(TargetLocationCandidateInvariant.INSTANCE));
    }
  }

  @Override
  protected boolean boundedModelCheck(final ReachedSet pReachedSet, final ProverEnvironment pProver, CandidateInvariant pInductionProblem) throws CPATransferException, InterruptedException, SolverException {
    if (!checkTargetStates) {
      return true;
    }
    
    return super.boundedModelCheck(pReachedSet, pProver, pInductionProblem);
  }

  /**
   * This method tries to find a feasible path to (one of) the target state(s).
   * It does so by asking the solver for a satisfying assignment.
   */
  @Override
  protected void analyzeCounterexample(final ReachedSet pReachedSet, final ProverEnvironment pProver)
      throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return;
    }

    stats.errorPathCreation.start();
    try {
      logger.log(Level.INFO, "Error found, creating error path");

      Set<ARGState> targetStates = from(pReachedSet).filter(IS_TARGET_STATE).filter(ARGState.class).toSet();

      final boolean shouldCheckBranching;
      if (targetStates.size() == 1) {
        ARGState state = Iterables.getOnlyElement(targetStates);
        while (state.getParents().size() == 1 && state.getChildren().size() <= 1) {
          state = Iterables.getOnlyElement(state.getParents());
        }
        shouldCheckBranching = (state.getParents().size() > 1)
            || (state.getChildren().size() > 1);
      } else {
        shouldCheckBranching = true;
      }

      if (shouldCheckBranching) {
        Iterable<ARGState> arg = from(pReachedSet).filter(ARGState.class);

        // get the branchingFormula
        // this formula contains predicates for all branches we took
        // this way we can figure out which branches make a feasible path
        BooleanFormula branchingFormula = pmgr.buildBranchingFormula(arg);

        if (bfmgr.isTrue(branchingFormula)) {
          logger.log(Level.WARNING, "Could not create error path because of missing branching information!");
          return;
        }

        // add formula to solver environment
        pProver.push(branchingFormula);
      }

      Model model;

      try {

        // need to ask solver for satisfiability again,
        // otherwise model doesn't contain new predicates
        boolean stillSatisfiable = !pProver.isUnsat();

        if (!stillSatisfiable) {
          // should not occur
          logger.log(Level.WARNING, "Could not create error path information because of inconsistent branching information!");
          return;
        }

        model = pProver.getModel();

      } catch (SolverException e) {
        logger.log(Level.WARNING, "Solver could not produce model, cannot create error path.");
        logger.logDebugException(e);
        return;

      } finally {
        if (shouldCheckBranching) {
          pProver.pop(); // remove branchingFormula
        }
      }


      // get precise error path
      Map<Integer, Boolean> branchingInformation = pmgr.getBranchingPredicateValuesFromModel(model);
      ARGState root = (ARGState)pReachedSet.getFirstState();

      ARGPath targetPath;
      try {
        targetPath = ARGUtils.getPathFromBranchingInformation(root, pReachedSet.asCollection(), branchingInformation);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return;
      }

      // create and store CounterexampleInfo object
      CounterexampleInfo counterexample;


      // replay error path for a more precise satisfying assignment
      Solver solver = this.solver;
      PathFormulaManager pmgr = this.pmgr;

      // SMTInterpol does not support reusing the same solver
      if (solver.getFormulaManager().getVersion().toLowerCase().contains("smtinterpol")) {
        try {
          solver = Solver.create(config, logger, shutdownNotifier);
          FormulaManagerView formulaManager = solver.getFormulaManager();
          pmgr = new PathFormulaManagerImpl(formulaManager, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
        } catch (InvalidConfigurationException e) {
          // Configuration has somehow changed and can no longer be used to create the solver and path formula manager
          logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
          return;
        }
      }
      PathChecker pathChecker = new PathChecker(logger, shutdownNotifier, pmgr, solver, machineModel);
      try {
        CounterexampleTraceInfo info = pathChecker.checkPath(targetPath.getInnerEdges());

        if (info.isSpurious()) {
          logger.log(Level.WARNING, "Inconsistent replayed error path!");
          counterexample = CounterexampleInfo.feasible(targetPath, RichModel
              .of(model));

        } else {
          counterexample = CounterexampleInfo.feasible(targetPath, info.getModel());

          counterexample.addFurtherInformation(fmgr.dumpFormula(bfmgr.and(info.getCounterExampleFormulas())),
              dumpCounterexampleFormula);
        }

      } catch (SolverException | CPATransferException e) {
        // path is now suddenly a problem
        logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
        counterexample = CounterexampleInfo.feasible(targetPath, RichModel
            .of(model));
      }
      ((ARGCPA) cpa).addCounterexample(targetPath.getLastState(), counterexample);

    } finally {
      stats.errorPathCreation.stop();
    }
  }
}
