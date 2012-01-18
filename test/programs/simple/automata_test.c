int main(){

  int a;
  int b;
  int c;
  int nondet;
  nondet = nondet_int();
  a = 0;
  b = nondet_int();

  if(nondet) {
    while(1){
      if(a == 10){
	goto L1;
      }
      else {
	a++;
      }
    }
    
  }

  else {
    if(b > 5) {
      c = 100;
      if(nondet < -10){
      }
      else if(nondet < -20){
      }
    }
    else {
      c = 0;
    }
  }

  L1:

  if(a != 10){
    if (b <= 5){
       if(c != 0){
         goto ERROR;
       }
    }
  }

  return (0);
  ERROR:
  return (-1);

}
