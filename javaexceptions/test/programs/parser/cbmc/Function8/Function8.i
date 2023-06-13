# 1 "Function8/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function8/main.c"



inline void baz(unsigned short ignore)
{

  __CPROVER_assert(0, "KABOOM");
}

static void foo()
{
  baz(1);
}

static void bar()
{
  baz(0);
}

int main()
{
  foo();

  bar();

  return 0;
}
