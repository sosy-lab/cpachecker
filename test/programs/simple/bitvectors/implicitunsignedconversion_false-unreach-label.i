# 1 "./implicitunsignedconversion_unsafe.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "./implicitunsignedconversion_unsafe.c"
int main() {
  unsigned int plus_one = 1;
  int minus_one = -1;

  if(plus_one < minus_one) {
    goto ERROR;
  }

  return (0);
  ERROR:
  return (-1);
}
