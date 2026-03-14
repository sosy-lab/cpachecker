// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  if (sizeof(_Bool) != 1) goto ERROR;
  if (sizeof(short) != 2) goto ERROR;
  if (sizeof(int) != 4) goto ERROR;
  if (sizeof(long) != 4) goto ERROR;
  if (sizeof(long long) != 8) goto ERROR;
  if (sizeof(void *) != 4) goto ERROR;

  return 0;
ERROR:
  return 1;
}
