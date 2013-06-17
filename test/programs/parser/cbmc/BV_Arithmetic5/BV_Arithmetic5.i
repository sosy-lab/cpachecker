# 1 "BV_Arithmetic5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "BV_Arithmetic5/main.c"
unsigned int test_log2(unsigned int v)
{
  unsigned c = 0;
  while (v >>= 1)
    {
      c++;
    }
  return c;
}

int main()
{
  int r;

  r=test_log2(128);
  assert(r==7);
}
