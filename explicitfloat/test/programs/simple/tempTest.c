int main(void){
  
  int a;
  int b;
  int c;
 
  a = 5;
  b = a + 1;
 
  c = asdad();
  b = sdsd();
 
  if(c == 89){
    b = 90;
  }
  else{
   b = 2000;
  }

  c = b;
  a = c - 1;
  
  if(a == 89){
  errorFn();
  }
  return (0);
 
}

void errorFn(void) 
{ 
  goto ERROR;
  ERROR: 
  return;
}
