# 1 "Float8/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Float8/main.c"
int main()
{
  double d, q, r;
  __CPROVER_assume(__CPROVER_isfinite(q));
  d=q;
  r=d+0;
  assert(r==d);
}
