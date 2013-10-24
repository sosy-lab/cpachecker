# 1 "Function4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function4/main.c"
int nondet_int();

int f1()
{
  int ret;
  return ret;
}

typedef struct {
  int x;
} Str1;

int main()
{
  Str1 st;
  int x;

  st.x = nondet_int();
  st.x = f1();
  x = nondet_int();
  st.x = x;

  return 0;
}
