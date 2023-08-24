extern void reach_error(); 


int main() { 
    int a[] = {1, 2, 3, 4, 5}; 
    int sum = 0;
    for (int i = 0; i <= 4; i++) {
        sum += a[i]; 
    }
    
    if (sum != 15) { reach_error(); } 
}