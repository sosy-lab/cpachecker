// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CMultiSelectionStatementEncoding;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;

public class SeqThreadStatementClauseUtil {

  public static ImmutableSet<SubstituteEdge> collectAllSubstituteEdges(
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    return FluentIterable.from(Iterables.concat(pClauses.values()))
        .transformAndConcat(clause -> clause.getBlocks())
        .transformAndConcat(block -> Objects.requireNonNull(block).getStatements())
        .transformAndConcat(
            statement -> Objects.requireNonNull(statement).data().getSubstituteEdges())
        .toSet();
  }

  public static CExpression getStatementExpressionByEncoding(
      CMultiSelectionStatementEncoding pEncoding,
      CExpression pExpression,
      int pStatementNumber,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build label expression for control encoding " + pEncoding);
      case BINARY_SEARCH_TREE, IF_ELSE_CHAIN ->
          pBinaryExpressionBuilder.buildBinaryExpression(
              pExpression,
              SeqExpressionBuilder.buildIntegerLiteralExpression(pStatementNumber),
              BinaryOperator.EQUALS);
      case SWITCH_CASE -> SeqExpressionBuilder.buildIntegerLiteralExpression(pStatementNumber);
    };
  }

  public static ImmutableListMultimap<CExportExpression, CExportStatement> mapExpressionToClause(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<CExportExpression, CExportStatement> rOriginPcs =
        ImmutableListMultimap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      CExpression labelExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              pOptions.controlEncodingStatement(),
              pPcLeftHandSide,
              clause.labelNumber,
              pBinaryExpressionBuilder);
      rOriginPcs.putAll(new CExpressionWrapper(labelExpression), clause.toCExportStatements());
    }
    return rOriginPcs.build();
  }

  /**
   * Maps the first {@link SeqThreadStatementBlock} in each {@link SeqThreadStatementClause}s to
   * their label number.
   */
  public static ImmutableMap<Integer, SeqThreadStatementClause> mapLabelNumberToClause(
      ImmutableCollection<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementClause> rOriginPcs = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      rOriginPcs.put(clause.labelNumber, clause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /** Maps {@link SeqThreadStatementBlock}s to their label numbers. */
  public static ImmutableMap<Integer, SeqThreadStatementBlock> mapLabelNumberToBlock(
      ImmutableCollection<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementBlock> rMap = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        rMap.put(block.getLabelNumber(), block);
      }
    }
    return rMap.buildOrThrow();
  }

  /**
   * Ensures that all {@code int} labels in {@code clauses} are numbered consecutively, i.e. the
   * numbers {@code 0} to {@code clauses.size() - 1} are present (no gaps).
   *
   * <p>This function also recursively searches for all target {@code pc} and adjusts them
   * accordingly.
   */
  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause>
      cloneWithConsecutiveLabelNumbers(
          ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rConsecutiveLabels =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      rConsecutiveLabels.putAll(thread, cloneWithConsecutiveLabelNumbers(pClauses.get(thread)));
    }
    return rConsecutiveLabels.build();
  }

  // Including Blocks ==============================================================================

  private static ImmutableList<SeqThreadStatementClause> cloneWithConsecutiveLabelNumbers(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rNewClauses = ImmutableList.builder();
    ImmutableMap<Integer, Integer> labelBlockMap = mapBlockLabelNumberToIndex(pClauses);
    ImmutableMap<Integer, Integer> labelClauseMap = mapClauseLabelNumberToIndex(pClauses);
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement mergedStatement : block.getStatements()) {
          newStatements.add(replaceTargetPc(mergedStatement, labelBlockMap, labelClauseMap));
        }
        int blockIndex = Objects.requireNonNull(labelBlockMap.get(block.getLabelNumber()));
        newBlocks.add(block.withLabelNumber(blockIndex).withStatements(newStatements.build()));
      }
      int clauseIndex = Objects.requireNonNull(labelClauseMap.get(clause.labelNumber));
      rNewClauses.add(clause.withLabelNumber(clauseIndex).withBlocks(newBlocks.build()));
    }
    return rNewClauses.build();
  }

  private static ImmutableMap<Integer, Integer> mapBlockLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = ProgramCounterVariables.INIT_PC;
    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        rLabelToIndex.put(block.getLabelNumber(), index++);
      }
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static ImmutableMap<Integer, Integer> mapClauseLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = ProgramCounterVariables.INIT_PC;
    for (SeqThreadStatementClause clause : pClauses) {
      rLabelToIndex.put(clause.labelNumber, index++);
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static SeqThreadStatement replaceTargetPc(
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, Integer> pLabelBlockMap,
      final ImmutableMap<Integer, Integer> pLabelClauseMap) {

    if (pCurrentStatement.isTargetPcValid()) {
      int targetPc = pCurrentStatement.targetPc().orElseThrow();
      // for pc writes, use clause labels
      int clauseIndex = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
      return pCurrentStatement.withTargetPc(clauseIndex);

    } else if (pCurrentStatement.targetGoto().isPresent()) {
      int label = pCurrentStatement.targetGoto().orElseThrow();
      // for gotos, use block labels
      int index = Objects.requireNonNull(pLabelBlockMap.get(label));
      return pCurrentStatement.withTargetGoto(index);
    }
    // no valid target pc or target goto -> no replacement
    return pCurrentStatement;
  }

  // Loops =========================================================================================

  /**
   * Returns {@code true} if {@code pClause} is a loop start that should be separated i.e. remain
   * directly reachable via a {@code pc}, instead of only {@code goto}.
   */
  public static boolean isSeparateLoopStart(
      MPOROptions pOptions, SeqThreadStatementClause pClause) {

    return pClause.getFirstBlock().isLoopStart() && pOptions.noBackwardLoopGoto();
  }

  // Path ==========================================================================================

  /** Returns {@code true} if the path from A to B has consecutive labels. */
  public static boolean isConsecutiveLabelPath(
      SeqThreadStatementClause pCurrent,
      final SeqThreadStatementClause pTarget,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pCurrent.equals(pTarget)) {
      return true;
    } else {
      SeqThreadStatement firstStatement = pCurrent.getFirstBlock().getFirstStatement();
      SeqThreadStatementClause next = pLabelClauseMap.get(firstStatement.targetPc().orElseThrow());
      assert next != null : "could not find target case clause";
      if (pCurrent.labelNumber + 1 == next.labelNumber) {
        return isConsecutiveLabelPath(next, pTarget, pLabelClauseMap);
      } else {
        return false;
      }
    }
  }

  // No Backward Goto ==============================================================================

  /**
   * Removes backward goto, e.g. {@code goto label;} where {@code label:} is in a lower line of code
   * i.e. higher up in the program. This is done by reordering blocks via a dependence graph.
   */
  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> removeBackwardGoto(
      boolean pValidateNoBackwardGoto,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rNoBackwardGoto =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(thread);
      ImmutableList<SeqThreadStatementBlock> allBlocks = getAllBlocksFromClauses(clauses);
      ImmutableList<SeqThreadStatementBlock> firstBlocks = getAllFirstBlocksFromClauses(clauses);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap = mapLabelNumberToBlock(clauses);
      // create set to track which blocks were placed already
      ImmutableList<SeqThreadStatementBlock> reorderedBlocks =
          reorderBlocks(firstBlocks.getFirst(), labelBlockMap);
      if (pValidateNoBackwardGoto) {
        SeqValidator.validateEqualBlocks(thread.id(), allBlocks, reorderedBlocks);
      }
      rNoBackwardGoto.putAll(thread, buildClausesFromReorderedBlocks(reorderedBlocks, firstBlocks));
    }
    return rNoBackwardGoto.build();
  }

  private static ImmutableList<SeqThreadStatementBlock> reorderBlocks(
      SeqThreadStatementBlock pFirstBlock,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (pLabelBlockMap.size() == 1) {
      // short circuit: if there is only one block, no reordering required
      return ImmutableList.of(pFirstBlock);
    }
    // create list to keep track of new, reordered blocks
    List<SeqThreadStatementBlock> foundOrder = new ArrayList<>();
    // create graph used for dependency checking
    ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> blockGraph =
        ArrayListMultimap.create();
    recursivelyBuildBlockGraph(pFirstBlock, blockGraph, pLabelBlockMap);
    recursivelyReorderBlocks(blockGraph, foundOrder);
    assert !foundOrder.isEmpty() : "could not find any order";
    return ImmutableList.copyOf(foundOrder);
  }

  private static void recursivelyBuildBlockGraph(
      SeqThreadStatementBlock pCurrentBlock,
      ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pGraph,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    for (SeqThreadStatement statement : pCurrentBlock.getStatements()) {
      int targetNumber = statement.getTargetNumber();
      if (targetNumber != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementBlock targetBlock =
            Objects.requireNonNull(pLabelBlockMap.get(targetNumber));
        // ensure that adding (pCurrentBlock, targetBlock) does not yield cycle in pGraph
        if (isCycleFree(pGraph, pCurrentBlock, targetBlock)) {
          // prevent duplicates
          if (!pGraph.get(pCurrentBlock).contains(targetBlock)) {
            pGraph.get(pCurrentBlock).add(targetBlock);
            recursivelyBuildBlockGraph(targetBlock, pGraph, pLabelBlockMap);
          }
        }
      }
    }
  }

  /**
   * Returns {@code true} if adding {@code pBlock} and {@code pTarget} would result in {@code
   * pGraph} being free of cycles.
   */
  private static boolean isCycleFree(
      ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pGraph,
      SeqThreadStatementBlock pBlock,
      SeqThreadStatementBlock pTarget) {

    // shortcut: if pBlock == pTarget, then cycle
    if (pBlock.equals(pTarget)) {
      return false;
    }
    // Build a Traverser that walks through successors from the multimap
    Traverser<SeqThreadStatementBlock> traverser = Traverser.forGraph(pGraph::get);
    // Check reachability: if pBlock is reachable from pTarget => cycle
    for (SeqThreadStatementBlock node : traverser.depthFirstPreOrder(pTarget)) {
      if (node.equals(pBlock)) {
        return false;
      }
    }
    // no path from pBlock to pTarget
    return true;
  }

  private static void recursivelyReorderBlocks(
      ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pBlockGraph,
      List<SeqThreadStatementBlock> pFoundOrder) {

    // first collect and sort blocks with no targeting blocks (= zero in degree)
    ImmutableList<SeqThreadStatementBlock> zeroInDegreeBlocks = getZeroInDegreeBlocks(pBlockGraph);
    ImmutableList<SeqThreadStatementBlock> sortedDescending =
        sortByLabelNumberDescending(zeroInDegreeBlocks);
    for (SeqThreadStatementBlock block : sortedDescending) {
      tryAddToFoundOrder(block, pFoundOrder);
      // add all targets of block that are independent, i.e. not origins or targets (except block)
      for (SeqThreadStatementBlock target : pBlockGraph.get(block)) {
        assert target != null : "target cannot be null";
        if (isTargetIndependent(pBlockGraph, block, target)) {
          tryAddToFoundOrder(target, pFoundOrder);
        }
      }
      // then remove "used" block
      pBlockGraph.removeAll(block);
    }
    // if there are still blocks in the graph, continue recursive reordering
    if (!pBlockGraph.keySet().isEmpty()) {
      recursivelyReorderBlocks(pBlockGraph, pFoundOrder);
    }
  }

  /** Returns the set of zero in-degree blocks i.e. blocks without any incoming edge. */
  private static ImmutableList<SeqThreadStatementBlock> getZeroInDegreeBlocks(
      final ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pBlockGraph) {

    List<SeqThreadStatementBlock> allBlocks = new ArrayList<>(pBlockGraph.keySet());
    for (SeqThreadStatementBlock block : pBlockGraph.keySet()) {
      // remove all blocks that are targeted by any block
      pBlockGraph.get(block).forEach(allBlocks::remove);
    }
    return ImmutableList.copyOf(allBlocks);
  }

  private static ImmutableList<SeqThreadStatementBlock> sortByLabelNumberDescending(
      ImmutableList<SeqThreadStatementBlock> pBlocks) {

    return ImmutableList.sortedCopyOf(
        Comparator.comparingInt((SeqThreadStatementBlock block) -> block.getLabelNumber())
            .reversed(),
        pBlocks);
  }

  private static void tryAddToFoundOrder(
      SeqThreadStatementBlock pBlock, List<SeqThreadStatementBlock> pFoundOrder) {

    // only add blocks once to orders to prevent duplication
    if (!pFoundOrder.contains(pBlock)) {
      pFoundOrder.add(pBlock);
    }
  }

  private static boolean isTargetIndependent(
      ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pBlockGraph,
      SeqThreadStatementBlock pBlock,
      SeqThreadStatementBlock pTarget) {

    if (pBlockGraph.containsKey(pTarget)) {
      // pTarget targets some other block -> not independent
      return false;
    }
    for (SeqThreadStatementBlock block : pBlockGraph.keySet()) {
      if (!Objects.requireNonNull(block).equals(pBlock)) {
        if (pBlockGraph.get(block).contains(pTarget)) {
          // pTarget is target of some other block -> not independent
          return false;
        }
      }
    }
    return true;
  }

  private static ImmutableList<SeqThreadStatementClause> buildClausesFromReorderedBlocks(
      ImmutableList<SeqThreadStatementBlock> pReorderedBlocks,
      ImmutableList<SeqThreadStatementBlock> pFirstBlocks) {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();
    for (int i = 0; i < pReorderedBlocks.size(); i++) {
      SeqThreadStatementBlock block = pReorderedBlocks.get(i);
      if (pFirstBlocks.contains(block)) {
        int start = pReorderedBlocks.indexOf(block);
        Optional<Integer> nextFirstBlockIndex =
            findNextFirstBlockIndex(start, pReorderedBlocks, pFirstBlocks);
        if (nextFirstBlockIndex.isPresent()) {
          int target = nextFirstBlockIndex.orElseThrow();
          // use the index of the next first block, since to is exclusive
          rClauses.add(new SeqThreadStatementClause(pReorderedBlocks.subList(start, target)));
        } else {
          rClauses.add(
              new SeqThreadStatementClause(
                  pReorderedBlocks.subList(start, pReorderedBlocks.size())));
        }
      }
    }
    return rClauses.build();
  }

  /**
   * Returns the last index in {@code pBlocks} that precedes a first block, starting in {@code
   * pStartIndex}.
   */
  private static Optional<Integer> findNextFirstBlockIndex(
      int pStartIndex,
      ImmutableList<SeqThreadStatementBlock> pBlocks,
      ImmutableList<SeqThreadStatementBlock> pFirstBlocks) {

    // from is inclusive, to is exclusive
    ImmutableList<SeqThreadStatementBlock> remainingBlocks =
        pBlocks.subList(pStartIndex + 1, pBlocks.size());
    for (SeqThreadStatementBlock block : remainingBlocks) {
      if (pFirstBlocks.contains(block)) {
        return Optional.of(pBlocks.indexOf(block));
      }
    }
    return Optional.empty();
  }

  // Helper ========================================================================================

  private static ImmutableList<SeqThreadStatementBlock> getAllBlocksFromClauses(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    return pClauses.stream()
        .flatMap(pClause -> pClause.getBlocks().stream())
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<SeqThreadStatementBlock> getAllFirstBlocksFromClauses(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    return transformedImmutableListCopy(
        pClauses,
        clause -> {
          assert clause != null : "clause cannot be null";
          return clause.getFirstBlock();
        });
  }
}
