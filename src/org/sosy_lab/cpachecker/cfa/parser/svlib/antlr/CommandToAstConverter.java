// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AnnotateTagContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AssertCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AttributeContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Cmd_declareFunContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareConstCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareFunCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareSortCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareVarContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DefineProcContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.GetWitnessContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.OptionContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ProcDeclarationArgumentsContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SetInfoCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SetLogicCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SetOptionCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SortContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SymbolContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.VerifyCallContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAssertCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibGetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetLogicCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVerifyCallCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibCustomType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

class CommandToAstConverter extends AbstractAntlrToAstConverter<SvLibCommand> {

  private final StatementToAstConverter statementConverter;

  private final TermToAstConverter termConverter;

  private final TagToAstConverter tagToAstConverter;

  private final SvLibUninterpretedScope uninterpretedScope;

  private final SvLibSortToAstTypeConverter sortToAstTypeConverter;

  public CommandToAstConverter(
      SvLibScope pScope,
      Path pFilePath,
      ImmutableMap.Builder<SvLibTagReference, SvLibScope> pTagReferenceToScopeBuilder) {
    super(pScope, pFilePath);
    uninterpretedScope = new SvLibUninterpretedScope();
    statementConverter =
        new StatementToAstConverter(pScope, pFilePath, pTagReferenceToScopeBuilder);
    termConverter = new TermToAstConverter(pScope, pFilePath);
    tagToAstConverter =
        // The map can be ignored, since here we are not interested in resolving the tags.
        new TagToAstConverter(uninterpretedScope, pFilePath, ImmutableMap.builder());
    sortToAstTypeConverter = new SvLibSortToAstTypeConverter(scope, pFilePath);
  }

  public CommandToAstConverter(
      SvLibScope pScope,
      ImmutableMap.Builder<SvLibTagReference, SvLibScope> pTagReferenceToScopeBuilder) {
    super(pScope);
    uninterpretedScope = new SvLibUninterpretedScope();
    statementConverter = new StatementToAstConverter(pScope, pTagReferenceToScopeBuilder);
    termConverter = new TermToAstConverter(pScope);
    tagToAstConverter = new TagToAstConverter(uninterpretedScope, ImmutableMap.builder());
    sortToAstTypeConverter = new SvLibSortToAstTypeConverter(scope);
  }

  @Override
  public SvLibCommand visitDeclareVar(DeclareVarContext ctx) {
    String variableName = ctx.symbol().getText();

    SvLibType variableType = sortToAstTypeConverter.visit(ctx.sort());
    SvLibParsingVariableDeclaration variableDeclaration =
        new SvLibParsingVariableDeclaration(
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
    SvLibParsingVariableDeclaration variableDeclaration =
        new SvLibParsingVariableDeclaration(
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

  private List<SvLibParsingParameterDeclaration> createParameterDeclarations(
      ProcDeclarationArgumentsContext pContext, String pProcedureName) {
    ImmutableList.Builder<SvLibParsingParameterDeclaration> parameters = ImmutableList.builder();
    for (int i = 0; i < pContext.symbol().size(); i++) {

      SymbolContext parameter = pContext.symbol(i);
      SortContext sort = pContext.sort(i);
      parameters.add(
          new SvLibParsingParameterDeclaration(
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
    List<SvLibParsingParameterDeclaration> inputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(0), procedureName);
    List<SvLibParsingParameterDeclaration> outputParameter =
        createParameterDeclarations(ctx.procDeclarationArguments(1), procedureName);
    List<SvLibParsingParameterDeclaration> localVariables =
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
        FluentIterable.from(pContext.attributeSvLib())
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
            new SvLibCustomType(sortName, arity),
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
        new SvLibSmtFunctionDeclaration(
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

  @Override
  public SvLibCommand visitSetInfoCommand(SetInfoCommandContext pContext) {
    AttributeContext option = pContext.cmd_setInfo().attribute();
    @Nullable String attributeValue =
        option.attribute_value() != null
            ?
            // We may be parsing a string literal here, so we remove the quotes.
            option.attribute_value().getText().replace("\"", "")
            : null;

    return new SvLibSetInfoCommand(
        option.keyword().getText(), attributeValue, fileLocationFromContext(option));
  }
}
