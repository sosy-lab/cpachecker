# 1 "Float-no-simp1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Float-no-simp1/main.c"
int main()
{
  unsigned int i, j;
  double d;

  i=100.0;
  d=i;
  j=d;
  assert(j==100);
}
