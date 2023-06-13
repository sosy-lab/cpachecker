# 1 "argv1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "argv1/main.c"
int main(int main_argc, char **main_argv)
{
  char *x;


  x=main_argv[0];
  assert(main_argc>=1);


  assert(main_argv[main_argc]==0);
}
