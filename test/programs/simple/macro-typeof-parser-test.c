void main(char *opts, ...) {
  void **optarg;
  __builtin_va_list p;
  __builtin_va_start(p,opts);
  optarg=__builtin_va_arg(p,__typeof__(optarg));
  __builtin_va_end(p);
}
