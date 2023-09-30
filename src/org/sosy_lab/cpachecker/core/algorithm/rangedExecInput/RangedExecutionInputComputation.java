// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rangedExecInput;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.rangedExecInputSequences.SequenceGenUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.aggressiveloopbound.AggressiveLoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.rangedExecutionInput")
public class RangedExecutionInputComputation implements Algorithm {

  private final CFA cfa;
  private final LogManager logger;

  @Option(
      secure = true,
      name = "namesOfRandomFunctions",
      description =
          "List of names (or a part of the name) of functions, that return a random value")
  private ImmutableSet<String> namesOfRandomFunctions =
      ImmutableSet.of("rand", "__VERIFIER_nondet_");

  @Option(
      secure = true,
      name = "blacklist",
      description = "List of Functions not exported in the sequences")
  private Set<String> blacklist = ImmutableSet.of("__VERIFIER_assert");

  @Option(
      secure = true,
      name = "testcaseName",
      description = "Names of the files for the testcases")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testcaseName = Path.of("testcase.0.xml");

  @Option(
      secure = true,
      name = "generateSequences",
      description = "Generate a sequence instead of a testcase")
  private boolean generateSequences = false;

  @Option(
      secure = true,
      name = "abortOnTrueSequence",
      description = "Throw an exception, if only a true sequence is generated")
  private boolean abortOnTrueSequence = false;

  private final Algorithm algorithm;
  Solver solver;
  FormulaManagerView fmgr;

  private final TestcaseGenUtils utils;
  private final SequenceGenUtils sequenceUtils;

  public RangedExecutionInputComputation(
      Configuration config,
      Algorithm pAlgorithm,
      LogManager pLogger,
      CFA pCfa,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, CPAException {
    config.inject(this, RangedExecutionInputComputation.class);
    algorithm = pAlgorithm;
    this.cfa = pCfa;
    logger = Objects.requireNonNull(pLogger);

    @SuppressWarnings("resource")
    @NonNull PredicateCPA predCPA =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, RangedExecutionInputComputation.class);
    solver = predCPA.getSolver();
    ControlAutomatonCPA automatonCPA =
        CPAs.retrieveCPAOrFail(
            pCpa, ControlAutomatonCPA.class, RangedExecutionInputComputation.class);

    AggressiveLoopBoundCPA loopBoundCPA =
        CPAs.retrieveCPAOrFail(
            pCpa, AggressiveLoopBoundCPA.class, RangedExecutionInputComputation.class);
    loopBoundCPA.setAutomatonTramsferRelation(automatonCPA.getTransferRelation());

    fmgr = solver.getFormulaManager();
    PathFormulaManager pfManager = predCPA.getPathFormulaManager();
    utils = new TestcaseGenUtils(namesOfRandomFunctions, solver, logger, pfManager, fmgr, cfa);
    sequenceUtils = new SequenceGenUtils(logger, cfa);
    if (testcaseName == null) {
      testcaseName = Path.of("output/testcase.0.xml");
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    if (!(pReached instanceof PartitionedReachedSet)) {
      throw new CPAException("Expecting a partioned reached set");
    }

    // Check, if there is any random call in the program or any loop.
    // if not, exit directly
    if (!hasLoop() || !hasRandom()) {
      throw new CPAException(
          "Cannot generate a testcase, because there is no random call or no loop in the program");
    }

    PartitionedReachedSet reached = (PartitionedReachedSet) pReached;

    // run algorithm

    algorithm.run(reached);
    //    if (reached.hasWaitingState()) {
    //      // Nested algortihm is not finished, hence do another round by returning to loop in
    // calling
    //      // class
    //      return status;
    //
    //    } else {

    AbstractState last = reached.getLastState();
    AbstractState first = reached.getFirstState();

    Set<ARGPath> paths =
        ARGUtils.getAllPathsFromTo(
            AbstractStates.extractStateByType(first, ARGState.class),
            AbstractStates.extractStateByType(last, ARGState.class));
    logger.log(Level.INFO, Lists.newArrayList(paths).get(0));
    if (paths.size() != 1) {
      throw new CPAException(
          "There are more than one path present. We cannot compute a testcase for this!");
    }
    if (generateSequences) {
      try {
        List<Pair<Boolean, Integer>> inputs =
            utils.computeSequenceForLoopbound(paths.stream().findFirst().get(), blacklist);
        if (inputs.isEmpty()) {
          throw new CPAException("We failed to generate a sequence, aborting!");
        }
        if (abortOnTrueSequence && inputs.stream().allMatch(b -> b.getFirst())) {
          throw new CPAException(
              "We generated a sequence with is only true, as configured, abort!");
        }
        sequenceUtils.printFileToOutput(inputs, testcaseName);
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      } catch (SolverException | IOException pE) {
        throw new CPAException(Throwables.getStackTraceAsString(pE));
      }
    } else {
      try {
        List<Pair<CIdExpression, Long>> inputs =
            utils.computeInputForLoopbound(paths.stream().findFirst().get());
        if (inputs.isEmpty()) {
          throw new CPAException("We failed to generate a sequence, aborting!");
        }
        utils.printFileToPutput(inputs, testcaseName);
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      } catch (SolverException | IOException pE) {
        throw new CPAException(Throwables.getStackTraceAsString(pE));
      }
    }
  }
  //  }

  private boolean hasRandom() {
    for (CFANode node : cfa.nodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (edge instanceof CStatementEdge
            && ((CStatementEdge) edge).getStatement() instanceof CAssignment) {
          if (CFAEdgeUtils.getRightHandSide(edge) instanceof CFunctionCallExpression
              && namesOfRandomFunctions.stream()
                  .anyMatch(
                      name ->
                          ((CFunctionCallExpression)
                                  Objects.requireNonNull(CFAEdgeUtils.getRightHandSide(edge)))
                              .getFunctionNameExpression()
                              .toString()
                              .startsWith(name))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean hasLoop() {
    return this.cfa.getLoopStructure().isPresent()
        && this.cfa.getLoopStructure().get().getCount() > 0;
  }
}
