# 1 "gcc_statement_expression3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "gcc_statement_expression3/main.c"
int state;

int doassert()
{
  assert(state == 3);
  return 0;
}

int main() {
  int x;

          (
    {
      state = 3;
      doassert();
    }
    );

  return 0;

}
