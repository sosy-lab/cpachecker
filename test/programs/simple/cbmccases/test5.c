int main(){
  
  int a;
  int b;
  int c;
 
  a = __BLAST_NONDET;
  b = __BLAST_NONDET;
  c = __BLAST_NONDET;  
 
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
