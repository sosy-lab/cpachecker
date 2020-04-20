typedef float F;

typedef union _U {
   F y;
} U;

void main() {
   U x;
   F a = 0.0f;
   x.y = -a;
ERROR:
   return;
}
