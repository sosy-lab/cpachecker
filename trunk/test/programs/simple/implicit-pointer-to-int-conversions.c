typedef long unsigned int size_t;
extern void *malloc (size_t __size);
extern int printf (__const char *__restrict __format, ...);

int main() {
  char *p1 = malloc(16);
  char* p2 = p1 + 4;
  int i;
  long l;

  printf("Sizeof int: %ld\n", sizeof(i));
  printf("Sizeof ptrdiff_t: %ld\n", sizeof(p1 - p2));

  i = p2 - p1; // On 64bit, this conversion looses precision!

  printf("i: %d\n", i);
  printf("p1: %p\n", p1);
  printf("p2: %p\n", p2);
  printf("p1-p2: %ld\n", p2-p1);

  i = p2; // On 64bit, this conversion looses precision!
  l = p2;
  printf("i: %d\n", i);
  printf("l: %ld\n", l);
  return 0;
}
