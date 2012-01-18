
void __blast_assert()
{
	ERROR: goto ERROR;
}

//#define assert(cond) do {if (!cond) __blast_assert();} while(0);
# define assert(expr)							\
  ((expr)								\
   ? (0) \
   : __blast_assert ())


