// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3ProcedureCallStatement extends K3CfaEdgeStatement implements AFunctionCall {
  @Serial private static final long serialVersionUID = -2879361994769890189L;

  private final K3ProcedureDeclaration procedureDeclaration;
  private final List<K3Term> arguments;
  private final List<K3VariableDeclaration> returnVariables;

  public K3ProcedureCallStatement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences,
      K3ProcedureDeclaration pProcedureDeclaration,
      List<K3Term> pArguments,
      List<K3VariableDeclaration> pReturnVariables) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    procedureDeclaration = pProcedureDeclaration;
    arguments = pArguments;
    returnVariables = pReturnVariables;
  }

  public K3ProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  @Override
  public <R, X extends Exception> R accept(K3StatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return super.getFileLocation();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(call "
        + procedureDeclaration.getOrigName()
        + " ("
        + String.join(
            " ", arguments.stream().map(arg -> arg.toASTString(pAAstNodeRepresentation)).toList())
        + ") ("
        + String.join(
            " ",
            returnVariables.stream().map(var -> var.toASTString(pAAstNodeRepresentation)).toList())
        + "))";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "";
  }

  public List<K3VariableDeclaration> getReturnVariables() {
    return returnVariables;
  }

  @Override
  public AFunctionCallExpression getFunctionCallExpression() {
    throw new RuntimeException(
        "K3ProcedureCallStatement does not have a function call expression. This is due to design"
            + " choices in the language, where function calls are only statements and not"
            + " expressions.");
  }

  @Override
  public K3ProcedureDeclaration getFunctionDeclaration() {
    return procedureDeclaration;
  }

  @Override
  public List<K3Term> getParameterExpressions() {
    return arguments;
  }

  @Override
  public String toASTString() {
    return procedureDeclaration.getName()
        + "("
        + Joiner.on(", ")
            .join(getParameterExpressions().stream().map(arg -> arg.toASTString()).toList())
        + ")";
  }
}
