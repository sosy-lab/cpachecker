// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibProcedureCallStatement extends SvLibStatement {
  @Serial private static final long serialVersionUID = -2879361994769890189L;

  private final SvLibProcedureDeclaration procedureDeclaration;
  private final ImmutableList<SvLibTerm> arguments;
  private final ImmutableList<SvLibSimpleParsingDeclaration> returnVariables;

  public SvLibProcedureCallStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      SvLibProcedureDeclaration pProcedureDeclaration,
      List<SvLibTerm> pArguments,
      List<SvLibSimpleParsingDeclaration> pReturnVariables) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    procedureDeclaration = pProcedureDeclaration;
    arguments = ImmutableList.copyOf(pArguments);
    returnVariables = ImmutableList.copyOf(pReturnVariables);

    checkArguments(arguments, procedureDeclaration);
    checkReturnValues(returnVariables, procedureDeclaration);
  }

  private void checkArguments(
      List<SvLibTerm> pArguments, SvLibProcedureDeclaration pProcedureDeclaration) {
    checkArgument(
        pArguments.size() == pProcedureDeclaration.getParameters().size(),
        "Number of arguments provided does not match the number of input parameters in the"
            + " procedure declaration");
    for (int i = 0; i < pArguments.size(); i++) {
      SvLibTerm argument = pArguments.get(i);
      SvLibType argumentType = argument.getExpressionType();
      SvLibParsingParameterDeclaration parameterDeclaration =
          pProcedureDeclaration.getParameters().get(i);
      SvLibType parameterType = parameterDeclaration.getType();

      if (!argumentType.equals(parameterType)) {
        throw new IllegalArgumentException(
            "The type "
                + argumentType
                + " of a procedure call argument does not match the the type "
                + parameterType
                + " of the declaration of the parameter "
                + parameterDeclaration.getName()
                + " in the procedure declaration of "
                + pProcedureDeclaration.getProcedureName()
                + "!");
      }
    }
  }

  private void checkReturnValues(
      List<SvLibSimpleParsingDeclaration> pReturnVariables,
      SvLibProcedureDeclaration pProcedureDeclaration) {
    checkArgument(
        pReturnVariables.size() == pProcedureDeclaration.getReturnValues().size(),
        "Number of return parameters of procedure call to procedure "
            + pProcedureDeclaration.getProcedureName()
            + " does not match number of return parameters of the procedure declaration!");
    for (int i = 0; i < pReturnVariables.size(); i++) {
      SvLibSimpleParsingDeclaration returnVariableDeclaration = pReturnVariables.get(i);
      SvLibParsingParameterDeclaration returnParameterDeclaration =
          pProcedureDeclaration.getReturnValues().get(i);
      if (!returnVariableDeclaration.getType().equals(returnParameterDeclaration.getType())) {
        throw new IllegalArgumentException(
            "The type "
                + returnVariableDeclaration.getType()
                + " of a return variable of the procedure call to "
                + pProcedureDeclaration.getProcedureName()
                + " does not match the type "
                + returnParameterDeclaration.getType()
                + " of the return parameter "
                + returnParameterDeclaration.getName()
                + " in the procedure declaration!");
      }
    }
  }

  public SvLibProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return super.getFileLocation();
  }

  @Override
  public String toASTStringWithoutTags() {
    return "(call "
        + procedureDeclaration.getName()
        + " ("
        + String.join(" ", arguments.stream().map(arg -> arg.toASTString()).toList())
        + ") ("
        + String.join(" ", returnVariables.stream().map(var -> var.toASTString()).toList())
        + "))";
  }

  public ImmutableList<SvLibSimpleParsingDeclaration> getReturnVariables() {
    return returnVariables;
  }

  public ImmutableList<SvLibTerm> getArguments() {
    return arguments;
  }
}
