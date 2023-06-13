# 1 "gcc_types_compatible_p1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "gcc_types_compatible_p1/main.c"



int i;
double d;

typedef enum T1 { hot, dog, poo, bear } dingos;
typedef enum T2 { janette, laura, amanda } cranberry;

typedef float same1;
typedef float same2;

dingos _dingos;
cranberry _cranberry;





int some_array[(__builtin_types_compatible_p(int, const int)) ? 1 : -1];;
int some_array[(__builtin_types_compatible_p(typeof (hot), int)) ? 1 : -1];;
int some_array[(__builtin_types_compatible_p(typeof (hot), typeof (laura))) ? 1 : -1];;
int some_array[(__builtin_types_compatible_p(int[5], int[])) ? 1 : -1];;
int some_array[(__builtin_types_compatible_p(same1, same2)) ? 1 : -1];;
int some_array[(__builtin_types_compatible_p(typeof (hot) *, int *)) ? 1 : -1];;


int some_array[(!__builtin_types_compatible_p(char *, int)) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(char *, const char *)) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(const char *, char *)) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(long double, double)) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(typeof (i), typeof (d))) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(typeof (dingos), typeof (cranberry))) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(typeof (_dingos), typeof (_cranberry))) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(char, int)) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(char *, char **)) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(typeof (hot), unsigned int)) ? 1 : -1];;
int some_array[(!__builtin_types_compatible_p(int[], int *)) ? 1 : -1];;



int main (void)
{
}
