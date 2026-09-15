// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;

/**
 * This interface represents a term in the SV-LIB abstract syntax tree (AST). A term can be a symbol
 * application, a constant, or an identifier. This corresponds to S-Expressions in SMT-LIB, which
 * SV-LIB builds upon. Note that this should be strictly sepparated from {@link SvLibExpression},
 * since the latter also includes some internal information of CPAchecker which we do not want to
 * arbitrarily combine inside of term applications.
 */
public sealed interface SvLibTerm
    extends SvLibAstNode, SvLibRelationalTerm, SvLibRightHandSide, SvLibExpression
    permits SvLibSymbolApplicationTerm, SvLibConstantTerm, SvLibIdTerm {

  static SvLibRelationalTerm booleanNegation(SvLibTerm pTerm) {
    checkArgument(pTerm.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL));
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(SmtLibTheoryDeclarations.BOOL_NEGATION, FileLocation.DUMMY),
        ImmutableList.of(pTerm),
        FileLocation.DUMMY);
  }

  static SvLibTerm booleanDisjunction(List<SvLibTerm> pTerms) {
    checkArgument(
        FluentIterable.from(pTerms)
            .allMatch(term -> term.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL)));

    // Some simplifications
    if (pTerms.isEmpty()) {
      // Return false for empty disjunction
      return new SvLibBooleanConstantTerm(false, FileLocation.DUMMY);
    } else if (pTerms.size() == 1) {
      // Return the single term for single-element disjunction
      return pTerms.getFirst();
    }

    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(
            SmtLibTheoryDeclarations.boolDisjunction(pTerms.size()), FileLocation.DUMMY),
        ImmutableList.copyOf(pTerms),
        FileLocation.DUMMY);
  }

  static SvLibTerm booleanConjunction(List<SvLibTerm> pTerms) {
    checkArgument(
        FluentIterable.from(pTerms)
            .allMatch(term -> term.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL)));
    // Some simplifications
    if (pTerms.isEmpty()) {
      // Return false for empty disjunction
      return new SvLibBooleanConstantTerm(true, FileLocation.DUMMY);
    } else if (pTerms.size() == 1) {
      // Return the single term for single-element disjunction
      return pTerms.getFirst();
    }

    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(
            SmtLibTheoryDeclarations.boolConjunction(pTerms.size()), FileLocation.DUMMY),
        ImmutableList.copyOf(pTerms),
        FileLocation.DUMMY);
  }

  static SvLibTerm implication(SvLibTerm pAssumption, SvLibTerm pConclusion) {
    checkArgument(
        pAssumption.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL)
            && pConclusion.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL));
    return new SvLibSymbolApplicationTerm(
        new SvLibIdTerm(SmtLibTheoryDeclarations.boolImplication(2), FileLocation.DUMMY),
        ImmutableList.of(pAssumption, pConclusion),
        FileLocation.DUMMY);
  }
}
