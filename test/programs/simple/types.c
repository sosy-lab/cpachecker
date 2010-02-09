#define  __attribute__(x)  /*NOTHING*/
#include <stdio.h>
#include <stdlib.h>

struct a;

struct a {
  char a1;
  int a2;
};
typedef struct {
  char a1;
  int a2;
} b;

typedef b *p;

const int const* const func(struct a *x, b *y)
//void func(struct a { char a1; long int a2; }* x, struct __anonstruct_b_22 { char a1; long int a2; }* y )
{
  x->a2 = 0;
  y->a2 = 0;
}

struct a *a;
enum e { e1 } e;

struct c {
  enum e e;
  int i;
};

struct test {
  int test;
};
typedef struct test test;
test testvar;

struct empty_struct { };
union empty_union { };

int main() {
  testvar.test = 42;
  char ch = 0x7F;
  printf("%hhd\n", ch+1);

  int ia[100];
  ia[99] = 42;

  int ia2[10];
  int ia3[1];

  int i1 = sizeof(char);
  int i2 = sizeof(short);
  int i3 = sizeof(int);
  int i4 = sizeof(long);
  int i5 = sizeof(long long);
  int i6 = sizeof(float);
  int i7 = sizeof(double);
  int i8 = sizeof(long double);
  int i9 = sizeof(char*);
  int i10 = sizeof(struct a);
  int i11 = sizeof(enum e);
  int i12 = sizeof(e);
  int i13 = sizeof(e1);
  int i14 = sizeof(void);
  int i15 = sizeof(ia2);
  int i16 = sizeof(ia3);
  int i17 = sizeof(main);
  int i18 = sizeof(struct empty_struct);
  int i19 = sizeof(union empty_union);

  printf("%d\n", i17);

  void *pv = malloc(1);
  printf("%d\n", ((int)(pv+1) - (int)pv));

  b *b;
  a->a2 = 0;
  b->a2 = 0;
  func(a, b);

  struct a c;
  c.a1 = 0;
  p p = &c;
  p->a2 = 0;
}
