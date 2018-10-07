int main() {
  const char * arr[2] = {"a","b"};
  if (arr[0][0] != 'a') return 0;
  if (arr[0][1] != 0) return 0;
  if (arr[1][0] != 'b') return 0;
  if (arr[1][1] != 0) return 0;
  ERROR:
  return 1;
}
