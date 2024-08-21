// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Contains classes related to dominator tree and dominance frontier computation and representation.
 *
 * <p>Dominance for graphs is a concept where a node {@code D} dominates a node {@code N} if every
 * path from the start node of a graph to {@code N} must go through {@code D}.
 */
package org.sosy_lab.cpachecker.util.graph.dominance;
