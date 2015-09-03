# 1 "asm2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "asm2/main.c"


int main()
{
  int x, y;

  asm goto ("jc %l[error];"
      : : "r"(x), "r"(&y) : "memory" : error);

error:
  return 0;
}
