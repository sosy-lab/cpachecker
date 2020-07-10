extern int __VERIFIER_nondet_int();

int main(){		
	int y = 0;
	int x = 5;
	x++;
	x = y + x;
	x--;
	if (x == 5)
		goto ERROR;
EXIT: return 0;
ERROR: return 1;
}
