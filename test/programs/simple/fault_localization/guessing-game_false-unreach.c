extern int __VERIFIER_nondet_int();
#include <time.h>
#include <stdlib.h>

int thinkOfANumber(){
	return __VERIFIER_nondet_int();
}

int main(){

	int range = 100;
	int min = 1;


	srand(time(NULL));
	int numberToGuess = thinkOfANumber();
	if(numberToGuess < min || numberToGuess > range+min){
		goto EXIT;	
	}
	

	int guess;
	for(int i = 0; i < 8; i++) {
		guess = rand();
		if(guess < 0) guess = guess * (-1);
		guess += min;
		if(guess < numberToGuess) {
			min = guess;
			range -= min;
		} else {
			range = guess - min;
		}
	}

	if(guess != numberToGuess) 
		goto ERROR;
	

EXIT: return 0;
ERROR: return 1;
}
