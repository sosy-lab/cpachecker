struct point {
  int a;
  int b;
};

int global;

int* f() {
  return &global;
}

int* memAlloc() {
  int *c;
  c = h();
  return c;
}
 
int ldv_main() {
  int *a, *b;
  /*struct point *A;
  A->x = 1;
  A->y = 2;*/
  b = f();
  a = memAlloc();
  b = (int *) a;
  *b = 1;
}