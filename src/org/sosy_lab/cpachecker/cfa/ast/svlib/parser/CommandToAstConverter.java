// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.parser;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssertCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCustomType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibDeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibGetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.AnnotateTagContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.AssertCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.Cmd_declareFunContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.DeclareConstCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.DeclareFunCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.DeclareSortCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.DeclareVarContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.DefineProcContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.GetWitnessContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.OptionContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.ProcDeclarationArgumentsContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.SetLogicCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.SetOptionCommandContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.SortContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.SymbolContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.VerifyCallContext;

class CommandToAstConverter extends AbstractAntlrToAstConverter<SvLibCommand> {

  private final StatementToAstConverter statementConverter;

  private final TermToAstConverter termConverter;

  private final TagToAstConverter tagToAstConverter;

  private final SvLibUninterpretedScope uninterpretedScope;

  private final SvLibSortToAstTypeConverter sortToAstTypeConverter;

  public CommandToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    uninterpretedScope = new SvLibUninterpretedScope();
    statementConverter = new StatementToAstConverter(pScope, pFilePath);
    termConverter = new TermToAstConverter(pScope, pFilePath);
    tagToAstConverter = new TagToAstConverter(uninterpretedScope, pFilePath);
    sortToAstTypeConverter = new SvLibSortToAstTypeConverter(scope, pFilePath);
  }

  public CommandToAstConverter(SvLibScope pScope) {
    super(pScope);
    uninterpretedScope = new SvLibUninterpretedScope();
    statementConverter = new StatementToAstConverter(pScope);
    termConverter = new TermToAstConverter(pScope);
    tagToAstConverter = new TagToAstConverter(uninterpretedScope);
    sortToAstTypeConverter = new SvLibSortToAstTypeConverter(scope);
  }

  @Override
  public SvLibCommand visitDeclareVar(DeclareVarContext ctx) {
    String variableName = ctx.symbol().getText();

    SvLibType variableType = sortToAstTypeConverter.visit(ctx.sort());
    SvLibVariableDeclaration variableDeclaration =
        new SvLibVariableDeclaration(
            fileLocationFromContext(ctx),
            true,
            false,
            variableType,
            variableName,
            variableName,
            variableName);

    scope.addVariable(variableDeclaration);

    return new SvLibVariableDeclarationCommand(variableDeclaration, fileLocationFromContext(ctx));
  }

  @Override
  public SvLibCommand visitDeclareConstCommand(DeclareConstCommandContext ctx) {
    String variableName = ctx.cmd_declareConst().symbol().getText();

    SvLibType variableType = sortToAstTypeConverter.visit(ctx.cmd_declareConst().sort());
    SvLibVariableDeclaration variableDeclaration =
        new SvLibVariableDeclaration(
            fileLocationFromContext(ctx),
            true,
            true,
            variableType,
            variableName,
            variableName,
            variableName);

    scope.addVariable(variableDeclaration);

    return new SvLibDeclareConstCommand(variableDeclaration, fileLocationFromContext(ctx));
  }

  private List<SvLibParameterDeclaration> createParameterDeclarations(
      ProcDeclarationArgumentsContext pContext, String pProcedureName) {
    ImmutableList.Builder<SvLibParameterDeclaration> parameters = ImmutableList.builder();
    for (int i = 0; i < pContext.symbol().size(); i++) {

      SymbolContext parameter = pContext.symbol(i);
      SortContext sort = pContext.sort(i);
      parameters.add(
          new SvLibParameterDeclaration(
              fileLocationFromContext(parameter, sort),
              sortToAstTypeConverter.visit(sort),
              parameter.getText(),
              pProcedureName));
    }

    return parameters.build();
  }

  @Override
  public SvLibCommand visitDefineProc(DefineProcContext ctx) {
    String procedureName = ctx.symbol().getText();
    List<SvLibParameterDeclaration> inputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(0), procedureName);
    List<SvLibParameterDeclaration> outputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(1), procedureName);
    List<SvLibParameterDeclaration> localVariables =
        createParameterDeclarations(ctx.procDeclarationArguments(2), procedureName);
    SvLibProcedureDeclaration procedureDeclaration =
        new SvLibProcedureDeclaration(
            fileLocationFromContext(ctx),
            procedureName,
            inputParameter,
            outputParameter,
            localVariables);

    scope.enterProcedure(
        FluentIterable.from(inputParameter)
            .append(localVariables)
            .append(outputParameter)
            .toList());

    SvLibStatement body = statementConverter.visit(ctx.statement());

    scope.leaveProcedure();

    scope.addProcedureDeclaration(procedureDeclaration);

    return new SvLibProcedureDefinitionCommand(
        fileLocationFromContext(ctx), procedureDeclaration, body);
  }

  @Override
  public SvLibCommand visitVerifyCall(VerifyCallContext pContext) {
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pContext.symbol().getText());
    List<SvLibTerm> terms = transformedImmutableListCopy(pContext.term(), termConverter::visit);

    return new SvLibVerifyCallCommand(
        procedureDeclaration, terms, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitAnnotateTag(AnnotateTagContext pContext) {
    List<SvLibTagProperty> tags =
        FluentIterable.from(pContext.attribute())
            .transform(attribute -> tagToAstConverter.visit(attribute))
            .filter(SvLibTagProperty.class)
            .toList();
    String tagName = pContext.symbol().getText();
    return new SvLibAnnotateTagCommand(tagName, tags, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitGetWitness(GetWitnessContext pContext) {
    return new SvLibGetWitnessCommand(fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitSetLogicCommand(SetLogicCommandContext pContext) {
    SmtLibLogic logic = SmtLibLogic.fromString(pContext.cmd_setLogic().symbol().getText());
    // We need to make all scopes aware of the selected logic.
    // Such that symbols can be resolved correctly.
    scope.addLogic(logic);
    uninterpretedScope.addLogic(logic);
    return new SvLibSetLogicCommand(logic, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitAssertCommand(AssertCommandContext pContext) {
    SvLibTerm term = termConverter.visit(pContext.cmd_assert().term());
    return new SvLibAssertCommand(term, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitDeclareSortCommand(DeclareSortCommandContext pContext) {
    SymbolContext symbolContext = pContext.cmd_declareSort().symbol();
    String sortName = symbolContext.getText();
    int arity = Integer.parseInt(pContext.cmd_declareSort().numeral().getText());
    SvLibSortDeclaration sortDeclaration =
        new SvLibSortDeclaration(
            fileLocationFromContext(symbolContext),
            true,
            new SvLibCustomType(sortName, arity),
            sortName,
            sortName,
            sortName);

    // We need to make all scopes aware of the declarations.
    // Such that symbols can be resolved correctly.
    scope.addSortDeclaration(sortDeclaration);
    uninterpretedScope.addSortDeclaration(sortDeclaration);

    return new SvLibDeclareSortCommand(sortDeclaration, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitDeclareFunCommand(DeclareFunCommandContext pContext) {
    Cmd_declareFunContext functionDecContext = pContext.cmd_declareFun();
    String functionName = functionDecContext.symbol().getText();
    List<SvLibType> allTypes =
        transformedImmutableListCopy(
            functionDecContext.sort(), sort -> sortToAstTypeConverter.visit(sort));
    SvLibType returnType = allTypes.getLast();
    List<SvLibType> parameterTypes = allTypes.subList(0, allTypes.size() - 1);

    return new SvLibDeclareFunCommand(
        new SvLibFunctionDeclaration(
            fileLocationFromContext(functionDecContext), functionName, parameterTypes, returnType),
        fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitSetOptionCommand(SetOptionCommandContext pContext) {
    OptionContext option = pContext.cmd_setOption().option();
    if (option.attribute() != null) {
      throw new UnsupportedOperationException(
          "Set option given by '"
              + option.getText()
              + "' with arbitrary attributes is not supported yet.");
    }

    Verify.verify(option.getChildCount() == 2);
    return new SvLibSetOptionCommand(
        option.getChild(0).getText(),
        // We may be parsing a string literal here, so we remove the quotes.
        option.getChild(1).getText().replace("\"", ""),
        fileLocationFromContext(option));
  }
}
