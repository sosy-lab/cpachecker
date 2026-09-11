// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void *memcpy(void *dest, const void *src, size_t n);

struct S {
  int pad;
  int arr[1];
};

struct T {
  long long value;
};

int main() {
  struct S s;
  struct T t;
  s.pad = 0;
  s.arr[0] = 0;
  t.value = 0x1122334400000000ULL;
  memcpy(&s, &t, sizeof(struct S));
  if (s.arr[0] == 0x11223344) {
    // this branch is reachable: memcpy reinterprets t's bytes 4..7 (0x11223344 in little-endian)
    // as s.arr[0], since arr starts right after pad at byte offset 4.
    reach_error();
  }
}
