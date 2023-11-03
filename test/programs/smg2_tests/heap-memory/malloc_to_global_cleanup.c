typedef long unsigned int size_t;
extern void *malloc (size_t __size);

int *x = ((void *)0);
int main() {
  x = malloc(3);
  return 0;
}
