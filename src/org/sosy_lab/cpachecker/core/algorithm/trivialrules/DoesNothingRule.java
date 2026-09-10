// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;

/** The rule for a program that does nothing, which satisfies every specification. */
final class DoesNothingRule {

  private DoesNothingRule() {}

  /** Every proposition that CPAchecker knows: a program that does nothing satisfies all of them. */
  private static final ImmutableSet<Property> PROPOSITIONS =
      ImmutableSet.copyOf(
          Arrays.stream(CommonVerificationProperty.values())
              .filter(Property::isVerification)
              .toList());

  static ImmutableList<TrivialRule> rules() {
    return ImmutableList.of(
        new TrivialRule(
            "program-does-nothing",
            "A program whose executions perform no operation at all does nothing, and a program"
                + " that does nothing satisfies every specification.",
            PROPOSITIONS,
            DoesNothingRule::checkProgramDoesNothing));
  }

  private static Optional<RuleVerdict> checkProgramDoesNothing(ProgramFacts pFacts) {
    for (CFAEdge edge : pFacts.reachableEdges()) {
      if (edge instanceof BlankEdge) {
        continue;
      }
      if (edge instanceof AReturnStatementEdge returnEdge && returnsNothingOrLiteral(returnEdge)) {
        continue;
      }
      return Optional.empty();
    }
    return RuleVerdict.proven(
        "every execution of the program reaches the end of the entry function without performing"
            + " an operation");
  }

  private static boolean returnsNothingOrLiteral(AReturnStatementEdge pEdge) {
    return pEdge.getExpression().map(value -> value instanceof CLiteralExpression).orElse(true);
  }
}
