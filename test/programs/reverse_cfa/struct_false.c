extern void reach_error(); 


struct st {
    int id; 
};


int main() 
{
    struct st s; 
    s.id = 1;
    for (int i = 1; i <= 10; i++) {
        s.id *= 2;
    }

    if (s.id == 1024) reach_error();
}