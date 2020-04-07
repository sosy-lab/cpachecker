extern int __VERIFIER_nondet_int(void);



int fib2(int n){

if(n <= 0){
return -1;
}

int fib = 0;
int prev = 1;
int i = 2;
while(i < n){
int temp = fib;
fib += prev;
prev = temp;
i++;
}
return fib;

}


int main() {
    int m = __VERIFIER_nondet_int();
   fib2(m);
}
