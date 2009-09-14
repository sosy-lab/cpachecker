#define  __attribute__(x) /*NOTHING*/
#include <assert.h>

void f1() {
    int x;
    int *p= &x;
    assert(p != 0); /* Blast says "safe" */
}

void f() {
  int array[1];
  int *p= &array[0]; /* Blast considers that p can have NULL value*/
  assert(p != 0); /* Blast says "Error" */
}

void g() {
  int array[1],i,j;
  i=0; array[i]=1;
  j=0; assert(array[j]==1);/* Blast says "Error"*/
}

void main(void) {
  f1();
  f();
  g();
}

