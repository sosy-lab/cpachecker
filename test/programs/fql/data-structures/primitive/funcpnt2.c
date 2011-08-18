

typedef int (*f)(int,int);

int test(int a, int b){
  return a-b;

}
int test2(int a, int b){
  return a+b;

}

int main(){
  f pnt[2];
  f* p;

  pnt[0] = test;
  pnt[0](3,4);
  pnt[1]=test2;
  pnt[1](3,9);
  p = &pnt[0];
  (**p)(3,4);

  
}



