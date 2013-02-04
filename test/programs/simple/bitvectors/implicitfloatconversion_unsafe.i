# 1 "./implicitfloatconversion_unsafe.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "./implicitfloatconversion_unsafe.c"
int main() {
  float f = 1;
  int i = f;
  if (i == 1){
    goto ERROR;
  }
  return (0);
  ERROR:
  return (-1);
}
