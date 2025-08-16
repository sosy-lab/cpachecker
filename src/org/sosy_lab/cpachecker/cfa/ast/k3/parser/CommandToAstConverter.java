// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Command;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Statement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.VerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.DeclareVarContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.DefineProcContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ProcDeclarationArgumentsContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SortContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.VariableContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.VerifyCallContext;

class CommandToAstConverter extends AbstractAntlrToAstConverter<K3Command> {

  private final StatementToAstConverter statementConverter;

  private final TermToAstConverter termConverter;

  public CommandToAstConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    statementConverter = new StatementToAstConverter(pScope, pFilePath);
    termConverter = new TermToAstConverter(pScope, pFilePath);
  }

  public CommandToAstConverter(K3Scope pScope) {
    super(pScope);
    statementConverter = new StatementToAstConverter(pScope);
    termConverter = new TermToAstConverter(pScope);
  }

  @Override
  public K3Command visitDeclareVar(DeclareVarContext ctx) {
    String variableName = ctx.variable().getText();
    K3Type variableType = K3Type.getTypeForString(ctx.sort().getText());
    K3VariableDeclaration variableDeclaration =
        new K3VariableDeclaration(
            fileLocationFromContext(ctx),
            true,
            variableType,
            variableName,
            variableName,
            variableName);

    scope.addVariable(variableDeclaration);

    return new K3VariableDeclarationCommand(variableDeclaration);
  }

  private List<K3ParameterDeclaration> createParameterDeclarations(
      ProcDeclarationArgumentsContext pContext) {
    ImmutableList.Builder<K3ParameterDeclaration> parameters = ImmutableList.builder();
    for (int i = 0; i < pContext.variable().size(); i++) {

      VariableContext parameter = pContext.variable(i);
      SortContext sort = pContext.sort(i);
      parameters.add(
          new K3ParameterDeclaration(
              fileLocationFromContext(parameter, sort),
              K3Type.getTypeForString(sort.getText()),
              parameter.getText()));
    }

    return parameters.build();
  }

  @Override
  public K3Command visitDefineProc(DefineProcContext ctx) {
    String procedureName = ctx.procedureName().getText();
    List<K3ParameterDeclaration> inputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(0));
    List<K3ParameterDeclaration> localVariables =
        createParameterDeclarations(ctx.procDeclarationArguments(1));
    List<K3ParameterDeclaration> outputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(2));
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            fileLocationFromContext(ctx),
            procedureName,
            inputParameter,
            localVariables,
            outputParameter);

    scope.enterFunctionProcedure(
        FluentIterable.from(inputParameter)
            .append(localVariables)
            .append(outputParameter)
            .toList());

    K3Statement body = statementConverter.visit(ctx.statement());

    scope.exitFunctionProcedure();

    scope.addProcedureDeclaration(procedureDeclaration);

    return new K3ProcedureDefinitionCommand(
        fileLocationFromContext(ctx), procedureDeclaration, body);
  }

  @Override
  public K3Command visitVerifyCall(VerifyCallContext pContext) {
    K3ProcedureDeclaration procedureDeclaration =
        scope.getFunctionDeclaration(pContext.procedureName().getText());
    List<K3Term> terms =
        FluentIterable.from(pContext.term()).transform(termConverter::visit).toList();

    return new VerifyCallCommand(procedureDeclaration, terms);
  }
}
