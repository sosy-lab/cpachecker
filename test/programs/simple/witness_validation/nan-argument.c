// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

int is_nan(float x) { return x != x; }

int main() {
  float x = 0.0f / 0.0f;
  if (!is_nan(x)) {
    reach_error();
  }
  return 0;
}
