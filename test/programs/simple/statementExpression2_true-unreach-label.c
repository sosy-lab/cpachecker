// unused inside expression list, tests scoping

int main() {
  int a = 5;
  (
    4,
    ({int a = 42;})
  );

  if(a != 5) {
    ERROR: // unreachable
      return 1;
  }

  return 0;
}
