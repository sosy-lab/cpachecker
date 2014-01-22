# 1 "Free2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Free2/main.c"
void *malloc(unsigned);
void free(void *);

int main()
{
  int *p=malloc(sizeof(int));
  int x;
  int i, y;
  i=y;

  if(i==4711) p=&x;


  free(p);
}
