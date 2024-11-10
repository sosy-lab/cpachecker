// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concolic;

public record NondetLocation(String fileName, int lineNumber, int columnNumberStart, int columnNumberEnd) {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NondetLocation that = (NondetLocation) o;
    return fileName.equals(that.fileName)
        && lineNumber == that.lineNumber
        && columnNumberStart == that.columnNumberStart
        && columnNumberEnd == that.columnNumberEnd;
  }
}
