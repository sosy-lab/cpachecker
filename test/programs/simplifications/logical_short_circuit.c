// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

static int unreachable_call(void) {
ERROR:
  goto ERROR;
}

int main() {
  int t = 1;
  int f = 0;

  if (!(t || f)) goto ERROR;
  if (t && f) goto ERROR;
  if ((42 == 42) != 1) goto ERROR;
  if ((42 != 42) != 0) goto ERROR;
  if ((t || unreachable_call()) != 1) goto ERROR;
  if ((f && unreachable_call()) != 0) goto ERROR;

  return 0;
ERROR:
  return 1;
}
