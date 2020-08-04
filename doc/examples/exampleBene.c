int main() {
  int i = 20;
  int a = 0;
  int b = 0;
  int c = 1;
  while (a < i) {
       a = a + 1;
       b = a;
       c = 0;
       int x = c;
	
       test(x);

    }


  while (a < 22) {
       if(b == 1 && a == 11 && c == 0){}
       a = a + 1;
       b = 1;
  }
 
 while (a < 22 && b < 1) {
       if(b == 1 && a == 11 && c == 0){}
       a = a + 1;
       b = 1;
  }

  while (a < 22 || b < 1) {
       if(b == 1 || a == 11 || c == 0){}
       a = a + 1;
       b = 1;
  }

  if (i < a) {
     goto ERROR;
  }

  for(int tmp = 0; tmp < a || tmp > b; tmp++){
       if(b == 1 || a == 11 || c == 0){}
	a = 10;
  }

  a = 10;

  return (0);
  ERROR:
  return (-1);
}

int test(int x){
int y = x;
while(y > 0){
return y;
}
return(0);
}

