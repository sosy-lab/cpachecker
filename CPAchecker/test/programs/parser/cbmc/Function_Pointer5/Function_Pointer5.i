# 1 "Function_Pointer5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function_Pointer5/main.c"


void signal1(int, void (*)(int));
void signal2(int, void (*h)(int));

void handler(int x)
{
}

int main()
{
  signal1(1, handler);
  signal2(2, handler);
}
