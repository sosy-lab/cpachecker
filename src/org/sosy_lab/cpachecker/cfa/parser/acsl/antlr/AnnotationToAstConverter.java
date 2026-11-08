// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssigns;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssumes;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslBehavior;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslCompletenessClause;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslEnsures;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLogicDefinitionAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslRequires;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslStatementContract;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.AcslCommentContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.AcslStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.AssignsClauseContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.AssumesClauseContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.CompletenessClauseContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.EnsuresClauseContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.ForBehaviorsContractContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.FunctionContractContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.IdContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LocationContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LogicDefContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LoopAnnotContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LoopAssignsContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LoopClauseContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LoopInvariantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.NamedBehaviorContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.RequiresClauseContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.SimpleClauseContext;

class AnnotationToAstConverter extends AbstractAntlrToAstConverter<AAcslAnnotation> {
  private final PredicateToAstConverter predicateConverter;
  private final MemoryLocationSetToAstConverter memoryLocationSetConverter;
  private final LogicDefinitionToAstConverter logicDefinitionConverter;
  private final FileLocation fileLocation;

  AnnotationToAstConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope, FileLocation pInitialFileLocation) {
    super(pCProgramScope, pAcslScope, pInitialFileLocation);
    fileLocation = pInitialFileLocation;
    predicateConverter =
        new PredicateToAstConverter(pCProgramScope, pAcslScope, pInitialFileLocation);
    memoryLocationSetConverter =
        new MemoryLocationSetToAstConverter(pCProgramScope, pAcslScope, pInitialFileLocation);
    logicDefinitionConverter = new LogicDefinitionToAstConverter(pAcslScope, pInitialFileLocation);
  }

  @Override
  public AAcslAnnotation visitAcslComment(AcslCommentContext ctx) {
    return super.visit(ctx.children.getFirst());
  }

  @Override
  public AAcslAnnotation visitAcslStatement(AcslStatementContext ctx) {
    return super.visit(ctx.children.getFirst());
  }

  @Override
  public AcslAssertion visitAssertion(AssertionContext ctx) {
    AcslPredicate predicate = predicateConverter.visit(ctx.pred());
    return new AcslAssertion(fileLocationFromContext(ctx), predicate);
  }

  @Override
  public AcslFunctionContract visitFunctionContract(FunctionContractContext ctx) {
    FluentIterable<AAcslAnnotation> clauses = visitClauses(ctx.children);
    return new AcslFunctionContract(
        fileLocationFromContext(ctx),
        clauses.filter(AcslRequires.class).toSet(),
        clauses.filter(AcslEnsures.class).toSet(),
        clauses.filter(AcslAssigns.class).toSet(),
        clauses.filter(AcslBehavior.class).toList(),
        clauses.filter(AcslCompletenessClause.class).toList());
  }

  @Override
  public AcslStatementContract visitForBehaviorsContract(ForBehaviorsContractContext ctx) {
    return new AcslStatementContract(
        fileLocationFromContext(ctx),
        FluentIterable.from(ctx.id()).transform(IdContext::getText).toList(),
        visitFunctionContract(ctx.functionContract()));
  }

  @Override
  public AcslBehavior visitNamedBehavior(NamedBehaviorContext ctx) {
    FluentIterable<AAcslAnnotation> clauses = visitClauses(ctx.behaviorBody().children);
    return new AcslBehavior(
        fileLocationFromContext(ctx),
        ctx.id().getText(),
        clauses.filter(AcslAssumes.class).toSet(),
        clauses.filter(AcslRequires.class).toSet(),
        clauses.filter(AcslEnsures.class).toSet(),
        clauses.filter(AcslAssigns.class).toSet());
  }

  @Override
  public AcslAssumes visitAssumesClause(AssumesClauseContext ctx) {
    return new AcslAssumes(fileLocationFromContext(ctx), predicateConverter.visit(ctx.pred()));
  }

  @Override
  public AcslCompletenessClause visitCompletenessClause(CompletenessClauseContext ctx) {
    return new AcslCompletenessClause(
        fileLocationFromContext(ctx),
        ctx.getChild(0).getText().equals("complete")
            ? AcslCompletenessClause.Kind.COMPLETE
            : AcslCompletenessClause.Kind.DISJOINT,
        FluentIterable.from(ctx.id()).transform(IdContext::getText).toList());
  }

  private FluentIterable<AAcslAnnotation> visitClauses(@Nullable List<ParseTree> pClauses) {
    List<AAcslAnnotation> clauses = new ArrayList<>();
    for (ParseTree clause : pClauses == null ? ImmutableList.<ParseTree>of() : pClauses) {
      clauses.add(super.visit(clause));
    }
    return FluentIterable.from(clauses);
  }

  @Override
  public AcslLoopAnnotation visitLoopAnnot(LoopAnnotContext ctx) {
    List<AAcslAnnotation> ls = new ArrayList<>();
    for (ParseTree c : ctx.children) {
      ls.add(super.visit(c));
    }
    FluentIterable<AAcslAnnotation> annotations = FluentIterable.from(ls);
    return new AcslLoopAnnotation(
        fileLocationFromContext(ctx),
        annotations
            .filter(a -> a instanceof AcslLoopInvariant)
            .transform(a -> (AcslLoopInvariant) a)
            .toSet(),
        annotations.filter(a -> a instanceof AcslAssigns).transform(a -> (AcslAssigns) a).toSet());
  }

  @Override
  public AAcslAnnotation visitLoopClause(LoopClauseContext ctx) {
    return super.visit(ctx.children.getFirst());
  }

  @Override
  public AcslLoopInvariant visitLoopInvariant(LoopInvariantContext ctx) {
    AcslPredicate predicate = predicateConverter.visit(ctx.pred());
    return new AcslLoopInvariant(fileLocationFromContext(ctx), predicate);
  }

  @Override
  public AcslEnsures visitEnsuresClause(EnsuresClauseContext ctx) {
    AcslPredicate predicate = predicateConverter.visit(ctx.pred());
    return new AcslEnsures(fileLocationFromContext(ctx), predicate);
  }

  @Override
  public AcslRequires visitRequiresClause(RequiresClauseContext ctx) {
    AcslPredicate predicate = predicateConverter.visit(ctx.pred());
    return new AcslRequires(fileLocationFromContext(ctx), predicate);
  }

  @Override
  public AcslAssigns visitAssignsClause(AssignsClauseContext ctx) {
    ImmutableSet.Builder<AcslMemoryLocationSet> locationSetBuilder = ImmutableSet.builder();
    for (LocationContext loc : ctx.locations().location()) {
      AcslMemoryLocationSet memLocation = memoryLocationSetConverter.visit(loc.tset());
      locationSetBuilder.add(memLocation);
    }
    return new AcslAssigns(fileLocationFromContext(ctx), locationSetBuilder.build());
  }

  @Override
  public AAcslAnnotation visitSimpleClause(SimpleClauseContext ctx) {
    return super.visit(ctx.children.getFirst());
  }

  @Override
  public AAcslAnnotation visitLogicDef(LogicDefContext ctx) {
    AcslLogicDefinition definition = logicDefinitionConverter.visit(ctx);
    return new AcslLogicDefinitionAnnotation(fileLocationFromContext(ctx), definition);
  }

  @Override
  public AAcslAnnotation visitLoopAssigns(LoopAssignsContext ctx) {
    ImmutableSet.Builder<AcslMemoryLocationSet> locationSetBuilder = ImmutableSet.builder();
    for (LocationContext loc : ctx.locations().location()) {
      AcslMemoryLocationSet memLocation = memoryLocationSetConverter.visit(loc.tset());
      locationSetBuilder.add(memLocation);
    }
    return new AcslAssigns(fileLocationFromContext(ctx), locationSetBuilder.build());
  }

  @Override
  protected AAcslAnnotation defaultResult() {
    throw new AcslParseRuntimeException(
        "Parsing of the Annotation at : "
            + fileLocation
            + " failed. Only 'assert', 'ensures', 'assigns' and 'loop invariant' are supported.");
  }
}
