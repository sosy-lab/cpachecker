// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Statement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AnnotatedStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AssumeStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AttributeContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.SequenceStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.StatementContext;

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
        default ->
            throw new IllegalArgumentException(
                "Unexpected type of tag property: " + tagProperty.getClass().getSimpleName());
      }
    }
    tagReferences = Optional.of(tagReferencesBuilder.build());
    tagAttributes = Optional.of(tagAttributeBuilder.build());
  }

  @Override
  public K3Statement visitSequenceStatement(SequenceStatementContext ctx) {
    // Could be simplified by using a FluentIterable, but this is easier
    // for debugging.
    ImmutableList.Builder<K3Statement> statementsBuilder = ImmutableList.builder();
    for (StatementContext statementContext : ctx.statement()) {
      K3Statement statement = visit(statementContext);
      statementsBuilder.add(statement);
    }
    List<K3Statement> statements = statementsBuilder.build();
    FileLocation location = fileLocationFromContext(ctx);
    return new K3SequenceStatement(statements, location, getTagAttributes(), getTagReferences());
  }

  @Override
  public K3Statement visitAnnotatedStatement(AnnotatedStatementContext ctx) {
    updateTagReferences(ctx.attribute());
    return visit(ctx.statement());
  }

  @Override
  public K3Statement visitAssumeStatement(AssumeStatementContext ctx) {
    FileLocation location = fileLocationFromContext(ctx);
    K3Term term = termToAstConverter.visit(ctx.term());
    return new K3AssumeStatement(location, term, getTagAttributes(), getTagReferences());
  }
}
