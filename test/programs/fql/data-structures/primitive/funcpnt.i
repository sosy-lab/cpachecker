# 1 "funcpnt.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "funcpnt.c"
struct test{
  int (*pnt)(int,int);

  int v;
};

int test(a,b){
  return a+b;


}
int test2(a,b){
  return test(a,a+a);
}

int test3(int (*a)(int,int),int b, int c,int *d){
  return a(b,c)+*d;
}

int (*fpnt)(int,int);
int main(){
  struct test s;

  struct test *pnt;
  pnt = &s;

  fpnt = test;
  int a =3 ;
  int b =4 ;
  int x = fpnt(a,b);
  pnt->pnt = fpnt;
  pnt->v = pnt->pnt(a,b);
  pnt->v++;
  test2(a,b);
  pnt->v = test3(test2,b,33,&a);
  pnt->v++;
  pnt->v=test(33,44);
  return 0;

}
