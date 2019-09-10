extern int __VERIFIER_nondet_int();
int func (int x) {
	if(x > 0){
		if(x<0) {
			G1:return -1;
		}
		G2: return 6;
	}
G3 : return 1;
}


int main(){
int x =__VERIFIER_nondet_int();
func(x);
return 0;
}


