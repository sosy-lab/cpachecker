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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules;

import static org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternBuilder.*;

import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPattern;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternBuilder;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternSelection;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternSelectionElement;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public final class GenericPatterns {

  public static SmtAstPattern f_of_x (final String pBindFunctionTo, final String pBindArgTo) {
    return matchAnyBind(pBindFunctionTo,
        or(
          match("select",
              matchAnyWithAnyArgs(),
              matchInSubtree(
                  matchAnyWithAnyArgsBind(pBindArgTo))),

          matchAny(
              matchAny(
                  matchInSubtree(
                      matchAnyWithAnyArgsBind(pBindArgTo))))
            ));
  }

  public static SmtAstPatternSelection substraction(
      final SmtAstPatternSelectionElement pOp1Matcher,
      final SmtAstPatternSelectionElement pOp2Matcher) {

    return
        or (
            match("-",
                and(
                    pOp1Matcher,
                    pOp2Matcher)),
            match("+",
                and (
                    pOp1Matcher,
                    match("-",
                        and(pOp2Matcher)))),
            match("+",
                and (
                    pOp1Matcher,
                    match("-",
                        and(
                            matchNullary("0"),
                            pOp2Matcher)))),
            match("+",
                and (
                    pOp1Matcher,
                    match("*",
                        and(
                            or(
                                matchNullary("-1"),
                                matchNullary("(- 1)"),
                                match("-",
                                    matchNullary("1")),
                                match("-",
                                    matchNullary("0"),
                                    matchNullary("1")),
                                SmtAstPatternBuilder.matchNegativeNumber()
                            ),
                            pOp2Matcher))))
          );
  }

  public static SmtAstPatternSelection substraction(
      final String pBindSubstrOp1Var,
      final String pBindSubstrOp2Var) {

    return substraction(
        matchAnyWithAnyArgsBind(pBindSubstrOp1Var),
        matchAnyWithAnyArgsBind(pBindSubstrOp1Var));
  }

  public static SmtAstPatternSelection f_of_x_variable (final String pBindFunctionTo, final String pBindArgTo) {
    return f_of_x_matcher(pBindFunctionTo, and(matchNumeralVariableBind(pBindArgTo)), matchAnyWithAnyArgs());
  }

  public static SmtAstPatternSelection f_of_x_variable_subtree (final String pBindFunctionTo, final String pBindArgTo) {
    return f_of_x_matcher(pBindFunctionTo, matchInSubtreeBoundedDepth(10, matchNumeralVariableBind(pBindArgTo)), matchAnyWithAnyArgs());
  }

  public static SmtAstPatternSelection f_of_x_expression (final String pBindFunctionTo, final String pBindArgTo) {
    return f_of_x_matcher(pBindFunctionTo, and(matchAnyWithAnyArgsBind(pBindArgTo)), matchAnyWithAnyArgs());
  }

  public static SmtAstPatternSelection array_at_index_subtree_matcher (final String pBindFunctionTo, final String pBindArgTo) {
    return array_at_index_matcher(pBindFunctionTo, matchInSubtreeBoundedDepth(10, matchNumeralExpressionBind(pBindArgTo)));
  }

  public static SmtAstPatternSelection array_at_index_matcher (final String pBindFunctionTo, final String pBindArgTo) {
    return array_at_index_matcher(pBindFunctionTo, and(matchNumeralExpressionBind(pBindArgTo)));
  }

  public static SmtAstPatternSelection array_at_index_matcher (final String pBindFunctionTo, final SmtAstPatternSelection pIndexMatcher) {
    return array_at_index_matcher(pBindFunctionTo, pIndexMatcher, matchAnyWithAnyArgs());
  }

  public static SmtAstPatternSelection array_at_index_matcher (
      final String pBindFunctionTo,
      final SmtAstPatternSelection pIndexMatcher,
      final SmtAstPatternSelectionElement pSecondOpLeaveMatcher) {

    return or(
        matchBind("not", pBindFunctionTo,
            match("not",
                matchAnyWithArgs(
                    and(
                      match("select",
                          and(
                              matchAnyWithAnyArgs(),
                              pIndexMatcher)),
                      pSecondOpLeaveMatcher)))),

        matchBind("not", pBindFunctionTo,
            matchAnyWithArgs(
                and(
                    match("select",
                        and(
                            matchAnyWithAnyArgs(),
                            pIndexMatcher)),
                     pSecondOpLeaveMatcher))),

        matchAnyBind(pBindFunctionTo,
            and(
              match("select",
                  and(
                      matchAnyWithAnyArgs(),
                      pIndexMatcher)),
              pSecondOpLeaveMatcher))
        );
  }

  public static SmtAstPatternSelection f_of_x_matcher (
      final String pBindFunctionTo,
      final SmtAstPatternSelection pFirstOpLeaveMatcher,
      final SmtAstPatternSelectionElement pSecondOpLeaveMatcher) {

    return or(
        matchBind("not", pBindFunctionTo,
            match("not",
                matchAnyWithArgs(
                  and(
                      match("select",
                          and(
                              matchAnyWithAnyArgs(),
                              pFirstOpLeaveMatcher)),
                        pSecondOpLeaveMatcher)))),

        matchBind("not", pBindFunctionTo,
            matchAnyWithArgs(
              and(
                  match("select",
                      and(
                          matchAnyWithAnyArgs(),
                          pFirstOpLeaveMatcher)),
                  pSecondOpLeaveMatcher))),

        matchAnyBind(pBindFunctionTo,
            and(
                match("select",
                    and(
                        matchAnyWithAnyArgs(),
                        pFirstOpLeaveMatcher)),
                pSecondOpLeaveMatcher)),

        matchBind("not", pBindFunctionTo,
            matchAnyWithArgs(
                and(
                    pFirstOpLeaveMatcher,
                    pSecondOpLeaveMatcher))),

        matchAnyBind(pBindFunctionTo,
            and(
                pFirstOpLeaveMatcher,
                pSecondOpLeaveMatcher))
        );
  }

}
