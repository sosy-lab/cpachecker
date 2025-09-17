// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rangedExecInputSequences;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.rangedExecInput.RangedExecutionInputComputation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.aggressiveloopbound.AggressiveLoopBoundCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "cpa.rangedExecutionInput")
public class RangedExecutionInputComputationSequence implements Algorithm {

  private final CFA cfa;

  @Option(
      secure = true,
      name = "testcaseName",
      description = "Names of the files for the testcases")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testcaseName = Path.of("testcase.0.xml");

  @Option(
      secure = true,
      name = "abortOnTrueSequence",
      description = "Throw an exception, if only a true sequence is generated")
  private boolean abortOnTrueSequence = false;

  @Option(
      secure = true,
      name = "blacklist",
      description = "List of Functions not exported in the sequences")
  private Set<String> blacklist = ImmutableSet.of("__VERIFIER_assert");

  private final Algorithm algorithm;

  private final SequenceGenUtils utils;

  public RangedExecutionInputComputationSequence(
      Configuration config,
      Algorithm pAlgorithm,
      LogManager pLogger,
      CFA pCfa,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, CPAException {
    config.inject(this, RangedExecutionInputComputationSequence.class);
    algorithm = pAlgorithm;
    this.cfa = pCfa;
    LogManager logger = Objects.requireNonNull(pLogger);

    utils = new SequenceGenUtils(logger, cfa);
    ControlAutomatonCPA automatonCPA =
        CPAs.retrieveCPAOrFail(
            pCpa, ControlAutomatonCPA.class, RangedExecutionInputComputation.class);

    AggressiveLoopBoundCPA loopBoundCPA =
        CPAs.retrieveCPAOrFail(
            pCpa, AggressiveLoopBoundCPA.class, RangedExecutionInputComputation.class);
    loopBoundCPA.setAutomatonTramsferRelation(automatonCPA.getTransferRelation());
    if (testcaseName == null) {
      testcaseName = Path.of("output/testcase.0.xml");
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    if (!(pReached instanceof PartitionedReachedSet reached)) {
      throw new CPAException("Expecting a partioned reached set");
    }

    // Check, if there is any random call in the program or any loop.
    // if not, exit directly
    if (!hasLoop()) {
      throw new CPAException("Cannot generate a testcase, because there is no loop in the program");
    }

    // run algorithm
    algorithm.run(reached);
    //    if (reached.hasWaitingState()) {
    //      // Nested algortihm is not finished, hence do another round by returning to loop in
    // calling
    //      // class
    //      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    //
    //    } else {

    AbstractState last = reached.getLastState();
    AbstractState first = reached.getFirstState();

    Set<ARGPath> paths =
        ARGUtils.getAllPathsFromTo(
            AbstractStates.extractStateByType(first, ARGState.class),
            AbstractStates.extractStateByType(last, ARGState.class));

    if (paths.size() != 1) {
      throw new CPAException(
          "There are more than one path present. We cannot compute a testcase for this!");
    }

    try {

      List<Pair<Boolean, Integer>> inputs =
          utils.computeSequenceForLoopbound(
              paths.stream().findFirst().orElseThrow(), blacklist, Optional.empty());
      if (inputs.isEmpty()) {
        throw new CPAException("We failed to generate a sequence, aborting!");
      }
      if (abortOnTrueSequence && inputs.stream().allMatch(b -> b.getFirst())) {
        throw new CPAException("We generated a sequence with is only true, as configured, abort!");
      }
      utils.printFileToOutput(inputs, testcaseName);
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    } catch (IOException e) {
      throw new CPAException(Throwables.getStackTraceAsString(e));
    }
    //    }
  }

  private boolean hasLoop() {
    return this.cfa.getLoopStructure().isPresent()
        && this.cfa.getLoopStructure().orElseThrow().getCount() > 0;
  }
}
