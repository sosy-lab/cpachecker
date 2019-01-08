extern int nondet();

int main() {

  int x = nondet();
  if(x > 0) {
    if(x < 0) {
      ERROR: return -1;
    }
  }

  return 0;
}