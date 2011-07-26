# 1 "free.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "free.c"
union un{
  char x;
  int v;

};

int main(){
  int *d = CPAmalloc(4);

  *d=4;
  int **f;
  f=&d;
  CPAfree(*f);

  union un *data = CPAmalloc(4);
  data->v =777777;
  data->x=data->x +72;
}
