// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.builder;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationRelationalTerm;

public class SvLibTermBuilder {

  public static SvLibFinalRelationalTerm booleanNegation(SvLibFinalRelationalTerm pTerm) {
    checkArgument(pTerm.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL));
    return new SvLibSymbolApplicationRelationalTerm(
        new SvLibIdTerm(SmtLibTheoryDeclarations.BOOL_NEGATION, FileLocation.DUMMY),
        ImmutableList.of(pTerm),
        FileLocation.DUMMY);
  }

  public static SvLibFinalRelationalTerm booleanDisjunction(List<SvLibFinalRelationalTerm> pTerms) {
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

    return new SvLibSymbolApplicationRelationalTerm(
        new SvLibIdTerm(
            SmtLibTheoryDeclarations.boolDisjunction(pTerms.size()), FileLocation.DUMMY),
        ImmutableList.copyOf(pTerms),
        FileLocation.DUMMY);
  }

  public static SvLibFinalRelationalTerm booleanConjunction(List<SvLibFinalRelationalTerm> pTerms) {
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

    return new SvLibSymbolApplicationRelationalTerm(
        new SvLibIdTerm(
            SmtLibTheoryDeclarations.boolConjunction(pTerms.size()), FileLocation.DUMMY),
        ImmutableList.copyOf(pTerms),
        FileLocation.DUMMY);
  }

  public static SvLibFinalRelationalTerm implication(
      SvLibFinalRelationalTerm pAssumption, SvLibFinalRelationalTerm pConclusion) {
    checkArgument(
        pAssumption.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL)
            && pConclusion.getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL));
    return new SvLibSymbolApplicationRelationalTerm(
        new SvLibIdTerm(SmtLibTheoryDeclarations.boolImplication(2), FileLocation.DUMMY),
        ImmutableList.of(pAssumption, pConclusion),
        FileLocation.DUMMY);
  }
}
