int x, y, a;

extern int __VERIFIER_nondet_int(void);
int select_one() {if (__VERIFIER_nondet_int()) return 1; else return 0;}
void __automaton_fail()
{
	goto error; 
	error:  ; 
}
extern int __VERIFIER_externModelSatisfied(char *);

int main() {
	x=select_one();
	y=select_one();

    if (__VERIFIER_externModelSatisfied("test/programs/simple/externalModel_model_UNSAFE.dimacs")) {
	if (x || !y) { // model is (x->y && y->z) (z is not used in this program) 
__automaton_fail();
	}
    }

    return (0);
}

