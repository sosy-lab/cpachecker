// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Shifting 1 into the sign bit of an int leaves the range of the type.
int main(void) {
  int x = 1 << 31;
  return x;
}
