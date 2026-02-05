// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslEnsures;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslRequires;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.EnsuresClauseContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LoopInvariantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.RequiresClauseContext;

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
  public AcslAssertion visitAssertion(AssertionContext ctx) {
    ParseTree predTree = ctx.getChild(1);
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(predTree);
    AcslAssertion assertion = new AcslAssertion(fileLocation, predicate);

    return assertion;
  }

  @Override
  public AcslLoopInvariant visitLoopInvariant(LoopInvariantContext ctx) {
    ParseTree predTree = ctx.getChild(2);
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(predTree);
    AcslLoopInvariant loopInvariant = new AcslLoopInvariant(fileLocation, predicate);

    return loopInvariant;
  }

  @Override
  public AcslEnsures visitEnsuresClause(EnsuresClauseContext ctx) {
    ParseTree predTree = ctx.getChild(1);
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(predTree);
    AcslEnsures ensures = new AcslEnsures(fileLocation, predicate);
    return ensures;
  }

  @Override
  public AcslRequires visitRequiresClause(RequiresClauseContext ctx) {
    ParseTree predTree = ctx.getChild(1);
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(predTree);
    AcslRequires requires = new AcslRequires(fileLocation, predicate);
    return requires;
  }
}
