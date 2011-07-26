


int main(){


  int x;
  x=3;
  x= 3||0;
  x++;
  x=x||4;
  x++;
  int y =0;
  x=0;
  x= x||y;
  x++;
  x=y||x;
  x++;
  int *pnt;
  pnt =&x;
  x = 0||pnt;
  int z;
  z = x &&pnt;
  z=z+1;
  x++;


  return 0;
}
