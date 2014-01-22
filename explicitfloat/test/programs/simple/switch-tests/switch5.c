#include <stdlib.h>

int main(void) {

int a = 1;
int b = 1;

if (1==a) {
  goto gotolabel;
}

switch (a) {
  case 1 :
    b = 3;
    break;

  default :
    b = 3;
    break;

  case 2 :
   gotolabel:
    b = 5;
    break;
}

return (0);
}
