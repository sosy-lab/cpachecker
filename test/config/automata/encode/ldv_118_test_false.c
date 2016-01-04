int _nondet_int() { int val; return val; } 

extern int ldv_read_lock(void* p);
extern int ldv_read_unlock(void* p);
extern int ldv_write_lock(void* p);
extern int ldv_write_unlock(void* p);

int main() {
    
    void* p = (void*) 0;

    while(1) {
        int a = _nondet_int();
        int b = _nondet_int();

        if (a) {
            void* l1 = (void*) 0;
            ldv_read_lock(l1);
            ldv_read_unlock(l1);
        } else if (b) {
            void* l2 = (void*) 0;
            ldv_read_lock(l2);
            ldv_read_unlock(l2);
            ldv_read_unlock(l2);
        }

        void* l3 = (void*) 0;
        ldv_read_lock(l3);
        ldv_read_unlock(l3);
    }


    return 0;
}
