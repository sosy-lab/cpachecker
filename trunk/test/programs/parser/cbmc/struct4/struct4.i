# 1 "struct4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "struct4/main.c"
void assert(_Bool cond);
# 13 "struct4/main.c"
typedef struct str1 {
  short x;
  struct str2* p2;
} Str1;

typedef struct str2 {
  short y;
  struct str1* p1;
} Str2;

int main()
{
  Str1 st1;
  Str2 st2 = { 1234, &st1 };

  assert( st2.y == 1234 );

  return 0;
}
