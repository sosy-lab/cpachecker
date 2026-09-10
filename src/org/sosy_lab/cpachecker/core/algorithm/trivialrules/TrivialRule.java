// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * A trivial rule: an argument that settles some propositions of a specification without reasoning
 * about what the program computes.
 *
 * @param name the name of the rule, used to select it and to report which rule decided a task
 * @param argument the argument that makes the rule sound, independently of the program
 * @param decides the propositions that this rule can settle
 * @param check applies the rule to a program; returns an empty {@link Optional} if the rule
 *     abstains, which it has to do whenever its argument does not hold for the program
 */
record TrivialRule(String name, String argument, ImmutableSet<Property> decides, Check check) {

  /** The check of a rule, cf. {@link TrivialRule#check()}. */
  @FunctionalInterface
  interface Check {
    Optional<RuleVerdict> apply(ProgramFacts pFacts) throws CPAException, InterruptedException;
  }
}
