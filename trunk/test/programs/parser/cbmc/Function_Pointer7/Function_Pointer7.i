# 1 "Function_Pointer7/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function_Pointer7/main.c"
struct file_ops {
  int (*open)(int);
};

struct dev {
  struct file_ops *ops;
};

struct dev *devs;

int my_open(int a)
{
  return a;
}

struct file_ops fops = { .open = my_open };

int main(void)
{
  struct dev tmp[4];

  devs = &tmp;

  (devs+0)->ops = &fops;

  assert(10 == (* devs[0].ops->open)(10));

  return 0;
}
