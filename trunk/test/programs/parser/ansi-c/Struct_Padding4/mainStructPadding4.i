# 1 "Struct_Padding4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Padding4/main.c"



struct Z1
{
  char ch;
  int i:3;
  char ch2;

} z1;

struct Z2
{
  char ch;
  char i:3;
  char ch2;

} z2;

struct Z3
{
  char ch;
  int i:3;
} z3;

struct Z4
{
  int i;
  long long int x;
  char ch;
} z4;

struct Z5
{
  char ch;
  long long int x[];
} z5;

int some_array__LINE__[(sizeof(struct Z1)==1+1+1+1) ? 1 : -1];


int some_array__LINE__[(__builtin_offsetof(struct Z1, ch2)==2) ? 1 : -1];


int some_array__LINE__[(sizeof(struct Z2)==1+1+1) ? 1 : -1];
int some_array__LINE__[(sizeof(struct Z3)==1+1+2) ? 1 : -1];
int some_array__LINE__[(sizeof(struct Z4)==4+4+8+1+7) ? 1 : -1];
int some_array__LINE__[(sizeof(struct Z5)==8) ? 1 : -1];

int main()
{
}
