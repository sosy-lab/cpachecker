OBSERVER AUTOMATON MonitorOperationSequenceAutomaton

INITIAL STATE Init;

STATE USEFIRST Init :
  MATCH  {$? = ssl3_get_client_hello();}  -> GOTO STATE1;
  MATCH  {$? = ssl3_send_server_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_change_cipher_spec();}  -> ERROR;
  MATCH  {$? = ssl3_get_finished();}  -> ERROR;
  MATCH  {$? = ssl3_send_finished();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_key_exchange();}  -> ERROR;
  MATCH  {$? = ssl3_send_certificate_request();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_certificate();}  -> ERROR;

STATE USEFIRST STATE1 :
  MATCH  {$? = ssl3_send_server_hello();}  -> GOTO STATE2;
  MATCH  {$? = ssl3_get_client_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_change_cipher_spec();}  -> ERROR;
  MATCH  {$? = ssl3_get_finished();}  -> ERROR;
  MATCH  {$? = ssl3_send_finished();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_key_exchange();}  -> ERROR;
  MATCH  {$? = ssl3_send_certificate_request();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_certificate();}  -> ERROR;

STATE USEFIRST STATE2 :
  MATCH  {$? = ssl3_send_server_key_exchange();}  -> GOTO STATE3;
  MATCH  {$? = ssl3_get_client_hello();}  -> GOTO STATE1;
  MATCH  {$? = ssl3_send_server_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_change_cipher_spec();}  -> GOTO STATE6;
  MATCH  {$? = ssl3_send_finished();}  -> ERROR;
  MATCH  {$? = ssl3_get_finished();}  -> GOTO STATE8;

STATE USEFIRST STATE3 :
  MATCH  {$? = ssl3_send_change_cipher_spec();}  -> GOTO STATE6;
  MATCH  {$? = ssl3_get_client_hello();}  -> GOTO STATE1;
  MATCH  {$? = ssl3_send_server_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_finished();}  -> ERROR;
  MATCH  {$? = ssl3_get_finished();}  -> GOTO STATE8;
  MATCH  {$? = ssl3_send_server_key_exchange();}  -> ERROR;

STATE USEFIRST STATE6 :
  MATCH  {$? = ssl3_get_client_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_change_cipher_spec();}  -> ERROR;
  MATCH  {$? = ssl3_send_finished();}  -> GOTO STATE7;
  MATCH  {$? = ssl3_get_finished();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_key_exchange();}  -> ERROR;
  MATCH  {$? = ssl3_send_certificate_request();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_certificate();}  -> ERROR;
  
STATE USEFIRST STATE7 : 
  MATCH  {$? = ssl3_get_client_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_change_cipher_spec();}  -> ERROR;
  MATCH  {$? = ssl3_get_finished();}  -> GOTO STATE8;
  MATCH  {$? = ssl3_send_finished();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_key_exchange();}  -> ERROR;
  MATCH  {$? = ssl3_send_certificate_request();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_certificate();}  -> ERROR;
   
STATE USEFIRST STATE8 :
  MATCH  {$? = ssl3_get_client_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_hello();}  -> ERROR;
  MATCH  {$? = ssl3_send_change_cipher_spec();}  -> GOTO STATE6;
  MATCH  {$? = ssl3_send_finished();}  -> ERROR; 
  MATCH  {$? = ssl3_get_finished();}  -> ERROR; 
  MATCH  {$? = ssl3_send_server_key_exchange();}  -> ERROR;
  MATCH  {$? = ssl3_send_certificate_request();}  -> ERROR;
  MATCH  {$? = ssl3_send_server_certificate();}  -> ERROR;
  
END AUTOMATON
