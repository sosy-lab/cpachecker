# 1 "Function1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function1/main.c"
int f(int a) {
 return a + 1;
}


int f(int);

int a[1];

int main() {
 int x, y;

 a[0] = y;
 a[0] = f(a[0]);

 assert(a[0] == y+1);
}
