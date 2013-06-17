# 1 "Linking1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Linking1/main.c"


struct
{
  int abc;
};

# 1 "Linking1/module.h" 1
void f();

typedef struct { int asd; } anon_struct;

extern anon_struct a_struct;
# 9 "Linking1/main.c" 2

anon_struct a_struct;

int i;

int main()
{
  assert(i==1);
  assert(a_struct.asd==0);

  f();

  assert(i==2);
  assert(a_struct.asd==123);
}
