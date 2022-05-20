// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

/**
 * Enum that defines the analysis direction. Using a boolean flag for this purpose is considered as
 * a bad practice, since it reduces the readability of code, and increases the risk of bugs. (see
 * http://blog.codinghorror.com/avoiding-booleans/)
 */
public enum AnalysisDirection {
  FORWARD,
  BACKWARD
}
