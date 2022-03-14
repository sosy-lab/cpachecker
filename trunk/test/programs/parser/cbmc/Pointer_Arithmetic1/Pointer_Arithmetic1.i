# 1 "Pointer_Arithmetic1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic1/main.c"
int array[100];

int main()
{
  int *p=array;
  int diff;

  p+=30;
  diff=(char *)p-(char *)array;

  assert(diff==30*sizeof(int));
}
