int main() {
  int st;
  int ok;
  int p;
  int cmd;

  st = 0;
  ok = 0;

  cmd = readcmd();

  while (1) {
    switch (st) {
    case 0:
      if (cmd == 177) { 
         st = 1; 
      } 
      else { 
         st = 2; 
      }
      break;
    case 1:
      if (cmd == 177) {
         st = 3; 
         cmd = readcmd(); 
      } 
      else { 
         st = 0; 
      }
      break;
    case 2:
      if (cmd != 177) {
         cmd = 79; 
         st = 4; 
         p = cmd;
         while (1) {
          if(p <= 0){
              goto L1;
          }
          else{
             --p;
          } 
         } 
         L1:
         if (p < 0) {
            goto ERROR;
         }  
      } 
      else { 
        goto ERROR; 
      }
      break;
    case 3:
      if (cmd == 78) { 
         st = 4; 
         ok = 1; 
      } 
      else { 
          st = 0; 
          cmd = readcmd(); 
      }
      break;
    case 4:
      if (ok) { 
         if (cmd != 78) {
            goto ERROR;
         }
      } 
      else { 
         if (cmd != 79) {
          goto ERROR; 
         }
      }
      goto cont;
    } 
  }
  cont: 
  p = cmd;
  while (1) {
          if(p <= 0){
              goto L2;
          }
          else{
             --p;
          } 
         } 
         L2:
  if (p > 0) {
     goto ERROR;
  }
  
  return (0);
  ERROR: 
  return (-1);
}
