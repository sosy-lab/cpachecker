#ifndef LOCAL
extern int __VERIFIER_nondet_int();
#endif

struct adresse {
	int straße;
	int hausnummer;
};

struct meerschweinchen {
	int farbe;
	int alter;
};


int gt(int a, int b,struct adresse adr) {
	if (a > b && adr.straße == 1 && adr.hausnummer = 8) {
		return a;
	}
	return b;
}

int gt_mutation_0(int a, int b,struct adresse adr) {
	if (a > b && adr.straße == 2 && adr.hausnummer = 8) {
		return a;
	}
	return b;
}

int main() {

}

int main_mutation_test(){ 
	int gt_a_input = __VERIFIER_nondet_int();
	int gt_b_input = __VERIFIER_nondet_int();
	struct adresse gt_adr_input;
	gt_adr_input.straße = __VERIFIER_nondet_int();
	gt_adr_input.hausnummer = __VERIFIER_nondet_int();

	int gt_result = gt(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_0_result = gt_mutation_0(gt_a_input,gt_b_input,gt_adr_input);


	if(gt_result != gt_mutation_0_result) 
		gt_mutation_0_goal: printf("gt_mutation_0_goal");

} 
