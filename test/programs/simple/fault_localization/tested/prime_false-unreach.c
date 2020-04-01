int __VERIFIER_nondet_int();

int isPrime(int check){
	if(check < 1){
		return 0;
	}
	//check/2 + 1 for fewer checks.
	for(int i = 1; i < check/2+1; i++){
		if(check % i == 0){
			return 0;
		}
	}
	return 1;
}

int main() {

	int check = __VERIFIER_nondet_int() % 10;
	int result = isPrime(check);

	
	//POST CONDITION
	if((result == 0 && check < 1) || (result == 0 && check == 1) || (result == 1 && check == 2) || (result == 1 && check == 3) || (result == 0 && check == 4) || (result == 1 && check == 5) || (result == 0 && check == 6) || (result == 1 && check == 7) || (result == 0 && check == 8) || (result == 0 && check == 9)){
		goto EXIT;
	} else {
		goto ERROR;
	}


	EXIT: return 0;
	ERROR: return 1;
}

