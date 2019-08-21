int main() {
  int i = 0;
  while (i != 4) {
    i++;
  }
  if (i != 4) {
    goto ERROR;
  }
  return (0);
  ERROR:
  return (-1);
}
