// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqParameterAssignmentStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementClauseUtil {

  /** Searches for all target {@code pc} in {@code pStatement}. */
  public static ImmutableSet<Integer> collectAllIntegerTargetPc(SeqThreadStatement pStatement) {
    ImmutableSet.Builder<Integer> rAllTargetPc = ImmutableSet.builder();
    if (pStatement.getTargetPc().isPresent()) {
      // add the direct target pc, if present
      rAllTargetPc.add(pStatement.getTargetPc().orElseThrow());
    }
    return rAllTargetPc.build();
  }

  public static ImmutableSet<SubstituteEdge> collectAllSubstituteEdges(
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableSet.Builder<SubstituteEdge> rEdges = ImmutableSet.builder();
    for (MPORThread thread : pClauses.keySet()) {
      for (SeqThreadStatementClause clause : pClauses.get(thread)) {
        for (SeqThreadStatementBlock block : clause.getBlocks()) {
          for (SeqThreadStatement statement : block.getStatements()) {
            rEdges.addAll(statement.getSubstituteEdges());
          }
        }
      }
    }
    return rEdges.build();
  }

  public static CExpression getStatementExpressionByEncoding(
      MultiControlStatementEncoding pEncoding,
      CExpression pExpression,
      int pStatementNumber,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build label expression for control encoding " + pEncoding);
      case BINARY_IF_TREE, IF_ELSE_CHAIN ->
          pBinaryExpressionBuilder.buildBinaryExpression(
              pExpression,
              SeqExpressionBuilder.buildIntegerLiteralExpression(pStatementNumber),
              BinaryOperator.EQUALS);
      case SWITCH_CASE -> SeqExpressionBuilder.buildIntegerLiteralExpression(pStatementNumber);
    };
  }

  public static ImmutableMap<CExpression, SeqThreadStatementClause> mapExpressionToClause(
      MPOROptions pOptions,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CExpression, SeqThreadStatementClause> rOriginPcs = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      CExpression labelExpression =
          SeqThreadStatementClauseUtil.getStatementExpressionByEncoding(
              pOptions.controlEncodingStatement,
              pPcLeftHandSide,
              clause.labelNumber,
              pBinaryExpressionBuilder);
      rOriginPcs.put(labelExpression, clause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /**
   * Maps the first {@link SeqThreadStatementBlock} in each {@link SeqThreadStatementClause}s to
   * their label number.
   */
  public static ImmutableMap<Integer, SeqThreadStatementClause> mapLabelNumberToClause(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementClause> rOriginPcs = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      rOriginPcs.put(clause.labelNumber, clause);
    }
    return rOriginPcs.buildOrThrow();
  }

  /** Maps {@link SeqThreadStatementBlock}s to their label numbers. */
  public static ImmutableMap<Integer, SeqThreadStatementBlock> mapLabelNumberToBlock(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, SeqThreadStatementBlock> rMap = ImmutableMap.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        rMap.put(block.getLabel().labelNumber, block);
      }
    }
    return rMap.buildOrThrow();
  }

  /**
   * Ensures that all {@code int} labels in {@code pClauses} are numbered consecutively, i.e. the
   * numbers {@code 0} to {@code pClauses.size() - 1} are present (no gaps).
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
        int blockIndex = Objects.requireNonNull(labelBlockMap.get(block.getLabel().labelNumber));
        newBlocks.add(
            block.cloneWithLabelNumber(blockIndex).cloneWithStatements(newStatements.build()));
      }
      int clauseIndex = Objects.requireNonNull(labelClauseMap.get(clause.labelNumber));
      rNewClauses.add(clause.cloneWithLabelNumber(clauseIndex).cloneWithBlocks(newBlocks.build()));
    }
    return rNewClauses.build();
  }

  private static ImmutableMap<Integer, Integer> mapBlockLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = Sequentialization.INIT_PC;
    for (SeqThreadStatementClause clause : pClauses) {
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        rLabelToIndex.put(block.getLabel().labelNumber, index++);
      }
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static ImmutableMap<Integer, Integer> mapClauseLabelNumberToIndex(
      ImmutableList<SeqThreadStatementClause> pClauses) {

    ImmutableMap.Builder<Integer, Integer> rLabelToIndex = ImmutableMap.builder();
    int index = Sequentialization.INIT_PC;
    for (SeqThreadStatementClause clause : pClauses) {
      rLabelToIndex.put(clause.labelNumber, index++);
    }
    return rLabelToIndex.buildOrThrow();
  }

  private static SeqThreadStatement replaceTargetPc(
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, Integer> pLabelBlockMap,
      final ImmutableMap<Integer, Integer> pLabelClauseMap) {

    if (isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      // for pc writes, use clause labels
      int clauseIndex = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
      // for injected statements (e.g. bitvector gotos), use the block label
      int blockIndex = Objects.requireNonNull(pLabelBlockMap.get(targetPc));
      ImmutableList<SeqInjectedStatement> replacingInjectedStatements =
          SeqThreadStatementClauseUtil.replaceTargetGotoLabel(
              pCurrentStatement.getInjectedStatements(), blockIndex);
      return pCurrentStatement
          .cloneWithTargetPc(clauseIndex)
          .cloneReplacingInjectedStatements(replacingInjectedStatements);

    } else if (pCurrentStatement.getTargetGoto().isPresent()) {
      SeqBlockLabelStatement label = pCurrentStatement.getTargetGoto().orElseThrow();
      // for gotos, use block labels
      int index = Objects.requireNonNull(pLabelBlockMap.get(label.labelNumber));
      return pCurrentStatement.cloneWithTargetGoto(label.cloneWithLabelNumber(index));
    }
    // no target pc or target goto -> no replacement
    return pCurrentStatement;
  }

  /**
   * Searches {@code pInjectedStatements} for {@link SeqBitVectorEvaluationStatement}s and replaces
   * their {@code goto} labels with the updated {@code pc}.
   */
  public static ImmutableList<SeqInjectedStatement> replaceTargetGotoLabel(
      ImmutableList<SeqInjectedStatement> pInjectedStatements, int pNewTargetPc) {

    ImmutableList.Builder<SeqInjectedStatement> rNewInjected = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement bitVectorEvaluation) {
        rNewInjected.add(bitVectorEvaluation.cloneWithGotoLabelNumber(pNewTargetPc));
      } else {
        rNewInjected.add(injectedStatement);
      }
    }
    return rNewInjected.build();
  }

  public static boolean isValidTargetPc(Optional<Integer> pTargetPc) {
    if (pTargetPc.isPresent()) {
      int targetPc = pTargetPc.orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        return true;
      }
    }
    return false;
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
      SeqThreadStatementClause next =
          pLabelClauseMap.get(firstStatement.getTargetPc().orElseThrow());
      assert next != null : "could not find target case clause";
      if (pCurrent.labelNumber + 1 == next.labelNumber) {
        return isConsecutiveLabelPath(next, pTarget, pLabelClauseMap);
      } else {
        return false;
      }
    }
  }

  // No Backward Goto ==============================================================================

  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> removeBackwardGoto(
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
          reorderBlocks(firstBlocks.get(0), labelBlockMap);
      assert SeqValidator.validateEqualBlocks(reorderedBlocks, allBlocks)
          : "block sets must be equal before and after reordering";
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
    Set<SeqThreadStatementBlock> visited = new HashSet<>();
    // add current block to visited to prevent infinite recursion
    visited.add(pFirstBlock);
    recursivelyBuildBlockGraph(pFirstBlock, blockGraph, pLabelBlockMap, visited);
    recursivelyReorderBlocks(blockGraph, foundOrder);
    assert !foundOrder.isEmpty() : "could not find any order";
    return ImmutableList.copyOf(foundOrder);
  }

  private static void recursivelyBuildBlockGraph(
      SeqThreadStatementBlock pCurrentBlock,
      ListMultimap<SeqThreadStatementBlock, SeqThreadStatementBlock> pGraph,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      Set<SeqThreadStatementBlock> pVisited) {

    for (SeqThreadStatement statement : pCurrentBlock.getStatements()) {
      Optional<Integer> targetNumber = SeqThreadStatementUtil.tryGetTargetPcOrGotoNumber(statement);
      if (targetNumber.isPresent()) {
        if (targetNumber.orElseThrow() != Sequentialization.EXIT_PC) {
          SeqThreadStatementBlock target = pLabelBlockMap.get(targetNumber.orElseThrow());
          assert target != null : "target could not be found in map";
          // non-loop starts are always added as targets,
          // loop starts are added only once to prevent backward jumps
          if (!target.isLoopStart() || pVisited.add(target)) {
            pGraph.get(pCurrentBlock).add(target);
            // if the target is a loop start, it is already in pVisited at this location
            if (target.isLoopStart() || pVisited.add(target)) {
              recursivelyBuildBlockGraph(target, pGraph, pLabelBlockMap, pVisited);
            }
          }
        }
      }
    }
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
      for (SeqThreadStatementBlock target : pBlockGraph.get(block)) {
        if (!pBlockGraph.keySet().contains(target)) {
          // if any target is not a key i.e. does not target another block -> add
          tryAddToFoundOrder(target, pFoundOrder);
        }
      }
      // remove "used" block
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
        Comparator.comparingInt((SeqThreadStatementBlock block) -> block.getLabel().labelNumber)
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

  // Pointer Parameters ============================================================================

  public static ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
      mapPointerParameterAssignments(
          ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableTable.Builder<ThreadEdge, CParameterDeclaration, CSimpleDeclaration> rAssignments =
        ImmutableTable.builder();
    for (MPORThread thread : pClauses.keySet()) {
      for (SeqThreadStatementClause clause : pClauses.get(thread)) {
        for (SeqThreadStatement statement : clause.getAllStatements()) {
          if (statement instanceof SeqParameterAssignmentStatements parameterStatement) {
            for (FunctionParameterAssignment assignment : parameterStatement.getAssignments()) {
              extractPointerParameterAssignment(assignment, rAssignments);
            }
          } else if (statement instanceof SeqThreadCreationStatement threadCreation) {
            extractPointerParameterAssignment(
                threadCreation.getStartRoutineArgAssignment(), rAssignments);
          }
        }
      }
    }
    return rAssignments.buildOrThrow();
  }

  private static void extractPointerParameterAssignment(
      FunctionParameterAssignment pFunctionParameterAssignment,
      ImmutableTable.Builder<ThreadEdge, CParameterDeclaration, CSimpleDeclaration> pAssignments) {

    if (pFunctionParameterAssignment.getOriginalRightHandSideDeclaration().isPresent()) {
      if (pFunctionParameterAssignment.isPointer()) {
        pAssignments.put(
            pFunctionParameterAssignment.getCallContext(),
            pFunctionParameterAssignment.getOriginalParameterDeclaration(),
            pFunctionParameterAssignment.getOriginalRightHandSideDeclaration().orElseThrow());
      }
    }
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
