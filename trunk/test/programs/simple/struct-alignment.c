enum e { e };
struct list_head {
   struct list_head *next ;
   struct list_head *prev ;
};
struct s {
  enum e e;
  struct list_head list;
  int x;
};

void main() {
  struct s s = {0, 1};
  if (s.x) {
ERROR:
    return;
  }
}
