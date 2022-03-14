# 1 "Struct_Padding5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Padding5/main.c"
# 16 "Struct_Padding5/main.c"
struct flowi
{
  char ch;
  char flexible[];
} __attribute__((__aligned__(64/8)));




int some_array__LINE__[(sizeof(struct flowi)==8) ? 1 : -1];


int some_array__LINE__[(__builtin_offsetof(struct flowi, flexible)==1) ? 1 : -1];


int main()
{
}
