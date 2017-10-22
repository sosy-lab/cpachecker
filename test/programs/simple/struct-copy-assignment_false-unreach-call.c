extern void* malloc(unsigned int);
struct s {
  int x;
  int y;
  char c[1];
};
void main() {
  struct s s1 = {1,2};
  struct s *p = malloc(sizeof(struct s));
  p->x = 0;
  p->y = 0;
  *p = s1;
  if (s1.x == p->x && s1.y == p->y) {
ERROR:
    return;
  }
}
