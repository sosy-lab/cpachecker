void main() {
  int i;
  // The nested "int i;" does not collide with "int i;" from main
  // (there was a bug that lead field declarations from struct declarations
  // inside statements be handled as variable declarations).
  i = sizeof(struct s { int i; });
}
