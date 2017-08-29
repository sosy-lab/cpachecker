typedef unsigned long size_t;

union my_union {
   char const   *nameptr ;
   char name[81] ;
};

struct my_struct {
   size_t my_size ;
   union my_union my_name ;
};

int main(struct my_struct const   *attr) {
  char const *name ;

  name = attr->my_size < (size_t const)sizeof(*attr) ? attr->my_name.nameptr : attr->my_name.name;
  if (name == 0) {
    return 0;
  }
ERROR:
  return 1;
}
