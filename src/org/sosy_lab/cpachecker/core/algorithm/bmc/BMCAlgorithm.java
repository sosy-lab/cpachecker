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

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPathExporter;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.GraphBuilder;
import org.sosy_lab.cpachecker.cpa.arg.InvariantProvider;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix="bmc")
public class BMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(secure=true, description="Check reachability of target states after analysis "
      + "(classical BMC). The alternative is to check the reachability "
      + "as soon as the target states are discovered, which is done if "
      + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  @Option(secure=true, description="Export auxiliary invariants used for induction.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path invariantsExport = Paths.get("invariants.graphml");

  private final ConfigurableProgramAnalysis cpa;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  private final Configuration config;
  private final CFA cfa;
  private final AssignmentToPathAllocator assignmentToPathAllocator;

  private final ARGPathExporter argPathExporter;

  public BMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {
    super(
        pAlgorithm,
        pCPA,
        pConfig,
        pLogger,
        pReachedSetFactory,
        pShutdownManager,
        pCFA,
        specification,
        new BMCStatistics(),
        false /* no invariant generator */,
        pAggregatedReachedSets);
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
    MachineModel machineModel = pCFA.getMachineModel();

    assignmentToPathAllocator = new AssignmentToPathAllocator(config, shutdownNotifier, pLogger, machineModel);
    argPathExporter = new ARGPathExporter(config, logger, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    try {
      return super.run(reachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure", e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    if (getTargetLocations().isEmpty() || !cfa.getAllLoopHeads().isPresent()) {
      return CandidateGenerator.EMPTY_GENERATOR;
    } else {
      Set<CFANode> loopHeads = getLoopHeads();
      return new StaticCandidateProvider(
          Sets.<CandidateInvariant>newHashSet(new TargetLocationCandidateInvariant(loopHeads)));
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
  protected void analyzeCounterexample(
      final BooleanFormula pCounterexampleFormula,
      final ReachedSet pReachedSet,
      final ProverEnvironment pProver)
      throws CPATransferException, InterruptedException {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.INFO, "Error found, but error path cannot be created without ARGCPA");
      return;
    }

    stats.errorPathCreation.start();
    try {
      logger.log(Level.INFO, "Error found, creating error path");

      Set<ARGState> targetStates = from(pReachedSet).filter(IS_TARGET_STATE).filter(ARGState.class).toSet();
      Set<ARGState> redundantStates = redundantStates(targetStates);
      redundantStates.forEach(state -> {
        state.removeFromARG();
      });
      pReachedSet.removeAll(redundantStates);
      targetStates = Sets.difference(targetStates, redundantStates);

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
        Set<ARGState> arg = from(pReachedSet).filter(ARGState.class).toSet();

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

      List<ValueAssignment> model;
      try {
        // need to ask solver for satisfiability again,
        // otherwise model doesn't contain new predicates
        boolean stillSatisfiable = !pProver.isUnsat();

        if (!stillSatisfiable) {
          // should not occur
          logger.log(Level.WARNING, "Could not create error path information because of inconsistent branching information!");
          return;
        }

        model = pProver.getModelAssignments();

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
        Set<AbstractState> arg = pReachedSet.asCollection();
        targetPath = ARGUtils.getPathFromBranchingInformation(root, arg, branchingInformation);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not create error path");
        return;
      }

      BooleanFormula cexFormula = pCounterexampleFormula;

      // replay error path for a more precise satisfying assignment
      PathChecker pathChecker;
      try {
        Solver solver = this.solver;
        PathFormulaManager pmgr = this.pmgr;

        if (solver.getVersion().toLowerCase().contains("smtinterpol")) {
          // SMTInterpol does not support reusing the same solver
          solver = Solver.create(config, logger, shutdownNotifier);
          FormulaManagerView formulaManager = solver.getFormulaManager();
          pmgr = new PathFormulaManagerImpl(formulaManager, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);
          // cannot dump pCounterexampleFormula, PathChecker would use wrong FormulaManager for it
          cexFormula = solver.getFormulaManager().getBooleanFormulaManager().makeTrue();
        }

        pathChecker = new PathChecker(config, logger, pmgr, solver, assignmentToPathAllocator);

      } catch (InvalidConfigurationException e) {
        // Configuration has somehow changed and can no longer be used to create the solver and path formula manager
        logger.logUserException(Level.WARNING, e, "Could not replay error path to get a more precise model");
        return;
      }

      CounterexampleTraceInfo cexInfo =
          CounterexampleTraceInfo.feasible(
              ImmutableList.<BooleanFormula>of(cexFormula), model, branchingInformation);
      CounterexampleInfo counterexample =
          pathChecker.createCounterexample(targetPath, cexInfo, shouldCheckBranching);
      counterexample.getTargetPath().getLastState().addCounterexampleInformation(counterexample);

    } finally {
      stats.errorPathCreation.stop();
    }
  }

  private Set<ARGState> redundantStates(Iterable<ARGState> pStates) {
    Multimap<ARGState, ARGState> parentToTarget = HashMultimap.create();
    for (ARGState state : FluentIterable.from(pStates).filter(AbstractStates.IS_TARGET_STATE)) {
      if (state.getChildren().isEmpty()) {
        Collection<ARGState> parents = state.getParents();
        for (ARGState parent : parents) {
          parentToTarget.put(parent, state);
        }
      }
    }
    Set<ARGState> redundantStates = Sets.newHashSet();
    for (Map.Entry<ARGState, Collection<ARGState>> family : parentToTarget.asMap().entrySet()) {
      ARGState parent = family.getKey();
      Collection<ARGState> children = family.getValue();
      Set<CFAEdge> edges = FluentIterable.from(children).transformAndConcat(parent::getEdgesToChild).toSet();
      if (edges.size() == 1 && !(edges.iterator().next() instanceof AssumeEdge)) {
        Iterables.addAll(redundantStates, Iterables.skip(children, 1));
      }
    }
    return redundantStates;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
            ARGState rootState =
                AbstractStates.extractStateByType(pReached.getFirstState(), ARGState.class);
            if (rootState != null && invariantsExport != null) {
              ExpressionTreeSupplier tmpExpressionTreeSupplier =
                  ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
              if (invariantGenerator.isStarted()) {
                try {
                  if (invariantGenerator instanceof KInductionInvariantGenerator) {
                    tmpExpressionTreeSupplier =
                        ((KInductionInvariantGenerator) invariantGenerator)
                            .getExpressionTreeSupplier();
                  } else {
                    tmpExpressionTreeSupplier =
                        new ExpressionTreeInvariantSupplier(invariantGenerator.get(), cfa);
                  }
                } catch (CPAException | InterruptedException e1) {
                  tmpExpressionTreeSupplier =
                      ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
                }
              }
              final ExpressionTreeSupplier expSup = tmpExpressionTreeSupplier;

              try (Writer w = MoreFiles.openOutputFile(invariantsExport, StandardCharsets.UTF_8)) {
                argPathExporter.writeProofWitness(
                    w,
                    rootState,
                    Predicates.alwaysTrue(),
                    Predicates.alwaysTrue(),
                    GraphBuilder.CFA_FULL,
                    new InvariantProvider() {

                      @Override
                      public ExpressionTree<Object> provideInvariantFor(
                          CFAEdge pCFAEdge,
                          Optional<? extends Collection<? extends ARGState>> pStates) {
                        CFANode node = pCFAEdge.getSuccessor();
                        ExpressionTree<Object> result = expSup.getInvariantFor(node);
                        if (ExpressionTrees.getFalse().equals(result) && !pStates.isPresent()) {
                          return ExpressionTrees.getTrue();
                        }
                        return result;
                      }
                    });
              } catch (IOException e) {
                logger.logUserException(
                    Level.WARNING, e, "Could not write invariants to file " + invariantsExport);
              }
            }
          }

          @Override
          public String getName() {
            return null; // return null because we do not print statistics
          }
        });
  }
}
