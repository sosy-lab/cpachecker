// unused inside expression list with side effects

int main() {
  int a = 5;
  (
    4,
    ({a++; int c = ++a;})
  );

  if(a != 7) {
    ERROR: // unreachable
      return 1;
  }

  return 0;
}
