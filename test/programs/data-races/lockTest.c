/* The main aim of this test is to check handling of variable links
 */

struct testStruct {
  int a;
  int b;
} *s;

int t, p;

struct testStruct *s1;

//Check disjoint sets
int f(int a)
{
  int *c = &t;
  intLock();
  *c = 2;
  kernDispatchDisable();
  *c = 4;
  intUnlock();
  *c = 3;
  kernDispatchEnable();
  return 0;
}

int ldv_main() 
{
  int a;
  int q = 1;
  int* temp;
  int* temp2;
  
  f(0);
  
  //Check links
  q = *temp;
  if (q == 1) {
    intLock();
    temp = sdlFirst(&(s->a));
    intUnlock();
  } 
  temp2 = sdlFirst(temp);
  temp2 = sdlFirst(temp2);
  //Important: there two links possible: temp and s->a
  *temp2 = 1;
  
  //Check parameter locks
  queLOCK(a);
  p = 1;
  queUNLOCK(q);  
  p = 2;
}
