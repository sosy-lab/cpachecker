//SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//SPDX-License-Identifier: Apache-2.0
extern void abort(void); 
void reach_error(){}
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }
#define SIZE 3
short __VERIFIER_nondet_short();
int main()
{
	int a[SIZE];
	int b[SIZE];
	int k;
	int i;

	for (i  = 0; i<SIZE ; i++)
	{
		a[i] = i; 
		b[i] = i ;
	}

	for (i=0; i< SIZE; i++)
	{
		if(__VERIFIER_nondet_short())
		{
			k = __VERIFIER_nondet_short();
			a[i] = k;
			b[i] = k / k ;// error in the assignment : b[i] = k / k  instead of b[i] = k * k 
		}
	}

	for (i=0; i< SIZE; i++)
	{
		__VERIFIER_assert(a[i] == b[i] || b[i]  == a[i] * a[i]);
	}
}