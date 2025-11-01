// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.builder;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SmtLibType;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.SmtLibTheoryDeclarations;

public class K3TermBuilder {

  public static K3RelationalTerm booleanNegation(K3RelationalTerm pTerm) {
    checkArgument(pTerm.getExpressionType().equals(K3SmtLibType.BOOL));
    return new K3SymbolApplicationRelationalTerm(
        new K3IdTerm(SmtLibTheoryDeclarations.BOOL_NEGATION, FileLocation.DUMMY),
        ImmutableList.of(pTerm),
        FileLocation.DUMMY);
  }

  public static K3RelationalTerm booleanDisjunction(List<K3RelationalTerm> pTerms) {
    checkArgument(
        FluentIterable.from(pTerms)
            .allMatch(term -> term.getExpressionType().equals(K3SmtLibType.BOOL)));

    // Some simplifications
    if (pTerms.isEmpty()) {
      // Return false for empty disjunction
      return new K3BooleanConstantTerm(false, FileLocation.DUMMY);
    } else if (pTerms.size() == 1) {
      // Return the single term for single-element disjunction
      return pTerms.getFirst();
    }

    return new K3SymbolApplicationRelationalTerm(
        new K3IdTerm(SmtLibTheoryDeclarations.boolDisjunction(pTerms.size()), FileLocation.DUMMY),
        ImmutableList.copyOf(pTerms),
        FileLocation.DUMMY);
  }

  public static K3RelationalTerm booleanConjunction(List<K3RelationalTerm> pTerms) {
    checkArgument(
        FluentIterable.from(pTerms)
            .allMatch(term -> term.getExpressionType().equals(K3SmtLibType.BOOL)));
    // Some simplifications
    if (pTerms.isEmpty()) {
      // Return false for empty disjunction
      return new K3BooleanConstantTerm(true, FileLocation.DUMMY);
    } else if (pTerms.size() == 1) {
      // Return the single term for single-element disjunction
      return pTerms.getFirst();
    }

    return new K3SymbolApplicationRelationalTerm(
        new K3IdTerm(SmtLibTheoryDeclarations.boolConjunction(pTerms.size()), FileLocation.DUMMY),
        ImmutableList.copyOf(pTerms),
        FileLocation.DUMMY);
  }
}
