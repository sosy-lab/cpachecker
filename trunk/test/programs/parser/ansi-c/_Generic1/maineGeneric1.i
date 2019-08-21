# 1 "_Generic1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "_Generic1/main.c"
# 13 "_Generic1/main.c"
struct some
{
} s;

int i;
char ch;
long double ld;
short sh;


int some_array__LINE__[(_Generic((i), long double: 1, default: 10, float: 2, int: 3, char: 4, struct some: 5 )==3) ? 1 : -1];;
int some_array__LINE__[(_Generic((sh), long double: 1, default: 10, float: 2, int: 3, char: 4, struct some: 5 )==10) ? 1 : -1];;
int some_array__LINE__[(_Generic((ld), long double: 1, default: 10, float: 2, int: 3, char: 4, struct some: 5 )==1) ? 1 : -1];;
int some_array__LINE__[(_Generic((ch), long double: 1, default: 10, float: 2, int: 3, char: 4, struct some: 5 )==4) ? 1 : -1];;
int some_array__LINE__[(_Generic((s), long double: 1, default: 10, float: 2, int: 3, char: 4, struct some: 5 )==5) ? 1 : -1];;






int main()
{
}
