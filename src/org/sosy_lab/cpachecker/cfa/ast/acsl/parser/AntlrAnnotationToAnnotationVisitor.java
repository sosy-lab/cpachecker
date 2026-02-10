// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslEnsures;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslRequires;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AcslStatementContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.Ensures_clauseContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.Loop_invariantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.Requires_clauseContext;

public class AntlrAnnotationToAnnotationVisitor
    extends AntlrToInternalAbstractConverter<AAcslAnnotation> {
  private final AntlrPredicateToPredicateConverter antlrPredicateToPredicateConverter;
  private final FileLocation fileLocation;

  protected AntlrAnnotationToAnnotationVisitor(
      CProgramScope pCProgramScope, AcslScope pAcslScope, FileLocation pFileLocation) {
    super(pCProgramScope, pAcslScope);
    fileLocation = pFileLocation;
    antlrPredicateToPredicateConverter =
        new AntlrPredicateToPredicateConverter(pCProgramScope, pAcslScope);
  }

  @Override
  public AAcslAnnotation visitAcslStatement(AcslStatementContext ctx) {
    return super.visitAcslStatement(ctx);
  }

  @Override
  public AcslAssertion visitAssertion(AssertionContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslAssertion(fileLocation, predicate);
  }

  @Override
  public AcslLoopInvariant visitLoop_invariant(Loop_invariantContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslLoopInvariant(fileLocation, predicate);
  }

  @Override
  public AcslEnsures visitEnsures_clause(Ensures_clauseContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslEnsures(fileLocation, predicate);
  }

  @Override
  public AcslRequires visitRequires_clause(Requires_clauseContext ctx) {
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(ctx.pred());
    return new AcslRequires(fileLocation, predicate);
  }
}
