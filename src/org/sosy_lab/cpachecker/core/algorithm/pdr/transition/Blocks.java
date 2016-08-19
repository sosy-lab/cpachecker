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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Utility class for blocks.
 */
public final class Blocks {

  private Blocks() {}

  public static Predicate<Block> applyToSuccessor(Predicate<AbstractState> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getSuccessor());
      }
    };
  }

  public static Predicate<Block> applyToSuccessorLocation(Predicate<CFANode> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getSuccessorLocation());
      }
    };
  }

  public static Predicate<Block> applyToPredecessor(Predicate<AbstractState> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getPredecessor());
      }
    };
  }

  public static Predicate<Block> applyToPredecessorLocation(Predicate<CFANode> pPredicate) {
    return new Predicate<Block>() {

      @Override
      public boolean apply(Block pBlock) {
        return pPredicate.apply(pBlock.getPredecessorLocation());
      }
    };
  }

  public static BooleanFormula formulaWithInvertedIndices(Block pBlock, FormulaManagerView pFormulaManager) {
    PathFormula finalContext = pBlock.getDirection() == AnalysisDirection.FORWARD
        ? pBlock.getPrimedContext()
        : pBlock.getUnprimedContext();
    return Reindexer.invertIndices(
        pBlock.getFormula(),
        finalContext.getSsa(),
        pFormulaManager);
  }

  public static BooleanFormula conjoinBlockFormulas(Iterable<Block> pBlocks, FormulaManagerView pFormulaManager) throws CPATransferException, InterruptedException {
    return conjoinFormulas(
        pBlocks,
        block -> block.getDirection() == AnalysisDirection.FORWARD ? block.getFormula() : formulaWithInvertedIndices(block, pFormulaManager),
        pFormulaManager);
  }

  public static BooleanFormula conjoinBranchingFormulas(Iterable<Block> pBlocks, FormulaManagerView pFormulaManager, PathFormulaManager pPathFormulaManager) throws CPATransferException, InterruptedException {
    return conjoinFormulas(
        pBlocks,
        new BlockToFormula() {

          @Override
          public BooleanFormula apply(Block pBlock) throws InterruptedException, CPATransferException {
            BooleanFormula branchingFormula = pPathFormulaManager
                .buildBranchingFormula(FluentIterable
                    .from(pBlock.getReachedSet())
                    .transform(AbstractStates.toState(ARGState.class))
                    .toSet());
            return pBlock.getDirection() == AnalysisDirection.FORWARD
                ? branchingFormula
                : Reindexer.invertIndices(
                    branchingFormula,
                    pBlock.getUnprimedContext().getSsa(),
                    pFormulaManager);
          }
        },
        pFormulaManager);
  }

  public static BooleanFormula conjoinFormulas(Iterable<Block> pBlocks, BlockToFormula pExtractFormula, FormulaManagerView pFormulaManager) throws CPATransferException, InterruptedException {
    BooleanFormulaManager booleanFormulaManager = pFormulaManager.getBooleanFormulaManager();
    BooleanFormula formula = booleanFormulaManager.makeTrue();

    if (Iterables.isEmpty(pBlocks)) {
      return formula;
    }

    CFANode expectedPredecessorLocation = null;
    PathFormula previousBlockSuccessorContext = null;

    for (Block block : pBlocks) {
      Preconditions.checkArgument(
          expectedPredecessorLocation == null || block.getPredecessorLocation().equals(expectedPredecessorLocation),
          "Blocks must connect.");
      BooleanFormula blockFormula = pExtractFormula.apply(block);
      if (previousBlockSuccessorContext != null) {
        final PathFormula previousContext = previousBlockSuccessorContext;
        blockFormula = Reindexer.reindex(
            blockFormula,
            block.getPrimedContext().getSsa(),
            (var, i) -> previousContext.getSsa().getIndex(var) + i - 1,
            pFormulaManager);
      }
      formula = booleanFormulaManager.and(formula, blockFormula);

      expectedPredecessorLocation = block.getSuccessorLocation();
      previousBlockSuccessorContext = block.getPrimedContext();
    }

    return formula;
  }

  public static ARGPath combineARGPaths(Iterable<Block> pBlocks, Map<Integer, Boolean> pBranchingInformation) {
    Preconditions.checkArgument(!Iterables.isEmpty(pBlocks), "Cannot create empty ARG path.");

    ARGPathBuilder argPathBuilder = ARGPath.reverseBuilder();
    ARGState firstState = null;
    CFANode expectedPredecessorLocation = null;

    for (Block block : pBlocks) {
      Preconditions.checkArgument(
          expectedPredecessorLocation == null || block.getPredecessorLocation().equals(expectedPredecessorLocation),
          "Blocks must connect.");

      ReachedSet reachedSet = block.getReachedSet();
      ARGPath blockPath = ARGUtils.getPathFromBranchingInformation(
          AbstractStates.extractStateByType(reachedSet.getFirstState(), ARGState.class),
          FluentIterable
            .from(reachedSet)
            .transform(AbstractStates.toState(ARGState.class))
            .toSet(),
          pBranchingInformation);

      List<CFAEdge> pathEdges = blockPath.getInnerEdges();
      List<ARGState> pathStates = blockPath.asStatesList();
      ListIterator<ARGState> stateIterator = pathStates.listIterator(pathStates.size());
      ListIterator<CFAEdge> edgeIterator = pathEdges.listIterator(pathEdges.size());

      while (stateIterator.hasPrevious()) {
        ARGState state = stateIterator.previous();
        if (edgeIterator.hasPrevious()) {
          argPathBuilder.add(state, edgeIterator.previous());
        } else if (firstState == null) {
          firstState = state;
        }
      }

      expectedPredecessorLocation = block.getSuccessorLocation();
    }

    return argPathBuilder.build(firstState);
  }

  public static void combineReachedSets(Iterable<Block> pBlocks, ReachedSet pTargetReachedSet) {
    if (Iterables.isEmpty(pBlocks)) {
      return;
    }

    ARGState argPreviousState = null;
    for (Block block : pBlocks) {
      ReachedSet reachedSet = block.getReachedSet();
      AbstractState firstState = block.getDirection() == AnalysisDirection.FORWARD ? block.getPredecessor() : block.getSuccessor();
      assert reachedSet.getFirstState() == firstState;

      if (argPreviousState == null) {
        pTargetReachedSet.add(firstState, reachedSet.getPrecision(firstState));
      } else {
        ARGState argFirstState = AbstractStates.extractStateByType(firstState, ARGState.class);
        for (ARGState childOfFirstState : argFirstState.getChildren()) {
          childOfFirstState.addParent(argPreviousState);
        }
        argFirstState.removeFromARG();
      }

      for (AbstractState state : reachedSet) {
        if (state != firstState) {
          pTargetReachedSet.add(state, reachedSet.getPrecision(state));
          pTargetReachedSet.removeOnlyFromWaitlist(state);
        }
      }

      argPreviousState = AbstractStates.extractStateByType(
          block.getDirection() == AnalysisDirection.FORWARD
              ? block.getSuccessor()
              : block.getPredecessor(),
          ARGState.class);

    }
  }

  public static interface BlockToFormula {

    BooleanFormula apply(Block pBlock) throws InterruptedException, CPATransferException;

  }
}
