// Test with always-true conditions that should generate test cases
int main() {
    int x = 1;    // Fixed value
    int y = 2;    // Fixed value
    int z = 0;    // Fixed value
    
    // always T
    if (x < y) { 
        // target 1 - always true 
    }
    
    if (x != y) { 
        // target 2 - always true 
    }
    
    if (x == 1) { 
        // target 3 - always true 
    }
    
    if (y > 0) { 
        // target 4 - always true 
    }
    
    if (z == 0) {
        // target 5 - always true
    }
    
    
    if (x > 0) {
        if (y > x) {
            // target 6 - always true
        }
    }
    
    return 0;
}
