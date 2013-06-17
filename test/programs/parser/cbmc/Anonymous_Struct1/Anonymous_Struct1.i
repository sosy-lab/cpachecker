# 1 "Anonymous_Struct1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Anonymous_Struct1/main.c"
typedef struct {
    int field;
} MyStruct;


void f(MyStruct *s)
{
    int y = s->field;
}

int main() {
 MyStruct s;

 f(&s);
}
