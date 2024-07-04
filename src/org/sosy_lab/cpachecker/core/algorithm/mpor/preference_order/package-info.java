// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Algorithms and data structures for the (positional) preference orders in the parallel C program,
 * i.e. if one (or more) CFAEdges a in A must be executed before CFAEdge b from another thread. E.g.
 * if one thread calls pthread_join(other_thread) = b, then other_thread needs to execute all
 * CFAEdges a in A until it stops.
 */
package org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order;
