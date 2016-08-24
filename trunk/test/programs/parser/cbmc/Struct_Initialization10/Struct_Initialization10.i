# 1 "Struct_Initialization10/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Initialization10/main.c"
typedef unsigned blue;

typedef struct { unsigned blue; } ar_t;
typedef struct { ar_t ar; } format_t;

int main () {

  format_t data = { .ar.blue = 1 };
  __CPROVER_assert(data.ar.blue==1, "initialization ok");
  return 1;
}
