// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibCfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatementVisitor;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagProperty;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagReference;

public final class SvLibProcedureCallStatement extends SvLibCfaEdgeStatement
    implements AFunctionCall {
  @Serial private static final long serialVersionUID = -2879361994769890189L;

  private final SvLibProcedureDeclaration procedureDeclaration;
  private final ImmutableList<SvLibTerm> arguments;
  private final ImmutableList<SvLibSimpleDeclaration> returnVariables;

  public SvLibProcedureCallStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      SvLibProcedureDeclaration pProcedureDeclaration,
      List<SvLibTerm> pArguments,
      List<SvLibSimpleDeclaration> pReturnVariables) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    procedureDeclaration = pProcedureDeclaration;
    arguments = ImmutableList.copyOf(pArguments);
    returnVariables = ImmutableList.copyOf(pReturnVariables);
  }

  public SvLibProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
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
    return toASTString(pAAstNodeRepresentation);
  }

  public ImmutableList<SvLibSimpleDeclaration> getReturnVariables() {
    return returnVariables;
  }

  @Override
  public AFunctionCallExpression getFunctionCallExpression() {
    throw new RuntimeException(
        "SvLibProcedureCallStatement does not have a function call expression. This is due to"
            + " design choices in the language, where function calls are only statements and not"
            + " expressions.");
  }

  @Override
  public SvLibProcedureDeclaration getFunctionDeclaration() {
    return procedureDeclaration;
  }

  @Override
  public ImmutableList<SvLibTerm> getParameterExpressions() {
    return arguments;
  }

  @Override
  public String toASTString() {
    return toASTString(AAstNodeRepresentation.ORIGINAL_NAMES);
  }
}
