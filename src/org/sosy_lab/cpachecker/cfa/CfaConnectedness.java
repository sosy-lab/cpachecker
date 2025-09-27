// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

/**
 * Indicates whether a program is represented by a single supergraph CFA or by multiple unconnected
 * CFAs - one for each function.
 */
public enum CfaConnectedness {

  /**
   * There is a CFA for each function. Functions are not connected by super-edges (i.e., function
   * call and return edges). There are no function summary edges and function calls are represented
   * by statement edges.
   */
  UNCONNECTED_FUNCTIONS,

  /**
   * There is only a single CFA for the whole program. Functions are connected by super-edges (i.e.,
   * function call and return edges). In a supergraph CFA, function call statement edges are
   * replaced by function call/return/summary edges where necessary.
   */
  SUPERGRAPH
}
