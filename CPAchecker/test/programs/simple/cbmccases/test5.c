extern int __VERIFIER_nondet_int();

int main(){
  
  int a;
  int b;
  int c;
 
  a = __VERIFIER_nondet_int();
  b = __VERIFIER_nondet_int();
  c = __VERIFIER_nondet_int();
 
  if(a == 1){
    a++;
    if(b == 1){
      b++;
      goto end;
    }
    else{
      b--;
      goto end;
    }
  }
  else{
    a--;
    if(c == 1){
      c++;
      goto end;
    }
    else{
      c--;
      goto end;
    }
  }

  end:
  ERROR:
  return(0);

}
