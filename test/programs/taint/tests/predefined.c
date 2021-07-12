// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int getchar();
extern int scanf();
extern int gets();
extern int fopen();

extern void printf(int);
extern void strcmp(int);
extern void fputs(int);
extern void fputc(int);
extern void fwrite(int);

extern void __VERIFIER_mark_tainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void){
	int a, b, c, d, t;
    
    a = getchar();
    b = scanf();
    c = gets();
    d = fopen();

    __VERIFIER_mark_tainted(t);

    __VERIFIER_assert_tainted(a);
    __VERIFIER_assert_tainted(b);
    __VERIFIER_assert_tainted(c);
    __VERIFIER_assert_tainted(d);

    printf(t);
    strcmp(t);
    fputs(t);
    fputc(t);
    fwrite(t);

	return 0;
}
