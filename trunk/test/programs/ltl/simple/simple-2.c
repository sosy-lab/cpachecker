// SPDX-FileCopyrightText: University of Freiburg
//
// SPDX-License-Identifier: LGPL-3.0-or-later

//#Safe
//@ ltl invariant positive: (<> AP(x > 100));

int x=0;
	
void foo(){
	if(x<10){
		x++;
	} else {
		x = x*5;
	}
	x++;
}

void main()
{
    while(1){
		foo();
    }
}
