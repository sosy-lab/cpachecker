// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern _Bool __VERIFIER_nondet_bool(void);

int main() {
  _Bool b = __VERIFIER_nondet_bool();

  if (b > 1) goto ERROR;
  if (b < 0) goto ERROR;
  if (!(b == 0 || b == 1)) goto ERROR;

  return 0;
ERROR:
  return 1;
}
