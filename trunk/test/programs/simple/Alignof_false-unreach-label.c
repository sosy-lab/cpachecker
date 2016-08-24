typedef long intptr_t;
struct my_struct {
   struct my_struct *p ;
   int a ;
   int b ;
};

int main() {
    struct my_struct *tl ;

    if (((unsigned long )((intptr_t )tl) & (__alignof__(tl) - 1UL)) == 0UL) {
ERROR:
      return 1;
    } else {
      return 0;
    }
}
