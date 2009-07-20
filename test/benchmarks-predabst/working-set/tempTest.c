int main(void){
  
  int a;
  int b;
  int c;
 
  a = 5;
  b = a + 1;

  if(a == 89){
    b++;
  }

  c = b;
  a = c - 1;
  
  if(a == 4){
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
