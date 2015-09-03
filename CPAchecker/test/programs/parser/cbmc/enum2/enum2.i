# 1 "enum2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "enum2/main.c"
enum flags { FLAG1, FLAG2, FLAG3, FLAG4 };

enum bool {false, true};
enum bool skipping;

int main()
{
  int height = (1 << FLAG4);
  assert(8 == height);


  skipping = 2 >= 1;
  assert(skipping);


  enum { FOO = 1 == 1 };
  enum { BAR = 1 == 0 };
  assert(FOO==1);
  assert(BAR==0);

  return 0;
}
