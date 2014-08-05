int input();
extern int __VERIFIER_nondet_int(void);

int __SELECTED_FEATURE_A;
int __SELECTED_FEATURE_B;

int globalVar;

void main()
{
    __SELECTED_FEATURE_A= __VERIFIER_nondet_int();
    __SELECTED_FEATURE_B= __VERIFIER_nondet_int();
    globalVar = input();
    if (globalVar<1)
		globalVar = 1;
		
    if (__SELECTED_FEATURE_A)
		globalVar++;
	
	if (globalVar>0) {
		goal3();
	}
}

void goal3() {
	if (globalVar>0) {
		globalVar++;
	}
}
