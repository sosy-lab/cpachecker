// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef struct {
  int a, b;
} pair;

int main(int argc, char* argv[]) {
  pair p[1];
  p->a = 0;
}
