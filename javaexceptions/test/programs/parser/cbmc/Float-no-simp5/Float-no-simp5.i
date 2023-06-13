# 1 "Float-no-simp5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Float-no-simp5/main.c"
int main()
{
  double a, b;

  union {
    double f;
    long long unsigned int i;
  } au, bu;

  au.f = a;
  bu.f = b;

  assert((au.i == bu.i) == __CPROVER_equal(a, b));
}
