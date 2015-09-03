# 1 "Struct_Hierarchy1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Hierarchy1/main.c"
struct tag1 {
 struct tag2 {
  int f;
 } y;
} x;

int main() {
 x.y.f = 0;
}
