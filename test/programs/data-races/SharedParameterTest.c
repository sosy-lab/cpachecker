//Test checks how the analysis may handle returning from function, which change sharedness of its parameter
//In this test there should be a race only on c
int global;

int f(int** a, int **b) {
  *a = &global;
  return 0;
}

int ldv_main() {
  int** c;
  int** d;
  c = malloc(sizeof(int*));
  *c = malloc(sizeof(int));
  d = malloc(sizeof(int*));
  *d = malloc(sizeof(int));
  
  f(c, d);
  //now it is global
  **c = 1;
  //still local
  **d = 2;
  
  //Second usages for races
  intLock();
  **c = 0;
  **d = 1;
  intUnlock();
}
