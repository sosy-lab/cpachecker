void some_cpu_work(int size){
  void** array = (void**)malloc(size * sizeof(int));
  for(int i = 1; i < size; i++){
     array[i] = array[i-1];
  }
  free(array);
}

void * big_array [10000];

void  cpu_work_no_malloc(int size){
  for(int i = 1; i < size; i++){
       big_array[i] = big_array[i-1];
    }
}

int main(void){
  for(int i = 0; i < 100; i++){
    cpu_work_no_malloc(10);
  }
  return 0;
}