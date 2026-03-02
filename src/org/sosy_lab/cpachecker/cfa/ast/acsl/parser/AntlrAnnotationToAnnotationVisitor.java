// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssigns;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslEnsures;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslRequires;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AntlrToInternalNotImplementedException;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AcslStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssignsClauseContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.EnsuresClauseContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.FunctionContractContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LocationContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LoopAnnotContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LoopClauseContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LoopInvariantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.RequiresClauseContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.SimpleClauseContext;

public class AntlrAnnotationToAnnotationVisitor
    extends AntlrToInternalAbstractConverter<AAcslAnnotation> {
  private final AntlrPredicateToPredicateConverter antlrPredicateToPredicateConverter;
  private final AntlrTsetToMemorySetConverter antlrTsetToMemorySetConverter;
  private final FileLocation fileLocation;

  protected AntlrAnnotationToAnnotationVisitor(
      CProgramScope pCProgramScope, AcslScope pAcslScope, FileLocation pFileLocation) {
    super(pCProgramScope, pAcslScope);
    fileLocation = pFileLocation;
    antlrPredicateToPredicateConverter =
        new AntlrPredicateToPredicateConverter(pCProgramScope, pAcslScope);
    antlrTsetToMemorySetConverter = new AntlrTsetToMemorySetConverter(pCProgramScope, pAcslScope);
  }

  @Override
  public AAcslAnnotation visitAcslStatement(AcslStatementContext ctx) {
    return super.visit(ctx.children.getFirst());
  }

  @Override
  public AcslAssertion visitAssertion(AssertionContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslAssertion(fileLocation, predicate);
  }

  @Override
  public AcslFunctionContract visitFunctionContract(FunctionContractContext ctx) {
    List<AAcslAnnotation> as = new ArrayList<>();
    for (ParseTree c : ctx.children) {
      as.add(super.visit(c));
    }

    FluentIterable<AAcslAnnotation> annotations = FluentIterable.from(as);
    return new AcslFunctionContract(
        fileLocation,
        annotations.filter(a -> a instanceof AcslEnsures).transform(a -> (AcslEnsures) a).toSet(),
        annotations.filter(a -> a instanceof AcslAssigns).transform(a -> (AcslAssigns) a).toSet(),
        annotations
            .filter(a -> a instanceof AcslRequires)
            .transform(a -> (AcslRequires) a)
            .toSet());
  }

  @Override
  public AcslLoopAnnotation visitLoopAnnot(LoopAnnotContext ctx) {
    List<AAcslAnnotation> ls = new ArrayList<>();
    for (ParseTree c : ctx.children) {
      ls.add(super.visit(c));
    }
    FluentIterable<AAcslAnnotation> annotations = FluentIterable.from(ls);
    return new AcslLoopAnnotation(
        fileLocation,
        annotations
            .filter(a -> a instanceof AcslLoopInvariant)
            .transform(a -> (AcslLoopInvariant) a)
            .toSet());
  }

  @Override
  public AAcslAnnotation visitLoopClause(LoopClauseContext ctx) {
    return super.visit(ctx.children.getFirst());
  }

  @Override
  public AcslLoopInvariant visitLoopInvariant(LoopInvariantContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslLoopInvariant(fileLocation, predicate);
  }

  @Override
  public AcslEnsures visitEnsuresClause(EnsuresClauseContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslEnsures(fileLocation, predicate);
  }

  @Override
  public AcslRequires visitRequiresClause(RequiresClauseContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslRequires(fileLocation, predicate);
  }

  @Override
  public AcslAssigns visitAssignsClause(AssignsClauseContext ctx) {
    ImmutableSet.Builder<AcslMemoryLocationSet> locationSetBuilder = ImmutableSet.builder();
    for (LocationContext loc : ctx.locations().location()) {
      AcslMemoryLocationSet memLocation = antlrTsetToMemorySetConverter.visit(loc);
      locationSetBuilder.add(memLocation);
    }
    ImmutableSet<AcslMemoryLocationSet> locationsSet = locationSetBuilder.build();
    return new AcslAssigns(fileLocation, locationsSet);
  }

  @Override
  public AAcslAnnotation visitSimpleClause(SimpleClauseContext ctx) {
    return super.visit(ctx.children.getFirst());
  }

  @Override
  protected AAcslAnnotation defaultResult() {
    throw new AntlrToInternalNotImplementedException(
        "Parsing of the Annotation at : "
            + fileLocation
            + " failed. Only 'assert', 'ensures', 'assigns' and 'loop invariant' are supported.");
  }
}
