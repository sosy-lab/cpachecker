// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_mark_tainted(void const *);
extern void __VERIFIER_mark_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void)  {
    int arr[] = {100, 200, 300};
    int *ptr;
    ptr = arr;
    
    __VERIFIER_assert_untainted(arr);
    __VERIFIER_assert_untainted(ptr);
    __VERIFIER_mark_tainted(arr);
    __VERIFIER_assert_tainted(arr);
    __VERIFIER_assert_tainted(ptr);

	return 0;
}