typedef unsigned int size_t;
void * __attribute__((__cdecl__)) malloc (size_t __size) ;
int main() {
    char *s = malloc(3 * sizeof(char));
    char *p = s;
    s[2] = 0;
    while (*p != 0) {
      p++;
    }
    if (p - s > 2) {
      ERROR:
      return 1;
    }
}
