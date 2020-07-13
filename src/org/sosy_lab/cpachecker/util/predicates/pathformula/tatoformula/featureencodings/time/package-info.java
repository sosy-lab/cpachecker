// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Classes in this package implement smt encodings of time (i.e. clocks and time passage) for timed
 * automata. A common interface provides factory methods for different time-related predicates
 * needed to construct the encoding of an automaton. The exact predicates differ depending on the
 * encoding provided by the implementing classes in this package.
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.time;
