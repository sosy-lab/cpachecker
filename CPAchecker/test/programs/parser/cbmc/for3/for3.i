# 1 "for3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "for3/main.c"
int x = 0;

_Bool f()
{
  x++;
  return x!=10;
}

int main() {
  int y =0;

  for( ; f(); ) {
    y++;
  }

  assert(y == 9);
  assert(x == 10);

}
