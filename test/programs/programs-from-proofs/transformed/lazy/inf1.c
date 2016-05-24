extern int __VERIFIER_nondet_int(void);

int flag=1;
int flag1, flag2;
int a, b, c;

void check()
{
  if (flag1 == 1){
		if (flag2 == 1){
			if( a < c) {
			  flag = 0;
			}
		}
	}
}

int main(){
    a = __VERIFIER_nondet_int();
    b = __VERIFIER_nondet_int();
    c =__VERIFIER_nondet_int();
	if (a > b) {
		flag1 = 1;
	} else {
		flag1 =0;
	}

	if (b > c){
		flag2 =1;
	} else {
		flag2 = 0;
	}

    check();	
    
	return 1;
}


