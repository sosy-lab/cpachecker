// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssertCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Command;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3DeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GetCounterexampleCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GetProofCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SetLogicCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Statement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.k3.VerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AnnotateTagContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AssertCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.DeclareConstCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.DeclareVarContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.DefineProcContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.GetCounterexampleContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.GetProofContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ProcDeclarationArgumentsContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SetLogicCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SortContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SymbolContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.VerifyCallContext;

class CommandToAstConverter extends AbstractAntlrToAstConverter<K3Command> {

  private final StatementToAstConverter statementConverter;

  private final TermToAstConverter termConverter;

  private final TagToAstConverter tagToAstConverter;

  private final K3UninterpretedScope uninterpretedScope;

  public CommandToAstConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    uninterpretedScope = new K3UninterpretedScope();
    statementConverter = new StatementToAstConverter(pScope, pFilePath);
    termConverter = new TermToAstConverter(pScope, pFilePath);
    tagToAstConverter = new TagToAstConverter(uninterpretedScope, pFilePath);
  }

  public CommandToAstConverter(K3Scope pScope) {
    super(pScope);
    uninterpretedScope = new K3UninterpretedScope();
    statementConverter = new StatementToAstConverter(pScope);
    termConverter = new TermToAstConverter(pScope);
    tagToAstConverter = new TagToAstConverter(uninterpretedScope);
  }

  @Override
  public K3Command visitDeclareVar(DeclareVarContext ctx) {
    String variableName = ctx.symbol().getText();
    K3Type variableType = K3Type.getTypeForString(ctx.sort().getText());
    K3VariableDeclaration variableDeclaration =
        new K3VariableDeclaration(
            fileLocationFromContext(ctx),
            true,
            false,
            variableType,
            variableName,
            variableName,
            variableName);

    scope.addVariable(variableDeclaration);

    return new K3VariableDeclarationCommand(variableDeclaration, fileLocationFromContext(ctx));
  }

  @Override
  public K3Command visitDeclareConstCommand(DeclareConstCommandContext ctx) {
    String variableName = ctx.cmd_declareConst().symbol().getText();
    K3Type variableType = K3Type.getTypeForString(ctx.cmd_declareConst().sort().getText());
    K3VariableDeclaration variableDeclaration =
        new K3VariableDeclaration(
            fileLocationFromContext(ctx),
            true,
            true,
            variableType,
            variableName,
            variableName,
            variableName);

    scope.addVariable(variableDeclaration);

    return new K3DeclareConstCommand(variableDeclaration, fileLocationFromContext(ctx));
  }

  private List<K3ParameterDeclaration> createParameterDeclarations(
      ProcDeclarationArgumentsContext pContext, String pProcedureName) {
    ImmutableList.Builder<K3ParameterDeclaration> parameters = ImmutableList.builder();
    for (int i = 0; i < pContext.symbol().size(); i++) {

      SymbolContext parameter = pContext.symbol(i);
      SortContext sort = pContext.sort(i);
      parameters.add(
          new K3ParameterDeclaration(
              fileLocationFromContext(parameter, sort),
              K3Type.getTypeForString(sort.getText()),
              parameter.getText(),
              pProcedureName));
    }

    return parameters.build();
  }

  @Override
  public K3Command visitDefineProc(DefineProcContext ctx) {
    String procedureName = ctx.symbol().getText();
    List<K3ParameterDeclaration> inputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(0), procedureName);
    List<K3ParameterDeclaration> localVariables =
        createParameterDeclarations(ctx.procDeclarationArguments(1), procedureName);
    List<K3ParameterDeclaration> outputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(2), procedureName);
    K3ProcedureDeclaration procedureDeclaration =
        new K3ProcedureDeclaration(
            fileLocationFromContext(ctx),
            procedureName,
            inputParameter,
            localVariables,
            outputParameter);

    scope.enterProcedure(
        FluentIterable.from(inputParameter)
            .append(localVariables)
            .append(outputParameter)
            .toList());

    K3Statement body = statementConverter.visit(ctx.statement());

    scope.leaveProcedure();

    scope.addProcedureDeclaration(procedureDeclaration);

    return new K3ProcedureDefinitionCommand(
        fileLocationFromContext(ctx), procedureDeclaration, body);
  }

  @Override
  public K3Command visitVerifyCall(VerifyCallContext pContext) {
    K3ProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pContext.symbol().getText());
    List<K3Term> terms = transformedImmutableListCopy(pContext.term(), termConverter::visit);

    return new VerifyCallCommand(procedureDeclaration, terms, fileLocationFromContext(pContext));
  }

  @Override
  public K3Command visitAnnotateTag(AnnotateTagContext pContext) {
    List<K3TagProperty> tags =
        FluentIterable.from(pContext.attribute())
            .transform(attribute -> tagToAstConverter.visit(attribute))
            .filter(K3TagProperty.class)
            .toList();
    String tagName = pContext.symbol().getText();
    return new K3AnnotateTagCommand(tagName, tags, fileLocationFromContext(pContext));
  }

  @Override
  public K3Command visitGetProof(GetProofContext pContext) {
    return new K3GetProofCommand(fileLocationFromContext(pContext));
  }

  @Override
  public K3Command visitGetCounterexample(GetCounterexampleContext pContext) {
    return new K3GetCounterexampleCommand(fileLocationFromContext(pContext));
  }

  @Override
  public K3Command visitSetLogicCommand(SetLogicCommandContext pContext) {
    SmtLibLogic logic = SmtLibLogic.fromString(pContext.cmd_setLogic().symbol().getText());
    // We need to make all scopes aware of the selected logic.
    // Such that symbols can be resolved correctly.
    scope.addLogic(logic);
    uninterpretedScope.addLogic(logic);
    return new K3SetLogicCommand(logic, fileLocationFromContext(pContext));
  }

  @Override
  public K3Command visitAssertCommand(AssertCommandContext pContext) {
    K3Term term = termConverter.visit(pContext.cmd_assert().term());
    return new K3AssertCommand(term, fileLocationFromContext(pContext));
  }
}
