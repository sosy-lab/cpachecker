# 1 "Struct_Padding3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Padding3/main.c"
# 10 "Struct_Padding3/main.c"
struct my_struct1a {
  char ch1;
  int i;
  char ch2;
} __attribute__((packed));

int some_array__LINE__[(__builtin_offsetof(struct my_struct1a, i)==1) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct1a, ch2)==5) ? 1 : -1];

struct my_struct1b {
  char ch1;

  int i __attribute__((packed));
  char ch2;
};

int some_array__LINE__[(__builtin_offsetof(struct my_struct1b, i)==1) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct1b, ch2)==5) ? 1 : -1];

struct my_struct1c {
  char ch1;

  struct
  {
    int i;
  } sub __attribute__((packed));
  char ch2;
};

int some_array__LINE__[(__builtin_offsetof(struct my_struct1c, sub.i)==1) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct1c, ch2)==5) ? 1 : -1];

struct my_struct1d {
  char ch0;

  struct
  {
    char ch1;
    int i;
  } sub __attribute__((packed));
  char ch2;
};

int some_array__LINE__[(__builtin_offsetof(struct my_struct1d, sub.ch1)==1) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct1d, sub.i)==5) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct1d, ch2)==9) ? 1 : -1];

struct __attribute__((packed)) my_struct2 {
  char ch1;
  int i;
  char ch2;
};

int some_array__LINE__[(__builtin_offsetof(struct my_struct2, i)==1) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct2, ch2)==5) ? 1 : -1];



struct my_struct3 {
  char ch1;
  char ch2;
  int i1;
  char ch3;
  long long i2;
};

int some_array__LINE__[(__builtin_offsetof(struct my_struct3, ch1)==0) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct3, ch2)==1) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct3, i1)==4) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct3, ch3)==8) ? 1 : -1];
int some_array__LINE__[(__builtin_offsetof(struct my_struct3, i2)==16) ? 1 : -1];

int main()
{
}
