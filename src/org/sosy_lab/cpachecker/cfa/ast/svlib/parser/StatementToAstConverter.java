// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.parser;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBreakStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibChoiceStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibContinueStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibGotoStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibLabelStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibWhileStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.AnnotatedStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.AssignStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.AssumeStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.AttributeContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.BreakStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.ChoiceStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.ContinueStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.GotoStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.HavocStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.IfStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.LabelStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.ReturnStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.SequenceStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.StatementContext;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.WhileStatementContext;

class StatementToAstConverter extends AbstractAntlrToAstConverter<SvLibStatement> {

  private final TermToAstConverter termToAstConverter;
  private final TagToAstConverter tagToAstConverter;

  private Optional<List<SvLibTagProperty>> tagAttributes = Optional.empty();

  private Optional<List<SvLibTagReference>> tagReferences = Optional.empty();

  public StatementToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    termToAstConverter = new TermToAstConverter(pScope, pFilePath);
    tagToAstConverter = new TagToAstConverter(pScope, pFilePath);
  }

  public StatementToAstConverter(SvLibScope pScope) {
    super(pScope);
    termToAstConverter = new TermToAstConverter(pScope);
    tagToAstConverter = new TagToAstConverter(pScope);
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

  private void updateTagReferences(List<AttributeContext> pAttributeContexts) {
    ImmutableList.Builder<SvLibTagReference> tagReferencesBuilder = ImmutableList.builder();
    ImmutableList.Builder<SvLibTagProperty> tagAttributeBuilder = ImmutableList.builder();
    for (AttributeContext attributeContext : pAttributeContexts) {
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
    updateTagReferences(ctx.attribute());
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

    List<SvLibSimpleDeclaration> variables =
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

    ImmutableMap.Builder<SvLibSimpleDeclaration, SvLibTerm> assignments = ImmutableMap.builder();
    for (int i = 0; i < ctx.symbol().size(); i++) {
      SvLibSimpleDeclaration leftHandSide =
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
}
