// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Whether the GCC attributes packed and aligned of a member declaration apply to the member or to
// the struct, union, or enum type that the declaration defines (#411).
// Expected values are from GCC on x86-64.

extern void reach_error(void);
#define offsetof(T, m) __builtin_offsetof(T, m)
#define check(e) if (!(e)) reach_error()

struct T0 { int x; };
struct P0 { char a; int b; };
typedef struct { int x; } TD;
typedef struct { char a; int b; } TDP;
enum EX { EXA, EXB };

// In-place definitions: attributes after the keyword or the closing brace apply to the type.
struct D1 { char c; struct __attribute__((aligned(16))) T1 { int x; } t; };
struct D2 { char c; struct T2 { int x; } __attribute__((aligned(16))) t; };
struct D3 { char c; struct __attribute__((packed)) T3 { char a; int b; } t; };
struct D4 { char c; struct T4 { char a; int b; } __attribute__((packed)) t; };
struct D5 { char c; union __attribute__((aligned(8))) U5 { int x; } u; };
struct D6 { char c; union U6 { char a; int b; } __attribute__((packed)) u; };
struct D7 { char c; enum E7 { A7, B7 } __attribute__((packed)) e; };
// Alignment specifiers and attributes of the declarator apply to the member.
struct D8 { char c; _Alignas(16) struct T8 { int x; } t; };
struct D9 { char c; struct __attribute__((packed)) T9 { char a; int b; } t __attribute__((aligned(8))); };

// References to existing types: the attributes apply to the member.
struct R1 { char c; struct T0 t __attribute__((aligned(16))); };
struct R2 { char c; struct T0 __attribute__((aligned(16))) t; };
struct R3 { char c; __attribute__((aligned(16))) struct T0 t; };
struct R4 { char c; TD __attribute__((aligned(16))) t; };
struct R5 { char c; __attribute__((packed)) TDP t; };
struct R6 { char c; __attribute__((packed)) struct P0 t; };
struct R7 { char c; struct P0 __attribute__((packed)) t; };
struct R8 { char c; enum EX __attribute__((packed)) e; };
// GCC ignores type attributes after the keyword if the type is already defined.
struct R9 { char c; struct __attribute__((aligned(16))) T0 t; };

int main() {
  check(sizeof(struct D1) == 32 && offsetof(struct D1, t) == 16);
  check(sizeof(struct T1) == 16 && _Alignof(struct T1) == 16);
  check(sizeof(struct D2) == 32 && offsetof(struct D2, t) == 16);
  check(sizeof(struct T2) == 16 && _Alignof(struct T2) == 16);
  check(sizeof(struct D3) == 6 && offsetof(struct D3, t) == 1);
  check(sizeof(struct T3) == 5 && _Alignof(struct T3) == 1);
  check(sizeof(struct D4) == 6 && offsetof(struct D4, t) == 1);
  check(sizeof(struct T4) == 5 && _Alignof(struct T4) == 1);
  check(sizeof(struct D5) == 16 && offsetof(struct D5, u) == 8);
  check(sizeof(union U5) == 8 && _Alignof(union U5) == 8);
  check(sizeof(struct D6) == 5 && offsetof(struct D6, u) == 1);
  check(sizeof(union U6) == 4 && _Alignof(union U6) == 1);
  check(sizeof(struct D7) == 2 && offsetof(struct D7, e) == 1);
  check(sizeof(struct D8) == 32 && offsetof(struct D8, t) == 16);
  check(sizeof(struct T8) == 4 && _Alignof(struct T8) == 4);
  check(sizeof(struct D9) == 16 && offsetof(struct D9, t) == 8);
  check(sizeof(struct T9) == 5 && _Alignof(struct T9) == 1);

  check(sizeof(struct R1) == 32 && offsetof(struct R1, t) == 16);
  check(sizeof(struct R2) == 32 && offsetof(struct R2, t) == 16);
  check(sizeof(struct R3) == 32 && offsetof(struct R3, t) == 16);
  check(sizeof(struct R4) == 32 && offsetof(struct R4, t) == 16);
  check(sizeof(struct R5) == 9 && offsetof(struct R5, t) == 1);
  check(sizeof(struct R6) == 9 && offsetof(struct R6, t) == 1);
  check(sizeof(struct R7) == 9 && offsetof(struct R7, t) == 1);
  check(sizeof(struct R8) == 5 && offsetof(struct R8, e) == 1);
  check(sizeof(struct R9) == 8 && offsetof(struct R9, t) == 4);
  check(sizeof(struct T0) == 4 && sizeof(struct P0) == 8 && sizeof(TD) == 4 && sizeof(TDP) == 8);
  return 0;
}
