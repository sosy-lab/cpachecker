 int global;
 
 int h(int i) {
	 int j = h(i);
	 i++;
	 if (j > i) {
		 j = j - i;
	 } else {
		 j = i - j;
		 global++;
	 }
	 j = h(j);
	 j++;
	 return j;
 }
 
 int g(int i) {
	 int j = h(i);
	 i++;
	 if (j > i) {
		 j = j - i;
	 } else {
		 j = i - j;
	 }
	 j = h(j);
	 j++;
	 return j;
 }
 
 int f(int i) {
	 int j = g(i);
	 i++;
	 if (j > i) {
		 j = j - i;
	 } else {
		 j = i - j;
	 }
	 j = g(j);
	 j++;
	 return j;
 }

int ldv_main() {
	int b;
	f(b);
	intLock();
	f(b++);
	intUnlock();
}
