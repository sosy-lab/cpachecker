// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

@Options(prefix = "cpa.loopbound")
public class LoopBoundPrecisionAdjustment implements PrecisionAdjustment {

  @Option(
      secure = true,
      description =
          """
          Bound for the number of complete loop unrollings
          of the program (0 is used for no bound).
          Works only if assumption storage CPA is enabled, because otherwise it would
          be unsound.\
          """)
  private int maxLoopIterations = 0;

  @Option(
      secure = true,
      description =
          "Maximum for adjusting the bound for the number of complete loop unrollings of the\n"
              + " program (0 is used for no maximum).\n"
              + "Only relevant in combination with a non-static adjuster for the bound for"
              + " loop-head visits.")
  private int maxLoopIterationsUpperBound = 0;

  @Option(
      secure = true,
      description =
          "This option controls how the maxLoopIterations condition is adjusted when a condition"
              + " adjustment is invoked.")
  private MaxLoopIterationAdjusters maxLoopIterationAdjusterFactory =
      MaxLoopIterationAdjusters.STATIC;

  @Option(
      secure = true,
      description =
          "Number of loop iterations before the loop counter is"
              + " abstracted. Zero is equivalent to no limit.")
  private int loopIterationsBeforeAbstraction = 0;

  @Option(
      secure = true,
      description =
          "Drop states that can never reach the bound for the number of loop unrollings,"
              + " i.e., states whose loop-iteration counter is still below the bound but from"
              + " whose location no loop head can be reached anymore.\n"
              + "Such states are useless for analyses that inspect only states at the bound,"
              + " most notably the induction step case of k-induction: it assumes the candidate"
              + " invariant at the loop-head states up to the bound and checks it at the states"
              + " exactly at the bound, so a state that can never reach the bound contributes"
              + " nothing.\n"
              + "Enabling this makes the reached set an incomplete unrolling of the program. Do"
              + " NOT enable it for analyses that inspect states below the bound at arbitrary"
              + " locations, such as plain BMC or k-induction with candidate invariants for"
              + " specific program locations.")
  private boolean dropStatesThatCannotReachBound = false;

  private final LogManager logger;

  /**
   * All nodes from which some loop head is reachable in the CFA, or {@code null} if {@link
   * #dropStatesThatCannotReachBound} is disabled and this information is thus not needed.
   *
   * <p>This is a context-insensitive over-approximation of reachability (it ignores from which call
   * site a function was entered), i.e., the set may be too large but never too small. Because we
   * only ever drop states whose location is <em>not</em> in this set, being too large is safe.
   */
  private final @Nullable ImmutableSet<CFANode> nodesThatCanReachALoopHead;

  public LoopBoundPrecisionAdjustment(Configuration pConfig, CFA pCFA, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (maxLoopIterations < 0) {
      throw new InvalidConfigurationException(
          "cpa.loopbound.maxLoopIterations must be a non-negative value, but is set to "
              + maxLoopIterations);
    }
    logger = pLogger;
    nodesThatCanReachALoopHead =
        dropStatesThatCannotReachBound ? computeNodesThatCanReachALoopHead(pCFA) : null;
  }

  /** Collects all CFA nodes from which some loop head can be reached, by backwards traversal. */
  private static ImmutableSet<CFANode> computeNodesThatCanReachALoopHead(CFA pCFA) {
    Set<CFANode> result = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    for (Loop loop : pCFA.getLoopStructure().orElseThrow().getAllLoops()) {
      for (CFANode loopHead : loop.getLoopHeads()) {
        if (result.add(loopHead)) {
          waitlist.push(loopHead);
        }
      }
    }
    while (!waitlist.isEmpty()) {
      // Use all entering edges (including summary edges) so that no predecessor is missed.
      for (CFAEdge enteringEdge : waitlist.pop().getAllEnteringEdges()) {
        CFANode predecessor = enteringEdge.getPredecessor();
        if (result.add(predecessor)) {
          waitlist.push(predecessor);
        }
      }
    }
    return ImmutableSet.copyOf(result);
  }

  int getMaxLoopIterations() {
    return maxLoopIterations;
  }

  void setMaxLoopIterations(int pMaxLoopIterations) {
    Preconditions.checkArgument(pMaxLoopIterations >= 0);
    maxLoopIterations = pMaxLoopIterations;
  }

  int getLoopIterationsBeforeAbstraction() {
    return loopIterationsBeforeAbstraction == 0
        ? Integer.MAX_VALUE
        : loopIterationsBeforeAbstraction;
  }

  private void setLoopIterationsBeforeAbstraction(int pLoopIterationsBeforeAbstraction) {
    Preconditions.checkArgument(pLoopIterationsBeforeAbstraction >= 0);
    loopIterationsBeforeAbstraction = pLoopIterationsBeforeAbstraction;
  }

  void incrementLoopIterationsBeforeAbstraction() {
    setLoopIterationsBeforeAbstraction(getLoopIterationsBeforeAbstraction() + 1);
  }

  @Override
  public String toString() {
    return "k = "
        + maxLoopIterations
        + ", adjustment strategy = "
        + maxLoopIterationAdjusterFactory;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    LoopBoundPrecision precision = (LoopBoundPrecision) pPrecision;
    LoopBoundPrecision adjustedPrecision =
        precision
            .withMaxLoopIterations(maxLoopIterations)
            .withLoopIterationsBeforeAbstraction(getLoopIterationsBeforeAbstraction());

    LoopBoundState state = (LoopBoundState) pState;
    LoopBoundState adjustedState =
        state
            .setStop(maxLoopIterations > 0 && state.getDeepestIteration() > maxLoopIterations)
            .enforceAbstraction(getLoopIterationsBeforeAbstraction());

    if (dropStatesThatCannotReachBound
        && maxLoopIterations > 0
        && adjustedState.getDeepestIteration() < maxLoopIterations
        && !canStillReachALoopHead(pFullState)) {
      // The loop-iteration counter only ever grows when a loop head is visited. Since no loop head
      // can be reached from here anymore, neither this state nor any of its successors can ever
      // reach the bound, so the whole subtree can be dropped.
      return Optional.empty();
    }

    PrecisionAdjustmentResult result =
        new PrecisionAdjustmentResult(adjustedState, adjustedPrecision, Action.CONTINUE);

    return Optional.of(result);
  }

  private boolean canStillReachALoopHead(AbstractState pFullState) {
    CFANode location = AbstractStates.extractLocation(pFullState);
    // Without unique location information we cannot decide this, so keep the state.
    return location == null || nodesThatCanReachALoopHead.contains(location);
  }

  private interface MaxLoopIterationAdjuster {

    int adjust(int currentValue);

    boolean canAdjust(int currentValue);
  }

  private interface MaxLoopIterationAdjusterFactory {

    MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(
        LoopBoundPrecisionAdjustment pPrecisionAdjustment);
  }

  private enum MaxLoopIterationAdjusters implements MaxLoopIterationAdjusterFactory {
    STATIC {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(
          LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
        return StaticLoopIterationAdjuster.INSTANCE;
      }
    },

    INCREMENT {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(
          LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
        return new IncrementalLoopIterationAdjuster(pPrecisionAdjustment);
      }
    },

    DOUBLE {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(
          LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
        return new DoublingLoopIterationAdjuster(pPrecisionAdjustment);
      }
    }
  }

  private enum StaticLoopIterationAdjuster implements MaxLoopIterationAdjuster {
    INSTANCE;

    @Override
    public int adjust(int pCurrentValue) {
      return pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return false;
    }
  }

  private static final class IncrementalLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    private final LoopBoundPrecisionAdjustment precisionAdjustment;

    IncrementalLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
      precisionAdjustment = pPrecisionAdjustment;
    }

    @Override
    public int adjust(int pCurrentValue) {
      return ++pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return precisionAdjustment.maxLoopIterationsUpperBound <= 0
          || pCurrentValue < precisionAdjustment.maxLoopIterationsUpperBound;
    }
  }

  private static final class DoublingLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    private final LoopBoundPrecisionAdjustment precisionAdjustment;

    DoublingLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
      precisionAdjustment = pPrecisionAdjustment;
    }

    @Override
    public int adjust(int pCurrentValue) {
      return 2 * pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return precisionAdjustment.maxLoopIterationsUpperBound <= 0
          || pCurrentValue * 2 <= precisionAdjustment.maxLoopIterationsUpperBound;
    }
  }

  public boolean nextState() {
    MaxLoopIterationAdjuster maxLoopIterationAdjuster =
        maxLoopIterationAdjusterFactory.getMaxLoopIterationAdjuster(this);
    if (maxLoopIterationAdjuster.canAdjust(getMaxLoopIterations())) {
      int adjustedMaxLoopIterations = maxLoopIterationAdjuster.adjust(getMaxLoopIterations());
      logger.log(Level.INFO, "Adjusting maxLoopIterations to " + adjustedMaxLoopIterations);
      setMaxLoopIterations(adjustedMaxLoopIterations);
      return true;
    }
    return false;
  }
}
