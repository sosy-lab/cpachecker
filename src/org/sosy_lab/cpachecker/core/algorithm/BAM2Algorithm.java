/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA2;
import org.sosy_lab.cpachecker.cpa.bam.BlockSummaryMissingException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class BAM2Algorithm implements Algorithm {

  private final LogManager logger;
  private final BAMCPA2 bamcpa;
  private final CPAAlgorithmFactory algorithmFactory;

  public BAM2Algorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    bamcpa = (BAMCPA2) pCpa;
    logger = pLogger;
    algorithmFactory = new CPAAlgorithmFactory(bamcpa, logger, pConfig, pShutdownNotifier);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet mainReachedSet) throws CPAException, InterruptedException {
    try {
      return run0(mainReachedSet);
    } catch (CPAException | InterruptedException e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private AlgorithmStatus run0(final ReachedSet mainReachedSet)
      throws Exception {

    ReachedSetDependencyGraph dependencyGraph = new ReachedSetDependencyGraph();
    boolean targetStateFound = false;
    Task task =
        new ReachedSetAnalyzer(mainReachedSet, mainReachedSet, dependencyGraph, targetStateFound);

    while (task != null) {
      if (task.targetStateFound) {
        targetStateFound = true;
      }
      task = task.call();
    }

    assert targetStateFound
        || (dependencyGraph.dependsOn.isEmpty()
            && dependencyGraph.dependsFrom.isEmpty()) : "dependencyGraph:" + dependencyGraph;

    readdStatesToWaitlists(dependencyGraph);

    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private void updateCacheEntryForBlockResult(
      ReachedSet rs, final ReachedSet mainReachedSet, boolean endsWithTargetState) {
    if (rs != mainReachedSet) {
      // update BAM-cache
      // we do not cache main reached set, because it should not be used internally
      AbstractState reducedInitialState = rs.getFirstState();
      Precision reducedInitialPrecision = rs.getPrecision(reducedInitialState);
      Block block = getBlockForState(reducedInitialState);
      Collection<AbstractState> exitStates = new ArrayList<>();
      if (endsWithTargetState) {
        exitStates.add(rs.getLastState());
      } else {
        for (AbstractState returnState : AbstractStates.filterLocations(rs,
            block.getReturnNodes())) {
          if (((ARGState) returnState).getChildren().isEmpty()) {
            exitStates.add(returnState);
          }
        }
      }
      Pair<ReachedSet, Collection<AbstractState>> check =
          bamcpa.getCache().get(reducedInitialState, reducedInitialPrecision, block);
      assert check.getFirst() == rs;
      assert check.getSecond() == null;
      bamcpa.getCache().put(reducedInitialState, reducedInitialPrecision, block, exitStates, null);
    }
  }

  private void reAddStatesToDependingWaitlists(
      ReachedSet rs, ReachedSetDependencyGraph dependencyGraph) {
    for (Entry<AbstractState, ReachedSet> entry : dependencyGraph.getDependsFrom(rs).entrySet()) {
      logger.log(
          Level.INFO,
          "re-adding",
          getId(entry.getKey()),
          "to",
          getId(entry.getValue().getFirstState()));
      entry.getValue().reAddToWaitlist(entry.getKey());
    }
  }

  private Block getBlockForState(AbstractState state) {
    CFANode location = extractLocation(state);
    assert bamcpa.getBlockPartitioning()
        .isCallNode(location) : "root of reached-set must be located at block entry.";
    return bamcpa.getBlockPartitioning().getBlockForCallNode(location);
  }

  /**
   * Cleanup when termination the algorithm. If a target state was not found, the dependency graph
   * should be empty If a target state was found, we must re-add all block entry states to their
   * reached-sets in order to have valid reached-sets and to re-explore them later.
   */
  private void readdStatesToWaitlists(ReachedSetDependencyGraph dependencyGraph) {
    for (Cell<ReachedSet, AbstractState, ReachedSet> entry : dependencyGraph.dependsOn.cellSet()) {
      entry.getRowKey().reAddToWaitlist(entry.getColumnKey());
    }
    dependencyGraph.dependsOn.clear();
    dependencyGraph.dependsFrom.clear();
  }

  private static int getId(final AbstractState state) {
    return ((ARGState) state).getStateId();
  }

  private static final class ReachedSetDependencyGraph {

    private final Table<ReachedSet, AbstractState, ReachedSet> dependsOn = HashBasedTable.create();
    private final Table<ReachedSet, AbstractState, ReachedSet> dependsFrom =
        HashBasedTable.create();

    /**
     * Adds a dependency for a child reachedsets. The child has to be analyzed first, afterwards the
     * parent can be analyzed further.
     */
    void addDependency(final ReachedSet parent, final ReachedSet child, final AbstractState state) {
      assert dependsOn.size() == dependsFrom.size();
      assert parent.contains(state) : "parent reachedset must contain entry state";
      dependsOn.put(parent, state, child);
      dependsFrom.put(child, state, parent);
      assert dependsOn.size() == dependsFrom.size();
    }

    void unregisterDependency(final ReachedSet child) {
      assert dependsOn.size() == dependsFrom.size();
      for (Entry<AbstractState, ReachedSet> entry : dependsFrom.row(child).entrySet()) {
        dependsOn.remove(entry.getValue(), entry.getKey());
      }
      dependsFrom.row(child).clear();
      assert dependsOn.size() == dependsFrom.size() : "sizes do not match: " + this;
    }

    ImmutableMap<AbstractState, ReachedSet> getDependsFrom(ReachedSet child) {
      return ImmutableMap.copyOf(dependsFrom.row(child));
    }

    ImmutableSet<ReachedSet> getDependsOn(final ReachedSet parent) {
      return ImmutableSet.copyOf(dependsOn.row(parent).values());
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder();

      str.append("dependsOn={");
      for (Cell<ReachedSet, AbstractState, ReachedSet> cell : dependsOn.cellSet()) {
        str.append(getId(cell.getRowKey().getFirstState()))
            .append("@")
            .append(getId(cell.getColumnKey()))
            .append("=>")
            .append(getId(cell.getValue().getFirstState()))
            .append(", ");
      }
      str.append("}, ");

      str.append("dependsFrom={");
      for (Cell<ReachedSet, AbstractState, ReachedSet> cell : dependsFrom.cellSet()) {
        str.append(getId(cell.getRowKey().getFirstState()))
            .append("<=")
            .append(getId(cell.getValue().getFirstState()))
            .append("@")
            .append(getId(cell.getColumnKey()))
            .append(", ");
      }
      str.append("}");

      return str.toString();
    }
  }

  abstract class Task {

    final ReachedSet rs;
    final ReachedSet mainReachedSet;
    final ReachedSetDependencyGraph dependencyGraph;
    boolean targetStateFound;

    Task(ReachedSet pRs, ReachedSet pMainReachedSet,
        ReachedSetDependencyGraph pDependencyGraph,
        boolean pTargetStateFound) {
      rs = pRs;
      mainReachedSet = pMainReachedSet;
      dependencyGraph = pDependencyGraph;
      targetStateFound = pTargetStateFound;
    }

    @SuppressWarnings("unused")
    abstract Task call() throws CPAException, InterruptedException;
  }

  class ReachedSetAnalyzer extends Task {

    ReachedSetAnalyzer(ReachedSet pRs, ReachedSet pMainReachedSet,
        ReachedSetDependencyGraph pDependencyGraph, boolean pTargetStateFound) {
      super(pRs, pMainReachedSet, pDependencyGraph, pTargetStateFound);
    }

    @Override
    public Task call() throws CPAException, InterruptedException {
      logger.log(Level.INFO, "using reached-set", getId(rs.getFirstState()));

      try {
        CPAAlgorithm algorithm = algorithmFactory.newInstance();

        @SuppressWarnings("unused")
        AlgorithmStatus tmpStatus = algorithm.run(rs);
        return new TerminationHandler(rs, mainReachedSet, dependencyGraph, targetStateFound);

      } catch (BlockSummaryMissingException bsme) {
        return new MissingBlockHandler(rs, mainReachedSet, dependencyGraph, targetStateFound, bsme);
      }
    }
  }

  class TerminationHandler extends Task {

    TerminationHandler(ReachedSet pRs, ReachedSet pMainReachedSet,
        ReachedSetDependencyGraph pDependencyGraph, boolean pTargetStateFound) {
      super(pRs, pMainReachedSet, pDependencyGraph, pTargetStateFound);
    }

    @Override
    public Task call() {
      boolean isFinished = !rs.hasWaitingState() && dependencyGraph.getDependsOn(rs).isEmpty();
      boolean endsWithTargetState =
          rs.getLastState() != null && AbstractStates.isTargetState(rs.getLastState());

      logger.log(
          Level.INFO,
          "leaving reached set",
          getId(rs.getFirstState()),
          ", isFinished=",
          isFinished,
          ", endsWithTargetState=",
          endsWithTargetState);

      if (isFinished || endsWithTargetState) {
        reAddStatesToDependingWaitlists(rs, dependencyGraph);
        dependencyGraph.unregisterDependency(rs);

        updateCacheEntryForBlockResult(rs, mainReachedSet, endsWithTargetState);
      }

      if (endsWithTargetState && !targetStateFound) {
        // TODO when finding the most inner target state,
        //      pop unnecessary reachedsets from the queue
        // TODO thread-safe access? updates of parent reached-set possible after finding target-state.
        targetStateFound = true;
      }
      return null;
    }
  }

  class MissingBlockHandler extends Task {

    private final BlockSummaryMissingException bsme;

    MissingBlockHandler(ReachedSet pRs,ReachedSet pMainReachedSet,
        ReachedSetDependencyGraph pDependencyGraph, boolean pTargetStateFound,
        BlockSummaryMissingException pBbsme) {
      super(pRs, pMainReachedSet, pDependencyGraph, pTargetStateFound);
      bsme = pBbsme;
    }

    @Override
    public Task call() {
      // remove current state from waitlist to avoid exploration until all sub-blocks are done.
      // The state was removed for exploration,
      // but re-added by CPA-algorithm when throwing the exception
      rs.removeOnlyFromWaitlist(bsme.getState());

      dependencyGraph.addDependency(rs, bsme.getReachedSet(), bsme.getState());

      // register as future work
      TaskList lst = new TaskList(rs, mainReachedSet, dependencyGraph, targetStateFound);
      if (targetStateFound) {
        // ignore further sub-analyses

      } else {
        // push for further analysis
        logger.log(
            Level.INFO, "pushing child reached-set", getId(bsme.getReachedSet().getFirstState()));
        lst.add(
            new ReachedSetAnalyzer(
                bsme.getReachedSet(), mainReachedSet, dependencyGraph, targetStateFound));
      }
      logger.log(Level.INFO, "pushing parent reached-set", getId(rs.getFirstState()));
      lst.add(new ReachedSetAnalyzer(rs, mainReachedSet, dependencyGraph, targetStateFound));

      return lst;
    }
  }

  class TaskList extends Task {

    private final List<Task> tasks = new LinkedList<>();

    TaskList(
        ReachedSet pRs,
        ReachedSet pMainReachedSet,
        ReachedSetDependencyGraph pDependencyGraph,
        boolean pTargetStateFound) {
      super(pRs, pMainReachedSet, pDependencyGraph, pTargetStateFound);
    }

    void add(Task task) {
      tasks.add(task);
    }

    @Override
    Task call() throws CPAException, InterruptedException {
      if (tasks.isEmpty()) {
        return null;
      }
      TaskList lst = new TaskList(rs, mainReachedSet, dependencyGraph, targetStateFound);
      Task succ = tasks.remove(0).call();
      if (succ != null) {
        lst.add(succ);
      }
      for (Task tmp : tasks) {
        lst.add(tmp);
      }
      return lst;
    }

  }
}
