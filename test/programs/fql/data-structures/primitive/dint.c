

signed int c;



int main(){
  short x;
  c =-503;
  c=-878+7;
  c=200+c;
  signed int b;
  x = (short) c;

  b= c;

  signed int *test;
  test = &b;
  
  signed int **v;
  signed int ***z;
  v = & test;
  **v = sizeof(c);
  z =&v;
  ***z +=10;
  return 0;
}

