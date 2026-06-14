

int f(int x){

    if(x <= 0)

        return x + 1;

    else 
        
        return x -1;
}


int main(){

    int nodet;

    int x = nodet ? f(1) : f(0);
    
    if(nodet){
        f(1);
    } else {
        f(2);
    }
    

}