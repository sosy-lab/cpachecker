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
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AnnotateTagCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AnnotateTagContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AssertCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AttributeContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Cmd_declareFunContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareConstCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareFunCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareSortCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DeclareVarContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DefineFunCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DefineProcContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.DefineProcsRecContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Function_defContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.GetWitnessContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.OptionContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ProcDeclarationArgumentsContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ProcDeclarationContext;
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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibDefineFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAssertCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareConstCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareFunCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibDeclareSortCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibGetWitnessCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProceduresRecDefinitionCommand;
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
    String variableName = nameOf(ctx.symbol());

    SvLibType variableType = sortToAstTypeConverter.visit(ctx.sort());
    SvLibParsingVariableDeclaration variableDeclaration =
        new SvLibParsingVariableDeclaration(
            fileLocationFromContext(ctx),
            true,
            false,
            variableType,
            variableName,
            variableName,
            null);

    scope.addVariable(variableDeclaration);

    return new SvLibVariableDeclarationCommand(variableDeclaration, fileLocationFromContext(ctx));
  }

  @Override
  public SvLibCommand visitDeclareConstCommand(DeclareConstCommandContext ctx) {
    return declareConstant(
        fileLocationFromContext(ctx),
        nameOf(ctx.cmd_declareConst().symbol()),
        sortToAstTypeConverter.visit(ctx.cmd_declareConst().sort()));
  }

  private SvLibDeclareConstCommand declareConstant(
      FileLocation pFileLocation, String pName, SvLibType pType) {
    SvLibParsingVariableDeclaration variableDeclaration =
        new SvLibParsingVariableDeclaration(pFileLocation, true, true, pType, pName, pName, null);
    scope.addVariable(variableDeclaration);
    return new SvLibDeclareConstCommand(variableDeclaration, pFileLocation);
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
              nameOf(parameter),
              pProcedureName));
    }

    return parameters.build();
  }

  @Override
  public SvLibCommand visitDefineFunCommand(DefineFunCommandContext ctx) {
    Function_defContext functionDefContext = ctx.cmd_defineFun().function_def();
    String functionName = nameOf(functionDefContext.symbol());
    SvLibSmtFunctionDefinition functionDeclaration =
        new SvLibSmtFunctionDefinition(
            fileLocationFromContext(ctx),
            functionName,
            FluentIterable.from(functionDefContext.sorted_var())
                .transform(sortedVarContext -> Objects.requireNonNull(sortedVarContext).sort())
                .transform(sortToAstTypeConverter::visit)
                .toList(),
            sortToAstTypeConverter.visit(functionDefContext.sort()),
            transformedImmutableListCopy(
                functionDefContext.sorted_var(),
                sortedVarContext ->
                    new SvLibParsingParameterDeclaration(
                        fileLocationFromContext(Objects.requireNonNull(sortedVarContext).symbol()),
                        sortToAstTypeConverter.visit(sortedVarContext.sort()),
                        nameOf(sortedVarContext.symbol()),
                        functionName)));

    // The parameters of the function have to be in the scope while its body is converted, because
    // the body refers to them.
    scope.enterProcedure(functionDeclaration.getParameters());
    SvLibTerm body;
    try {
      body = termConverter.visit(functionDefContext.term());
    } finally {
      scope.leaveProcedure();
    }

    // As in SMT-LIB, a function without parameters is a constant, which is used like a variable
    if (functionDeclaration.getParameters().isEmpty()) {
      scope.addVariable(
          SvLibParsingVariableDeclaration.definedConstant(
              fileLocationFromContext(ctx), functionDeclaration.getReturnType(), functionName));
    } else {
      scope.addFunctionDeclaration(
          new SvLibSmtFunctionDeclaration(
              fileLocationFromContext(ctx),
              functionName,
              transformedImmutableListCopy(
                  functionDeclaration.getParameters(), SvLibParsingParameterDeclaration::getType),
              functionDeclaration.getReturnType(),
              true));
    }

    return new SmtLibDefineFunCommand(functionDeclaration, body, fileLocationFromContext(ctx));
  }

  @Override
  public SvLibCommand visitDefineProc(DefineProcContext ctx) {
    String procedureName = nameOf(ctx.symbol());
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
  public SvLibCommand visitDefineProcsRec(DefineProcsRecContext ctx) {
    ImmutableList.Builder<SvLibProcedureDeclaration> procedureDeclarationsCollector =
        ImmutableList.builder();
    ImmutableList.Builder<SvLibStatement> bodiesCollector = ImmutableList.builder();

    for (ProcDeclarationContext declaration : ctx.procDeclaration()) {
      String procedureName = nameOf(declaration.symbol());
      List<SvLibParsingParameterDeclaration> inputParameter =
          createParameterDeclarations(declaration.procDeclarationArguments(0), procedureName);
      List<SvLibParsingParameterDeclaration> outputParameter =
          createParameterDeclarations(declaration.procDeclarationArguments(1), procedureName);
      List<SvLibParsingParameterDeclaration> localVariables =
          createParameterDeclarations(declaration.procDeclarationArguments(2), procedureName);

      SvLibProcedureDeclaration procedureDeclaration =
          new SvLibProcedureDeclaration(
              fileLocationFromContext(declaration),
              procedureName,
              inputParameter,
              outputParameter,
              localVariables);

      procedureDeclarationsCollector.add(procedureDeclaration);
      scope.addProcedureDeclaration(procedureDeclaration);
    }

    ImmutableList<SvLibProcedureDeclaration> collectedProcedureDeclarations =
        procedureDeclarationsCollector.build();

    for (int i = 0; i < ctx.statement().size(); i++) {
      SvLibProcedureDeclaration currentProcedure = collectedProcedureDeclarations.get(i);
      scope.enterProcedure(
          FluentIterable.from(currentProcedure.getParameters())
              .append(currentProcedure.getReturnValues())
              .append(currentProcedure.getLocalVariables())
              .stream()
              .toList());

      SvLibStatement body = statementConverter.visit(ctx.statement(i));
      bodiesCollector.add(body);

      scope.leaveProcedure();
    }

    return new SvLibProceduresRecDefinitionCommand(
        fileLocationFromContext(ctx), collectedProcedureDeclarations, bodiesCollector.build());
  }

  @Override
  public SvLibCommand visitVerifyCall(VerifyCallContext pContext) {
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(nameOf(pContext.symbol()));
    List<SvLibTerm> terms = transformedImmutableListCopy(pContext.term(), termConverter::visit);

    return new SvLibVerifyCallCommand(
        procedureDeclaration, terms, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibCommand visitAnnotateTag(AnnotateTagContext pContext) {
    AnnotateTagCommandContext annotateTag = pContext.annotateTagCommand();
    List<SvLibTagProperty> tags =
        FluentIterable.from(annotateTag.attributeSvLib())
            .transform(attribute -> tagToAstConverter.visit(attribute))
            .filter(SvLibTagProperty.class)
            .toList();
    String tagName = nameOf(annotateTag.symbol());
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
    String sortName = nameOf(symbolContext);
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
    String functionName = nameOf(functionDecContext.symbol());
    List<SvLibType> allTypes =
        transformedImmutableListCopy(
            functionDecContext.sort(), sort -> sortToAstTypeConverter.visit(sort));
    SvLibType returnType = allTypes.getLast();
    List<SvLibType> parameterTypes = allTypes.subList(0, allTypes.size() - 1);

    // In SMT-LIB, (declare-const c T) is an abbreviation of (declare-fun c () T)
    if (parameterTypes.isEmpty()) {
      return declareConstant(fileLocationFromContext(pContext), functionName, returnType);
    }

    SvLibSmtFunctionDeclaration functionDeclaration =
        new SvLibSmtFunctionDeclaration(
            fileLocationFromContext(functionDecContext), functionName, parameterTypes, returnType);
    // The declaration has to be in the scope, so that applications of the function can be resolved.
    scope.addFunctionDeclaration(functionDeclaration);
    return new SvLibDeclareFunCommand(functionDeclaration, fileLocationFromContext(pContext));
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
