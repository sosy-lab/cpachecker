extern int __VERIFIER_nondet_int();


int fib(int x){
if(x == 0){
return 0;
}
if(x == 1){
return 1;
}
if(x==2){
return 1;
}
if(x > 1){
return fib(x-1) + fib(x-2);
}
return -1;
}

int main() {
	int x = __VERIFIER_nondet_int();
	fib(x);
}	
