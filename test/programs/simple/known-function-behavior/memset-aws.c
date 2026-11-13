// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The byte-wise memset implementation of the aws-c-common proof harnesses in sv-benchmarks:
// memset_impl is a MEMSET implementation, my_memset a wrapper of it.

typedef unsigned long size_t;

extern void abort(void);
extern void reach_error(void);
extern unsigned long __VERIFIER_nondet_ulong(void);

void assume_abort_if_not(int cond) {
  if (!cond) {
    abort();
  }
}

void *memset_impl(void *s, int c, size_t n) {
  assume_abort_if_not(((n) == 0) || (s));
  if (n > 0) {
    char *sp = (char *)s;
    for (unsigned long i = 0; i < n; i++) {
      sp[i] = c;
    }
  }
  return s;
}

void *my_memset(void *s, int c, size_t n) { return memset_impl(s, c, n); }

struct aws_array_list {
  void *alloc;
  size_t current_size;
  size_t length;
  size_t item_size;
  void *data;
};

int main() {
  struct aws_array_list list = {
      0, __VERIFIER_nondet_ulong(), __VERIFIER_nondet_ulong(), __VERIFIER_nondet_ulong(), 0};
  my_memset(&list, 0, sizeof(list));
  if (list.current_size != 0 || list.data != 0) {
    reach_error();
  }
  return 0;
}
