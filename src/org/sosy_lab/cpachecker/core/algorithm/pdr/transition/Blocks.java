/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.transition;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/**
 * Utility class for blocks.
 */
public final class Blocks {

  private Blocks() {}

  /**
   * Create a new predicate for blocks that applies the given predicate to the
   * successor state of a block.
   *
   * @param pPredicate the predicate to be applied to block successor states.
   *
   * @return a new predicate for blocks that applies the given predicate to the
   * successor state of a block.
   */
  public static Predicate<Block> applyToSuccessor(Predicate<AbstractState> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getSuccessor());
      }
    };
  }

  /**
   * Create a new predicate for blocks that applies the given predicate to the
   * successor location of a block.
   *
   * @param pPredicate the predicate to be applied to block successor
   * locations.
   *
   * @return a new predicate for blocks that applies the given predicate to the
   * successor location of a block.
   */
  public static Predicate<Block> applyToSuccessorLocation(Predicate<CFANode> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getSuccessorLocation());
      }
    };
  }

  /**
   * Create a new predicate for blocks that applies the given predicate to the
   * predecessor state of a block.
   *
   * @param pPredicate the predicate to be applied to block predecessor states.
   *
   * @return a new predicate for blocks that applies the given predicate to the
   * predecessor state of a block.
   */
  public static Predicate<Block> applyToPredecessor(Predicate<AbstractState> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getPredecessor());
      }
    };
  }

  /**
   * Create a new predicate for blocks that applies the given predicate to the
   * predecessor location of a block.
   *
   * @param pPredicate the predicate to be applied to block predecessor
   * locations.
   *
   * @return a new predicate for blocks that applies the given predicate to the
   * predecessor location of a block.
   */
  public static Predicate<Block> applyToPredecessorLocation(Predicate<CFANode> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getPredecessorLocation());
      }
    };
  }

  /**
   * Changes all variable indices of the block formula of the given block such
   * that where previously a variable appeared with the highest index, it now
   * appears with the lowest index, and vice versa, and the same concept
   * applies to variables with the next-highest and next-lowest indices, and so
   * on.
   *
   * For example, {@code x_2 = x_1 + 1} thus becomes {@code x_1 = x_2 + 1}.
   *
   * The satisfiability of the block formula remains unchanged.
   *
   * @param pBlock the block to take the formula from.
   * @param pFormulaManager the formula manager to be used to change the
   * indices.
   *
   * @return the changed formula.
   */
  public static BooleanFormula formulaWithInvertedIndices(
      Block pBlock, FormulaManagerView pFormulaManager) {
    PathFormula finalContext = pBlock.getDirection() == AnalysisDirection.FORWARD
        ? pBlock.getPrimedContext()
        : pBlock.getUnprimedContext();
    return Reindexer.invertIndices(
        pBlock.getFormula(),
        finalContext.getSsa(),
        pFormulaManager);
  }

  public static PathAndBranchingFormula buildPathAndBranchingFormulas(
      List<Block> pBlocks,
      ReachedSet pTargetReachedSet,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager)
      throws CPATransferException, InterruptedException {
    BlockToFormula extractPathFormula =
        block ->
            block.getDirection() == AnalysisDirection.FORWARD
                ? block.getFormula()
                : formulaWithInvertedIndices(block, pFormulaManager);
    BlockToFormula extractBranchingFormula =
        new BlockToFormula() {

          @Override
          public BooleanFormula apply(Block pBlock)
              throws InterruptedException, CPATransferException {
            BooleanFormula branchingFormula =
                pPathFormulaManager.buildBranchingFormula(
                    FluentIterable.from(pTargetReachedSet)
                        .transform(AbstractStates.toState(ARGState.class))
                        .filter(
                            s -> {
                              ReachedSet blockReached = pBlock.getReachedSet();
                              return blockReached.contains(s)
                                  || s.getParents().stream().anyMatch(blockReached::contains);
                            })
                        .toSet());
            return pBlock.getDirection() == AnalysisDirection.FORWARD
                ? branchingFormula
                : Reindexer.invertIndices(
                    branchingFormula, pBlock.getUnprimedContext().getSsa(), pFormulaManager);
          }
        };

    BooleanFormulaManager booleanFormulaManager = pFormulaManager.getBooleanFormulaManager();
    BooleanFormula pathFormula = booleanFormulaManager.makeTrue();
    BooleanFormula branchingFormula = booleanFormulaManager.makeTrue();

    if (!Iterables.isEmpty(pBlocks)) {
      CFANode expectedPredecessorLocation = null;
      SSAMap previousBlockSuccessorContext = null;

      for (Block block : pBlocks) {
        Preconditions.checkArgument(
            expectedPredecessorLocation == null
                || block.getPredecessorLocation().equals(expectedPredecessorLocation),
            "Blocks must connect.");
        BooleanFormula blockPathFormula = extractPathFormula.apply(block);
        BooleanFormula blockBranchingFormula = extractBranchingFormula.apply(block);
        SSAMap blockSuccessorContext =
            (block.getDirection() == AnalysisDirection.FORWARD
                    ? block.getPrimedContext()
                    : block.getUnprimedContext())
                .getSsa();
        if (previousBlockSuccessorContext != null) {
          final SSAMap previousContext = previousBlockSuccessorContext;
          blockPathFormula =
              Reindexer.reindex(
                  blockPathFormula,
                  blockSuccessorContext,
                  (var, i) -> previousContext.withDefault(1).getIndex(var) + i - 1,
                  pFormulaManager);
          previousBlockSuccessorContext =
              combine(
                  previousBlockSuccessorContext,
                  blockSuccessorContext,
                  blockPathFormula,
                  pFormulaManager);
          blockBranchingFormula =
              Reindexer.reindex(
                  blockBranchingFormula,
                  blockSuccessorContext,
                  (var, i) -> previousContext.withDefault(1).getIndex(var) + i - 1,
                  pFormulaManager);
        } else {
          previousBlockSuccessorContext =
              Reindexer.adjustToFormula(blockPathFormula, blockSuccessorContext, pFormulaManager);
        }
        pathFormula = booleanFormulaManager.and(pathFormula, blockPathFormula);
        branchingFormula = booleanFormulaManager.and(branchingFormula, blockBranchingFormula);

        expectedPredecessorLocation = block.getSuccessorLocation();
      }
    }
    return new PathAndBranchingFormula(pathFormula, branchingFormula);
  }

  private static SSAMap combine(
      SSAMap pPreviousBlockSuccessorContext,
      SSAMap pBlockSuccessorContext,
      BooleanFormula pBlockFormula,
      FormulaManagerView pFormulaManager) {
    SSAMapBuilder builder = pPreviousBlockSuccessorContext.builder();
    SSAMap previousBlockSuccessorContext = pPreviousBlockSuccessorContext.withDefault(1);
    SSAMap blockSuccessorContext =
        Reindexer.adjustToFormula(
            pBlockFormula, pBlockSuccessorContext.withDefault(1), pFormulaManager);
    for (String variable : blockSuccessorContext.allVariables()) {
      int previousIndex = previousBlockSuccessorContext.getIndex(variable);
      int blockIndex = blockSuccessorContext.getIndex(variable);
      builder.setIndex(
          variable, blockSuccessorContext.getType(variable), previousIndex - 1 + blockIndex);
    }
    return builder.build();
  }

  /**
   * Combine the reached sets of the given blocks and insert their states into
   * the given target reached set.
   *
   * @param pBlocks the blocks. The order of the blocks is expected to be
   * 'forward', e.g. from the program entry to a target state.
   *
   * @param pTargetReachedSet the target reached set.
   */
  public static void combineReachedSets(
      Iterable<Block> pBlocks,
      ReachedSet pTargetReachedSet) {
    if (Iterables.isEmpty(pBlocks)) {
      return;
    }

    List<Iterable<StateWithPrecision>> blockStates = new ArrayList<>();

    for (Block block : pBlocks) {
      ReachedSet reachedSet = block.getReachedSet();

      Iterable<StateWithPrecision> remainingStates =
          FluentIterable.from(reachedSet)
              .filter(as -> !AbstractStates.extractStateByType(as, ARGState.class).isDestroyed())
              .transform(as -> new StateWithPrecision(as, reachedSet.getPrecision(as)));
      blockStates.add(remainingStates);

    }
    ListIterator<Iterable<StateWithPrecision>> blockStatesIterator = blockStates.listIterator();
    while (blockStatesIterator.hasNext()) {
      for (StateWithPrecision stateWithPrecision : blockStatesIterator.next()) {
        pTargetReachedSet.add(stateWithPrecision.state, stateWithPrecision.precision);
        pTargetReachedSet.removeOnlyFromWaitlist(stateWithPrecision.state);
      }
    }
  }

  private static class StateWithPrecision {

    private final AbstractState state;

    private final Precision precision;

    public StateWithPrecision(AbstractState pState, Precision pPrecision) {
      state = Objects.requireNonNull(pState);
      precision = Objects.requireNonNull(pPrecision);
    }

    @Override
    public String toString() {
      return state.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(state, precision);
    }

    @Override
    public boolean equals(Object pObj) {
      if (pObj == this) {
        return true;
      }
      if (pObj instanceof StateWithPrecision) {
        StateWithPrecision other = (StateWithPrecision) pObj;
        return state.equals(other.state) && precision.equals(other.precision);
      }
      return false;
    }

  }

  private static interface BlockToFormula {

    BooleanFormula apply(Block pBlock) throws InterruptedException, CPATransferException;

  }

  public static class PathAndBranchingFormula {

    private final BooleanFormula pathFormula;

    private final BooleanFormula branchingFormula;

    public PathAndBranchingFormula(BooleanFormula pPathFormula, BooleanFormula pBranchingFormula) {
      pathFormula = Objects.requireNonNull(pPathFormula);
      branchingFormula = Objects.requireNonNull(pBranchingFormula);
    }

    public BooleanFormula getPathFormula() {
      return pathFormula;
    }

    public BooleanFormula getBranchingFormula() {
      return branchingFormula;
    }

    @Override
    public String toString() {
      return pathFormula + ", " + branchingFormula;
    }

  }
}
