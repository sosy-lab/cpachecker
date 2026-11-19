// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void __VERIFIER_nondet_memory(void *ptr, size_t size);

unsigned char g[2] = {0, 0};

int main() {
  __VERIFIER_nondet_memory(&g[0], sizeof(g));
  if (g[0] == 1 && g[1] == 2) {
    reach_error();
  }
}
