int __VERIFIER_nondet_int(void);

void compute_some_values(){
  static int array [1000];
  for(int i = 1; i < 1000; i++){
    array[i-1] = __VERIFIER_nondet_int();
  }
}

void main(void){
  for(int i = 0; i < 100; i++){
    compute_some_values();
  }
}