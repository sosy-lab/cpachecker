int f1(){
  int x;
  x = f2();
  return 1+x;
}

int f2(){
  int x;
  x = f3();
  return 2+x;
}

int f3(){
  int x;
  x = f4();
  return 3+x;
}

int f4(){
  int x;
  x = f5();
  return 4+x;
}

int f5(){
  return 5;
}

int main()
{
  int x;
  x = f1();
  if(x!=15){
    ERROR: return -1;
  }
  return 0;
}
