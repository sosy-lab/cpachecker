#include <stdlib.h>
/*int *a;
int **b;

int main() {
  int *l;
  *l = *a;
  (*l)++;
  a = malloc(8);
  l = 5 + a;
}*/

int* funcp(int* a) {
  a += 1;
  return a;
}

int funci() {
  return 1;
}

int* g;
int* h;

int* functest() {
  int local;
  g = &local;
  h = g;
  return &local;
}

int main() {
  int i = 1;
  int* l = malloc(-1);
  int **a = malloc(9*sizeof(int*));
  int **a_backup = a;
  if (!a) {
    return 1;
  }
  if (0 == a) {
    return 1;
  }
  *a = &a;
  a++;
  *a = &l;
  a += 1;
  *a = &g;
  a = a+1;
  *a = g;
  a = a+1;
  *a = l;
  a = a+i;
  a += funci();
  a = a + funci();
  a = funcp(a);
  *a = &l;
  functest();
//  h = *g;
  free(a);
  free(a_backup);
  *a = 0;
  return 0;
}
