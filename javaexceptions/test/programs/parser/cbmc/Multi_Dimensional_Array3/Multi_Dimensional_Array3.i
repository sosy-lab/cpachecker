# 1 "Multi_Dimensional_Array3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Multi_Dimensional_Array3/main.c"
unsigned int nondet_uint();

typedef int *iptr;

int x, y, z;

int main()
{



  iptr array[3][3]={{&x,0,0},{&y,0,0},{&z,0,0}};

  unsigned int a, b;
  a = nondet_uint();
  b = nondet_uint();
  __CPROVER_assume (a < 3 && b < 3);

  array[a][b] = &z;

  iptr p;
  p=array[a][b];

  *p=1;

  assert(z==1);
}
