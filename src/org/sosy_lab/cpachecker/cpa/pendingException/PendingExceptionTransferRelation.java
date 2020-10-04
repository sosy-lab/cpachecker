// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pendingException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class PendingExceptionTransferRelation
    extends ForwardingTransferRelation<PendingExceptionState, PendingExceptionState, Precision> {

  @Override
  protected PendingExceptionState handleDeclarationEdge(
      JDeclarationEdge cfaEdge, JDeclaration decl) {
    return state; // TODO
  }

  @Override
  protected @Nullable PendingExceptionState handleAssumption(
      JAssumeEdge cfaEdge, JExpression expression, boolean truthAssumption) {
    return state; // TODO
  }

  @Override
  protected PendingExceptionState handleStatementEdge(
      JStatementEdge cfaEdge,
      JStatement statement) {
    return state; // TODO
  }
}
