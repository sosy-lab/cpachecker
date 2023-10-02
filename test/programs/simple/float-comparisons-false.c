// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern double nan(const char*);

int main() {
  double a = nan("");
  int c1 = (a == 0.0);
  int c2 = (!(a != 0.0));
  int c3 = (a > 0.0);
  int c4 = (a >= 0.0);
  int c5 = (a < 0.0);
  int c6 = (a <= 0.0);

  int n1 = !(a == 0.0);
  int n2 = (a != 0.0);
  int n3 = !(a > 0.0);
  int n4 = !(a >= 0.0);
  int n5 = !(a < 0.0);
  int n6 = !(a <= 0.0);

  if (c1) return 0;
  if (c2) return 0;
  if (c3) return 0;
  if (c4) return 0;
  if (c5) return 0;
  if (c6) return 0;

  if (n1 && n2 && n3 && n4 && n5 && n6) {
ERROR:
    return 1;
  }

  return 0;
}
