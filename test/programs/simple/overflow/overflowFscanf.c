// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// fscanf's return value is discarded here, so the write to *&data only
// happens through its output parameter. An analysis that doesn't model
// this call has no reason to forget that data was 0 right before it.
#include <stdio.h>

int main() {
  long data = 0;
  fscanf(stdin, "%ld", &data);
  long result = data + 1;
  return 0;
}
