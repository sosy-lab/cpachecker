// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SingleControlExpressionUtil {

  static String buildStatementString(
      SeqSingleControlExpression pStatement, String pExpressionString) {

    return pStatement.getEncoding().keyword
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInBrackets(pExpressionString);
  }

  static String buildExpressionString(
      Optional<CExpression> pCExpression, Optional<SeqExpression> pSeqExpression)
      throws UnrecognizedCodeException {

    checkArgument(
        pCExpression.isPresent() || pSeqExpression.isPresent(),
        "either pCExpression or pSeqExpression must be present");
    if (pCExpression.isPresent()) {
      return pCExpression.orElseThrow().toASTString();
    } else {
      return pSeqExpression.orElseThrow().toASTString();
    }
  }
}
