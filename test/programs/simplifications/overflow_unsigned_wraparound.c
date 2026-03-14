// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  unsigned int max = 0U - 1U;
  unsigned int zero = max + 1U;

  if (zero != 0U) goto ERROR;
  if ((unsigned int)(0U - 1U) != max) goto ERROR;

  return 0;
ERROR:
  return 1;
}
