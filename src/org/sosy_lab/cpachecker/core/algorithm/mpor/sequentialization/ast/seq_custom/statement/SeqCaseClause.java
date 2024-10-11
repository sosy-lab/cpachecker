// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

/** Represents a case clause, i.e. a case label and its case block. */
public class SeqCaseClause implements SeqStatement {

  public enum CaseBlockEndingType {
    BREAK(SeqToken.BREAK),
    CONTINUE(SeqToken.CONTINUE);

    private final String asString;

    CaseBlockEndingType(String pAsString) {
      asString = pAsString;
    }
  }

  private static long currentId = 0;

  public final long id;

  public final SeqCaseLabel caseLabel;

  public final SeqCaseBlock caseBlock;

  public final CaseBlockEndingType caseBlockEndingType;

  public SeqCaseClause(
      int pCaseLabelValue,
      ImmutableList<SeqCaseBlockStatement> pCaseBlockStatements,
      CaseBlockEndingType pCaseBlockEndingType) {

    id = createNewId();
    caseLabel = new SeqCaseLabel(pCaseLabelValue);
    caseBlock = new SeqCaseBlock(pCaseBlockStatements);
    caseBlockEndingType = pCaseBlockEndingType;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqCaseClause(
      long pId,
      SeqCaseLabel pCaseLabel,
      SeqCaseBlock pCaseBlocks,
      CaseBlockEndingType pCaseBlockEndingType) {
    id = pId;
    caseLabel = pCaseLabel;
    caseBlock = pCaseBlocks;
    caseBlockEndingType = pCaseBlockEndingType;
  }

  public SeqCaseClause cloneWithCaseLabel(SeqCaseLabel pCaseLabel) {
    return new SeqCaseClause(id, pCaseLabel, caseBlock, caseBlockEndingType);
  }

  private static long createNewId() {
    return currentId++;
  }

  /**
   * Returns true if all statements in the {@link SeqCaseBlock} are blank, i.e. they only update a
   * pc.
   */
  public boolean isPrunable() {
    for (SeqCaseBlockStatement stmt : caseBlock.statements) {
      if (!(stmt instanceof SeqBlankStatement)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toASTString() {
    return caseLabel.toASTString()
        + SeqSyntax.SPACE
        + caseBlock.toASTString()
        + caseBlockEndingType.asString
        + SeqSyntax.SEMICOLON
        + SeqSyntax.NEWLINE;
  }
}
