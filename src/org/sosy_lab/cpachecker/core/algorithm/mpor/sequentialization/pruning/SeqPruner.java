// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseLabel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcReadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnValueAssignmentSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqPruner {

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pruneCaseClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rPruned = ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      ImmutableList<SeqCaseClause> caseClauses = entry.getValue();
      if (isPrunable(caseClauses)) {
        MPORThread thread = entry.getKey();
        // if all case clauses are prunable then we want to include only the thread termination case
        //  e.g. goblint-regression/13-privatized_66-mine-W-init_true.i (t_fun exits immediately)
        if (allPrunable(caseClauses)) {
          // TODO we should check that there are not multiple thread exits when all are prunable
          SeqCaseClause threadExit = getThreadExitCaseClause(caseClauses);
          // ensure that the single thread exit case clause has label INIT_PC
          rPruned.put(
              thread,
              ImmutableList.of(
                  threadExit.label.value == Sequentialization.INIT_PC
                      ? threadExit
                      : threadExit.cloneWithLabel(new SeqCaseLabel(Sequentialization.INIT_PC))));
        } else {
          rPruned.put(thread, pruneSingleThreadCaseClauses(caseClauses));
        }
      }
    }
    // the initial pc are (often) not targeted here, we update them later to INIT_PC
    //  -> no validation of cases here
    return rPruned.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> pruneSingleThreadCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) throws UnrecognizedCodeException {

    // map the original pc to pc that are found after pruning
    ImmutableMap<Integer, CExpression> pcUpdates = createPrunedPcUpdates(pCaseClauses);
    // update each target pc so that it targets a non-blank case
    ImmutableList<SeqCaseClause> updatedTargetPc =
        updateTargetPcToNonPruned(pCaseClauses, pcUpdates);
    // update all label pc in return value assignments to updated return_pc targets
    ImmutableList<SeqCaseClause> rUpdatedLabelPc =
        updateLabelPcToNonPruned(updatedTargetPc, pcUpdates);
    Verify.verify(!isPrunable(rUpdatedLabelPc), "pruned case clauses are still prunable");
    return rUpdatedLabelPc;
  }

  private static ImmutableList<SeqCaseClause> updateTargetPcToNonPruned(
      ImmutableList<SeqCaseClause> pCaseClauses, ImmutableMap<Integer, CExpression> pPcUpdates)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqCaseClause> rUpdatedTargetPc = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.onlyWritesPc()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          if (statement.getTargetPc().isPresent()) {
            int targetPc = statement.getTargetPc().orElseThrow();
            if (pPcUpdates.containsKey(targetPc)) {
              // if pc was updated in prune, clone statement with new target pc
              newStatements.add(statement.cloneWithTargetPc(pPcUpdates.get(targetPc)));
              continue;
            }
          }
          // otherwise add unchanged statement
          newStatements.add(statement);
        }
        SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build(), Terminator.CONTINUE);
        rUpdatedTargetPc.add(caseClause.cloneWithBlock(newBlock));
      }
    }
    return rUpdatedTargetPc.build();
  }

  private static ImmutableList<SeqCaseClause> updateLabelPcToNonPruned(
      ImmutableList<SeqCaseClause> pCaseClauses, ImmutableMap<Integer, CExpression> pPcUpdates) {

    ImmutableList.Builder<SeqCaseClause> rUpdatedLabelPc = ImmutableList.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (isSingleReturnValueAssignmentSwitchStatement(caseClause.block.statements)) {
        SeqReturnValueAssignmentSwitchStatement switchStatement =
            (SeqReturnValueAssignmentSwitchStatement) caseClause.block.statements.get(0);
        ImmutableList.Builder<SeqCaseClause> newCaseClauses = ImmutableList.builder();
        for (SeqCaseClause switchCaseClause : switchStatement.caseClauses) {
          // if the pc for the return_pc that we switch over is updated, update labels
          int label = switchCaseClause.label.value;
          if (pPcUpdates.containsKey(label)) {
            CExpression labelExpression = pPcUpdates.get(label);
            Verify.verify(
                labelExpression instanceof CIntegerLiteralExpression,
                "updated label must be CIntegerLiteralExpression");
            SeqCaseLabel newLabel =
                new SeqCaseLabel((CIntegerLiteralExpression) requireNonNull(labelExpression));
            newCaseClauses.add(switchCaseClause.cloneWithLabel(newLabel));
          } else {
            newCaseClauses.add(switchCaseClause);
          }
        }
        SeqReturnValueAssignmentSwitchStatement clonedSwitch =
            switchStatement.cloneWithCaseClauses(newCaseClauses.build());
        SeqCaseBlock newBlock =
            new SeqCaseBlock(ImmutableList.of(clonedSwitch), Terminator.CONTINUE);
        rUpdatedLabelPc.add(caseClause.cloneWithBlock(newBlock));
      } else {
        rUpdatedLabelPc.add(caseClause);
      }
    }
    return rUpdatedLabelPc.build();
  }

  private static boolean isSingleReturnValueAssignmentSwitchStatement(
      ImmutableList<SeqCaseBlockStatement> pStatements) {

    return pStatements.size() == 1
        && pStatements.get(0) instanceof SeqReturnValueAssignmentSwitchStatement;
  }

  /**
   * Maps pre prune {@code int pc} to post prune {@link CExpression}. Not all target {@code pc} are
   * present as keys.
   */
  private static ImmutableMap<Integer, CExpression> createPrunedPcUpdates(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    Set<Integer> visitedPrePrunePc = new HashSet<>();
    ImmutableMap.Builder<Integer, CExpression> rMap = ImmutableMap.builder();
    // TODO the current reason why we dont use the list directly is that the unpruned cases have
    //  gaps in their label pcs -> try and refactor
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        mapCaseLabelValueToCaseClauses(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.onlyWritesPc()) {
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          if (statement.getTargetPc().isPresent()) {
            int targetPc = statement.getTargetPc().orElseThrow();
            Optional<CExpression> postPrunePc =
                findTargetPcExpression(caseClause, statement, labelValueMap);
            if (postPrunePc.isPresent() && visitedPrePrunePc.add(targetPc)) {
              rMap.put(targetPc, postPrunePc.orElseThrow());
            }
          }
        }
      }
    }
    return rMap.buildOrThrow();
  }

  private static Optional<CExpression> findTargetPcExpression(
      SeqCaseClause pCaseClause,
      SeqCaseBlockStatement pStatement,
      ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqCaseClause nextCaseClause = requireNonNull(pLabelValueMap.get(targetPc));
        if (nextCaseClause.onlyWritesPc()) {
          SeqCaseClause nonBlank =
              findNonBlankCaseClause(pCaseClause, nextCaseClause, pLabelValueMap);
          return Optional.of(extractTargetPcExpression(nonBlank));
        }
      }
    }
    return Optional.empty();
  }

  private static CExpression extractTargetPcExpression(SeqCaseClause pNonBlank) {
    SeqCaseBlockStatement nonBlankSingleStatement = pNonBlank.block.statements.get(0);
    if (nonBlankSingleStatement instanceof SeqReturnPcReadStatement) {
      // if we read a return pc (pc[i] = return_pc), then return return_pc as target
      return nonBlankSingleStatement.getTargetPcExpression().orElseThrow();
    }

    if (nonBlankSingleStatement instanceof SeqBlankStatement) {
      Verify.verify(validPrunableCaseClause(pNonBlank));
      int nonBlankTargetPc = pNonBlank.block.statements.get(0).getTargetPc().orElseThrow();
      Verify.verify(nonBlankTargetPc == Sequentialization.EXIT_PC);
      // if the found non-blank is still blank, it must be an exit location
      return SeqIntegerLiteralExpression.buildIntegerLiteralExpression(nonBlankTargetPc);
    }

    int nonBlankLabelPc = pNonBlank.label.value;
    // otherwise return label pc of the found non-blank as target pc
    return SeqIntegerLiteralExpression.buildIntegerLiteralExpression(nonBlankLabelPc);
  }

  /**
   * A helper mapping {@link SeqCaseClause}s to their {@link SeqCaseLabel} values, which are always
   * {@code int} values in the sequentialization.
   */
  private static ImmutableMap<Integer, SeqCaseClause> mapCaseLabelValueToCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableMap.Builder<Integer, SeqCaseClause> rOriginPcs = ImmutableMap.builder();
    for (SeqCaseClause caseClause : pCaseClauses) {
      rOriginPcs.put(caseClause.label.value, caseClause);
    }
    return rOriginPcs.buildOrThrow();
  }

  private static SeqCaseClause getThreadExitCaseClause(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        if (statement.getTargetPc().orElseThrow() == Sequentialization.EXIT_PC) {
          return caseClause;
        }
      }
    }
    throw new AssertionError("no thread exit found in pCaseClauses");
  }

  /**
   * Returns the first non-prunable {@link SeqCaseClause} in the {@code case} path, starting in
   * pInitial.
   */
  public static SeqCaseClause findNonBlankCaseClause(
      final SeqCaseClause pInitial,
      SeqCaseClause pCurrent,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    checkArgument(!pInitial.onlyWritesPc(), "pInitial must not be prunable");
    if (pCurrent.onlyWritesPc()) {
      SeqCaseBlockStatement singleStatement = pCurrent.block.statements.get(0);
      if (singleStatement instanceof SeqReturnPcReadStatement) {
        return pCurrent;
      }
      Verify.verify(validPrunableCaseClause(pCurrent));
      int targetPc = singleStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqCaseClause nextCaseClause = requireNonNull(pLabelValueMap.get(targetPc));
        return findNonBlankCaseClause(pInitial, nextCaseClause, pLabelValueMap);
      }
    }
    return pCurrent;
  }

  /**
   * Returns {@code true} if {@code pCaseClause} has exactly 1 {@link SeqBlankStatement} and a
   * target {@code pc} and throws a {@link IllegalArgumentException} otherwise.
   */
  private static boolean validPrunableCaseClause(SeqCaseClause pCaseClause) {
    checkArgument(
        pCaseClause.block.statements.size() == 1,
        "prunable case clauses must contain exactly 1 statement");
    SeqCaseBlockStatement statement = pCaseClause.block.statements.get(0);
    checkArgument(statement.onlyWritesPc(), "prunable case clauses must only write pc");
    checkArgument(
        statement.getTargetPc().isPresent() || statement.getTargetPcExpression().isPresent(),
        "prunable case clauses must contain a target pc");
    return true;
  }

  /**
   * Returns {@code true} if any {@link SeqCaseClause} can be pruned, i.e. contains only blank
   * statements, and {@code false} otherwise.
   */
  private static boolean isPrunable(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (caseClause.onlyWritesPc()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if all {@link SeqCaseClause}s can be pruned, i.e. contains only blank
   * statements, and {@code false} otherwise.
   */
  private static boolean allPrunable(ImmutableList<SeqCaseClause> pCaseClauses) {
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.onlyWritesPc()) {
        return false;
      }
    }
    return true;
  }
}
