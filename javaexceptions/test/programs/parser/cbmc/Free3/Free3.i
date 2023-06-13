# 1 "Free3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Free3/main.c"
void *malloc(unsigned size);
void free(void *p);

int main()
{
  int *p;
  unsigned int n;

  p=malloc(sizeof(int)*10);

  free(p);


  free(p);
}
