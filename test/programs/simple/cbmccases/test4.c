int main(){
  
  int a;
  int b;
  int c;
 
  a = __BLAST_NONDET;
  b = __BLAST_NONDET;
  c = __BLAST_NONDET;  
 
  if(a == 1){
    goto end;
  }

  else{
    if(b == 1){
      if(c ==1){
        c++;
        goto end;
      }
      else{
        
      }
    }
    else{
      goto lab;
    }
    b--;
    lab: ;
    a++;
    goto end;
  }

  end:

  ERROR:
  return (1);

}
