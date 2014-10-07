# 1 "Client.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Client.c"

# 1 "Client.h" 1
# 1 "ClientLib.h" 1



int initClient();

char* getClientName(int handle);

void setClientName(int handle, char* value);

int getClientOutbuffer(int handle);

void setClientOutbuffer(int handle, int value);

int getClientAddressBookSize(int handle);

void setClientAddressBookSize(int handle, int value);

int createClientAddressBookEntry(int handle);

int getClientAddressBookAlias(int handle, int index);

void setClientAddressBookAlias(int handle, int index, int value);

int getClientAddressBookAddress(int handle, int index);

void setClientAddressBookAddress(int handle, int index, int value);


int getClientAutoResponse(int handle);

void setClientAutoResponse(int handle, int value);

int getClientPrivateKey(int handle);

void setClientPrivateKey(int handle, int value);

int getClientKeyringSize(int handle);

int createClientKeyringEntry(int handle);

int getClientKeyringUser(int handle, int index);

void setClientKeyringUser(int handle, int index, int value);

int getClientKeyringPublicKey(int handle, int index);

void setClientKeyringPublicKey(int handle, int index, int value);

int getClientForwardReceiver(int handle);

void setClientForwardReceiver(int handle, int value);

int getClientId(int handle);

void setClientId(int handle, int value);

int findPublicKey(int handle, int userid);

int findClientAddressBookAlias(int handle, int userid);
# 2 "Client.h" 2

# 1 "Email.h" 1
# 1 "featureselect.h" 1







int __SELECTED_FEATURE_Base;


int __SELECTED_FEATURE_Keys;


int __SELECTED_FEATURE_Encrypt;


int __SELECTED_FEATURE_AutoResponder;


int __SELECTED_FEATURE_AddressBook;


int __SELECTED_FEATURE_Sign;


int __SELECTED_FEATURE_Forward;


int __SELECTED_FEATURE_Verify;


int __SELECTED_FEATURE_Decrypt;


int __GUIDSL_ROOT_PRODUCTION;

int __GUIDSL_NON_TERMINAL_main;



int select_one();

void select_features();

void select_helpers();

int valid_product();
# 2 "Email.h" 2

# 1 "EmailLib.h" 1



int initEmail();

int getEmailId(int handle);

void setEmailId(int handle, int value);

int getEmailFrom(int handle);

void setEmailFrom(int handle, int value);

int getEmailTo(int handle);

void setEmailTo(int handle, int value);

char* getEmailSubject(int handle);

void setEmailSubject(int handle, char* value);

char* getEmailBody(int handle);

void setEmailBody(int handle, char* value);

int isEncrypted(int handle);

void setEmailIsEncrypted(int handle, int value);

int getEmailEncryptionKey(int handle);

void setEmailEncryptionKey(int handle, int value);

int isSigned(int handle);

void setEmailIsSigned(int handle, int value);

int getEmailSignKey(int handle);

void setEmailSignKey(int handle, int value);

int isVerified(int handle);

void setEmailIsSignatureVerified(int handle, int value);
# 4 "Email.h" 2


void printMail (int msg);


int isReadable (int msg);


int createEmail (int from, int to);


int cloneEmail(int msg);
# 4 "Client.h" 2

# 1 "EmailLib.h" 1



int initEmail();

int getEmailId(int handle);

void setEmailId(int handle, int value);

int getEmailFrom(int handle);

void setEmailFrom(int handle, int value);

int getEmailTo(int handle);

void setEmailTo(int handle, int value);

char* getEmailSubject(int handle);

void setEmailSubject(int handle, char* value);

char* getEmailBody(int handle);

void setEmailBody(int handle, char* value);

int isEncrypted(int handle);

void setEmailIsEncrypted(int handle, int value);

int getEmailEncryptionKey(int handle);

void setEmailEncryptionKey(int handle, int value);

int isSigned(int handle);

void setEmailIsSigned(int handle, int value);

int getEmailSignKey(int handle);

void setEmailSignKey(int handle, int value);

int isVerified(int handle);

void setEmailIsSignatureVerified(int handle, int value);
# 6 "Client.h" 2
# 14 "Client.h"
void queue (int client, int msg);


int is_queue_empty ();

int get_queued_client ();

int get_queued_email ();


void mail (int client, int msg);

void outgoing (int client, int msg);

void deliver (int client, int msg);

void incoming (int client, int msg);

int createClient(char *name);


void sendEmail (int sender, int receiver);



int
isKeyPairValid (int publicKey, int privateKey);


void
generateKeyPair (int client, int seed);
# 3 "Client.c" 2


int queue_empty = 1;


int queued_message;


int queued_client;



void
mail (int client, int msg)
{

  incoming (getEmailTo(msg), msg);
}



void
outgoing (int client, int msg)
{
  setEmailFrom(msg, getClientId(client));
  mail(client, msg);
}



void
deliver (int client, int msg)
{

}



void
incoming (int client, int msg)
{
  deliver (client, msg);
}


int createClient(char *name) {
    int client = initClient();
    return client;
}


void
sendEmail (int sender, int receiver)
{
  int email = createEmail (0, receiver);
  outgoing (sender, email);


}


void
queue (int client, int msg)
{
    queue_empty = 0;
    queued_message = msg;
    queued_client = client;
}


int
is_queue_empty ()
{
    return queue_empty;
}


int
get_queued_client ()
{
    return queued_client;
}


int
get_queued_email ()
{
    return queued_message;
}

int
isKeyPairValid (int publicKey, int privateKey)
{

  if (!publicKey || !privateKey)
    return 0;
  return privateKey == publicKey;
}


void
generateKeyPair (int client, int seed)
{
    setClientPrivateKey(client, seed);
}
# 1 "ClientLib.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "ClientLib.c"

# 1 "ClientLib.h" 1



int initClient();

char* getClientName(int handle);

void setClientName(int handle, char* value);

int getClientOutbuffer(int handle);

void setClientOutbuffer(int handle, int value);

int getClientAddressBookSize(int handle);

void setClientAddressBookSize(int handle, int value);

int createClientAddressBookEntry(int handle);

int getClientAddressBookAlias(int handle, int index);

void setClientAddressBookAlias(int handle, int index, int value);

int getClientAddressBookAddress(int handle, int index);

void setClientAddressBookAddress(int handle, int index, int value);


int getClientAutoResponse(int handle);

void setClientAutoResponse(int handle, int value);

int getClientPrivateKey(int handle);

void setClientPrivateKey(int handle, int value);

int getClientKeyringSize(int handle);

int createClientKeyringEntry(int handle);

int getClientKeyringUser(int handle, int index);

void setClientKeyringUser(int handle, int index, int value);

int getClientKeyringPublicKey(int handle, int index);

void setClientKeyringPublicKey(int handle, int index, int value);

int getClientForwardReceiver(int handle);

void setClientForwardReceiver(int handle, int value);

int getClientId(int handle);

void setClientId(int handle, int value);

int findPublicKey(int handle, int userid);

int findClientAddressBookAlias(int handle, int userid);
# 3 "ClientLib.c" 2

int __ste_Client_counter = 0;

int initClient() {
 if (__ste_Client_counter < 3) {
  return ++__ste_Client_counter;
 } else {
  return -1;
 }
}

char* __ste_client_name0 = 0;

char* __ste_client_name1 = 0;

char* __ste_client_name2 = 0;


char* getClientName(int handle) {
 if (handle == 1) {
  return __ste_client_name0;
 } else if (handle == 2) {
  return __ste_client_name1;
 } else if (handle == 3) {
  return __ste_client_name2;
 } else {
  return 0;
 }
}

void setClientName(int handle, char* value) {
 if (handle == 1) {
  __ste_client_name0 = value;
 } else if (handle == 2) {
  __ste_client_name1 = value;
 } else if (handle == 3) {
  __ste_client_name2 = value;
 }
}

int __ste_client_outbuffer0 = 0;

int __ste_client_outbuffer1 = 0;

int __ste_client_outbuffer2 = 0;

int __ste_client_outbuffer3 = 0;


int getClientOutbuffer(int handle) {
 if (handle == 1) {
  return __ste_client_outbuffer0;
 } else if (handle == 2) {
  return __ste_client_outbuffer1;
 } else if (handle == 3) {
  return __ste_client_outbuffer2;
 } else {
  return 0;
 }
}

void setClientOutbuffer(int handle, int value) {
 if (handle == 1) {
  __ste_client_outbuffer0 = value;
 } else if (handle == 2) {
  __ste_client_outbuffer1 = value;
 } else if (handle == 3) {
  __ste_client_outbuffer2 = value;
 }
}



int __ste_ClientAddressBook_size0 = 0;

int __ste_ClientAddressBook_size1 = 0;

int __ste_ClientAddressBook_size2 = 0;


int getClientAddressBookSize(int handle){
 if (handle == 1) {
  return __ste_ClientAddressBook_size0;
 } else if (handle == 2) {
  return __ste_ClientAddressBook_size1;
 } else if (handle == 3) {
  return __ste_ClientAddressBook_size2;
 } else {
  return 0;
 }
}

void setClientAddressBookSize(int handle, int value) {
 if (handle == 1) {
  __ste_ClientAddressBook_size0 = value;
 } else if (handle == 2) {
  __ste_ClientAddressBook_size1 = value;
 } else if (handle == 3) {
  __ste_ClientAddressBook_size2 = value;
 }
}

int createClientAddressBookEntry(int handle){
    int size = getClientAddressBookSize(handle);
 if (size < 3) {
     setClientAddressBookSize(handle, size + 1);
  return size + 1;
 } else return -1;
}


int __ste_Client_AddressBook0_Alias0 = 0;

int __ste_Client_AddressBook0_Alias1 = 0;

int __ste_Client_AddressBook0_Alias2 = 0;

int __ste_Client_AddressBook1_Alias0 = 0;

int __ste_Client_AddressBook1_Alias1 = 0;

int __ste_Client_AddressBook1_Alias2 = 0;

int __ste_Client_AddressBook2_Alias0 = 0;

int __ste_Client_AddressBook2_Alias1 = 0;

int __ste_Client_AddressBook2_Alias2 = 0;


int getClientAddressBookAlias(int handle, int index) {
 if (handle == 1) {
  if (index == 0) {
   return __ste_Client_AddressBook0_Alias0;
  } else if (index == 1) {
   return __ste_Client_AddressBook0_Alias1;
  } else if (index == 2) {
   return __ste_Client_AddressBook0_Alias2;
  } else {
   return 0;
  }
 } else if (handle == 2) {
  if (index == 0) {
   return __ste_Client_AddressBook1_Alias0;
  } else if (index == 1) {
   return __ste_Client_AddressBook1_Alias1;
  } else if (index == 2) {
   return __ste_Client_AddressBook1_Alias2;
  } else {
   return 0;
  }
 } else if (handle == 3) {
  if (index == 0) {
   return __ste_Client_AddressBook2_Alias0;
  } else if (index == 1) {
   return __ste_Client_AddressBook2_Alias1;
  } else if (index == 2) {
   return __ste_Client_AddressBook2_Alias2;
  } else {
   return 0;
  }
 } else {
  return 0;
 }
}


int findClientAddressBookAlias(int handle, int userid) {
 if (handle == 1) {
  if (userid == __ste_Client_AddressBook0_Alias0) {
   return 0;
  } else if (userid == __ste_Client_AddressBook0_Alias1) {
   return 1;
  } else if (userid == __ste_Client_AddressBook0_Alias2) {
   return 2;
  } else {
   return -1;
  }
 } else if (handle == 2) {
  if (userid == __ste_Client_AddressBook1_Alias0) {
   return 0;
  } else if (userid == __ste_Client_AddressBook1_Alias1) {
   return 1;
  } else if (userid == __ste_Client_AddressBook1_Alias2) {
   return 2;
  } else {
   return -1;
  }
 } else if (handle == 3) {
  if (userid == __ste_Client_AddressBook2_Alias0) {
   return 0;
  } else if (userid == __ste_Client_AddressBook2_Alias1) {
   return 1;
  } else if (userid == __ste_Client_AddressBook2_Alias2) {
   return 2;
  } else {
   return -1;
  }
 } else {
  return -1;
 }
}


void setClientAddressBookAlias(int handle, int index, int value) {
 if (handle == 1) {
  if (index == 0) {
   __ste_Client_AddressBook0_Alias0 = value;
  } else if (index == 1) {
   __ste_Client_AddressBook0_Alias1 = value;
  } else if (index == 2) {
   __ste_Client_AddressBook0_Alias2 = value;
  }
 } else if (handle == 2) {
  if (index == 0) {
   __ste_Client_AddressBook1_Alias0 = value;
  } else if (index == 1) {
   __ste_Client_AddressBook1_Alias1 = value;
  } else if (index == 2) {
   __ste_Client_AddressBook1_Alias2 = value;
  }
 } else if (handle == 3) {
  if (index == 0) {
   __ste_Client_AddressBook2_Alias0 = value;
  } else if (index == 1) {
   __ste_Client_AddressBook2_Alias1 = value;
  } else if (index == 2) {
   __ste_Client_AddressBook2_Alias2 = value;
  }
 }
}

int __ste_Client_AddressBook0_Address0 = 0;

int __ste_Client_AddressBook0_Address1 = 0;

int __ste_Client_AddressBook0_Address2 = 0;

int __ste_Client_AddressBook1_Address0 = 0;

int __ste_Client_AddressBook1_Address1 = 0;

int __ste_Client_AddressBook1_Address2 = 0;

int __ste_Client_AddressBook2_Address0 = 0;

int __ste_Client_AddressBook2_Address1 = 0;

int __ste_Client_AddressBook2_Address2 = 0;


int getClientAddressBookAddress(int handle, int index) {
 if (handle == 1) {
  if (index == 0) {
   return __ste_Client_AddressBook0_Address0;
  } else if (index == 1) {
   return __ste_Client_AddressBook0_Address1;
  } else if (index == 2) {
   return __ste_Client_AddressBook0_Address2;
  } else {
   return 0;
  }
 } else if (handle == 2) {
  if (index == 0) {
   return __ste_Client_AddressBook1_Address0;
  } else if (index == 1) {
   return __ste_Client_AddressBook1_Address1;
  } else if (index == 2) {
   return __ste_Client_AddressBook1_Address2;
  } else {
   return 0;
  }
 } else if (handle == 3) {
  if (index == 0) {
   return __ste_Client_AddressBook2_Address0;
  } else if (index == 1) {
   return __ste_Client_AddressBook2_Address1;
  } else if (index == 2) {
   return __ste_Client_AddressBook2_Address2;
  } else {
   return 0;
  }
 } else {
  return 0;
 }
}

void setClientAddressBookAddress(int handle, int index, int value) {
 if (handle == 1) {
  if (index == 0) {
   __ste_Client_AddressBook0_Address0 = value;
  } else if (index == 1) {
   __ste_Client_AddressBook0_Address1 = value;
  } else if (index == 2) {
   __ste_Client_AddressBook0_Address2 = value;
  }
 } else if (handle == 2) {
  if (index == 0) {
   __ste_Client_AddressBook1_Address0 = value;
  } else if (index == 1) {
   __ste_Client_AddressBook1_Address1 = value;
  } else if (index == 2) {
   __ste_Client_AddressBook1_Address2 = value;
  }
 } else if (handle == 3) {
  if (index == 0) {
   __ste_Client_AddressBook2_Address0 = value;
  } else if (index == 1) {
   __ste_Client_AddressBook2_Address1 = value;
  } else if (index == 2) {
   __ste_Client_AddressBook2_Address2 = value;
  }
 }
}

int __ste_client_autoResponse0 = 0;

int __ste_client_autoResponse1 = 0;

int __ste_client_autoResponse2 = 0;


int getClientAutoResponse(int handle) {
 if (handle == 1) {
  return __ste_client_autoResponse0;
 } else if (handle == 2) {
  return __ste_client_autoResponse1;
 } else if (handle == 3) {
  return __ste_client_autoResponse2;
 } else {
  return -1;
 }
}

void setClientAutoResponse(int handle, int value) {
 if (handle == 1) {
  __ste_client_autoResponse0 = value;
 } else if (handle == 2) {
  __ste_client_autoResponse1 = value;
 } else if (handle == 3) {
  __ste_client_autoResponse2 = value;
 }
}

int __ste_client_privateKey0 = 0;

int __ste_client_privateKey1 = 0;

int __ste_client_privateKey2 = 0;


int getClientPrivateKey(int handle) {
 if (handle == 1) {
  return __ste_client_privateKey0;
 } else if (handle == 2) {
  return __ste_client_privateKey1;
 } else if (handle == 3) {
  return __ste_client_privateKey2;
 } else {
  return 0;
 }
}

void setClientPrivateKey(int handle, int value) {
 if (handle == 1) {
  __ste_client_privateKey0 = value;
 } else if (handle == 2) {
  __ste_client_privateKey1 = value;
 } else if (handle == 3) {
  __ste_client_privateKey2 = value;
 }
}

int __ste_ClientKeyring_size0 = 0;

int __ste_ClientKeyring_size1 = 0;

int __ste_ClientKeyring_size2 = 0;


int getClientKeyringSize(int handle){
 if (handle == 1) {
  return __ste_ClientKeyring_size0;
 } else if (handle == 2) {
  return __ste_ClientKeyring_size1;
 } else if (handle == 3) {
  return __ste_ClientKeyring_size2;
 } else {
  return 0;
 }
}

void setClientKeyringSize(int handle, int value) {
 if (handle == 1) {
  __ste_ClientKeyring_size0 = value;
 } else if (handle == 2) {
  __ste_ClientKeyring_size1 = value;
 } else if (handle == 3) {
  __ste_ClientKeyring_size2 = value;
 }
}

int createClientKeyringEntry(int handle){
    int size = getClientKeyringSize(handle);
 if (size < 2) {
     setClientKeyringSize(handle, size + 1);
  return size + 1;
 } else return -1;
}

int __ste_Client_Keyring0_User0 = 0;

int __ste_Client_Keyring0_User1 = 0;

int __ste_Client_Keyring0_User2 = 0;

int __ste_Client_Keyring1_User0 = 0;

int __ste_Client_Keyring1_User1 = 0;

int __ste_Client_Keyring1_User2 = 0;

int __ste_Client_Keyring2_User0 = 0;

int __ste_Client_Keyring2_User1 = 0;

int __ste_Client_Keyring2_User2 = 0;


int getClientKeyringUser(int handle, int index) {
 if (handle == 1) {
  if (index == 0) {
   return __ste_Client_Keyring0_User0;
  } else if (index == 1) {
   return __ste_Client_Keyring0_User1;
  }

      else {
   return 0;
  }
 } else if (handle == 2) {
  if (index == 0) {
   return __ste_Client_Keyring1_User0;
  } else if (index == 1) {
   return __ste_Client_Keyring1_User1;
  }

      else {
   return 0;
  }
 } else if (handle == 3) {
  if (index == 0) {
   return __ste_Client_Keyring2_User0;
  } else if (index == 1) {
   return __ste_Client_Keyring2_User1;
  }

      else {
   return 0;
  }
 } else {
  return 0;
 }
}





void setClientKeyringUser(int handle, int index, int value) {
 if (handle == 1) {
  if (index == 0) {
   __ste_Client_Keyring0_User0 = value;
  } else if (index == 1) {
   __ste_Client_Keyring0_User1 = value;
  }


 } else if (handle == 2) {
  if (index == 0) {
   __ste_Client_Keyring1_User0 = value;
  } else if (index == 1) {
   __ste_Client_Keyring1_User1 = value;
  }


 } else if (handle == 3) {
  if (index == 0) {
   __ste_Client_Keyring2_User0 = value;
  } else if (index == 1) {
   __ste_Client_Keyring2_User1 = value;
  }


 }
}

int __ste_Client_Keyring0_PublicKey0 = 0;

int __ste_Client_Keyring0_PublicKey1 = 0;

int __ste_Client_Keyring0_PublicKey2 = 0;

int __ste_Client_Keyring1_PublicKey0 = 0;

int __ste_Client_Keyring1_PublicKey1 = 0;

int __ste_Client_Keyring1_PublicKey2 = 0;

int __ste_Client_Keyring2_PublicKey0 = 0;

int __ste_Client_Keyring2_PublicKey1 = 0;

int __ste_Client_Keyring2_PublicKey2 = 0;


int getClientKeyringPublicKey(int handle, int index) {
 if (handle == 1) {
  if (index == 0) {
   return __ste_Client_Keyring0_PublicKey0;
  } else if (index == 1) {
   return __ste_Client_Keyring0_PublicKey1;
  }

      else {
   return 0;
  }
 } else if (handle == 2) {
  if (index == 0) {
   return __ste_Client_Keyring1_PublicKey0;
  } else if (index == 1) {
   return __ste_Client_Keyring1_PublicKey1;
  }

      else {
   return 0;
  }
 } else if (handle == 3) {
  if (index == 0) {
   return __ste_Client_Keyring2_PublicKey0;
  } else if (index == 1) {
   return __ste_Client_Keyring2_PublicKey1;
  }

      else {
   return 0;
  }
 } else {
  return 0;
 }
}


int findPublicKey(int handle, int userid) {

 if (handle == 1) {
  if (userid == __ste_Client_Keyring0_User0) {
   return __ste_Client_Keyring0_PublicKey0;
  } else if (userid == __ste_Client_Keyring0_User1) {
   return __ste_Client_Keyring0_PublicKey1;
  }

      else {
   return 0;
  }
 } else if (handle == 2) {
  if (userid == __ste_Client_Keyring1_User0) {
   return __ste_Client_Keyring1_PublicKey0;
  } else if (userid == __ste_Client_Keyring1_User1) {
   return __ste_Client_Keyring1_PublicKey1;
  }

      else {
   return 0;
  }
 } else if (handle == 3) {
  if (userid == __ste_Client_Keyring2_User0) {
   return __ste_Client_Keyring2_PublicKey0;
  } else if (userid == __ste_Client_Keyring2_User1) {
   return __ste_Client_Keyring2_PublicKey1;
  }

      else {
   return 0;
  }
 } else {
  return 0;
 }
}


void setClientKeyringPublicKey(int handle, int index, int value) {
 if (handle == 1) {
  if (index == 0) {
   __ste_Client_Keyring0_PublicKey0 = value;
  } else if (index == 1) {
   __ste_Client_Keyring0_PublicKey1 = value;
  }


 } else if (handle == 2) {
  if (index == 0) {
   __ste_Client_Keyring1_PublicKey0 = value;
  } else if (index == 1) {
   __ste_Client_Keyring1_PublicKey1 = value;
  }


 } else if (handle == 3) {
  if (index == 0) {
   __ste_Client_Keyring2_PublicKey0 = value;
  } else if (index == 1) {
   __ste_Client_Keyring2_PublicKey1 = value;
  }


 }
}

int __ste_client_forwardReceiver0 =0;

int __ste_client_forwardReceiver1 =0;

int __ste_client_forwardReceiver2 =0;

int __ste_client_forwardReceiver3 =0;

int getClientForwardReceiver(int handle) {
 if (handle == 1) {
  return __ste_client_forwardReceiver0;
 } else if (handle == 2) {
  return __ste_client_forwardReceiver1;
 } else if (handle == 3) {
  return __ste_client_forwardReceiver2;
 } else {
  return 0;
 }
}

void setClientForwardReceiver(int handle, int value) {
 if (handle == 1) {
  __ste_client_forwardReceiver0 = value;
 } else if (handle == 2) {
  __ste_client_forwardReceiver1 = value;
 } else if (handle == 3) {
  __ste_client_forwardReceiver2 = value;
 }
}

int __ste_client_idCounter0 = 0;

int __ste_client_idCounter1 = 0;

int __ste_client_idCounter2 = 0;


int getClientId(int handle) {
 if (handle == 1) {
  return __ste_client_idCounter0;
 } else if (handle == 2) {
  return __ste_client_idCounter1;
 } else if (handle == 3) {
  return __ste_client_idCounter2;
 } else {
  return 0;
 }
}

void setClientId(int handle, int value) {
 if (handle == 1) {
  __ste_client_idCounter0 = value;
 } else if (handle == 2) {
  __ste_client_idCounter1 = value;
 } else if (handle == 3) {
  __ste_client_idCounter2 = value;
 }
}
# 1 "Email.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Email.c"

# 1 "Email.h" 1
# 1 "featureselect.h" 1







int __SELECTED_FEATURE_Base;


int __SELECTED_FEATURE_Keys;


int __SELECTED_FEATURE_Encrypt;


int __SELECTED_FEATURE_AutoResponder;


int __SELECTED_FEATURE_AddressBook;


int __SELECTED_FEATURE_Sign;


int __SELECTED_FEATURE_Forward;


int __SELECTED_FEATURE_Verify;


int __SELECTED_FEATURE_Decrypt;


int __GUIDSL_ROOT_PRODUCTION;

int __GUIDSL_NON_TERMINAL_main;



int select_one();

void select_features();

void select_helpers();

int valid_product();
# 2 "Email.h" 2

# 1 "EmailLib.h" 1



int initEmail();

int getEmailId(int handle);

void setEmailId(int handle, int value);

int getEmailFrom(int handle);

void setEmailFrom(int handle, int value);

int getEmailTo(int handle);

void setEmailTo(int handle, int value);

char* getEmailSubject(int handle);

void setEmailSubject(int handle, char* value);

char* getEmailBody(int handle);

void setEmailBody(int handle, char* value);

int isEncrypted(int handle);

void setEmailIsEncrypted(int handle, int value);

int getEmailEncryptionKey(int handle);

void setEmailEncryptionKey(int handle, int value);

int isSigned(int handle);

void setEmailIsSigned(int handle, int value);

int getEmailSignKey(int handle);

void setEmailSignKey(int handle, int value);

int isVerified(int handle);

void setEmailIsSignatureVerified(int handle, int value);
# 4 "Email.h" 2


void printMail (int msg);


int isReadable (int msg);


int createEmail (int from, int to);


int cloneEmail(int msg);
# 3 "Email.c" 2



void
printMail (int msg)
{






}


int
isReadable (int msg)
{
  return 1;
}


int cloneEmail(int msg) {
    return msg;
}


int createEmail (int from, int to) {
  int msg = 1;
  setEmailFrom(msg, from);
  setEmailTo(msg, to);
  return msg;
}
# 1 "EmailLib.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "EmailLib.c"

# 1 "EmailLib.h" 1



int initEmail();

int getEmailId(int handle);

void setEmailId(int handle, int value);

int getEmailFrom(int handle);

void setEmailFrom(int handle, int value);

int getEmailTo(int handle);

void setEmailTo(int handle, int value);

char* getEmailSubject(int handle);

void setEmailSubject(int handle, char* value);

char* getEmailBody(int handle);

void setEmailBody(int handle, char* value);

int isEncrypted(int handle);

void setEmailIsEncrypted(int handle, int value);

int getEmailEncryptionKey(int handle);

void setEmailEncryptionKey(int handle, int value);

int isSigned(int handle);

void setEmailIsSigned(int handle, int value);

int getEmailSignKey(int handle);

void setEmailSignKey(int handle, int value);

int isVerified(int handle);

void setEmailIsSignatureVerified(int handle, int value);
# 3 "EmailLib.c" 2

int __ste_Email_counter = 0;

int initEmail() {
 if (__ste_Email_counter < 2) {
  return ++__ste_Email_counter;
 } else {
  return -1;
 }
}

int __ste_email_id0 = 0;

int __ste_email_id1 = 0;

int getEmailId(int handle) {
 if (handle == 1) {
  return __ste_email_id0;
 } else if (handle == 2) {
  return __ste_email_id1;
 } else {
  return 0;
 }
}

void setEmailId(int handle, int value) {
 if (handle == 1) {
  __ste_email_id0 = value;
 } else if (handle == 2) {
  __ste_email_id1 = value;
 }
}

int __ste_email_from0 = 0;

int __ste_email_from1 = 0;

int getEmailFrom(int handle) {
 if (handle == 1) {
  return __ste_email_from0;
 } else if (handle == 2) {
  return __ste_email_from1;
 } else {
  return 0;
 }
}

void setEmailFrom(int handle, int value) {
 if (handle == 1) {
  __ste_email_from0 = value;
 } else if (handle == 2) {
  __ste_email_from1 = value;
 }
}

int __ste_email_to0 = 0;

int __ste_email_to1 = 0;

int getEmailTo(int handle) {
 if (handle == 1) {
  return __ste_email_to0;
 } else if (handle == 2) {
  return __ste_email_to1;
 } else {
  return 0;
 }
}

void setEmailTo(int handle, int value) {
 if (handle == 1) {
  __ste_email_to0 = value;
 } else if (handle == 2) {
  __ste_email_to1 = value;
 }
}

char* __ste_email_subject0;

char* __ste_email_subject1;

char* getEmailSubject(int handle) {
 if (handle == 1) {
  return __ste_email_subject0;
 } else if (handle == 2) {
  return __ste_email_subject1;
 } else {
  return 0;
 }
}

void setEmailSubject(int handle, char* value) {
 if (handle == 1) {
  __ste_email_subject0 = value;
 } else if (handle == 2) {
  __ste_email_subject1 = value;
 }
}

char* __ste_email_body0 = 0;

char* __ste_email_body1 = 0;

char* getEmailBody(int handle) {
 if (handle == 1) {
  return __ste_email_body0;
 } else if (handle == 2) {
  return __ste_email_body1;
 } else {
  return 0;
 }
}

void setEmailBody(int handle, char* value) {
 if (handle == 1) {
  __ste_email_body0 = value;
 } else if (handle == 2) {
  __ste_email_body1 = value;
 }
}

int __ste_email_isEncrypted0 = 0;

int __ste_email_isEncrypted1 = 0;

int isEncrypted(int handle) {
 if (handle == 1) {
  return __ste_email_isEncrypted0;
 } else if (handle == 2) {
  return __ste_email_isEncrypted1;
 } else {
  return 0;
 }
}

void setEmailIsEncrypted(int handle, int value) {
 if (handle == 1) {
  __ste_email_isEncrypted0 = value;
 } else if (handle == 2) {
  __ste_email_isEncrypted1 = value;
 }
}

int __ste_email_encryptionKey0 = 0;

int __ste_email_encryptionKey1 = 0;

int getEmailEncryptionKey(int handle) {
 if (handle == 1) {
  return __ste_email_encryptionKey0;
 } else if (handle == 2) {
  return __ste_email_encryptionKey1;
 } else {
  return 0;
 }
}

void setEmailEncryptionKey(int handle, int value) {
 if (handle == 1) {
  __ste_email_encryptionKey0 = value;
 } else if (handle == 2) {
  __ste_email_encryptionKey1 = value;
 }
}

int __ste_email_isSigned0 = 0;

int __ste_email_isSigned1 = 0;

int isSigned(int handle) {
 if (handle == 1) {
  return __ste_email_isSigned0;
 } else if (handle == 2) {
  return __ste_email_isSigned1;
 } else {
  return 0;
 }
}

void setEmailIsSigned(int handle, int value) {
 if (handle == 1) {
  __ste_email_isSigned0 = value;
 } else if (handle == 2) {
  __ste_email_isSigned1 = value;
 }
}

int __ste_email_signKey0 = 0;

int __ste_email_signKey1 = 0;

int getEmailSignKey(int handle) {
 if (handle == 1) {
  return __ste_email_signKey0;
 } else if (handle == 2) {
  return __ste_email_signKey1;
 } else {
  return 0;
 }
}

void setEmailSignKey(int handle, int value) {
 if (handle == 1) {
  __ste_email_signKey0 = value;
 } else if (handle == 2) {
  __ste_email_signKey1 = value;
 }
}

int __ste_email_isSignatureVerified0;

int __ste_email_isSignatureVerified1;

int isVerified(int handle) {
 if (handle == 1) {
  return __ste_email_isSignatureVerified0;
 } else if (handle == 2) {
  return __ste_email_isSignatureVerified1;
 } else {
  return 0;
 }
}

void setEmailIsSignatureVerified(int handle, int value) {
 if (handle == 1) {
  __ste_email_isSignatureVerified0 = value;
 } else if (handle == 2) {
  __ste_email_isSignatureVerified1 = value;
 }
}
# 1 "featureselect.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "featureselect.c"
# 1 "featureselect.h" 1







int __SELECTED_FEATURE_Base;


int __SELECTED_FEATURE_Keys;


int __SELECTED_FEATURE_Encrypt;


int __SELECTED_FEATURE_AutoResponder;


int __SELECTED_FEATURE_AddressBook;


int __SELECTED_FEATURE_Sign;


int __SELECTED_FEATURE_Forward;


int __SELECTED_FEATURE_Verify;


int __SELECTED_FEATURE_Decrypt;


int __GUIDSL_ROOT_PRODUCTION;

int __GUIDSL_NON_TERMINAL_main;



int select_one();

void select_features();

void select_helpers();

int valid_product();
# 2 "featureselect.c" 2






extern int __VERIFIER_nondet_int(void);

int select_one() {if (__VERIFIER_nondet_int()) return 1; else return 0;}


void select_features() {

}



void select_helpers() {

}


int valid_product() {
  return 1;
}
# 1 "scenario.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "scenario.c"
void test() {
    int op1 = 0;
    int op2 = 0;
    int op3 = 0;
    int op4 = 0;
    int op5 = 0;
    int op6 = 0;
    int op7 = 0;
    int op8 = 0;
    int op9 = 0;
    int op10 = 0;
    int op11 = 0;
    int splverifierCounter = 0;
    while(splverifierCounter < 4) {
        op1 = 0;
        op2 = 0;
        op3 = 0;
        op4 = 0;
        op5 = 0;
        op6 = 0;
        op7 = 0;
        op8 = 0;
        op9 = 0;
        op10 = 0;
        op11 = 0;
        splverifierCounter = splverifierCounter + 1;
        if (!op1 && get_nondet()) {
            bobKeyAdd();
            op1 = 1;
        }
        else if (!op2 && get_nondet()) {
            op2 = 1;
        }
        else if (!op3 && get_nondet()) {
            rjhDeletePrivateKey();
            op3 = 1;
        }
        else if (!op4 && get_nondet()) {
            rjhKeyAdd();
            op4 = 1;
        }
        else if (!op5 && get_nondet()) {
            chuckKeyAddRjh();
            op5 = 1;
        }
        else if (!op6 && get_nondet()) {
            op6 = 1;
        }
        else if (!op7 && get_nondet()) {
            rjhKeyChange();
            op7 = 1;
        }
        else if (!op8 && get_nondet()) {
            op8 = 1;
        }
        else if (!op9 && get_nondet()) {
            chuckKeyAdd();
            op9 = 1;
        }
        else if (!op10 && get_nondet()) {
            bobKeyChange();
            op10 = 1;
        }
        else if (!op11 && get_nondet()) {
            chuckKeyAdd();
            op11 = 1;
        }
        else break;
    }
    bobToRjh();
}
# 1 "Test.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Test.c"



# 1 "Client.h" 1
# 1 "ClientLib.h" 1



int initClient();

char* getClientName(int handle);

void setClientName(int handle, char* value);

int getClientOutbuffer(int handle);

void setClientOutbuffer(int handle, int value);

int getClientAddressBookSize(int handle);

void setClientAddressBookSize(int handle, int value);

int createClientAddressBookEntry(int handle);

int getClientAddressBookAlias(int handle, int index);

void setClientAddressBookAlias(int handle, int index, int value);

int getClientAddressBookAddress(int handle, int index);

void setClientAddressBookAddress(int handle, int index, int value);


int getClientAutoResponse(int handle);

void setClientAutoResponse(int handle, int value);

int getClientPrivateKey(int handle);

void setClientPrivateKey(int handle, int value);

int getClientKeyringSize(int handle);

int createClientKeyringEntry(int handle);

int getClientKeyringUser(int handle, int index);

void setClientKeyringUser(int handle, int index, int value);

int getClientKeyringPublicKey(int handle, int index);

void setClientKeyringPublicKey(int handle, int index, int value);

int getClientForwardReceiver(int handle);

void setClientForwardReceiver(int handle, int value);

int getClientId(int handle);

void setClientId(int handle, int value);

int findPublicKey(int handle, int userid);

int findClientAddressBookAlias(int handle, int userid);
# 2 "Client.h" 2

# 1 "Email.h" 1
# 1 "featureselect.h" 1







int __SELECTED_FEATURE_Base;


int __SELECTED_FEATURE_Keys;


int __SELECTED_FEATURE_Encrypt;


int __SELECTED_FEATURE_AutoResponder;


int __SELECTED_FEATURE_AddressBook;


int __SELECTED_FEATURE_Sign;


int __SELECTED_FEATURE_Forward;


int __SELECTED_FEATURE_Verify;


int __SELECTED_FEATURE_Decrypt;


int __GUIDSL_ROOT_PRODUCTION;

int __GUIDSL_NON_TERMINAL_main;



int select_one();

void select_features();

void select_helpers();

int valid_product();
# 2 "Email.h" 2

# 1 "EmailLib.h" 1



int initEmail();

int getEmailId(int handle);

void setEmailId(int handle, int value);

int getEmailFrom(int handle);

void setEmailFrom(int handle, int value);

int getEmailTo(int handle);

void setEmailTo(int handle, int value);

char* getEmailSubject(int handle);

void setEmailSubject(int handle, char* value);

char* getEmailBody(int handle);

void setEmailBody(int handle, char* value);

int isEncrypted(int handle);

void setEmailIsEncrypted(int handle, int value);

int getEmailEncryptionKey(int handle);

void setEmailEncryptionKey(int handle, int value);

int isSigned(int handle);

void setEmailIsSigned(int handle, int value);

int getEmailSignKey(int handle);

void setEmailSignKey(int handle, int value);

int isVerified(int handle);

void setEmailIsSignatureVerified(int handle, int value);
# 4 "Email.h" 2


void printMail (int msg);


int isReadable (int msg);


int createEmail (int from, int to);


int cloneEmail(int msg);
# 4 "Client.h" 2

# 1 "EmailLib.h" 1



int initEmail();

int getEmailId(int handle);

void setEmailId(int handle, int value);

int getEmailFrom(int handle);

void setEmailFrom(int handle, int value);

int getEmailTo(int handle);

void setEmailTo(int handle, int value);

char* getEmailSubject(int handle);

void setEmailSubject(int handle, char* value);

char* getEmailBody(int handle);

void setEmailBody(int handle, char* value);

int isEncrypted(int handle);

void setEmailIsEncrypted(int handle, int value);

int getEmailEncryptionKey(int handle);

void setEmailEncryptionKey(int handle, int value);

int isSigned(int handle);

void setEmailIsSigned(int handle, int value);

int getEmailSignKey(int handle);

void setEmailSignKey(int handle, int value);

int isVerified(int handle);

void setEmailIsSignatureVerified(int handle, int value);
# 6 "Client.h" 2
# 14 "Client.h"
void queue (int client, int msg);


int is_queue_empty ();

int get_queued_client ();

int get_queued_email ();


void mail (int client, int msg);

void outgoing (int client, int msg);

void deliver (int client, int msg);

void incoming (int client, int msg);

int createClient(char *name);


void sendEmail (int sender, int receiver);



int
isKeyPairValid (int publicKey, int privateKey);


void
generateKeyPair (int client, int seed);
# 5 "Test.c" 2

# 1 "Test.h" 1

int bob;


int rjh;


int chuck;


void setup_bob(int bob);


void setup_rjh(int rjh);


void setup_chuck(int chuck);


void before();


void bobToRjh();


void rjhToBob();


void test();


void setup();


int main();
void bobKeyAdd();


void bobKeyAddChuck();


void rjhKeyAdd();


void rjhKeyAddChuck();


void chuckKeyAdd();


void bobKeyChange();


void rjhKeyChange();


void rjhDeletePrivateKey();


void chuckKeyAddRjh();
# 7 "Test.c" 2

# 1 "Util.h" 1
int prompt(char* msg);
# 9 "Test.c" 2




void
setup_bob__wrappee__Base(int bob)
{
    setClientId(bob, bob);
}
void
setup_bob(int bob)
{
  setup_bob__wrappee__Base(bob);
  setClientPrivateKey(bob, 123);

}



void
setup_rjh__wrappee__Base(int rjh)
{
    setClientId(rjh, rjh);
}


void
setup_rjh(int rjh)
{

  setup_rjh__wrappee__Base(rjh);
  setClientPrivateKey(rjh, 456);

}


void
setup_chuck__wrappee__Base(int chuck)
{
    setClientId(chuck, chuck);
}


void
setup_chuck(int chuck)
{
  setup_chuck__wrappee__Base(chuck);
  setClientPrivateKey(chuck, 789);
}






void
bobToRjh()
{


  sendEmail(bob,rjh);
  if (!is_queue_empty()) {
    outgoing(get_queued_client(), get_queued_email());
  }
}


void
rjhToBob()
{


  sendEmail(rjh,bob);
}


extern int input();

int get_nondet() {
    int nd;
    nd=input();
    return nd;
}



void setup() {
  bob = 1;
  setup_bob(bob);


  rjh = 2;
  setup_rjh(rjh);


  chuck = 3;
  setup_chuck(chuck);

}


int
main (void)
{
  select_helpers();
  select_features();
  if (valid_product()) {
      setup();
      test();
  }

}


void
bobKeyAdd()
{
    createClientKeyringEntry(bob);
    setClientKeyringUser(bob, 0, 2);
    setClientKeyringPublicKey(bob, 0, 456);



}


void
rjhKeyAdd()
{
    createClientKeyringEntry(rjh);
    setClientKeyringUser(rjh, 0, 1);
    setClientKeyringPublicKey(rjh, 0, 123);
}


void
rjhKeyAddChuck()
{
    createClientKeyringEntry(rjh);
    setClientKeyringUser(rjh, 0, 3);
    setClientKeyringPublicKey(rjh, 0, 789);
}



void
bobKeyAddChuck()
{
    createClientKeyringEntry(bob);
    setClientKeyringUser(bob, 1, 3);
    setClientKeyringPublicKey(bob, 1, 789);
}


void
chuckKeyAdd()
{
    createClientKeyringEntry(chuck);
    setClientKeyringUser(chuck, 0, 1);
    setClientKeyringPublicKey(chuck, 0, 123);
}


void
chuckKeyAddRjh()
{
    createClientKeyringEntry(chuck);
    setClientKeyringUser(chuck, 0, 2);
    setClientKeyringPublicKey(chuck, 0, 456);
}


void
rjhDeletePrivateKey()
{
    setClientPrivateKey(rjh, 0);
}


void
bobKeyChange()
{
  generateKeyPair(bob, 777);
}


void
rjhKeyChange()
{
  generateKeyPair(rjh, 666);
}
# 1 "Util.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Util.c"

# 1 "Util.h" 1
int prompt(char* msg);
# 3 "Util.c" 2



int
prompt(char* msg)
{

    int retval;

    return retval;
}
