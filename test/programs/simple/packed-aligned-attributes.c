// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Layout of types with the GCC attributes packed and aligned and with _Alignas
// (#411, #1817). Expected values are from GCC on x86-64.

extern void reach_error(void);
#define offsetof(T, m) __builtin_offsetof(T, m)
#define check(e) if (!(e)) reach_error()

typedef unsigned long kernel_ulong_t;

struct P1 { char c; int i; } __attribute__((packed));
struct __attribute__((packed)) P2 { char c; int i; };
struct P3 { char c; int i __attribute__((packed)); short s; };
struct A1 { char c; int i __attribute__((aligned(8))); };
struct A2 { char c; __attribute__((aligned(16))) int i, j; };
struct A3 { char c; int i; } __attribute__((aligned(32)));
struct A4 { char c; int i __attribute__((aligned(1))); };
struct A5 { char c; int i __attribute__((aligned)); };
struct E1 { char c; int i __attribute__((aligned((1) << (6)))); };
struct E2 { char c; kernel_ulong_t i __attribute__((aligned(2 * sizeof(kernel_ulong_t)))); };
struct E3 { char c; short s __attribute__((aligned(__alignof__(void *)))); };
struct S1 { char c; _Alignas(8) int i; };
struct S2 { char c; _Alignas(long) int i; };
struct __attribute__((packed, aligned(4))) PA1 { char c; int i; char d; };
struct __attribute__((packed)) PA2 { char c; int i __attribute__((aligned(2))); };
struct __attribute__((packed)) PA3 { char c; struct A3 a; };
struct __attribute__((packed)) PB { char c; int b : 3; int d : 30; };
struct __attribute__((packed)) PZ { char c; int : 0; char d; };
union __attribute__((packed)) U1 { char c; int i; };
union U2 { char c; int i __attribute__((aligned(16))); };
union U3 { char c[5]; int i; };
enum EN1 { EA = -1, EB = 200 } __attribute__((packed));
struct EN2 { char c; enum EN1 e; };
// packed structs and unions have alignment 1, also if the first member is not a char
struct P4 { int i; char c; } __attribute__((packed));
struct __attribute__((packed)) P5 { long l; short s; };
struct P6 { int i __attribute__((packed)); char c; };
struct __attribute__((packed)) P7 { struct { int a; char b; } in; char c; };
typedef struct __attribute__((packed)) { int i; char c; } P8;
struct N1 { char c; struct P4 p; };
struct __attribute__((packed)) PA4 { int i; char c __attribute__((aligned(2))); };
struct __attribute__((packed)) PA5 { int i; char c; } __attribute__((aligned(2)));
union __attribute__((packed)) U4 { int i; char c[5]; };
// self references, nested types, and several declarators in the first member declaration
struct __attribute__((packed, aligned(2))) L { char c; struct L *next; };
struct __attribute__((packed)) O { char c; struct { char d; struct O *o; } inner; };
struct __attribute__((aligned(8))) M { char a, b; int i; };

// example from #1079
#define PADDING_SIZE (64 - sizeof(unsigned char) - sizeof(unsigned int))
typedef struct __attribute__((packed, aligned(64))) {
  unsigned char uchar;
  unsigned int uint;
  unsigned char padding[PADDING_SIZE];
} a_t;
typedef struct __attribute__((packed, aligned(64))) {
  unsigned int uint;
  unsigned char uchar;
  unsigned char padding[PADDING_SIZE];
} b_t;

int main() {
  check(sizeof(struct P1) == 5 && _Alignof(struct P1) == 1 && offsetof(struct P1, i) == 1);
  check(sizeof(struct P2) == 5 && offsetof(struct P2, i) == 1);
  check(sizeof(struct P3) == 8 && offsetof(struct P3, i) == 1 && offsetof(struct P3, s) == 6);
  check(sizeof(struct A1) == 16 && _Alignof(struct A1) == 8 && offsetof(struct A1, i) == 8);
  check(sizeof(struct A2) == 48 && offsetof(struct A2, i) == 16 && offsetof(struct A2, j) == 32);
  check(sizeof(struct A3) == 32 && _Alignof(struct A3) == 32);
  check(sizeof(struct A4) == 8 && offsetof(struct A4, i) == 4);
  check(sizeof(struct A5) == 32 && offsetof(struct A5, i) == 16);
  check(sizeof(struct E1) == 128 && offsetof(struct E1, i) == 64);
  check(sizeof(struct E2) == 32 && offsetof(struct E2, i) == 16);
  check(sizeof(struct E3) == 16 && offsetof(struct E3, s) == 8);
  check(sizeof(struct S1) == 16 && offsetof(struct S1, i) == 8);
  check(sizeof(struct S2) == 16 && offsetof(struct S2, i) == 8);
  check(sizeof(struct PA1) == 8 && _Alignof(struct PA1) == 4 && offsetof(struct PA1, d) == 5);
  check(sizeof(struct PA2) == 6 && offsetof(struct PA2, i) == 2);
  check(sizeof(struct PA3) == 33 && offsetof(struct PA3, a) == 1);
  check(sizeof(struct PB) == 6 && _Alignof(struct PB) == 1);
  check(sizeof(struct PZ) == 5 && offsetof(struct PZ, d) == 4);
  check(sizeof(union U1) == 4 && _Alignof(union U1) == 1);
  check(sizeof(union U2) == 16 && _Alignof(union U2) == 16);
  check(sizeof(union U3) == 8);
  check(sizeof(enum EN1) == 2 && sizeof(struct EN2) == 4 && offsetof(struct EN2, e) == 2);
  check(sizeof(struct P4) == 5 && _Alignof(struct P4) == 1 && offsetof(struct P4, c) == 4);
  check(sizeof(struct P4[3]) == 15);
  check(sizeof(struct P5) == 10 && _Alignof(struct P5) == 1);
  check(sizeof(struct P6) == 5 && _Alignof(struct P6) == 1);
  check(sizeof(struct P7) == 9 && _Alignof(struct P7) == 1 && offsetof(struct P7, c) == 8);
  check(sizeof(P8) == 5 && _Alignof(P8) == 1);
  check(sizeof(struct N1) == 6 && _Alignof(struct N1) == 1 && offsetof(struct N1, p) == 1);
  check(sizeof(struct PA4) == 6 && _Alignof(struct PA4) == 2 && offsetof(struct PA4, c) == 4);
  check(sizeof(struct PA5) == 6 && _Alignof(struct PA5) == 2);
  check(sizeof(union U4) == 5 && _Alignof(union U4) == 1);
  check(sizeof(a_t) == 64 && sizeof(a_t) == sizeof(b_t));
  struct L l1, l2;
  l1.next = &l2;
  l2.c = 7;
  check(sizeof(struct L) == 10 && _Alignof(struct L) == 2 && offsetof(struct L, next) == 1);
  check(sizeof(*l1.next) == 10 && l1.next->c == 7);
  check(sizeof(struct O) == 17 && offsetof(struct O, inner) == 1 && offsetof(struct O, inner.o) == 9);
  check(sizeof(struct M) == 8 && _Alignof(struct M) == 8 && offsetof(struct M, b) == 1);
  return 0;
}
