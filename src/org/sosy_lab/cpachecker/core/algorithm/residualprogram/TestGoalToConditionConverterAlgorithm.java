// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.NestingAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.TraversalMethod;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.stopatleaves.StopAtLeavesCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;

/** Algorithm to convert a list of covered test goal labels to a condition. */
@Options(prefix = "conditional_testing")
public class TestGoalToConditionConverterAlgorithm extends NestingAlgorithm {
  private final Algorithm outerAlgorithm;
  private final ConfigurableProgramAnalysis outerCpa;
  private final IGoalFindingStrategy goalFindingStrategy;

  @Option(secure = true, name = "strategy", required = true, description = "The strategy to use")
  Strategy strategy;

  private Algorithm backwardsCpaAlgorithm;
  private ConfigurableProgramAnalysis backwardsCpa;

  @Option(
      secure = true,
      required = true,
      name = "inputfile",
      description = "The input file with all goals that were previously reached")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path inputfile;

  private final CFA cfa;

  public TestGoalToConditionConverterAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Algorithm pOuter,
      ConfigurableProgramAnalysis pOuterCpa)
      throws InvalidConfigurationException, InterruptedException {

    super(pConfig, pLogger, pShutdownNotifier, Specification.alwaysSatisfied());
    pConfig.inject(this);

    cfa = pCfa;

    switch (strategy) {
      case NAIVE:
        goalFindingStrategy = new LeafGoalStrategy();
        break;
      case PROPAGATION:
        goalFindingStrategy = new LeafGoalWithPropagationStrategy();
        break;
      default:
        throw new InvalidConfigurationException("A strategy must be selected!");
    }
    try {
      var backwardsCpaTriple =
          createAlgorithm(
              Path.of("config/components/goalConverterBackwardsSearch.properties"),
              pCfa.getMainFunction(),
              pCfa,
              ShutdownManager.createWithParent(pShutdownNotifier),
              AggregatedReachedSets.empty(),
              ImmutableList.of(
                  "analysis.testGoalConverter",
                  "cpa",
                  "specification",
                  "ARGCPA.cpa",
                  "cpa.property_reachability.noFollowBackwardsUnreachable",
                  "analysis.initialStatesFor",
                  "CompositeCPA.cpas",
                  "cpa.callstack.traverseBackwards",
                  "analysis.collectAssumptions",
                  "assumptions.automatonFile"),
              new HashSet<>());

      backwardsCpaAlgorithm = backwardsCpaTriple.getFirst();
      backwardsCpa = backwardsCpaTriple.getSecond();
    } catch (CPAException | IOException e) {
      throw new InvalidConfigurationException("Couldn't create backwards CPA algorithm!", e);
    }

    if (pOuter == null || pOuterCpa == null) {
      throw new InvalidConfigurationException("A valid pOuter algorithm must be specified!");
    }

    outerAlgorithm = pOuter;
    outerCpa = pOuterCpa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    try {
      var leafGoals = getPartitionedLeafGoals();

      var stopAtLeavesCpa =
          CPAs.retrieveCPAOrFail(outerCpa, StopAtLeavesCPA.class, StopAtLeavesCPA.class);
      stopAtLeavesCpa.setLeaves(leafGoals.get(LeafStates.UNCOVERED));

      return outerAlgorithm.run(reachedSet);
    } catch (InvalidConfigurationException e) {
      throw new CPAException(e.getLocalizedMessage());
    }
  }

  /**
   * Reads the input file and extracts all covered goals.
   *
   * @return A list of all covered goals.
   * @throws CPAException when file could not be read or another IO error occurs.
   */
  private Set<String> getCoveredGoals() throws CPAException {
    try (var lines = Files.lines(inputfile)) {
      return lines.collect(ImmutableSet.toImmutableSet());
    } catch (IOException e) {
      throw new CPAException(e.getLocalizedMessage());
    }
  }

  /**
   * Gets all leaf goals in the program. They are partitioned into LeafStates.COVERED and
   * LeaftStates.UNCOVERED.
   *
   * @return A map with two keys (COVERED, UNCOVERED) of all leaf goals
   * @throws CPAException Thrown when there is an error in the cpa algorithm
   */
  private Map<LeafStates, List<CFANode>> getPartitionedLeafGoals()
      throws CPAException, InterruptedException {

    var reachedSet = buildBackwardsReachedSet();
    var coveredGoals = getCoveredGoals();

    backwardsCpaAlgorithm.run(reachedSet);

    Deque<ARGState> waitList = new ArrayDeque<>();
    // We're doing a backwards analysis; hence the first state here is the end of the ARG
    waitList.add((ARGState) reachedSet.getFirstState());

    return goalFindingStrategy.findGoals(waitList, coveredGoals);
  }

  /**
   * This function builds a reached set that has PROGRAM_SINKS as initial states. We need to build
   * it by hand since the option
   *
   * <pre>analysis.initialStatesFor = PROGRAM_SINKS</pre>
   *
   * is only read once when constructing @see{CPAchecker}.
   *
   * @return A reached set that has the PROGRAM_SINKS as initial states
   */
  private ReachedSet buildBackwardsReachedSet() throws InterruptedException {
    var reachedSet = new PartitionedReachedSet(backwardsCpa, TraversalMethod.DFS);
    var initialLocations =
        ImmutableSet.<CFANode>builder()
            .addAll(
                CFAUtils.getProgramSinks(
                    cfa, cfa.getLoopStructure().orElseThrow(), cfa.getMainFunction()))
            .build();

    for (var loc : initialLocations) {
      var partition = StateSpacePartition.getDefaultPartition();
      var state = backwardsCpa.getInitialState(loc, partition);
      var precision = backwardsCpa.getInitialPrecision(loc, partition);
      reachedSet.add(state, precision);
    }

    return reachedSet;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {}

  /** States a goal can be in. */
  enum LeafStates {
    COVERED(true),
    UNCOVERED(false);

    private final boolean isCovered;

    LeafStates(boolean b) {
      isCovered = b;
    }

    public boolean isCovered() {
      return isCovered;
    }
  }

  /**
   * A list of the strategy to use. Is used by our configuration. Note: Actually this is bad style.
   * We should rather use reflection. If someone were to implement a new strategy they also would
   * have to modify this enum which shouldn't be necessary.
   */
  public enum Strategy {
    NAIVE,
    PROPAGATION
  }
}
