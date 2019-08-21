#include <assert.h>

/* 
 * Function f(pointer) saves the pointer into array pp 
 * and changes the state of the model bound to it using array pstate. 
 * Function g(pointer) checks that pointer is saved 
 * and checks the bound state in array. 
 * System is safe but BLAST says that "The system is unsafe :-("
 */
int VERDICT_SAFE;
int CURRENTLY_UNSAFE;

#define NULL 0

void *pp[2];
int pstate[2];

void init() {
	int i;
	for(i=0; i<2; i++) {
		pp[i]=NULL;
		pstate[i]=0;
	}
}

void f(void *pointer) {
	int i;
	for(i=0; i<2; i++) {
		if(pp[i]==NULL) {
			pp[i]=pointer;
			pstate[i]=1;
			break;
		} 
	}
}

void g(void *pointer) {
	int i;
	for(i=0; i<2; i++) {
		if(pp[i]==pointer) {
			//all is ok
			assert(pstate[i]==1);
			pstate[i]=2;
		}
	}
}

void * __undefined_pointer();

int counter = 1;
void *malloc(int size) {
	return counter++;
	//return __undefined_pointer();
}

int main() {
	int *a;
	int *b;
	init();
	a = malloc(sizeof(int));
	b = malloc(sizeof(int));
	if(a==NULL || b==NULL)
		return -1;

	f(a);
	f(b);
	g(a);
	//g(a);
	g(b);
	//assert(pstate[0]==2);
	//assert(pstate[1]==2);

	return 0;
}
