#include <time.h>
#include <stdio.h>
#include <stdlib.h>

int sum; //avoid optimization

int anti_op() {
	sum = sum + 1;
}

int __VERIFIER_nondet_int() {
	return rand() % 2;
}

void init() {
	anti_op();
}

void lock() {
	anti_op();
}

void unlock() {
	anti_op();
}

#include "token_ring.02.cil.c"

void t1_started() {
	anti_op();	
}
void t2_started() {
	anti_op();	
}

void reset() {
	m_pc  =    0;
	t1_pc  =    0;
	t2_pc  =    0;
	M_E  =    2;
	T1_E  =    2;
	T2_E  =    2;
	E_M  =    2;
	E_1  =    2;
	E_2  =    2;
}

#define ITERATIONS 30000000

int main() {
	time_t start = time(NULL);

	sum = 0; 
	
	for(int i = 0; i < ITERATIONS; i++) {
		srand(42);
		reset();
		entry();		
		sum += i;
	}	
	time_t diff_time = time(NULL) - start;

	printf("%d took %lds (sum=%d)\n", ITERATIONS, diff_time, sum);
}
