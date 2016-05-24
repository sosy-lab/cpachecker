extern void init();
extern void lock();
extern void unlock();

void main() {
  int n;
  init();
  lock();
  int lastLock = 0;
  int i = 1;
  while(i<n) 
  {
    if(i-lastLock == 2)
    {
      lock();
      lastLock = i;
    }
    else
    {
      unlock();
    }
    i=i+1;
  }
}