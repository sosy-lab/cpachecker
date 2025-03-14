// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalOrExpression;

/**
 * An assumption as an implication of the form {@code !(A && B) || C} where {@code !(A && B)} is the
 * antecedent and {@code C} is the consequent.
 */
public class SeqAssumption {

  public final SeqLogicalNotExpression antecedent;

  public final CBinaryExpression consequent;

  public SeqAssumption(SeqLogicalNotExpression pAntecedent, CBinaryExpression pConsequent) {
    antecedent = pAntecedent;
    consequent = pConsequent;
  }

  public SeqLogicalOrExpression toLogicalOrExpression() {
    return new SeqLogicalOrExpression(antecedent, new CToSeqExpression(consequent));
  }
}
