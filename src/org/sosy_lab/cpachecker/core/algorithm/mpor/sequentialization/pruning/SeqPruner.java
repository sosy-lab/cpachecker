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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseLabel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcReadStatement;
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

    ImmutableList.Builder<SeqCaseClause> pruned = ImmutableList.builder();
    // TODO the current reason why we dont use the list directly is that the unpruned cases have
    //  gaps in their label pcs -> try and refactor
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        mapCaseLabelValueToCaseClauses(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      if (!caseClause.onlyWritesPc()) {
        ImmutableList<SeqCaseBlockStatement> newStatements =
            cloneStatementsWithNonBlankTarget(caseClause, labelValueMap);
        SeqCaseBlock newBlock = caseClause.block.cloneWithStatements(newStatements);
        pruned.add(caseClause.cloneWithBlock(newBlock));
      }
    }
    ImmutableList<SeqCaseClause> rPruned = pruned.build();
    Verify.verify(!isPrunable(rPruned));
    return rPruned;
  }

  /**
   * Updates the target {@code pc} of all case clauses in {@code pCaseClause} so that they target a
   * non-blank {@link SeqCaseClause}, i.e. a case clause that does not only update a pc but has
   * actual statements.
   */
  private static ImmutableList<SeqCaseBlockStatement> cloneStatementsWithNonBlankTarget(
      SeqCaseClause pCaseClause, ImmutableMap<Integer, SeqCaseClause> pLabelValueMap)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
    for (SeqCaseBlockStatement statement : pCaseClause.block.statements) {
      if (statement.getTargetPc().isPresent()) {
        int targetPc = statement.getTargetPc().orElseThrow();
        if (targetPc != Sequentialization.EXIT_PC) {
          SeqCaseClause nextCaseClause = requireNonNull(pLabelValueMap.get(targetPc));
          if (nextCaseClause.onlyWritesPc()) {
            SeqCaseClause nonBlank =
                findNonBlankCaseClause(pCaseClause, nextCaseClause, pLabelValueMap);
            newStatements.add(cloneFromNonBlank(statement, nonBlank));
            continue;
          }
        }
      }
      newStatements.add(statement);
    }
    return newStatements.build();
  }

  private static SeqCaseBlockStatement cloneFromNonBlank(
      SeqCaseBlockStatement pStatement, SeqCaseClause pNonBlank) throws UnrecognizedCodeException {

    SeqCaseBlockStatement nonBlankSingleStatement = pNonBlank.block.statements.get(0);
    if (nonBlankSingleStatement instanceof SeqReturnPcReadStatement) {
      CExpression returnPc = nonBlankSingleStatement.getTargetPcExpression().orElseThrow();
      // if we read a return pc (pc[i] = return_pc), then clone statement with return_pc as target
      return pStatement.cloneWithTargetPc(returnPc);
    }

    if (nonBlankSingleStatement instanceof SeqBlankStatement) {
      Verify.verify(validPrunableCaseClause(pNonBlank));
      int nonBlankTargetPc = pNonBlank.block.statements.get(0).getTargetPc().orElseThrow();
      Verify.verify(nonBlankTargetPc == Sequentialization.EXIT_PC);
      // if the found non-blank is still blank, it must be an exit location
      CIntegerLiteralExpression targetPcExpression =
          SeqIntegerLiteralExpression.buildIntegerLiteralExpression(nonBlankTargetPc);
      return pStatement.cloneWithTargetPc(targetPcExpression);
    }

    int nonBlankLabelPc = pNonBlank.label.value;
    CIntegerLiteralExpression labelPcExpression =
        SeqIntegerLiteralExpression.buildIntegerLiteralExpression(nonBlankLabelPc);
    // otherwise clone statement with the label pc of the found non-blank as target pc
    return pStatement.cloneWithTargetPc(labelPcExpression);
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
