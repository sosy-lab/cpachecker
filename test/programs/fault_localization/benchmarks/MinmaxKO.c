/*
The program Minmax (implemented in function foo) takes 
three integers as input and assigns the smallest value 
to the variable least and the biggest value to the variable most.

There is an error in this program in the assignment "most = in2",
the assignment should be "least = in2". For an input equal to 
{in1=2, in2=1, in3=3}, and we should obtain an output with 
least=1 and most=3, which is not conform with the given post-condition 
"(least<=most)".

SPDX-FileCopyrightText: Mohammed Bekkouche <http://www.i3s.unice.fr>
SPDX-License-Identifier: GPL-3.0-or-later
 */

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}    
void foo(int in1,int in2,int in3){
	int least;
	int most;
    least = in1;
	most = in1;
	if (most < in2){
	    most = in2;
	}
	if (most < in3){
	    most = in3;
	}
	if (least > in2){ 
	    most = in2; // error in the assignment : most = in2 instead of least = in2
	}
	if (least > in3){ 
	    least = in3; 
	}
    __VERIFIER_assert(least <= most);
}
int main() 
{ 
  
  foo( __VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int());
    return 0; 
} 

      
      

