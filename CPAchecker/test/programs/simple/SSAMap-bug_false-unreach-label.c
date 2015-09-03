extern int __VERIFIER_nondet_int();
extern int *__VERIFIER_nondet_pointer();

void  __Main(int flag)
{
	int *p1 = 0, *p2;
	if (flag) {
		if (__VERIFIER_nondet_int()) {
                        p2 = p1;
			p1 = 0;
		} else {
			p1 = 0;
		}
	} else {
		p1 = __VERIFIER_nondet_pointer();
	}
	p2 = __VERIFIER_nondet_pointer();
        if (p1 != p2)  {
	ERROR: goto ERROR;	
	}	
}

int main() {
	__Main(0);
	return 0;
}
