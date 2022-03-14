# 1 "Pointer_Arithmetic2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic2/main.c"
int my_array[100];

int main()
{
  int *p=my_array;
  char *q;
  int diff;

  q=(char *)p;
  q+=30*sizeof(int);
  p=(int *)q;

  *p=1;

  assert(my_array[30]==1);
}
