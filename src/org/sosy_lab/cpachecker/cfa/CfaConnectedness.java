// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

/**
 * Indicates whether functions are connected by super-edges (i.e., function call and return edges).
 */
public enum CfaConnectedness {

  /** Functions are not connected by super-edges. */
  UNCONNECTED_FUNCTIONS,

  /** Functions are connected by super-edges. */
  SUPERGRAPH
}
