//Test should check handling of conservative functions like netdev_priv
int *p;

struct A {
	int* a;
};

int* netdev_priv(int *a) {
    
}

int f(int* p) {
    //Not a race
    *p = 1;
} 

int main() {
    int *a = malloc(sizeof(int));
    int *b = netdev_priv(a);
    //Not a race
    *a = 1;
    //Not a race
    *b = 1;
    f(b);
}

int ldv_main() {
	main();
}
