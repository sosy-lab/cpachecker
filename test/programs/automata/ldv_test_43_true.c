/* Test file for automaton cpa. Use with automaton spcifications
 * 43_1a_fixed.spc, 43_1a.spc */
void spin_lock() {}
void spin_unlock() {}
void spin_trylock() {}

void * alloc_atomic(int flags)
{
  void * ret;
  return ret;
}

void * alloc_nonatomic()
{
  void * ret;
  return ret;
}

void main()
{ 
  spin_lock();
  alloc_atomic(2);
  spin_unlock();
}

