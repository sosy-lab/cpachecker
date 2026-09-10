// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Trivial rules: checks that settle a specification without reasoning about what the program
 * computes. "A program without loops and without recursion terminates" is such a rule, "a program
 * that never allocates memory cannot leak memory" is another one. Every rule may abstain, and the
 * analysis answers UNKNOWN if no rule decides the task, so a rule never guesses.
 *
 * @see org.sosy_lab.cpachecker.core.algorithm.trivialrules.TrivialRulesAlgorithm
 */
package org.sosy_lab.cpachecker.core.algorithm.trivialrules;
