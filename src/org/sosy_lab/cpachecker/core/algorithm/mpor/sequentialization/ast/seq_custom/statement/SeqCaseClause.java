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

  /** The suffix of the case block, either {@code break;} or {@code continue;} */
  public enum CaseBlockTerminator {
    BREAK(SeqToken.BREAK),
    CONTINUE(SeqToken.CONTINUE);

    private final String asString;

    CaseBlockTerminator(String pAsString) {
      asString = pAsString;
    }
  }

  private static long currentId = 0;

  public final long id;

  public final boolean isGlobal;

  public final SeqCaseLabel caseLabel;

  public final SeqCaseBlock caseBlock;

  public final CaseBlockTerminator caseBlockTerminator;

  public SeqCaseClause(
      boolean pIsGlobal,
      int pCaseLabelValue,
      ImmutableList<SeqCaseBlockStatement> pCaseBlockStatements,
      CaseBlockTerminator pCaseBlockTerminator) {

    id = createNewId();
    isGlobal = pIsGlobal;
    caseLabel = new SeqCaseLabel(pCaseLabelValue);
    caseBlock = new SeqCaseBlock(pCaseBlockStatements);
    caseBlockTerminator = pCaseBlockTerminator;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqCaseClause(
      long pId,
      boolean pIsGlobal,
      SeqCaseLabel pCaseLabel,
      SeqCaseBlock pCaseBlocks,
      CaseBlockTerminator pCaseBlockEndingType) {
    id = pId;
    isGlobal = pIsGlobal;
    caseLabel = pCaseLabel;
    caseBlock = pCaseBlocks;
    caseBlockTerminator = pCaseBlockEndingType;
  }

  public SeqCaseClause cloneWithCaseLabel(SeqCaseLabel pCaseLabel) {
    return new SeqCaseClause(id, isGlobal, pCaseLabel, caseBlock, caseBlockTerminator);
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
        + caseBlockTerminator.asString
        + SeqSyntax.SEMICOLON
        + SeqSyntax.NEWLINE;
  }
}
