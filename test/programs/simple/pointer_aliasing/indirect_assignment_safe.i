# 1 "./indirect_assignment.SAFE.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "./indirect_assignment.SAFE.c"
void test(int x) {
 if (!x) {
ERROR: goto ERROR;
 }
}

void minimal() {
 int a = 123;
 int* p = &a;

 test(*p == 123);

 *p = 50;
 test(a == 50);
 test(*p == 50);
 test(a == *p);
}

void minimal_failing_r6276() {
 int a = 123;
 int* p = &a;

 int** q = &p;

 *p = 50;
 test(a == 50);
 test(*p == 50);
 test(a == *p);
}

void main() {
 minimal();
 minimal_failing_r6276();
}
