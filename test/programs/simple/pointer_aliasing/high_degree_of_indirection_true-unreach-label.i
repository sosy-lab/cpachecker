# 1 "./high_degree_of_indirection.SAFE.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "./high_degree_of_indirection.SAFE.c"

void test(int x) {
 if (!x) {
ERROR: goto ERROR;
 }
}


void main() {
 int x;
 int y;
 int* p1;
 int** p2;
 int*** p3;
 int* q1;
 int** q2;
 int*** q3;

 x = 674;
 p1 = &x;
 p2 = &p1;
 p3 = &p2;
 q3 = p3;
 q2 = *q3;
 q1 = *q2;
 y = *q1;
 test(x == y);
}
