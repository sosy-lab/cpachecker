// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Different strategies to compute interpolants for a sequence of path formulae.
 * Some analyses depend on special strategies, e.g.
 * predicate analysis needs an inductive sequence of interpolation and
 * the analysis of recursive procedures with BAM and predicate analysis uses tree interpolation.
 * There are also strategies that directly ask the SMT solver for a complete solution
 * instead of querying every interpolant separately.
 */
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;
