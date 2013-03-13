# 1 "Struct_Initialization3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Initialization3/main.c"
typedef struct
{
 int a;
} S;

int main(void)
{
  S s;
  S *var1=&s;
  S var2 = *var1;
}
