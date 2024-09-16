// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.SeqDataEntity;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class ReturnExpr implements SeqExpression {

  public final SeqDataEntity dataEntity;

  public ReturnExpr(SeqDataEntity pDataEntity) {
    dataEntity = pDataEntity;
  }

  @Override
  public String createString() {
    return SeqToken.RETURN + SeqSyntax.SPACE + dataEntity.createString() + SeqSyntax.SEMICOLON;
  }
}
