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

// Note, since this returns a pointer array, this is pass-by-reference for the values!
int ** makeArrayWithPointerArray(int * numArray) {
    int ** newArray;
    newArray = malloc(10 * sizeof(int *));
    for (int i = 0; i < 10; i++) {
        *(newArray + i) = &*(numArray + i);
    }
    return newArray;
}

// Test pointer arithmetics and comparisons (==, !=, <, >)
int main() {
	int * arr;
    arr = malloc(10 * sizeof(int));
    for (int i = 0; i < 10; i++) {
        int n = __VERIFIER_nondet_int();
        if (n > 10 + i || n < 0 + i) {
            return 0;
        }
        // Fill array with nondets, limited from 0 + index to 10 + index
        *(arr + i) = n;
    }
    
    // Check that the numbers adhere to the limits
    for (int i = 0; i < 10; i++) {
        assert(*(arr + i) >= 0 + i && *(arr + i) <= 10 + i);
        assert(!(*(arr + i) > 10 + i || *(arr + i) < 0 + i));
    }

    // Copy the values and check equalities and ranges
    int * arr2 = malloc(10 * sizeof(int));
    for (int i = 0; i < 10; i++) {
        *(arr2 + i) = *(arr + i);
    }
    for (int i = 0; i < 10; i++) {
        assert(*(arr2 + i) >= 0 + i && *(arr2 + i) <= 10 + i);
        assert(!(*(arr2 + i) > 10 + i || *(arr2 + i) < 0 + i));
        assert(*(arr + i) == *(arr2 + i));
    }
    assert(arr != arr2);

    // Pass pointers to a new array and check equalities
    int ** arr3 = makeArrayWithPointerArray(arr);
    for (int i = 0; i < 10; i++) {
        assert(**(arr3 + i) >= 0 + i && **(arr3 + i) <= 10 + i);
        assert(!(**(arr3 + i) > 10 + i || **(arr3 + i) < 0 + i));
        assert(*(arr + i) == **(arr3 + i));
        assert(&*(arr + i) == *(arr3 + i));
    }
    assert((void *) arr != (void *) arr3);

    // If we now change something in the original array, arr3 should reflect it, while arr2 is unchanged
    // We increment all numbers in arr by 11, so that they are never equal to the same number as before in that index
    for (int i = 0; i < 10; i++) {
        *(arr + i) = *(arr + i) + 11;
    }
    // Check arr3 == arr
    for (int i = 0; i < 10; i++) {
        assert(**(arr3 + i) >= 0 + i + 11 && **(arr3 + i) <= 10 + i + 11);
        assert(!(**(arr3 + i) > 10 + i + 11 || **(arr3 + i) < 0 + i + 11));
        assert(*(arr + i) == **(arr3 + i));
        assert(&*(arr + i) == *(arr3 + i));
    }
    assert((void *) arr != (void *) arr3);

    // Check inequality to arr2
    for (int i = 0; i < 10; i++) {
        assert(*(arr2 + i) >= 0 + i && *(arr2 + i) <= 10 + i);
        assert(!(*(arr2 + i) > 10 + i || *(arr2 + i) < 0 + i));
        assert(*(arr + i) != *(arr2 + i));
    }
    assert((void *) arr != (void *) arr2);

    // Now we use pointer arithmetics to increment a copy of arr3 and check that is is larger than arr3
    int ** arr3_2 = arr3;
    arr3_2 = arr3_2 + 1;
    assert(arr3 < arr3_2);
    int ** arr3_3 = arr3_2 + 1;
    assert(arr3 < arr3_3);
    assert(arr3_3 > arr3_2);
    arr3_2++;
    assert(arr3_2 == arr3_3);
    --arr3_3;
    assert(arr3_3 < arr3_2);
    arr3_3 = arr3_3 - 1;
    assert((void *) arr3 == (void *) arr3_3);
    assert((void *) arr3_2 > (void *) arr3 && arr3_3 < arr3_2);

    // SAFE
	return 0;	
}
