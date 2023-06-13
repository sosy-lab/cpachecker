# 1 "for1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "for1/main.c"
int main() {

  int i=0;

  for(;;)
  {
    i++;
    if(i==100) break;
  }

  assert(i==100);

  return 0;
}
