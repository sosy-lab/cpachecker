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
  double nanAlternative = 0.0/0.0;

  int c1 = (a == 0.0);
  int c2 = (!(a != 0.0));
  int c3 = (a > 0.0);
  int c4 = (a >= 0.0);
  int c5 = (a < 0.0);
  int c6 = (a <= 0.0);
  int c7 = (a == nanAlternative);
  int c8 = (a == a);

  int n1 = !(a == 0.0);
  int n2 = (a != 0.0);
  int n3 = !(a > 0.0);
  int n4 = !(a >= 0.0);
  int n5 = !(a < 0.0);
  int n6 = !(a <= 0.0);
  int n7 = !(a == nanAlternative);
  int n8 = !(a == a);

  if (c1) goto ERROR;
  if (c2) goto ERROR;
  if (c3) goto ERROR;
  if (c4) goto ERROR;
  if (c5) goto ERROR;
  if (c6) goto ERROR;
  if (c7) goto ERROR;
  if (c8) goto ERROR;

  if (n1 && n2 && n3 && n4 && n5 && n6 && n7 && n8) {
    return 0;
  }

ERROR:
  return 1;
}
