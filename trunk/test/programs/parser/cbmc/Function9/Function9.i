# 1 "Function9/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function9/main.c"



unsigned short g;

inline void baz()
{
  unsigned short ignore;
  ignore=g;

  __CPROVER_assert(0, "KABOOM");
}

static void foo()
{
    baz();
}

static void bar()
{
    baz();
}

int main()
{
  g=0;
  foo();

  g=1;
  bar();

  return 0;
}
