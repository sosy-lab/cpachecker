int* test(int *a){
  *a= *a+2;
  return a;
}

int test2(int* aber){
  *aber =*aber+3;

}


int main(){
  int b=66;
  int* c;
  int *a;
  a=&b;
  c=test(a);

  b=b+*c;

  test2(&b);
  test2(c);
  b=b+4;


  return 0;
}
