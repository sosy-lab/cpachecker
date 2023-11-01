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

int * makeArrayLimitedWithin(int lower, int upper) {
    int * newArray;
    newArray = malloc(10 * sizeof(int));
    for (int i = 0; i < 10; i++) {
        int num = __VERIFIER_nondet_int();
        if (num > upper + i || num < lower + i) {
            return 0;
        }
        newArray[i] = num;
    }
    return newArray;
}

// Note, since we deref the value pointers, this is pass by value!
int * makeArrayWithPointerArray(int ** num) {
    int * newArray;
    newArray = malloc(10 * sizeof(int));
    for (int i = 0; i < 10; i++) {
        newArray[i] = *(num[i]);
    }
    return newArray;
}

// Test heap arrays (size 10) and pointer games with it
// The goal is to test that constraints on symbolic values are kept even when playing around with pointers etc.
int main() {
	int * arr;
    arr = malloc(10 * sizeof(int));
    for (int i = 0; i < 10; i++) {
        int n = __VERIFIER_nondet_int();
        if (n > 10 + i || n < 0 + i) {
            return 0;
        }
        // Fill array with nondets, limited from 0 + index to 10 + index
        arr[i] = n;
    }
    
    // Check that the numbers adhere to the limits
    for (int i = 0; i < 10; i++) {
        assert(arr[i] >= 0 + i && arr[i] <= 10 + i);
        assert(!(arr[i] > 10 + i || arr[i] < 0 + i));
    }

    // Copy the values and check equalities and ranges
    int *arr2 = malloc(10 * sizeof(int));
    for (int i = 0; i < 10; i++) {
        arr2[i] = arr[i];
    }
    for (int i = 0; i < 10; i++) {
        assert(arr2[i] >= 0 + i && arr2[i] <= 10 + i);
        assert(!(arr2[i] > 10 + i || arr2[i] < 0 + i));
        assert(arr[i] == arr2[i]);
    }
    assert(arr != arr2);

    // With pointer copy
    int * arr3 = arr;
    for (int i = 0; i < 10; i++) {
        assert(arr3[i] >= 0 + i && arr3[i] <= 10 + i);
        assert(!(arr3[i] > 10 + i || arr3[i] < 0 + i));
        assert(arr[i] == arr3[i]);
    }
    assert(arr == arr3);

    // If we now change something in the original array, arr3 should reflect it, while arr2 is unchanged
    // We increment all numbers in arr by 11, so that they are never equal to the same number as before in that index
    for (int i = 0; i < 10; i++) {
        arr[i] = arr[i] + 11;
    }
    // Check arr3 == arr
    for (int i = 0; i < 10; i++) {
        assert(arr3[i] >= 0 + i + 11 && arr3[i] <= 10 + i + 11);
        assert(!(arr3[i] > 10 + i + 11 || arr3[i] < 0 + i + 11));
        assert(arr[i] == arr3[i]);
    }
    assert(arr == arr3);

    // Check inequality to arr2
    for (int i = 0; i < 10; i++) {
        assert(arr2[i] >= 0 + i && arr2[i] <= 10 + i);
        assert(!(arr2[i] > 10 + i || arr2[i] < 0 + i));
        assert(arr[i] != arr2[i]);
    }
    assert(arr != arr2);

    // Create a new array with the original rules in a function and return the pointer
    int * arr4 = makeArrayLimitedWithin(0, 10);
    if (arr4 == 0) {
        return 0;
    }
    // Check != arr values
    for (int i = 0; i < 10; i++) {
        assert(arr4[i] >= 0 + i && arr4[i] <= 10 + i);
        assert(!(arr4[i] > 10 + i || arr4[i] < 0 + i));
        assert(arr[i] != arr4[i]);
    }
    assert(arr != arr4);

    // Check == to arr2 values 
    // (the values may be equal, they may be not! But there is a chance that they are. 
    // But they are never more than |value_arr2 - value_arr4| <= 10 from each other.)
    for (int i = 0; i < 10; i++) {
        // assert(arr4[i] == arr2[i]);
        assert(arr4[i] - arr2[i] <= 10 && arr4[i] - arr2[i] >= -10);
    }
    assert(arr4 != arr2);

    // Make a array of value pointers
    int ** valuePointerArray = malloc(10 * sizeof(int *));
    // get the current values of arr into it
    for (int i = 0; i < 10; i++) {
        valuePointerArray[i] = &(arr[i]);
    }
    int * arr5 = makeArrayWithPointerArray(valuePointerArray);
    if (arr5 == 0) {
        return 0;
    }
    // arr5 is now equal (in values only) to arr and arr3 and therefore unequal to arr4 and arr2
    for (int i = 0; i < 10; i++) {
        assert(arr5[i] >= 0 + i + 11 && arr5[i] <= 10 + i + 11);
        assert(!(arr5[i] > 10 + i + 11 || arr5[i] < 0 + i + 11));
        assert(arr[i] == arr5[i]);
        assert(arr3[i] == arr5[i]);
    }
    assert(arr3 != arr5);
    assert(arr != arr5);

    for (int i = 0; i < 10; i++) {
        assert(arr4[i] != arr5[i]);
        assert(arr2[i] != arr5[i]);
    }
    assert(arr2 != arr5);
    assert(arr4 != arr5);

    // SAFE
	return 0;	
}
