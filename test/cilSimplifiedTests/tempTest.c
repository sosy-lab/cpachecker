int d;

int main(void){
  
  int a;
  int b;
  int c;
  c = random();
  a = 5;
  b = 6;
  
  if(b == 6){
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
