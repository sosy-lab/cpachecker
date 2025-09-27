// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>

extern int __VERIFIER_nondet_int(void);
extern void abort(void);

struct Foo {
  int n;
};

struct Foo makeStructWith(int num) {
    struct Foo newStruct;
    newStruct.n = num;
    return newStruct;
}

int main() {
	struct Foo s;
    s.n = __VERIFIER_nondet_int();

    if (s.n > 10 || s.n < 0) {
        return 0;
    }
	
    // 10 >= s.n >= 0
    assert(s.n >= 0 && s.n <= 10);
    assert(!(s.n > 10 || s.n < 0));

    // Copy the stack struct and check that the newly copied struct has the same predicates/restrictions
    struct Foo s2;
    s2 = s;
    assert(s2.n >= 0 && s2.n <= 10);
    assert(!(s2.n > 10 || s2.n < 0));
    assert(s2.n == s.n);

    // Again with immidiate assignment
    struct Foo s3 = s;
    assert(s3.n >= 0 && s3.n <= 10);
    assert(!(s3.n > 10 || s3.n < 0));
    assert(s3.n == s.n);

    // Again but with assignment through a return edge
    struct Foo s4 = makeStructWith(s.n);
    assert(s4.n >= 0 && s4.n <= 10);
    assert(!(s4.n > 10 || s4.n < 0));
    assert(s4.n == s.n);

    // SAFE
	return 0;	
}
