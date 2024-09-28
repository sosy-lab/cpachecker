// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.SeqDataEntity;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class IncrementExpr implements SeqExpression {

  public final SeqDataEntity dataEntity;

  public IncrementExpr(SeqDataEntity pDataEntity) {
    dataEntity = pDataEntity;
  }

  @Override
  public String toString() {
    return dataEntity.toString() + SeqOperator.INCREMENT + SeqSyntax.SEMICOLON;
  }
}
