int d;

int main(void){
  
  int a;
  int b;
  int c;
  c = random();
  a = 5;
  b = 0;
  c = 4;
  

  while(1){
   if(c > 20){
     goto loopend;
   }
   else{ 
     //b++;
     c++;
   }
  }
  loopend:

  if(a != 5){
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
