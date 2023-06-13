# 1 "gcc_vector1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "gcc_vector1/main.c"
# 1 "/usr/include/assert.h" 1 3 4
# 37 "/usr/include/assert.h" 3 4
# 1 "/usr/include/features.h" 1 3 4
# 324 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/predefs.h" 1 3 4
# 325 "/usr/include/features.h" 2 3 4
# 357 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 1 3 4
# 378 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/wordsize.h" 1 3 4
# 379 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 2 3 4
# 358 "/usr/include/features.h" 2 3 4
# 389 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 1 3 4



# 1 "/usr/include/x86_64-linux-gnu/bits/wordsize.h" 1 3 4
# 5 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 2 3 4




# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs-64.h" 1 3 4
# 10 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 2 3 4
# 390 "/usr/include/features.h" 2 3 4
# 38 "/usr/include/assert.h" 2 3 4
# 68 "/usr/include/assert.h" 3 4



extern void __assert_fail (__const char *__assertion, __const char *__file,
      unsigned int __line, __const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));


extern void __assert_perror_fail (int __errnum, __const char *__file,
      unsigned int __line,
      __const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));




extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));



# 2 "gcc_vector1/main.c" 2


typedef int base_type;

typedef base_type v4si __attribute__((vector_size(16)));

typedef union
{
  v4si v;
  base_type members[4];
} vector_u;

int main()
{
  ((sizeof(int)==4) ? (void) (0) : __assert_fail ("sizeof(int)==4", "gcc_vector1/main.c", 16, __PRETTY_FUNCTION__));
  ((sizeof(v4si)==16) ? (void) (0) : __assert_fail ("sizeof(v4si)==16", "gcc_vector1/main.c", 17, __PRETTY_FUNCTION__));

  vector_u x, y, z;

  z.v=x.v+y.v;

  ((z.members[0]==x.members[0]+y.members[0]) ? (void) (0) : __assert_fail ("z.members[0]==x.members[0]+y.members[0]", "gcc_vector1/main.c", 23, __PRETTY_FUNCTION__));
  ((z.members[1]==x.members[1]+y.members[1]) ? (void) (0) : __assert_fail ("z.members[1]==x.members[1]+y.members[1]", "gcc_vector1/main.c", 24, __PRETTY_FUNCTION__));
  ((z.members[2]==x.members[2]+y.members[2]) ? (void) (0) : __assert_fail ("z.members[2]==x.members[2]+y.members[2]", "gcc_vector1/main.c", 25, __PRETTY_FUNCTION__));
  ((z.members[3]==x.members[3]+y.members[3]) ? (void) (0) : __assert_fail ("z.members[3]==x.members[3]+y.members[3]", "gcc_vector1/main.c", 26, __PRETTY_FUNCTION__));

  z.v=x.v-y.v;

  ((z.members[0]==x.members[0]-y.members[0]) ? (void) (0) : __assert_fail ("z.members[0]==x.members[0]-y.members[0]", "gcc_vector1/main.c", 30, __PRETTY_FUNCTION__));
  ((z.members[1]==x.members[1]-y.members[1]) ? (void) (0) : __assert_fail ("z.members[1]==x.members[1]-y.members[1]", "gcc_vector1/main.c", 31, __PRETTY_FUNCTION__));
  ((z.members[2]==x.members[2]-y.members[2]) ? (void) (0) : __assert_fail ("z.members[2]==x.members[2]-y.members[2]", "gcc_vector1/main.c", 32, __PRETTY_FUNCTION__));
  ((z.members[3]==x.members[3]-y.members[3]) ? (void) (0) : __assert_fail ("z.members[3]==x.members[3]-y.members[3]", "gcc_vector1/main.c", 33, __PRETTY_FUNCTION__));

  z.v=-x.v;

  ((z.members[0]==-x.members[0]) ? (void) (0) : __assert_fail ("z.members[0]==-x.members[0]", "gcc_vector1/main.c", 37, __PRETTY_FUNCTION__));
  ((z.members[1]==-x.members[1]) ? (void) (0) : __assert_fail ("z.members[1]==-x.members[1]", "gcc_vector1/main.c", 38, __PRETTY_FUNCTION__));
  ((z.members[2]==-x.members[2]) ? (void) (0) : __assert_fail ("z.members[2]==-x.members[2]", "gcc_vector1/main.c", 39, __PRETTY_FUNCTION__));
  ((z.members[3]==-x.members[3]) ? (void) (0) : __assert_fail ("z.members[3]==-x.members[3]", "gcc_vector1/main.c", 40, __PRETTY_FUNCTION__));

  z.v=~x.v;

  ((z.members[0]==~x.members[0]) ? (void) (0) : __assert_fail ("z.members[0]==~x.members[0]", "gcc_vector1/main.c", 44, __PRETTY_FUNCTION__));
  ((z.members[1]==~x.members[1]) ? (void) (0) : __assert_fail ("z.members[1]==~x.members[1]", "gcc_vector1/main.c", 45, __PRETTY_FUNCTION__));
  ((z.members[2]==~x.members[2]) ? (void) (0) : __assert_fail ("z.members[2]==~x.members[2]", "gcc_vector1/main.c", 46, __PRETTY_FUNCTION__));
  ((z.members[3]==~x.members[3]) ? (void) (0) : __assert_fail ("z.members[3]==~x.members[3]", "gcc_vector1/main.c", 47, __PRETTY_FUNCTION__));


  z.v=(v4si){ 0, 1, 2, 3 };
  ((z.members[0]==0 && z.members[1]==1 && z.members[2]==2 && z.members[3]==3) ? (void) (0) : __assert_fail ("z.members[0]==0 && z.members[1]==1 && z.members[2]==2 && z.members[3]==3", "gcc_vector1/main.c", 51, __PRETTY_FUNCTION__));


  v4si some_vector={ 10, 11, 12, 13 };
  z.v=some_vector;
  ((z.members[0]==10 && z.members[1]==11 && z.members[2]==12 && z.members[3]==13) ? (void) (0) : __assert_fail ("z.members[0]==10 && z.members[1]==11 && z.members[2]==12 && z.members[3]==13", "gcc_vector1/main.c", 56, __PRETTY_FUNCTION__));


  v4si other_vector={ 0 };
  z.v=other_vector;


  v4si image[] = { other_vector };

  ((z.members[1]==0) ? (void) (0) : __assert_fail ("z.members[1]==0", "gcc_vector1/main.c", 65, __PRETTY_FUNCTION__));
}
