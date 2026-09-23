// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// ILP32/LP64: safe; obtains but does not use an alloca(0) result.
// GCC special-cases a known zero size: no allocation is performed and it returns a valid
// stack-related address with zero usable bytes, which must not be dereferenced.
int main(void) {
  void *ptr = __builtin_alloca(0);
  (void)&*ptr; // &* is a no-op
  return 0;
}
