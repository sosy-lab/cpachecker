/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.simpleformulas.translators.c;

import java.util.Collections;
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
      return Collections.emptySet();
    }

    @Override
    public Set<String> visit(Variable pVariable) {
      return Collections.singleton(pVariable.toString());
    }

  }

}
