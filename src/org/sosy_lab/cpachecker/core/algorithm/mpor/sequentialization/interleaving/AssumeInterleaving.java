// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.interleaving;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.BooleanExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.DeclareExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SwitchCaseExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.VariableExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqValue;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class AssumeInterleaving implements SeqElement {

  private static final Variable nextStatement = new Variable(SeqToken.NEXT_STATEMENT);

  private static final DeclareExpr declareNextStatement =
      new DeclareExpr(
          new VariableExpr(SeqDataType.INT, nextStatement),
          new FunctionCallExpr(SeqToken.NON_DET, Optional.empty()));

  private final FunctionCallExpr assumeNextStatement;

  private final SwitchCaseExpr switchCaseExpr;

  public AssumeInterleaving(ImmutableSet<CFAEdge> pEdges) {
    Value numStatements = new Value(Integer.toString(pEdges.size()));
    assumeNextStatement =
        new FunctionCallExpr(SeqToken.ASSUME, Optional.of(initAssumeNextStatement(numStatements)));
    switchCaseExpr = new SwitchCaseExpr(nextStatement, initAssumeCases(pEdges));
  }

  private static ImmutableList<SeqExpression> initAssumeNextStatement(Value pNumStatements) {
    ImmutableList.Builder<SeqExpression> rAssumeCondition = ImmutableList.builder();
    rAssumeCondition.add(
        new BooleanExpr(
            new BooleanExpr(new Value(SeqValue.ZERO), SeqOperator.LESS_OR_EQUAL, nextStatement),
            SeqOperator.AND,
            new BooleanExpr(nextStatement, SeqOperator.LESS, pNumStatements)));
    return rAssumeCondition.build();
  }

  // TODO include goto state statements (execute edges and create new states beforehand)
  private static ImmutableSet<String> initAssumeCases(ImmutableSet<CFAEdge> pEdges) {
    ImmutableSet.Builder<String> assumeCases = ImmutableSet.builder();
    int caseNum = 0;
    for (CFAEdge edge : pEdges) {
      assumeCases.add(
          SeqUtil.generateCase(Integer.toString(caseNum++), SeqUtil.createLineOfCode(edge)));
    }
    return assumeCases.build();
  }

  @Override
  public String createString() {
    // TODO
    return "";
  }
}
