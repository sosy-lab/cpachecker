# 1 "Pointer_Arithmetic4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic4/main.c"
int a[]={ 10, 20, 30 };
int *p, z;

int main()
{
  p=a-1;
  z=p[2];
  assert(z==20);
}
