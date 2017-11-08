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
	if (a > b && 1 == 1) {
		return a;
	}
	return b;
}

int main() {

}


int gt_mutation_0(int a, int b,struct adresse adr) {
if ( a < b && 1 == 1 ) {
return a ;
}

return b ;
}

int gt_mutation_1(int a, int b,struct adresse adr) {
if ( a > b && 1 > 1 ) {
return a ;
}

return b ;
}

int gt_mutation_2(int a, int b,struct adresse adr) {
if ( a == b && 1 == 1 ) {
return a ;
}

return b ;
}

int gt_mutation_3(int a, int b,struct adresse adr) {
if ( a > b && 1 <= 1 ) {
return a ;
}

return b ;
}

int gt_mutation_4(int a, int b,struct adresse adr) {
if ( a > b && 1 != 1 ) {
return a ;
}

return b ;
}

int gt_mutation_5(int a, int b,struct adresse adr) {
if ( a > b && 1 < 1 ) {
return a ;
}

return b ;
}

int gt_mutation_6(int a, int b,struct adresse adr) {
if ( a != b && 1 == 1 ) {
return a ;
}

return b ;
}

int gt_mutation_7(int a, int b,struct adresse adr) {
if ( a <= b && 1 == 1 ) {
return a ;
}

return b ;
}

int gt_mutation_8(int a, int b,struct adresse adr) {
if ( a > b && 1 >= 1 ) {
return a ;
}

return b ;
}

int gt_mutation_9(int a, int b,struct adresse adr) {
if ( a >= b && 1 == 1 ) {
return a ;
}

return b ;
}

int main_mutation_test(){ 
	int gt_a_input = __VERIFIER_nondet_int();
	int gt_b_input = __VERIFIER_nondet_int();
	struct adresse gt_adr_input;
	gt_adr_input.straße = __VERIFIER_nondet_int();
	gt_adr_input.hausnummer = __VERIFIER_nondet_int();

	int gt_result = gt(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_0_result = gt_mutation_0(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_1_result = gt_mutation_1(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_2_result = gt_mutation_2(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_3_result = gt_mutation_3(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_4_result = gt_mutation_4(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_5_result = gt_mutation_5(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_6_result = gt_mutation_6(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_7_result = gt_mutation_7(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_8_result = gt_mutation_8(gt_a_input,gt_b_input,gt_adr_input);
	int gt_mutation_9_result = gt_mutation_9(gt_a_input,gt_b_input,gt_adr_input);

	if(gt_result != gt_mutation_0_result) 
		gt_mutation_0_goal: printf("gt_mutation_0_goal");

	if(gt_result != gt_mutation_1_result) 
		gt_mutation_1_goal: printf("gt_mutation_1_goal");

	if(gt_result != gt_mutation_2_result) 
		gt_mutation_2_goal: printf("gt_mutation_2_goal");

	if(gt_result != gt_mutation_3_result) 
		gt_mutation_3_goal: printf("gt_mutation_3_goal");

	if(gt_result != gt_mutation_4_result) 
		gt_mutation_4_goal: printf("gt_mutation_4_goal");

	if(gt_result != gt_mutation_5_result) 
		gt_mutation_5_goal: printf("gt_mutation_5_goal");

	if(gt_result != gt_mutation_6_result) 
		gt_mutation_6_goal: printf("gt_mutation_6_goal");

	if(gt_result != gt_mutation_7_result) 
		gt_mutation_7_goal: printf("gt_mutation_7_goal");

	if(gt_result != gt_mutation_8_result) 
		gt_mutation_8_goal: printf("gt_mutation_8_goal");

	if(gt_result != gt_mutation_9_result) 
		gt_mutation_9_goal: printf("gt_mutation_9_goal");

} 
