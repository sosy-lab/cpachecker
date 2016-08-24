# 1 "Lvalue1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Lvalue1/main.c"
void *stat()
{
}

void *lstat()
{
}

int main()
{
 int link_p;
 (void*(*)(const char*,void*))&(*(link_p ? &stat : &lstat));
  &(*(&lstat));
  const char *f=&(__FUNCTION__[2]);
  char *p=&(char){':'};
}
