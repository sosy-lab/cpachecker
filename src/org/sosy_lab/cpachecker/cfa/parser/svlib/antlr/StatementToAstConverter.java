// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AnnotatedStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AssignStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AssumeStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AttributeSvLibContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.BreakStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.CallStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ChoiceStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ContinueStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.GotoStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.HavocStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.IfStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.LabelStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ReturnStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SequenceStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.StatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.WhileStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibBreakStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibChoiceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibContinueStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibGotoStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibLabelStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibWhileStatement;

class StatementToAstConverter extends AbstractAntlrToAstConverter<SvLibStatement> {

  private final TermToAstConverter termToAstConverter;
  private final TagToAstConverter tagToAstConverter;

  private Optional<List<SvLibTagProperty>> tagAttributes = Optional.empty();

  private Optional<List<SvLibTagReference>> tagReferences = Optional.empty();

  public StatementToAstConverter(
      SvLibScope pScope,
      Path pFilePath,
      ImmutableMap.Builder<SvLibTagReference, SvLibScope> pTagReferenceToScopeBuilder) {
    super(pScope, pFilePath);
    termToAstConverter = new TermToAstConverter(pScope, pFilePath);
    tagToAstConverter = new TagToAstConverter(pScope, pFilePath, pTagReferenceToScopeBuilder);
  }

  public StatementToAstConverter(
      SvLibScope pScope,
      ImmutableMap.Builder<SvLibTagReference, SvLibScope> pTagReferenceToScopeBuilder) {
    super(pScope);
    termToAstConverter = new TermToAstConverter(pScope);
    tagToAstConverter = new TagToAstConverter(pScope, pTagReferenceToScopeBuilder);
  }

  private List<SvLibTagProperty> getTagAttributes() {
    List<SvLibTagProperty> attributes = tagAttributes.orElse(ImmutableList.of());
    tagAttributes = Optional.empty();
    return attributes;
  }

  private List<SvLibTagReference> getTagReferences() {
    List<SvLibTagReference> references = tagReferences.orElse(ImmutableList.of());
    tagReferences = Optional.empty();
    return references;
  }

  private void updateTagReferences(List<AttributeSvLibContext> pAttributeContexts) {
    ImmutableList.Builder<SvLibTagReference> tagReferencesBuilder = ImmutableList.builder();
    ImmutableList.Builder<SvLibTagProperty> tagAttributeBuilder = ImmutableList.builder();
    for (AttributeSvLibContext attributeContext : pAttributeContexts) {
      SvLibTagAttribute tagProperty = tagToAstConverter.visit(attributeContext);
      switch (tagProperty) {
        case SvLibTagReference tagReference -> tagReferencesBuilder.add(tagReference);
        case SvLibTagProperty tagAttribute -> tagAttributeBuilder.add(tagAttribute);
      }
    }
    tagReferences = Optional.of(tagReferencesBuilder.build());
    tagAttributes = Optional.of(tagAttributeBuilder.build());
  }

  @Override
  public SvLibStatement visitSequenceStatement(SequenceStatementContext ctx) {
    // Could be simplified by using a FluentIterable, but this is easier
    // for debugging.
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();
    ImmutableList.Builder<SvLibStatement> statementsBuilder = ImmutableList.builder();
    for (StatementContext statementContext : ctx.statement()) {
      SvLibStatement statement = visit(statementContext);
      statementsBuilder.add(statement);
    }
    List<SvLibStatement> statements = statementsBuilder.build();
    FileLocation location = fileLocationFromContext(ctx);
    return new SvLibSequenceStatement(statements, location, properties, references);
  }

  @Override
  public SvLibStatement visitAnnotatedStatement(AnnotatedStatementContext ctx) {
    updateTagReferences(ctx.attributeSvLib());
    return visit(ctx.statement());
  }

  @Override
  public SvLibStatement visitAssumeStatement(AssumeStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();
    FileLocation location = fileLocationFromContext(ctx);
    SvLibTerm term = termToAstConverter.visit(ctx.term());
    return new SvLibAssumeStatement(location, term, properties, references);
  }

  @Override
  public SvLibStatement visitReturnStatement(ReturnStatementContext ctx) {
    return new SvLibReturnStatement(
        fileLocationFromContext(ctx), getTagAttributes(), getTagReferences());
  }

  @Override
  public SvLibStatement visitIfStatement(IfStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();

    SvLibTerm controllingExpression = termToAstConverter.visit(ctx.term());
    SvLibStatement thenStatement = visit(ctx.statement(0));
    if (ctx.statement().size() == 2) {
      SvLibStatement elseStatement = visit(ctx.statement(1));
      return new SvLibIfStatement(
          fileLocationFromContext(ctx),
          properties,
          references,
          controllingExpression,
          thenStatement,
          elseStatement);
    } else {
      return new SvLibIfStatement(
          fileLocationFromContext(ctx),
          properties,
          references,
          controllingExpression,
          thenStatement);
    }
  }

  @Override
  public SvLibStatement visitWhileStatement(WhileStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();

    SvLibTerm controllingExpression = termToAstConverter.visit(ctx.term());
    SvLibStatement body = visit(ctx.statement());
    return new SvLibWhileStatement(
        controllingExpression, body, properties, references, fileLocationFromContext(ctx));
  }

  @Override
  public SvLibStatement visitBreakStatement(BreakStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();
    return new SvLibBreakStatement(fileLocationFromContext(ctx), properties, references);
  }

  @Override
  public SvLibStatement visitContinueStatement(ContinueStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();
    return new SvLibContinueStatement(fileLocationFromContext(ctx), properties, references);
  }

  @Override
  public SvLibStatement visitHavocStatement(HavocStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();

    List<SvLibSimpleParsingDeclaration> variables =
        transformedImmutableListCopy(
            ctx.symbol(), x -> scope.getVariable(Objects.requireNonNull(x).getText()));
    FileLocation location = fileLocationFromContext(ctx);
    return new SvLibHavocStatement(location, properties, references, variables);
  }

  @Override
  public SvLibStatement visitGotoStatement(GotoStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();

    String label = ctx.symbol().getText();
    FileLocation location = fileLocationFromContext(ctx);
    return new SvLibGotoStatement(location, properties, references, label);
  }

  @Override
  public SvLibStatement visitLabelStatement(LabelStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();

    String label = ctx.symbol().getText();
    FileLocation location = fileLocationFromContext(ctx);
    return new SvLibLabelStatement(location, properties, references, label);
  }

  @Override
  public SvLibStatement visitAssignStatement(AssignStatementContext ctx) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();

    ImmutableMap.Builder<SvLibSimpleParsingDeclaration, SvLibTerm> assignments =
        ImmutableMap.builder();
    for (int i = 0; i < ctx.symbol().size(); i++) {
      SvLibSimpleParsingDeclaration leftHandSide =
          scope.getVariable(Objects.requireNonNull(ctx.symbol(i)).getText());
      SvLibTerm rightHandSide = termToAstConverter.visit(Objects.requireNonNull(ctx.term(i)));
      assignments.put(leftHandSide, rightHandSide);
    }
    FileLocation location = fileLocationFromContext(ctx);
    return new SvLibAssignmentStatement(
        assignments.buildOrThrow(), location, properties, references);
  }

  @Override
  public SvLibStatement visitChoiceStatement(ChoiceStatementContext pContext) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();

    ImmutableList.Builder<SvLibStatement> optionsBuilder = ImmutableList.builder();
    for (StatementContext statementContext : pContext.statement()) {
      SvLibStatement option = visit(statementContext);
      optionsBuilder.add(option);
    }

    return new SvLibChoiceStatement(
        optionsBuilder.build(), fileLocationFromContext(pContext), properties, references);
  }

  @Override
  public SvLibStatement visitCallStatement(CallStatementContext pContext) {
    List<SvLibTagProperty> properties = getTagAttributes();
    List<SvLibTagReference> references = getTagReferences();
    SvLibProcedureDeclaration procedureDeclaration =
        scope.getProcedureDeclaration(pContext.symbol().getFirst().getText());
    List<SvLibTerm> arguments =
        transformedImmutableListCopy(
            pContext.term(), termContext -> termToAstConverter.visit(termContext));
    List<SvLibSimpleParsingDeclaration> returnVariables =
        transformedImmutableListCopy(
            pContext.symbol().subList(1, pContext.symbol().size()),
            symbolContext -> scope.getVariable(Objects.requireNonNull(symbolContext).getText()));

    return new SvLibProcedureCallStatement(
        fileLocationFromContext(pContext),
        properties,
        references,
        procedureDeclaration,
        arguments,
        returnVariables);
  }
}
