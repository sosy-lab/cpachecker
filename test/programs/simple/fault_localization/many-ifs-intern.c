extern int __VERIFIER_nondet_int();

int main(){
	int x = __VERIFIER_nondet_int();
	if(x > 0)
		if(x < 5)
			if(x > 1) 
				if(x != -1 && x != 6) {
					goto ERROR;
				}
EXIT: return 0;
ERROR: return 1;
}
