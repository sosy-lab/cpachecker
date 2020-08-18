// SPDX-FileCopyrightText: University of Freiburg
//
// SPDX-License-Identifier: LGPL-3.0-or-later

//#Safe
//@ ltl invariant positive: (<> AP(output == 26));

extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern void __VERIFIER_assume() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int() __attribute__ ((__noreturn__));

int input;
int output;

int aux=0;

int calculate_output(int input) {
	if(input == 4){
		return 26;
	} else {
		aux++;
		if( aux > 10){
			return aux;
		} else{
			return 0;
		}
	}
}
	
int main()
{
    // default output
    output = -1;

    // main i/o-loop
    while(1)
    {
        // read input
        input = __VERIFIER_nondet_int();
		__VERIFIER_assume(input == 4 ||
		input == 2 ||
		input == 3 ||
		input == 6 ||
		input == 5 ||
		input == 1 ); 

        // operate eca engine
        output = calculate_output(input);
    }
}

