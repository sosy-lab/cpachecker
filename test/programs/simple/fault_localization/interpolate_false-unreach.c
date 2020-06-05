extern int __VERIFIER_nondet_int();

int main(){
	int x = 5;
	x = x + 5;
	x++;
	x--;
	x++;
	x--;
	x++;
	x--;
	x++;
	x--;
	if (x == 10)
		goto ERROR;
EXIT: return 0;
ERROR: return 1;
}
