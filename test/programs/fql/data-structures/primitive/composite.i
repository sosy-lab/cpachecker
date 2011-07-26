# 1 "composite.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "composite.c"
void * CPAmalloc(int size);

struct inner{
  char x1;
  int x2;

};

struct label{
  int data;
  int data2;
  char test[300];
  int *pnt;
  struct inner in;
} ;

void test(struct label *tst){
  tst->data2+=4;
  tst->in.x1=22;
}


int main(){
  struct label name;
  int *pnt;
  pnt= &name;
  pnt++;
  *pnt =33;
  int x;
  struct inner *k;
  name.test[5]= 4;
  name.test[5]++;
  name.test[299]=44;
  name.test[299]++;
  if(name.test[5]>3){
    name.test[5]++;
  }
  int a;
  a=3;
  name.pnt = &x;
  *name.pnt= 44;
  x++;
  struct label *stpnt;

  stpnt = &name;
  stpnt->data =4;
  name.data++;

  stpnt = CPAmalloc(318);
  stpnt->data =28 + name.data;
  stpnt->data++;
  stpnt->test[5]=4;
  stpnt->data2=0;
  test(stpnt);
  stpnt->data2++;
  test(&name);
  name.data2++;
  name.in.x1 +=name.data2;


  k = &name.in;
  k->x2=k->x1 -2;

  return 0;
}
