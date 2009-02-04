int GLOBAL;

int main(void) 
{
  int LOCAL;
  int i;

  {
    GLOBAL = 0;
    LOCAL = 0;
    i = GLOBAL; // i = LOCAL; would make it work
    if (i == 1) {
      if (i == 1) {
      } else {
      }
    } else {

    }
  }
}
