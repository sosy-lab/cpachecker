extern int __VERIFIER_nondet_int();

int isErr(int x){
	if(x != -1 && x != 6) {
		return 1;
	}
	return 0;
}


int main(){
	int x = __VERIFIER_nondet_int();
	if(x > 0)
		if(x < 5)
			if(x > 1) {
				int t = isErr(x);			
				if(t) {
					goto ERROR;
				}
			}
EXIT: return 0;
ERROR: return 1;
}
