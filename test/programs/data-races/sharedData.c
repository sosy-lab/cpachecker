//Now the tool can't understand, that in 49 line there is a shared data

struct point {
  int *x;
  int y;
};

struct line {
  struct point* start;
  struct point* finish;
};

int t;

extern void* malloc(int size);
struct point *f(int *m) {
  struct point *C = malloc(sizeof(struct point));
  *m = 10;
  C->x = m;
  C->y = 1;
  return C;
}

struct point* getStart(struct line *l) {
  struct point *r;
  if ( l != 0) {
    r = f(&t);
    l->start = r;
    return r;
  } else {
    return 0;
  }
}

int ldv_main() {
  int *a;
  int b;
  struct point* B;
  struct line *L2;
  
  L2 = malloc(sizeof(struct line));
 
  intLock();
  B = getStart(L2);
  intUnlock();
  if (L2->finish != 0) {
      b++;
  }
  (*(L2->start->x))--;
}
