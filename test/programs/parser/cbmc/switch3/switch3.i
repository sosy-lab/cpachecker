# 1 "switch3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "switch3/main.c"
char nondet_char();

int main()
{
  char ch=nondet_char();

  switch(ch)
  {
  case 'P':
  case 'p':
    assert(ch==80 || ch==112);
    break;

  default:
    assert(ch!=80 && ch!=112);
  }
}
