// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SingleControlExpressionUtil {

  static String buildStatementString(
      SeqSingleControlExpression pStatement, String pExpressionString) {

    return pStatement.getEncoding().getKeyword()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInBrackets(pExpressionString);
  }
}
