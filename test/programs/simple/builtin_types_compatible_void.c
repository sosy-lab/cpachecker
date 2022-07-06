// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  if (__builtin_types_compatible_p(void, int)) {
    goto ERROR;
  }
  if (__builtin_types_compatible_p(int, void)) {
    goto ERROR;
  }
  return 0;
ERROR:
  return 1;
}
