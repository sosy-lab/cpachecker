// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

static int counter = 0;

static int side_effect(void) {
  counter++;
  return 5;
}

int main() {
  int x = 0;

  x = 0 * side_effect();
  if (x != 0) goto ERROR;
  if (counter != 1) goto ERROR;

  x = 1 + side_effect() - 1;
  if (x != 5) goto ERROR;
  if (counter != 2) goto ERROR;

  x = (side_effect(), 0);
  if (x != 0) goto ERROR;
  if (counter != 3) goto ERROR;

  return 0;
ERROR:
  return 1;
}
