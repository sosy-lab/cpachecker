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
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BreakStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ContinueStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GotoStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3HavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IfStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3LabelStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Statement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3WhileStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AnnotatedStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AssignStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AssumeStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AttributeContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.BreakStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ContinueStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.GotoStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.HavocStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.IfStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.LabelStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ReturnStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SequenceStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.StatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.WhileStatementContext;

class StatementToAstConverter extends AbstractAntlrToAstConverter<K3Statement> {

  private final TermToAstConverter termToAstConverter;
  private final TagToAstConverter tagToAstConverter;

  private Optional<List<K3TagProperty>> tagAttributes = Optional.empty();

  private Optional<List<K3TagReference>> tagReferences = Optional.empty();

  public StatementToAstConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    termToAstConverter = new TermToAstConverter(pScope, pFilePath);
    tagToAstConverter = new TagToAstConverter(pScope, pFilePath);
  }

  public StatementToAstConverter(K3Scope pScope) {
    super(pScope);
    termToAstConverter = new TermToAstConverter(pScope);
    tagToAstConverter = new TagToAstConverter(pScope);
  }

  private List<K3TagProperty> getTagAttributes() {
    List<K3TagProperty> attributes = tagAttributes.orElse(ImmutableList.of());
    tagAttributes = Optional.empty();
    return attributes;
  }

  private List<K3TagReference> getTagReferences() {
    List<K3TagReference> references = tagReferences.orElse(ImmutableList.of());
    tagReferences = Optional.empty();
    return references;
  }

  private void updateTagReferences(List<AttributeContext> pAttributeContexts) {
    ImmutableList.Builder<K3TagReference> tagReferencesBuilder = ImmutableList.builder();
    ImmutableList.Builder<K3TagProperty> tagAttributeBuilder = ImmutableList.builder();
    for (AttributeContext attributeContext : pAttributeContexts) {
      K3TagAttribute tagProperty = tagToAstConverter.visit(attributeContext);
      switch (tagProperty) {
        case K3TagReference tagReference -> tagReferencesBuilder.add(tagReference);
        case K3TagProperty tagAttribute -> tagAttributeBuilder.add(tagAttribute);
      }
    }
    tagReferences = Optional.of(tagReferencesBuilder.build());
    tagAttributes = Optional.of(tagAttributeBuilder.build());
  }

  @Override
  public K3Statement visitSequenceStatement(SequenceStatementContext ctx) {
    // Could be simplified by using a FluentIterable, but this is easier
    // for debugging.
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();
    ImmutableList.Builder<K3Statement> statementsBuilder = ImmutableList.builder();
    for (StatementContext statementContext : ctx.statement()) {
      K3Statement statement = visit(statementContext);
      statementsBuilder.add(statement);
    }
    List<K3Statement> statements = statementsBuilder.build();
    FileLocation location = fileLocationFromContext(ctx);
    return new K3SequenceStatement(statements, location, properties, references);
  }

  @Override
  public K3Statement visitAnnotatedStatement(AnnotatedStatementContext ctx) {
    updateTagReferences(ctx.attribute());
    return visit(ctx.statement());
  }

  @Override
  public K3Statement visitAssumeStatement(AssumeStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();
    FileLocation location = fileLocationFromContext(ctx);
    K3Term term = termToAstConverter.visit(ctx.term());
    return new K3AssumeStatement(location, term, properties, references);
  }

  @Override
  public K3Statement visitReturnStatement(ReturnStatementContext ctx) {
    return new K3ReturnStatement(
        fileLocationFromContext(ctx), getTagAttributes(), getTagReferences());
  }

  @Override
  public K3Statement visitIfStatement(IfStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();

    K3Term controllingExpression = termToAstConverter.visit(ctx.term());
    K3Statement thenStatement = visit(ctx.statement(0));
    if (ctx.statement().size() == 2) {
      K3Statement elseStatement = visit(ctx.statement(1));
      return new K3IfStatement(
          fileLocationFromContext(ctx),
          properties,
          references,
          controllingExpression,
          thenStatement,
          elseStatement);
    } else {
      return new K3IfStatement(
          fileLocationFromContext(ctx),
          properties,
          references,
          controllingExpression,
          thenStatement);
    }
  }

  @Override
  public K3Statement visitWhileStatement(WhileStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();

    K3Term controllingExpression = termToAstConverter.visit(ctx.term());
    K3Statement body = visit(ctx.statement());
    return new K3WhileStatement(
        controllingExpression, body, properties, references, fileLocationFromContext(ctx));
  }

  @Override
  public K3Statement visitBreakStatement(BreakStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();
    return new K3BreakStatement(fileLocationFromContext(ctx), properties, references);
  }

  @Override
  public K3Statement visitContinueStatement(ContinueStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();
    return new K3ContinueStatement(fileLocationFromContext(ctx), properties, references);
  }

  @Override
  public K3Statement visitHavocStatement(HavocStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();

    List<K3SimpleDeclaration> variables =
        FluentIterable.from(ctx.symbol())
            .transform(x -> scope.getVariable(Objects.requireNonNull(x).getText()))
            .toList();
    FileLocation location = fileLocationFromContext(ctx);
    return new K3HavocStatement(location, properties, references, variables);
  }

  @Override
  public K3Statement visitGotoStatement(GotoStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();

    String label = ctx.symbol().getText();
    FileLocation location = fileLocationFromContext(ctx);
    return new K3GotoStatement(location, properties, references, label);
  }

  @Override
  public K3Statement visitLabelStatement(LabelStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();

    String label = ctx.symbol().getText();
    FileLocation location = fileLocationFromContext(ctx);
    return new K3LabelStatement(location, properties, references, label);
  }

  @Override
  public K3Statement visitAssignStatement(AssignStatementContext ctx) {
    List<K3TagProperty> properties = getTagAttributes();
    List<K3TagReference> references = getTagReferences();

    ImmutableMap.Builder<K3SimpleDeclaration, K3Term> assignments = ImmutableMap.builder();
    for (int i = 0; i < ctx.symbol().size(); i++) {
      K3SimpleDeclaration leftHandSide =
          scope.getVariable(Objects.requireNonNull(ctx.symbol(i)).getText());
      K3Term rightHandSide = termToAstConverter.visit(Objects.requireNonNull(ctx.term(i)));
      assignments.put(leftHandSide, rightHandSide);
    }
    FileLocation location = fileLocationFromContext(ctx);
    return new K3AssignmentStatement(assignments.build(), location, properties, references);
  }
}
