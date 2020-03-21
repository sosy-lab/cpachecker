extern char __VERIFIER_nondet_char();
extern void __VERIFIER_error();

int main(){

int x = 0;
if(__VERIFIER_nondet_char())
x = x+1;
else{

	x = x-1;
	if(x<1 && ((x<0 && x<-1)|| x>0)){
		x--;
	}
	x = x+2;
}
       ERROR: __VERIFIER_error();

}