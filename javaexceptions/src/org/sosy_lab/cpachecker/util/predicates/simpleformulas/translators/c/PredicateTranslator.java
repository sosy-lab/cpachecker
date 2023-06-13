// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.simpleformulas.translators.c;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Constant;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Predicate;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.TermVisitor;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Variable;

public class PredicateTranslator {

  private static Map<Predicate, String> mCache = new HashMap<>();

  public static String translate(Predicate pPredicate) {
    if (mCache.containsKey(pPredicate)) {
      return mCache.get(pPredicate);
    }

    Set<String> lVariables = new HashSet<>();

    Visitor lVisitor = new Visitor();
    lVariables.addAll(pPredicate.getLeftTerm().accept(lVisitor));
    lVariables.addAll(pPredicate.getRightTerm().accept(lVisitor));

    StringBuilder lResult = new StringBuilder();

    lResult.append("void predicate(");

    boolean isFirst = true;

    for (String lVariable : lVariables) {
      if (isFirst) {
        isFirst = false;
      } else {
        lResult.append(", ");
      }

      lResult.append("int ");
      lResult.append(lVariable);
    }

    lResult.append(") { (");
    lResult.append(pPredicate.toString());
    lResult.append("); }");

    mCache.put(pPredicate, lResult.toString());

    return lResult.toString();
  }

  private static class Visitor implements TermVisitor<Set<String>> {

    @Override
    public Set<String> visit(Constant pConstant) {
      return ImmutableSet.of();
    }

    @Override
    public Set<String> visit(Variable pVariable) {
      return ImmutableSet.of(pVariable.toString());
    }
  }
}
