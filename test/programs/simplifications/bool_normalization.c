// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  _Bool a = 42;
  _Bool b = 0;

  if (a != 1) goto ERROR;
  if (b != 0) goto ERROR;
  if (!!5 != 1) goto ERROR;
  if (!!0 != 0) goto ERROR;
  if (!0 != 1) goto ERROR;
  if (!7 != 0) goto ERROR;

  return 0;
ERROR:
  return 1;
}
