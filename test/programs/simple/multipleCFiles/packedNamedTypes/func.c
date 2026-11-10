// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Same name as the unpacked struct in main.c, but a different layout (#411).
struct S {
  char c;
  int i;
} __attribute__((packed));

unsigned long size_packed(void) { return sizeof(struct S); }
