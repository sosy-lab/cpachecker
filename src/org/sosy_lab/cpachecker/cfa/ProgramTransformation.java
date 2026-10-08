// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

/**
 * A transformation of a program into another program that is analyzed in its stead. Implementations
 * carry whatever a consumer needs to map a result for the analyzed program back to the original
 * one, so this interface only provides what every transformation has.
 */
public interface ProgramTransformation {

  /** Returns the CFA of the program that this transformation was applied to. */
  CFA originalCfa();
}
