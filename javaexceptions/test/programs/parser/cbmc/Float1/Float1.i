# 1 "Float1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Float1/main.c"
int main() {
  double x;
  int y;

  x=2;
  x-=0.6;
  y=x;

  assert(y==1);

  x=2;
  x-=0.4;
  y=x;


  assert(y==1);
}
