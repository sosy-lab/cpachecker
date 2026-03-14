// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned char __VERIFIER_nondet_uchar(void);
extern signed char __VERIFIER_nondet_schar(void);

int main() {
  unsigned char uc = __VERIFIER_nondet_uchar();
  signed char sc = __VERIFIER_nondet_schar();

  if (uc < 0) goto ERROR;
  if (uc > 255) goto ERROR;
  if (sc < -128) goto ERROR;
  if (sc > 127) goto ERROR;

  return 0;
ERROR:
  return 1;
}
