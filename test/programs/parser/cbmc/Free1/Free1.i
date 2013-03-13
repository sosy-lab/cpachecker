# 1 "Free1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Free1/main.c"
void *malloc(unsigned);
void free(void *);

int main()
{
  int *p=malloc(sizeof(int));
  int *q=p;
  int i, x;
  i=x;

  if(i==4711) free(q);


  *p=1;
}
