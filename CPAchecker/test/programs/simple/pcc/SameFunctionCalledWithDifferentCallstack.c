int f()
{
  return 2;
}

int f2(int p)
{
  int y;
  y=f();
  return y+p;
}

int f3(){
  int w;
  w= f2(0);
  return w;
}

int main(){
  int x;
  x= f2(0);
  x=f();
  int z;
  z=f3();
  if(z!=x){
    ERROR: return -1;
  }
  return 0;
}
