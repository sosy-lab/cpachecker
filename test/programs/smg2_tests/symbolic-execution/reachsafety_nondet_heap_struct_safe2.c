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

struct FooPtr {
  int * n;
};

struct Foo * makeStructWithNumber(int num) {
    struct Foo * newStruct;
    newStruct = malloc(sizeof(struct Foo));
    (*newStruct).n = num;
    return newStruct;
}

struct FooPtr * makeStructWithPointer(int * num) {
    struct FooPtr * newStruct;
    newStruct = malloc(sizeof(struct FooPtr));
    (*newStruct).n = num;
    return newStruct;
}

// Test heap structs w numbers and pointer restrictions
// Tests deref only (no -> operator)
// There are negated testcases, as i want to make sure that negations or simplifications work
int main() {
	struct Foo * s;
    s = malloc(sizeof(struct Foo));
    (*s).n = __VERIFIER_nondet_int();

    if ((*s).n > 10 || (*s).n < 0) {
        return 0;
    }
	
    // 10 >= s.n >= 0
    assert((*s).n >= 0 && (*s).n <= 10);
    assert(!((*s).n > 10 || (*s).n < 0));

    // Copy the heap struct and check that the newly copied struct has the same predicates/restrictions
    // Heap copy == just the pointer is copied ;D
    struct Foo * s2;
    s2 = s;
    assert((*s2).n >= 0 && (*s2).n <= 10);
    assert(!((*s2).n > 10 || (*s2).n < 0));
    assert((*s2).n == (*s).n);
    assert(s2 == s);

    // With a new struct and new value
    struct Foo * s3 = malloc(sizeof(struct Foo));
    int number = __VERIFIER_nondet_int();
    (*s3).n = number;

    // This also filters number, as the value is the same!
    if ((*s3).n > 10 || (*s3).n < 0) {
        return 0;
    }

    assert((*s3).n >= 0 && (*s3).n <= 10);
    assert(!((*s3).n > 10 || (*s3).n < 0));

    if ((*s3).n == (*s).n) {
        assert(!((*s3).n != (*s).n));
    } else {
        assert(!((*s3).n == (*s).n));
    }
    assert(number == (*s3).n);
    assert(&((*s3).n) != &((*s).n));

    // Now create a struct that takes the pointer of a number instead of a number
    struct FooPtr * sptr = malloc(sizeof(struct FooPtr));
    (*sptr).n = &number;
    // Note here that while the value of number must not be > 10 and < 0, its pointer is not equal to any other here besides (*sptr).n!
    assert(*((*sptr).n) >= 0 && *((*sptr).n) <= 10);
    assert(!(*((*sptr).n) > 10 || *((*sptr).n) < 0));
    assert((*sptr).n == &number);
    assert((*sptr).n != &((*s).n));

    // Again but with assignment through a return edge
    struct FooPtr * sptr2 = makeStructWithPointer(&number);
    assert(*((*sptr2).n) >= 0 && *((*sptr2).n) <= 10);
    assert(!(*((*sptr2).n) > 10 || *((*sptr2).n) < 0));
    assert((*sptr2).n == (*sptr).n);
    assert(sptr2 != sptr);
    assert(*((*sptr2).n) == *((*sptr).n));

    // SAFE
	return 0;	
}
