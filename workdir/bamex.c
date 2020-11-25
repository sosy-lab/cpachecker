void some_cpu_work(int size){
  void** array = (void**)malloc(size * sizeof(int));
  for(int i = 1; i < size; i++){
     array[i] = array[i-1];
  }
  free(array);
}

int main(void){
  for(int i = 0; i < 100; i++){
    some_cpu_work(10);
  }
  return 0;
}