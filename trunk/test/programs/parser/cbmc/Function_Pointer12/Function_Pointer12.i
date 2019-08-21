# 1 "Function_Pointer12/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function_Pointer12/main.c"
typedef void (ft)();

void foo()
{
}

void zz(ft f1, ft *f2)
{
  assert(f1==foo);
  assert(f2==foo);
}

int main()
{

  zz(foo, foo);
}
