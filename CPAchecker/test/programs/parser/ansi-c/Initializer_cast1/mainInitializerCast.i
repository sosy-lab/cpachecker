# 1 "Initializer_cast1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Initializer_cast1/main.c"
struct S
{
  int a, b, c;
  struct T
  {
    int x, y, z;
  } sub;
};

union U
{
  int a;
  struct S s;
};

typedef int array_type[10];

int main()
{
  long l;
  struct S s;
  union U u;


  l=(long){0x1};


  s=(struct S){ 1, 2, 3, 4, 5, 6 };


  u=(union U)s;


  u=(union U){ 1 };


  const int *a=(array_type){ 1, 2, 3, 4 };
}
