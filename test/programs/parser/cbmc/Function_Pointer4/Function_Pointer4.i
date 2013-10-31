# 1 "Function_Pointer4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function_Pointer4/main.c"
struct device {
  int (*func)();
};

int one()
{
  return 1;
}

int main(void)
{
  struct device devices[1];
  int x;

  devices[0].func = one;

  x=(* devices[0].func)();
  assert(x == 1);
}
