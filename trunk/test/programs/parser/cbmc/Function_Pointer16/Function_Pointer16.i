# 1 "Function_Pointer16/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function_Pointer16/main.c"
typedef signed char int8_t;

struct FIRST {
   int (*get)();
   int8_t (*isEmpty)(int8_t);
};

struct FIRST aVar;
struct FIRST anotherVar;

int aFun(int a) {
 return 0;
}

int main() {

   aVar.isEmpty = &aFun;
   int x = aVar.isEmpty(1);
}
