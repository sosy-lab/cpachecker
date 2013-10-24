# 1 "Bitfields1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Bitfields1/main.c"
typedef int INT;

typedef enum _INTEL_CACHE_TYPE {
    IntelCacheNull,
    IntelCacheTrace=10
} INTEL_CACHE_TYPE;

struct bft {
  unsigned int a:3;
  unsigned int b:1;


  signed int :2;


  INT x:1;


  unsigned int abc: sizeof(int);


  INTEL_CACHE_TYPE Type : 5;


  INTEL_CACHE_TYPE Field2 : IntelCacheTrace;
};


int main() {
  struct bft bf;

  assert(bf.a<=7);
  assert(bf.b<=1);

  bf.Type=IntelCacheTrace;
}
