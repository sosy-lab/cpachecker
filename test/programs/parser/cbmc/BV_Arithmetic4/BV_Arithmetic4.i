# 1 "BV_Arithmetic4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "BV_Arithmetic4/main.c"

int test_extend2(int x, unsigned width)
{
  int mask = 1 << (width-1);
  int bit = (x & mask) << 1;
  mask <<= 1;
  while (mask) {
    x = bit | (~mask & x);
    mask <<= 1;
    bit <<= 1;
  }
  return x;
}

int main()
{
  int r;

  r=test_extend2(4, 3);
  assert(r==-4);
}
