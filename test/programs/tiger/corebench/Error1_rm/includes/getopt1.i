# 1 "lib/getopt1.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 31 "<command-line>"
# 1 "/usr/include/stdc-predef.h" 1 3 4
# 32 "<command-line>" 2
# 1 "lib/getopt1.c"
# 21 "lib/getopt1.c"
# 1 "lib/config.h" 1
# 22 "lib/getopt1.c" 2


# 1 "/usr/include/getopt.h" 1 3 4
# 24 "/usr/include/getopt.h" 3 4
# 1 "/usr/include/features.h" 1 3 4
# 424 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 1 3 4
# 427 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/wordsize.h" 1 3 4
# 428 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 2 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/long-double.h" 1 3 4
# 429 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 2 3 4
# 425 "/usr/include/features.h" 2 3 4
# 448 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 1 3 4
# 10 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs-64.h" 1 3 4
# 11 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 2 3 4
# 449 "/usr/include/features.h" 2 3 4
# 25 "/usr/include/getopt.h" 2 3 4
# 35 "/usr/include/getopt.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/getopt_core.h" 1 3 4
# 28 "/usr/include/x86_64-linux-gnu/bits/getopt_core.h" 3 4









# 36 "/usr/include/x86_64-linux-gnu/bits/getopt_core.h" 3 4
extern char *optarg;
# 50 "/usr/include/x86_64-linux-gnu/bits/getopt_core.h" 3 4
extern int optind;




extern int opterr;



extern int optopt;
# 91 "/usr/include/x86_64-linux-gnu/bits/getopt_core.h" 3 4
extern int getopt (int ___argc, char *const *___argv, const char *__shortopts)
       __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (2, 3)));


# 36 "/usr/include/getopt.h" 2 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/getopt_ext.h" 1 3 4
# 27 "/usr/include/x86_64-linux-gnu/bits/getopt_ext.h" 3 4

# 50 "/usr/include/x86_64-linux-gnu/bits/getopt_ext.h" 3 4
struct option
{
  const char *name;


  int has_arg;
  int *flag;
  int val;
};







extern int getopt_long (int ___argc, char *const *___argv,
   const char *__shortopts,
          const struct option *__longopts, int *__longind)
       __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (2, 3)));
extern int getopt_long_only (int ___argc, char *const *___argv,
        const char *__shortopts,
               const struct option *__longopts, int *__longind)
       __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (2, 3)));


# 37 "/usr/include/getopt.h" 2 3 4
# 25 "lib/getopt1.c" 2
# 1 "lib/getopt_int.h" 1
# 25 "lib/getopt_int.h"

# 25 "lib/getopt_int.h"
extern int _getopt_internal (int ___argc, char **___argv,
        const char *__shortopts,
        const struct option *__longopts, int *__longind,
        int __long_only, int __posixly_correct);
# 55 "lib/getopt_int.h"
enum __ord
  {
    REQUIRE_ORDER, PERMUTE, RETURN_IN_ORDER
  };


struct _getopt_data
{



  int optind;
  int opterr;
  int optopt;
  char *optarg;




  int __initialized;







  char *__nextchar;


  enum __ord __ordering;







  int __first_nonopt;
  int __last_nonopt;
};





extern int _getopt_internal_r (int ___argc, char **___argv,
          const char *__shortopts,
          const struct option *__longopts, int *__longind,
          int __long_only, struct _getopt_data *__data,
          int __posixly_correct);

extern int _getopt_long_r (int ___argc, char **___argv,
      const char *__shortopts,
      const struct option *__longopts, int *__longind,
      struct _getopt_data *__data);

extern int _getopt_long_only_r (int ___argc, char **___argv,
    const char *__shortopts,
    const struct option *__longopts,
    int *__longind,
    struct _getopt_data *__data);
# 26 "lib/getopt1.c" 2

int
getopt_long (int argc, char *
# 28 "lib/getopt1.c" 3 4
                            const 
# 28 "lib/getopt1.c"
                                                *argv, const char *options,
      const struct option *long_options, int *opt_index)
{
  return _getopt_internal (argc, (char **) argv, options, long_options,
      opt_index, 0, 0);
}

int
_getopt_long_r (int argc, char **argv, const char *options,
  const struct option *long_options, int *opt_index,
  struct _getopt_data *d)
{
  return _getopt_internal_r (argc, argv, options, long_options, opt_index,
        0, d, 0);
}






int
getopt_long_only (int argc, char *
# 50 "lib/getopt1.c" 3 4
                                 const 
# 50 "lib/getopt1.c"
                                                     *argv,
    const char *options,
    const struct option *long_options, int *opt_index)
{
  return _getopt_internal (argc, (char **) argv, options, long_options,
      opt_index, 1, 0);
}

int
_getopt_long_only_r (int argc, char **argv, const char *options,
       const struct option *long_options, int *opt_index,
       struct _getopt_data *d)
{
  return _getopt_internal_r (argc, argv, options, long_options, opt_index,
        1, d, 0);
}
