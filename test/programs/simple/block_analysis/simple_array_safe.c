// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int,
                           const char *) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));

void reach_error() {
  __assert_fail("0", "simple_array_safe.c", 21, "reach_error");
}

int main() {

  int x = 5;
  int arr[5] = {1, 2, 3, 4, 10};
  int sum = 0;
  for (int i = 0; i < x - 1; i++) {
    sum += arr[i];
  }
  if (arr[x - 1] != sum) {
    arr[x - 1] = 5;
    arr[x - 1] = arr[x - 1] + 2;
    reach_error();
  }
  return 0;
}
