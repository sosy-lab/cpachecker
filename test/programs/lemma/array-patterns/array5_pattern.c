/*
 This file is part of CPAchecker,
 a tool for configurable software verification:
 https://cpachecker.sosy-lab.org

 SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>

 SPDX-License-Identifier: Apache-2.0
*/
/*


   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/* Program Description :-
 * Two arrays are declared of same size. All elements are initialized to 0.
 * In while(1) loop, any index is selected non-deterministically.
 * If index is an even number Array1[index] is initialized with count, and
 * Array2[index] is initialized with -count. However, when index is odd,
 * exactly reverse is intialized.
 * Sum of both array should be always zero.
 *
 * Changelog --> 22 November 2019
 * Variable count is reset on reaching 200. One can have any other integer value as well
 * to reset count. It is an additional challenge for Tools/solvers using supervised learning
 * based techniques for algorithm selection. 200 is the max unrolling for most of the reachsafe
 * programs to reach property violation. Presence of 200 as part of statement assignment 
 * inside loop can be picked up by algorithm selector to deploy only falsification/correctness proving
 * techniques.
 * */

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "array5_pattern.c", 36, "reach_error"); }
extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }
extern int __VERIFIER_nondet_int() ;
extern short __VERIFIER_nondet_short() ;

// signed long long ARR_SIZE ;

int main()
{
	// ARR_SIZE = (signed long long)__VERIFIER_nondet_short() ;
	// assume_abort_if_not(ARR_SIZE > 0) ;
signed long long ARR_SIZE = 10000;

	int array1[10000] ;
	int array2[10000] ;
	int count = 0, num = -1 ;
       	signed long long sum = 0 ;
	int temp ;
	short index ;

	for(count=0;count<ARR_SIZE;count++)
	{
		array1[count] = 0 ;
		array2[count] = 0 ;
	}

	count = 1 ;

	while(1)
        {
		
		index = __VERIFIER_nondet_short() ;	
		assume_abort_if_not(index>=0 && index < ARR_SIZE) ;
		

		if(index % 2 == 0){
			array1[index] = num * (num * count) ;
			array2[index] = num * count ;
		}
		else{
			array1[index] = num * count ;
			array2[index] = num * (num * count) ;
		}

		if(count == 200)
			count = 0 ;	
		count++ ;

		temp = __VERIFIER_nondet_int() ;
		if(temp == 0) break ;
	}

	for(count=0;count<ARR_SIZE;count++)
	{
		sum = sum  + array1[count] + array2[count];
	}

	__VERIFIER_assert(sum == 0) ;
	return 0 ;
}

		
