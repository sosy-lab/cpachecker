// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Functions that look like a byte-wise memset implementation, but do not set exactly the n bytes
// starting at s to c. None of them may be recognized as MEMSET implementation.

typedef unsigned long size_t;

extern void reach_error(void);

// writes c + 1 instead of c
void *writes_other_value(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 0; i < n; i++) {
    bytes[i] = c + 1;
  }
  return s;
}

// leaves the last byte unchanged
void *stops_one_early(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 0; i < n - 1; i++) {
    bytes[i] = c;
  }
  return s;
}

// leaves the first byte unchanged
void *starts_at_one(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 1; i < n; i++) {
    bytes[i] = c;
  }
  return s;
}

// writes the bytes from s + 1 to s + n
void *shifted_index(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 0; i < n; i++) {
    bytes[i + 1] = c;
  }
  return s;
}

// also writes the byte after the last one
void *writes_twice(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 0; i < n; i++) {
    bytes[i] = c;
  }
  bytes[n] = c;
  return s;
}

// moves the pointer in addition to the index, so it only writes every second byte
void *moves_the_pointer(void *s, int c, size_t n) {
  char *bytes = (char *)s;
  for (size_t i = 0; i < n; i++) {
    bytes[i] = c;
    bytes = bytes + 1;
  }
  return s;
}

int main() {
  char buffer[8] = {0, 0, 0, 0, 0, 0, 0, 0};
  writes_other_value(buffer, 0, 2);
  stops_one_early(buffer, 0, 2);
  starts_at_one(buffer, 0, 2);
  shifted_index(buffer, 0, 2);
  writes_twice(buffer, 0, 2);
  moves_the_pointer(buffer, 0, 2);
  // writes_other_value sets buffer[0] to 1, stops_one_early sets it back to 0
  if (buffer[0] != 0) {
    reach_error();
  }
  return 0;
}
