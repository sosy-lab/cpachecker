# 1 "enum3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "enum3/main.c"
enum ts { Ax, Bx, Cx=(Bx<<1)>>1 };

int main(void)
{
  enum ts token;

  if(token!=Bx) token=Bx;

  assert(token==Cx);

  return 1;
}
