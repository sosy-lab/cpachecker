# 1 "_Static_assert1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "_Static_assert1/main.c"
struct S
{
  _Static_assert(1, "in struct");
  int x;
} asd;

_Static_assert(1, "global scope");

int main()
{
  _Static_assert(1, "in function");
}
