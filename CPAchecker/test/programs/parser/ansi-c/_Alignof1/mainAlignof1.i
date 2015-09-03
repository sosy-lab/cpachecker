# 1 "_Alignof1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "_Alignof1/main.c"
# 22 "_Alignof1/main.c"
int f();

int some_array__LINE__[(_Alignof(char)==1) ? 1 : -1];;
int some_array__LINE__[(_Alignof(char[10])==1) ? 1 : -1];;


int some_array__LINE__[(_Alignof(char[f()])==1) ? 1 : -1];;


int main()
{
}
