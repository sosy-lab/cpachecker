#define FALSE 0
#define TRUE 1
int __VERIFIER_nondet_int() {
	return 1;
}
//extern int __VERIFIER_nondet_int();
int helper = 0;

void autoRespond(int client, int msg);
int createEmail(int from, int to);
int findPublicKey(int handle, int userid);
void forward(int client, int msg);
int getClientAddressBookAddress(int handle, int index);
int getClientAddressBookSize(int handle);
int getClientAutoResponse(int handle);
int getClientForwardReceiver(int handle);
int getClientId(int handle);
int getClientPrivateKey(int handle);
int getEmailEncryptionKey(int handle);
int getEmailFrom(int handle);
int getEmailSignKey(int handle);
int getEmailTo(int handle);
void incoming(int client, int msg);
int initClient(void);
int isEncrypted(int handle);
int isKeyPairValid(int publicKey, int privateKey);
int isReadable(int msg);
int isSigned(int handle);
void printMail(int msg);
void sendToAddressBook(int client, int msg);
void setClientPrivateKey(int handle, int value);
void setEmailEncryptionKey(int handle, int value);
void setEmailFrom(int handle, int value);
void setEmailIsEncrypted(int handle, int value);
void setEmailIsSignatureVerified(int handle, int value);
void setEmailIsSigned(int handle, int value);
void setEmailSignKey(int handle, int value);
void setEmailTo(int handle, int value);
void sign(int client, int msg);
void test(void);
void verify(int client, int msg);

//void setEmailEncryptionKey(int handle, int value);
//void queue(int client, int msg);
//int is_queue_empty(void);
//int get_queued_client(void);
//int get_queued_email(void);
//void mail(int client, int msg);
//void outgoing(int client, int msg);
//void deliver(int client, int msg);
//int createClient(char *name);
//void sendEmail(int sender, int receiver);
//void generateKeyPair(int client, int seed);
//void select_features(void);
//void select_helpers(void);
//int valid_product(void);
//int cloneEmail(int msg);

struct JoinPoint {
	void **(*fp)(struct JoinPoint *);
	void **args;
	int argsCount;
	char const **argsType;
	void *(*arg)(int, struct JoinPoint *);
	char const *(*argType)(int, struct JoinPoint *);
	void **retValue;
	char const *retType;
	char const *funcName;
	char const *targetName;
	char const *fileName;
	char const *kind;
	void *excep_return;
};

/*struct __UTAC__CFLOW_FUNC {
 int(*func)(int, int);
 int val;
 struct __UTAC__CFLOW_FUNC *next;
 };

 struct __UTAC__EXCEPTION {
 void *jumpbuf;
 unsigned long long prtValue;
 int pops;
 struct __UTAC__CFLOW_FUNC *cflowfuncs;
 };

 struct __ACC__ERR {
 void *v;
 struct __ACC__ERR *next;
 };*/

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
int __SELECTED_FEATURE_security;
int __SELECTED_FEATURE_signature;
int __SELECTED_FEATURE_helper_keys;

void __automaton_fail(void) {
	goto error;
	error: helper = helper + 1;
	return;
}

__inline void __utac_acc__VerifyForward_spec__1(int client, int msg) {
	int pubkey;
#ifdef PRINTING
	puts("before deliver\n");
#endif // PRINTING
	if (isVerified(msg)) {
		pubkey = findPublicKey(client, getEmailFrom(msg));
		if (pubkey == 0) {
			err_VerifyForward_spec__1: __automaton_fail();
		} else {

		}
	} else {

	}
	return;
}

void __utac_acc__DecryptAutoResponder_spec__1(int client, int msg) {
	int tmp;
#ifdef PRINTING
	puts("before autoRespond\n");
#endif // PRINTING
	tmp = isReadable(msg);
	if (tmp) {

	} else {
		err_DecryptAutoResponder_spec__1: __automaton_fail();
	}
	return;
}

int in_encrypted_spec9 = 0;

void __utac_acc__EncryptForward_spec__1(int msg) {
#ifdef PRINTING
	char const * __restrict __cil_tmp2;
	puts("before incoming\n");
#endif // PRINTING
	in_encrypted_spec9 = isEncrypted(msg);
#ifdef PRINTING
	__cil_tmp2 = (char const * __restrict)"in_encrypted=%d\n";
	printf(__cil_tmp2, in_encrypted_spec9);
#endif // PRINTING
	return;
}

void __utac_acc__EncryptForward_spec__2(int msg) {
#ifdef PRINTING
	char const * __restrict __cil_tmp3;
	puts("before mail\n");
	__cil_tmp3 = (char const * __restrict)"in_encrypted=%d\n";
	printf(__cil_tmp3, in_encrypted_spec9);
#endif // PRINTING
	if (in_encrypted_spec9) {
		if (isEncrypted(msg)) {

		} else {
			err_EncryptForward_spec__2: __automaton_fail();
		}
	} else {

	}
	return;
}

int in_encrypted = 0;

__inline void __utac_acc__EncryptAutoResponder_spec__1(int msg) {
#ifdef PRINTING
	char const * __restrict __cil_tmp2;
	puts("before incoming\n");
#endif
	in_encrypted = isEncrypted(msg);
#ifdef PRINTING
	__cil_tmp2 = (char const * __restrict)"in_encrypted=%d\n";
	printf(__cil_tmp2, in_encrypted);
#endif
	return;
}

__inline void __utac_acc__EncryptAutoResponder_spec__2(int msg) {
#ifdef PRINTING
	char const * __restrict __cil_tmp3;
	puts("before mail\n");
	__cil_tmp3 = (char const * __restrict)"in_encrypted=%d\n";
	printf(__cil_tmp3, in_encrypted);
#endif
	if (in_encrypted) {
		if (isEncrypted(msg)) {

		} else {
			err_EncryptAutoResponder_spec__2: __automaton_fail();
		}
	} else {

	}
	return;
}

void __utac_acc__EncryptVerify_spec__1(int msg) {

	if (isReadable(msg)) {

	} else {
		err_EncryptVerify_spec__1: __automaton_fail();
	}
	return;
}

int sent_encrypted = -1;

__inline void __utac_acc__EncryptDecrypt_spec__1(int msg) {
#ifdef PRINTING
	char const * __restrict __cil_tmp2;
	puts("before mail\n");
#endif
	sent_encrypted = isEncrypted(msg);
#ifdef PRINTING
	__cil_tmp2 = (char const * __restrict)"sent_encrypted=%d\n";
	printf(__cil_tmp2, sent_encrypted);
#endif
	return;
}

__inline void __utac_acc__EncryptDecrypt_spec__2(int client, int msg) {
#ifdef PRINTING
	char const * __restrict __cil_tmp6;
	puts("before decrypt\n");
	__cil_tmp6 = (char const * __restrict)"sent_encrypted=%d\n";
	printf(__cil_tmp6, sent_encrypted);
#endif // PRINTING	
	if (sent_encrypted == 1) {
		if (isKeyPairValid(getEmailEncryptionKey(msg),
				getClientPrivateKey(client))) {

		} else {
			err_EncryptDecrypt_spec__2: __automaton_fail();
		}
	} else {

	}
	return;
}

int mail_is_sensitive = -1;
__inline void __utac_acc__AddressBookEncrypt_spec__1(int client, int msg) {
	if (mail_is_sensitive == -1) {
		mail_is_sensitive = isEncrypted(msg);
	} else {

		if (mail_is_sensitive != isEncrypted(msg)) {
			err_AddressBookEncrypt_spec__1: __automaton_fail();
		} else {

		}
	}
	return;
}

void __utac_acc__DecryptForward_spec__1(int msg)
//Die und �hnliche ist wohl daf�r da um fehlverhalten sichtbar zu machen
{
	int tmp;
	tmp = isReadable(msg);
	if (tmp) {

	} else {
		err_DecryptForward_spec__1: __automaton_fail();
	}
	return;
}
int sent_signed = -1;

void __utac_acc__SignVerify_spec__1(int msg) {
#ifdef PRINTING
	char const * __restrict __cil_tmp2;
	puts("before mail\n");
#endif // PRINTING
	sent_signed = isSigned(msg);
#ifdef PRINTING
	__cil_tmp2 = (char const * __restrict)"sent_signed=%d\n";
	printf(__cil_tmp2, sent_signed);
#endif // PRINTING
	return;
}

void __utac_acc__SignVerify_spec__2(int client, int msg) {
	int pubkey;
#ifdef PRINTING
	char const * __restrict __cil_tmp8;
	puts("before verify\n");
	__cil_tmp8 = (char const * __restrict)"sent_signed=%d\n";
	printf(__cil_tmp8, sent_signed);
#endif // PRINTING
	if (sent_signed == 1) {
		pubkey = findPublicKey(client, getEmailFrom(msg));
		if (pubkey == 0) {
			__automaton_fail();
		} else {
			if (isKeyPairValid(getEmailSignKey(msg), pubkey)) {

			} else {
				err_SignVerify_spec__2: __automaton_fail();
			}
		}
	} else {

	}
	return;
}

void __utac_acc__SignForward_spec__1(int client, int msg) {
#ifdef PRINTING
	puts("before mail\n");
#endif // PRINTING
	if (isSigned(msg)) {
		if (getClientPrivateKey(client) == 0) {
			err_SignForward_spec__1: __automaton_fail();
		} else {

		}
	} else {

	}
	return;
}

void select_features(void) {
	__SELECTED_FEATURE_Base = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Keys = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Encrypt = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_AutoResponder = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_AddressBook = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Sign = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Forward = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Verify = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_Decrypt = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_security = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_signature = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_helper_keys = __VERIFIER_nondet_int();

	return;
}

void select_helpers(void) {
	__GUIDSL_ROOT_PRODUCTION = 1;
	return;
}

int valid_product(void) {
	int retValue_acc;
	/*if (__SELECTED_FEATURE_Base && (!__SELECTED_FEATURE_security || __SELECTED_FEATURE_Base)
	 && (!__SELECTED_FEATURE_signature || __SELECTED_FEATURE_Base) //&& (!__SELECTED_FEATURE_Address_Book || __SELECTED_FEATURE_Base)
	 /*&& (!__SELECTED_FEATURE_Autoresponder || __SELECTED_FEATURE_Base)* / && (!__SELECTED_FEATURE_Forward || __SELECTED_FEATURE_Base)
	 //&& (!__SELECTED_FEATURE_Remail || __SELECTED_FEATURE_Base)
	 //&& (!__SELECTED_FEATURE_RND || __SELECTED_FEATURE_Base)
	 //&& (!__SELECTED_FEATURE_Filter || __SELECTED_FEATURE_Base)
	 && (!__SELECTED_FEATURE_helper_keys || __SELECTED_FEATURE_Base)
	 && (!__SELECTED_FEATURE_security || __SELECTED_FEATURE_Encrypt) && (!__SELECTED_FEATURE_security || __SELECTED_FEATURE_Decrypt)
	 && (!__SELECTED_FEATURE_Encrypt || __SELECTED_FEATURE_security) && (!__SELECTED_FEATURE_Decrypt || __SELECTED_FEATURE_security)
	 && (!__SELECTED_FEATURE_signature || __SELECTED_FEATURE_Sign) && (!__SELECTED_FEATURE_signature || __SELECTED_FEATURE_Verify)
	 && (!__SELECTED_FEATURE_Sign || __SELECTED_FEATURE_signature) && (!__SELECTED_FEATURE_Verify || __SELECTED_FEATURE_signature)
	 && (!__SELECTED_FEATURE_security || __SELECTED_FEATURE_helper_keys) && (!__SELECTED_FEATURE_signature || __SELECTED_FEATURE_helper_keys)
	 && (!__SELECTED_FEATURE_helper_keys || __SELECTED_FEATURE_security || __SELECTED_FEATURE_signature) && TRUE  &&  !FALSE
	 && (__SELECTED_FEATURE_Decrypt || __SELECTED_FEATURE_signature //|| __SELECTED_FEATURE_Autoresponder
	 || __SELECTED_FEATURE_security //|| __SELECTED_FEATURE_Address_Book
	 //|| __SELECTED_FEATURE_Remail || __SELECTED_FEATURE_RND
	 || __SELECTED_FEATURE_helper_keys || __SELECTED_FEATURE_Encrypt
	 //|| __SELECTED_FEATURE_Filter
	 || __SELECTED_FEATURE_Verify || __SELECTED_FEATURE_Forward
	 || __SELECTED_FEATURE_Sign || TRUE)) {
	 if (__SELECTED_FEATURE_Base) {
	 feat_Base:;
	 }
	 if (__SELECTED_FEATURE_Keys) {
	 feat_Keys:;
	 }
	 if (__SELECTED_FEATURE_Encrypt) {
	 feat_Encrypt:;
	 }
	 if (__SELECTED_FEATURE_AutoResponder) {
	 feat_AutoResp:;
	 }
	 if (__SELECTED_FEATURE_AddressBook) {
	 feat_AddressBook:;
	 }
	 if (__SELECTED_FEATURE_Sign) {
	 feat_Sign:;
	 }
	 if (__SELECTED_FEATURE_Forward) {
	 feat_Forward:;
	 }
	 if (__SELECTED_FEATURE_Verify) {
	 feat_Verify:;
	 }
	 if (__SELECTED_FEATURE_Decrypt) {
	 feat_Decrypt:;
	 }
	 if (__SELECTED_FEATURE_security) {
	 feat_security:;
	 }
	 if (__SELECTED_FEATURE_signature) {
	 feat_Signature:;
	 }
	 if (__SELECTED_FEATURE_helper_keys) {
	 feat_helper_keys:;
	 }
	 retValue_acc = 1;
	 }
	 else {
	 retValue_acc = 0;
	 }*/
	int tmp;
	{
		if (!__SELECTED_FEATURE_Encrypt) {
			goto _L___4;
		} else {
			if (__SELECTED_FEATURE_Decrypt) {
				_L___4: /* CIL Label */
				if (!__SELECTED_FEATURE_Decrypt) {
					goto _L___3;
				} else {
					if (__SELECTED_FEATURE_Encrypt) {
						_L___3: /* CIL Label */
						if (!__SELECTED_FEATURE_Encrypt) {
							goto _L___2;
						} else {
							if (__SELECTED_FEATURE_Keys) {
								_L___2: /* CIL Label */
								if (!__SELECTED_FEATURE_Sign) {
									goto _L___1;
								} else {
									if (__SELECTED_FEATURE_Verify) {
										_L___1: /* CIL Label */
										if (!__SELECTED_FEATURE_Verify) {
											goto _L___0;
										} else {
											if (__SELECTED_FEATURE_Sign) {
												_L___0: /* CIL Label */
												if (!__SELECTED_FEATURE_Sign) {
													goto _L;
												} else {
													if (__SELECTED_FEATURE_Keys) {
														_L: /* CIL Label */
														if (__SELECTED_FEATURE_Base) {
															tmp = 1;
														} else {
															tmp = 0;
														}
													} else {
														tmp = 0;
													}
												}
											} else {
												tmp = 0;
											}
										}
									} else {
										tmp = 0;
									}
								}
							} else {
								tmp = 0;
							}
						}
					} else {
						tmp = 0;
					}
				}
			} else {
				tmp = 0;
			}
		}
		retValue_acc = tmp;
		if (__SELECTED_FEATURE_Base) {
			feat_Base: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_Keys) {
			feat_Keys: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_Encrypt) {
			feat_Encrypt: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_AutoResponder) {
			feat_AutoResp: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_AddressBook) {
			feat_AddressBook: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_Sign) {
			feat_Sign: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_Forward) {
			feat_Forward: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_Verify) {
			feat_Verify: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_Decrypt) {
			feat_Decrypt: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_security) {
			feat_security: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_signature) {
			feat_Signature: helper = helper + 1;
		}
		if (__SELECTED_FEATURE_helper_keys) {
			feat_helper_keys: helper = helper + 1;
		}
	}
	return (retValue_acc);
}

int queue_empty = 1;
int queued_message;
int queued_client;

void mail(int client, int msg) {
	int __utac__ad__arg1;
	int __utac__ad__arg2;
	int tmp;

	__utac__ad__arg1 = client;
	__utac__ad__arg2 = msg;
	__utac_acc__AddressBookEncrypt_spec__1(__utac__ad__arg1, __utac__ad__arg2);
	__utac__ad__arg1 = msg;
	__utac_acc__SignVerify_spec__1(__utac__ad__arg1);
	__utac__ad__arg1 = client;
	__utac__ad__arg2 = msg;
	__utac_acc__SignForward_spec__1(__utac__ad__arg1, __utac__ad__arg2);
	__utac__ad__arg1 = msg;
	__utac_acc__EncryptDecrypt_spec__1(__utac__ad__arg1);
	__utac__ad__arg1 = msg;
	__utac_acc__EncryptAutoResponder_spec__2(__utac__ad__arg1);
	__utac__ad__arg1 = msg;
	__utac_acc__EncryptForward_spec__2(__utac__ad__arg1);
#ifdef PRINTING
	puts("mail sent");
#endif // PRINTING
	tmp = getEmailTo(msg);
	incoming(tmp, msg);
	return;
}

void outgoing__before__Encrypt(int client, int msg) {
	int tmp;
	tmp = getClientId(client);
	setEmailFrom(msg, tmp);
	mail(client, msg);
	return;
}

void outgoing__role__Encrypt(int client, int msg) {
	int receiver;
	int pubkey;
	receiver = getEmailTo(msg);
	pubkey = findPublicKey(client, receiver);
	if (pubkey) {
		setEmailEncryptionKey(msg, pubkey);
		setEmailIsEncrypted(msg, 1);
	} else {

	}
	outgoing__before__Encrypt(client, msg);
	return;
}

void outgoing__before__AddressBook(int client, int msg) {
	if (__SELECTED_FEATURE_Encrypt) {
		outgoing__role__Encrypt(client, msg);
		return;
	} else {
		outgoing__before__Encrypt(client, msg);
		return;
	}
}

void outgoing__role__AddressBook(int client, int msg) {
	int size;
	int receiver;
	int second;
	int tmp;
	size = getClientAddressBookSize(client);
	if (size) {
		sendToAddressBook(client, msg);
#ifdef PRINTING
		puts("sending to alias in address book\n");
#endif // PRINTING
		receiver = getEmailTo(msg);
#ifdef PRINTING
		puts("sending to second receipient\n");
#endif // PRINTING
		second = getClientAddressBookAddress(client, 1);
		setEmailTo(msg, second);
		outgoing__before__AddressBook(client, msg);
		tmp = getClientAddressBookAddress(client, 0);
		setEmailTo(msg, tmp);
		outgoing__before__AddressBook(client, msg);
	} else {
		outgoing__before__AddressBook(client, msg);
	}
	return;
}

void outgoing__before__Sign(int client, int msg) {
	if (__SELECTED_FEATURE_AddressBook) {
		outgoing__role__AddressBook(client, msg);
		return;
	} else {
		outgoing__before__AddressBook(client, msg);
		return;
	}
}

void outgoing__role__Sign(int client, int msg) {
	sign(client, msg);
	outgoing__before__Sign(client, msg);
	return;
}

void outgoing(int client, int msg) {
	if (__SELECTED_FEATURE_Sign) {
		outgoing__role__Sign(client, msg);
		return;
	} else {
		outgoing__before__Sign(client, msg);
		return;
	}
}

void deliver(int client, int msg) {
	int __utac__ad__arg1;
	int __utac__ad__arg2;
	__utac__ad__arg1 = client;
	__utac__ad__arg2 = msg;
	__utac_acc__VerifyForward_spec__1(__utac__ad__arg1, __utac__ad__arg2);
#ifdef PRINTING
	puts("mail delivered\n");
#endif // PRINTING
	return;
}

void incoming__before__AutoResponder(int client, int msg) {
	deliver(client, msg);
	return;
}

void incoming__role__AutoResponder(int client, int msg) {
	int tmp;
	incoming__before__AutoResponder(client, msg);
	tmp = getClientAutoResponse(client);
	if (tmp) {
		autoRespond(client, msg);
	} else {

	}
	return;
}

void incoming__before__Forward(int client, int msg) {
	if (__SELECTED_FEATURE_AutoResponder) {
		incoming__role__AutoResponder(client, msg);
		return;
	} else {
		incoming__before__AutoResponder(client, msg);
		return;
	}
}

void incoming__role__Forward(int client, int msg) {
	int fwreceiver;
	incoming__before__Forward(client, msg);
	fwreceiver = getClientForwardReceiver(client);
	if (fwreceiver) {
		setEmailTo(msg, fwreceiver);
		forward(client, msg);
	} else {

	}
	return;
}

void incoming__before__Verify(int client, int msg) {
	if (__SELECTED_FEATURE_Forward) {
		incoming__role__Forward(client, msg);
		return;
	} else {
		incoming__before__Forward(client, msg);
		return;
	}
}

void incoming__role__Verify(int client, int msg) {
	verify(client, msg);
	incoming__before__Verify(client, msg);
	return;
}

void incoming__before__Decrypt(int client, int msg) {
	if (__SELECTED_FEATURE_Verify) {
		incoming__role__Verify(client, msg);
		return;
	} else {
		incoming__before__Verify(client, msg);
		return;
	}
}

void incoming__role__Decrypt(int client, int msg) {
	int privkey;
	int tmp___0;
	int tmp___1;
	int tmp___2;
	privkey = getClientPrivateKey(client);
	if (privkey) {
		tmp___0 = isEncrypted(msg);
		if (tmp___0) {
			tmp___1 = getEmailEncryptionKey(msg);
			tmp___2 = isKeyPairValid(tmp___1, privkey);
			if (tmp___2) {
				setEmailIsEncrypted(msg, 0);
				setEmailEncryptionKey(msg, 0);
			} else {

			}
		} else {

		}
	} else {

	}
	incoming__before__Decrypt(client, msg);
	return;
}

void incoming(int client, int msg) {
	int __utac__ad__arg1;
	int __utac__ad__arg2;
	__utac__ad__arg1 = client;
	__utac__ad__arg2 = msg;
	__utac_acc__EncryptDecrypt_spec__2(__utac__ad__arg1, __utac__ad__arg2);
	__utac__ad__arg1 = msg;
	__utac_acc__EncryptAutoResponder_spec__1(__utac__ad__arg1);
	__utac__ad__arg1 = msg;
	__utac_acc__EncryptForward_spec__1(__utac__ad__arg1);
	if (__SELECTED_FEATURE_Decrypt) {
		incoming__role__Decrypt(client, msg);
		return;
	} else {
		incoming__before__Decrypt(client, msg);
		return;
	}
}

//int createClient(char *name)
int createClient() {
	int retValue_acc;
	retValue_acc = initClient();
	return (retValue_acc);
}

void sendEmail(int sender, int receiver) {
	int email;
	email = createEmail(0, receiver);
	outgoing(sender, email);
	return;
}

void queue(int client, int msg) {
	queue_empty = 0;
	queued_message = msg;
	queued_client = client;
	return;
}

int is_queue_empty(void) {
	return queue_empty;
}

int get_queued_client(void) {
	return queued_client;
}

int get_queued_email(void) {
	return queued_message;
}

int isKeyPairValid(int publicKey, int privateKey) {
	int retValue_acc;
	if (!publicKey) {
		retValue_acc = 0;
		return (retValue_acc);
	} else {
		if (!privateKey) {
			retValue_acc = 0;
			return (retValue_acc);
		} else {

		}
	}
	retValue_acc = privateKey == publicKey;
	return (retValue_acc);
}

void generateKeyPair(int client, int seed) {
	setClientPrivateKey(client, seed);
	return;
}

void autoRespond(int client, int msg) {
	int __utac__ad__arg1;
	int __utac__ad__arg2;
	int sender;
	__utac__ad__arg1 = client;
	__utac__ad__arg2 = msg;
	__utac_acc__DecryptAutoResponder_spec__1(__utac__ad__arg1,
			__utac__ad__arg2);
#ifdef PRINTING
	puts("sending autoresponse\n");
#endif // PRINTING
	sender = getEmailFrom(msg);
	setEmailTo(msg, sender);
	queue(client, msg);
	return;
}

void sendToAddressBook(int client, int msg) {
	return;
}

void sign(int client, int msg) {
	int privkey;
	privkey = getClientPrivateKey(client);
	if (!privkey) {
		return;
	} else {

	}
	setEmailIsSigned(msg, 1);
	setEmailSignKey(msg, privkey);
	return;
}

void forward(int client, int msg) {
	__utac_acc__DecryptForward_spec__1(msg);
#ifdef PRINTING
	puts("Forwarding message.\n");
	printMail(msg);
#endif // PRINTING
	queue(client, msg);
	return;
}

void verify(int client, int msg) {
	int pubkey;
	int __utac__ad__arg1;
	__utac__ad__arg1 = msg;
	__utac_acc__EncryptVerify_spec__1(__utac__ad__arg1);
	__utac_acc__SignVerify_spec__2(client, msg);
	if (isReadable(msg)) {
		if (isSigned(msg)) {

		} else {
			return;
		}
	} else {
		return;
	}
	pubkey = findPublicKey(client, getEmailFrom(msg));
	if (pubkey) {
		if (isKeyPairValid(getEmailSignKey(msg), pubkey)) {
			setEmailIsSignatureVerified(msg, 1);
		} else {

		}
	} else {

	}
	return;
}

/*int prompt(char *msg)
 {
 int retValue_acc;
 int retval;
 char const   * __restrict  __cil_tmp4;

 __cil_tmp4 = (char const   * __restrict)"%s\n";
 printf(__cil_tmp4, msg);
 retValue_acc = retval;
 return (retValue_acc);
 }*/

/*char *getClientName(int handle);
 void setClientName(int handle, char *value);
 int getClientOutbuffer(int handle);
 void setClientOutbuffer(int handle, int value);
 void setClientAddressBookSize(int handle, int value);
 int createClientAddressBookEntry(int handle);
 int getClientAddressBookAlias(int handle, int index);
 void setClientAddressBookAlias(int handle, int index, int value);
 void setClientAddressBookAddress(int handle, int index, int value);
 void setClientAutoResponse(int handle, int value);
 int getClientKeyringSize(int handle);
 int createClientKeyringEntry(int handle);
 int getClientKeyringUser(int handle, int index);
 void setClientKeyringUser(int handle, int index, int value);
 int getClientKeyringPublicKey(int handle, int index);
 void setClientKeyringPublicKey(int handle, int index, int value);
 void setClientForwardReceiver(int handle, int value);
 void setClientId(int handle, int value);
 int findClientAddressBookAlias(int handle, int userid);*/

int __ste_Client_counter = 0;

int initClient(void) {
	int retValue_acc;
	if (__ste_Client_counter < 3) {
		__ste_Client_counter = __ste_Client_counter + 1;
		retValue_acc = __ste_Client_counter;
		return (retValue_acc);
	} else {
		retValue_acc = -1;
		return (retValue_acc);
	}
}

char *__ste_client_name0 = (char *) 0;
char *__ste_client_name1 = (char *) 0;
char *__ste_client_name2 = (char *) 0;

char *getClientName(int handle) {
	char *retValue_acc;
	void *__cil_tmp3;
	if (handle == 1) {
		retValue_acc = __ste_client_name0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_client_name1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_client_name2;
				return (retValue_acc);
			} else {
				__cil_tmp3 = (void *) 0;
				retValue_acc = (char *) __cil_tmp3;
				return (retValue_acc);
			}
		}
	}
	return (retValue_acc);
}

void setClientName(int handle, char *value) {
	if (handle == 1) {
		__ste_client_name0 = value;
	} else {
		if (handle == 2) {
			__ste_client_name1 = value;
		} else {
			if (handle == 3) {
				__ste_client_name2 = value;
			} else {

			}
		}
	}
	return;
}

int __ste_client_outbuffer0 = 0;
int __ste_client_outbuffer1 = 0;
int __ste_client_outbuffer2 = 0;
int __ste_client_outbuffer3 = 0;

int getClientOutbuffer(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_client_outbuffer0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_client_outbuffer1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_client_outbuffer2;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
	return (retValue_acc);
}

void setClientOutbuffer(int handle, int value) {
	if (handle == 1) {
		__ste_client_outbuffer0 = value;
	} else {
		if (handle == 2) {
			__ste_client_outbuffer1 = value;
		} else {
			if (handle == 3) {
				__ste_client_outbuffer2 = value;
			} else {

			}
		}
	}
	return;
}

int __ste_ClientAddressBook_size0 = 0;
int __ste_ClientAddressBook_size1 = 0;
int __ste_ClientAddressBook_size2 = 0;

int getClientAddressBookSize(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_ClientAddressBook_size0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_ClientAddressBook_size1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_ClientAddressBook_size2;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
	return (retValue_acc);
}

void setClientAddressBookSize(int handle, int value) {
	if (handle == 1) {
		__ste_ClientAddressBook_size0 = value;
	} else {
		if (handle == 2) {
			__ste_ClientAddressBook_size1 = value;
		} else {
			if (handle == 3) {
				__ste_ClientAddressBook_size2 = value;
			} else {

			}
		}
	}
	return;
}

int createClientAddressBookEntry(int handle) {
	int retValue_acc;
	int size;
	size = getClientAddressBookSize(handle);
	if (size < 3) {
		{
			setClientAddressBookSize(handle, size + 1);
			retValue_acc = size + 1;
		}
		return (retValue_acc);
	} else {
		retValue_acc = -1;
		return (retValue_acc);
	}
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
	int retValue_acc;
	if (handle == 1) {
		if (index == 0) {
			retValue_acc = __ste_Client_AddressBook0_Alias0;
			return (retValue_acc);
		} else {
			if (index == 1) {
				retValue_acc = __ste_Client_AddressBook0_Alias1;
				return (retValue_acc);
			} else {
				if (index == 2) {
					retValue_acc = __ste_Client_AddressBook0_Alias2;
					return (retValue_acc);
				} else {
					retValue_acc = 0;
					return (retValue_acc);
				}
			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				retValue_acc = __ste_Client_AddressBook1_Alias0;
				return (retValue_acc);
			} else {
				if (index == 1) {
					retValue_acc = __ste_Client_AddressBook1_Alias1;
					return (retValue_acc);
				} else {
					if (index == 2) {
						retValue_acc = __ste_Client_AddressBook1_Alias2;
						return (retValue_acc);
					} else {
						retValue_acc = 0;
						return (retValue_acc);
					}
				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					retValue_acc = __ste_Client_AddressBook2_Alias0;
					return (retValue_acc);
				} else {
					if (index == 1) {
						retValue_acc = __ste_Client_AddressBook2_Alias1;
						return (retValue_acc);
					} else {
						if (index == 2) {
							retValue_acc = __ste_Client_AddressBook2_Alias2;
							return (retValue_acc);
						} else {
							retValue_acc = 0;
							return (retValue_acc);
						}
					}
				}
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
}

int findClientAddressBookAlias(int handle, int userid) {
	int retValue_acc;
	if (handle == 1) {
		if (userid == __ste_Client_AddressBook0_Alias0) {
			retValue_acc = 0;
			return (retValue_acc);
		} else {
			if (userid == __ste_Client_AddressBook0_Alias1) {
				retValue_acc = 1;
				return (retValue_acc);
			} else {
				if (userid == __ste_Client_AddressBook0_Alias2) {
					retValue_acc = 2;
					return (retValue_acc);
				} else {
					retValue_acc = -1;
					return (retValue_acc);
				}
			}
		}
	} else {
		if (handle == 2) {
			if (userid == __ste_Client_AddressBook1_Alias0) {
				retValue_acc = 0;
				return (retValue_acc);
			} else {
				if (userid == __ste_Client_AddressBook1_Alias1) {
					retValue_acc = 1;
					return (retValue_acc);
				} else {
					if (userid == __ste_Client_AddressBook1_Alias2) {
						retValue_acc = 2;
						return (retValue_acc);
					} else {
						retValue_acc = -1;
						return (retValue_acc);
					}
				}
			}
		} else {
			if (handle == 3) {
				if (userid == __ste_Client_AddressBook2_Alias0) {
					retValue_acc = 0;
					return (retValue_acc);
				} else {
					if (userid == __ste_Client_AddressBook2_Alias1) {
						retValue_acc = 1;
						return (retValue_acc);
					} else {
						if (userid == __ste_Client_AddressBook2_Alias2) {
							retValue_acc = 2;
							return (retValue_acc);
						} else {
							retValue_acc = -1;
							return (retValue_acc);
						}
					}
				}
			} else {
				retValue_acc = -1;
				return (retValue_acc);
			}
		}
	}
}

void setClientAddressBookAlias(int handle, int index, int value) {
	if (handle == 1) {
		if (index == 0) {
			__ste_Client_AddressBook0_Alias0 = value;
		} else {
			if (index == 1) {
				__ste_Client_AddressBook0_Alias1 = value;
			} else {
				if (index == 2) {
					__ste_Client_AddressBook0_Alias2 = value;
				} else {

				}
			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				__ste_Client_AddressBook1_Alias0 = value;
			} else {
				if (index == 1) {
					__ste_Client_AddressBook1_Alias1 = value;
				} else {
					if (index == 2) {
						__ste_Client_AddressBook1_Alias2 = value;
					} else {

					}
				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					__ste_Client_AddressBook2_Alias0 = value;
				} else {
					if (index == 1) {
						__ste_Client_AddressBook2_Alias1 = value;
					} else {
						if (index == 2) {
							__ste_Client_AddressBook2_Alias2 = value;
						} else {

						}
					}
				}
			} else {

			}
		}
	}
	return;
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
	int retValue_acc;
	if (handle == 1) {
		if (index == 0) {
			retValue_acc = __ste_Client_AddressBook0_Address0;
			return (retValue_acc);
		} else {
			if (index == 1) {
				retValue_acc = __ste_Client_AddressBook0_Address1;
				return (retValue_acc);
			} else {
				if (index == 2) {
					retValue_acc = __ste_Client_AddressBook0_Address2;
					return (retValue_acc);
				} else {
					retValue_acc = 0;
					return (retValue_acc);
				}
			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				retValue_acc = __ste_Client_AddressBook1_Address0;
				return (retValue_acc);
			} else {
				if (index == 1) {
					retValue_acc = __ste_Client_AddressBook1_Address1;
					return (retValue_acc);
				} else {
					if (index == 2) {
						retValue_acc = __ste_Client_AddressBook1_Address2;
						return (retValue_acc);
					} else {
						retValue_acc = 0;
						return (retValue_acc);
					}
				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					retValue_acc = __ste_Client_AddressBook2_Address0;
					return (retValue_acc);
				} else {
					if (index == 1) {
						retValue_acc = __ste_Client_AddressBook2_Address1;
						return (retValue_acc);
					} else {
						if (index == 2) {
							retValue_acc = __ste_Client_AddressBook2_Address2;
							return (retValue_acc);
						} else {
							retValue_acc = 0;
							return (retValue_acc);
						}
					}
				}
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
	return (retValue_acc);
}

void setClientAddressBookAddress(int handle, int index, int value) {
	if (handle == 1) {
		if (index == 0) {
			__ste_Client_AddressBook0_Address0 = value;
		} else {
			if (index == 1) {
				__ste_Client_AddressBook0_Address1 = value;
			} else {
				if (index == 2) {
					__ste_Client_AddressBook0_Address2 = value;
				} else {

				}
			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				__ste_Client_AddressBook1_Address0 = value;
			} else {
				if (index == 1) {
					__ste_Client_AddressBook1_Address1 = value;
				} else {
					if (index == 2) {
						__ste_Client_AddressBook1_Address2 = value;
					} else {

					}
				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					__ste_Client_AddressBook2_Address0 = value;
				} else {
					if (index == 1) {
						__ste_Client_AddressBook2_Address1 = value;
					} else {
						if (index == 2) {
							__ste_Client_AddressBook2_Address2 = value;
						} else {

						}
					}
				}
			} else {

			}
		}
	}
	return;
}

int __ste_client_autoResponse0 = 0;
int __ste_client_autoResponse1 = 0;
int __ste_client_autoResponse2 = 0;

int getClientAutoResponse(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_client_autoResponse0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_client_autoResponse1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_client_autoResponse2;
				return (retValue_acc);
			} else {
				retValue_acc = -1;
				return (retValue_acc);
			}
		}
	}
}

void setClientAutoResponse(int handle, int value) {
	if (handle == 1) {
		__ste_client_autoResponse0 = value;
	} else {
		if (handle == 2) {
			__ste_client_autoResponse1 = value;
		} else {
			if (handle == 3) {
				__ste_client_autoResponse2 = value;
			} else {

			}
		}
	}
	return;
}

int __ste_client_privateKey0 = 0;
int __ste_client_privateKey1 = 0;
int __ste_client_privateKey2 = 0;

int getClientPrivateKey(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_client_privateKey0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_client_privateKey1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_client_privateKey2;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
}

void setClientPrivateKey(int handle, int value) {
	if (handle == 1) {
		__ste_client_privateKey0 = value;
	} else {
		if (handle == 2) {
			__ste_client_privateKey1 = value;
		} else {
			if (handle == 3) {
				__ste_client_privateKey2 = value;
			} else {

			}
		}
	}
	return;
}

int __ste_ClientKeyring_size0 = 0;
int __ste_ClientKeyring_size1 = 0;
int __ste_ClientKeyring_size2 = 0;

int getClientKeyringSize(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_ClientKeyring_size0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_ClientKeyring_size1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_ClientKeyring_size2;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
	return (retValue_acc);
}

void setClientKeyringSize(int handle, int value) {
	if (handle == 1) {
		__ste_ClientKeyring_size0 = value;
	} else {
		if (handle == 2) {
			__ste_ClientKeyring_size1 = value;
		} else {
			if (handle == 3) {
				__ste_ClientKeyring_size2 = value;
			} else {

			}
		}
	}
	return;
}

int createClientKeyringEntry(int handle) {
	int retValue_acc;
	int size;
	size = getClientKeyringSize(handle);
	if (size < 2) {
		{
			setClientKeyringSize(handle, size + 1);
			retValue_acc = size + 1;
		}
		return (retValue_acc);
	} else {
		retValue_acc = -1;
		return (retValue_acc);
	}
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
	int retValue_acc;
	if (handle == 1) {
		if (index == 0) {
			retValue_acc = __ste_Client_Keyring0_User0;
			return (retValue_acc);
		} else {
			if (index == 1) {
				retValue_acc = __ste_Client_Keyring0_User1;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				retValue_acc = __ste_Client_Keyring1_User0;
				return (retValue_acc);
			} else {
				if (index == 1) {
					retValue_acc = __ste_Client_Keyring1_User1;
					return (retValue_acc);
				} else {
					retValue_acc = 0;
					return (retValue_acc);
				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					retValue_acc = __ste_Client_Keyring2_User0;
					return (retValue_acc);
				} else {
					if (index == 1) {
						retValue_acc = __ste_Client_Keyring2_User1;
						return (retValue_acc);
					} else {
						retValue_acc = 0;
						return (retValue_acc);
					}
				}
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
}

void setClientKeyringUser(int handle, int index, int value) {
	if (handle == 1) {
		if (index == 0) {
			__ste_Client_Keyring0_User0 = value;
		} else {
			if (index == 1) {
				__ste_Client_Keyring0_User1 = value;
			} else {

			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				__ste_Client_Keyring1_User0 = value;
			} else {
				if (index == 1) {
					__ste_Client_Keyring1_User1 = value;
				} else {

				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					__ste_Client_Keyring2_User0 = value;
				} else {
					if (index == 1) {
						__ste_Client_Keyring2_User1 = value;
					} else {

					}
				}
			} else {

			}
		}
	}
	return;
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
	int retValue_acc;
	if (handle == 1) {
		if (index == 0) {
			retValue_acc = __ste_Client_Keyring0_PublicKey0;
			return (retValue_acc);
		} else {
			if (index == 1) {
				retValue_acc = __ste_Client_Keyring0_PublicKey1;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				retValue_acc = __ste_Client_Keyring1_PublicKey0;
				return (retValue_acc);
			} else {
				if (index == 1) {
					retValue_acc = __ste_Client_Keyring1_PublicKey1;
					return (retValue_acc);
				} else {
					retValue_acc = 0;
					return (retValue_acc);
				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					retValue_acc = __ste_Client_Keyring2_PublicKey0;
					return (retValue_acc);
				} else {
					if (index == 1) {
						retValue_acc = __ste_Client_Keyring2_PublicKey1;
						return (retValue_acc);
					} else {
						retValue_acc = 0;
						return (retValue_acc);
					}
				}
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
}

int findPublicKey(int handle, int userid) {
	int retValue_acc;
	if (handle == 1) {
		if (userid == __ste_Client_Keyring0_User0) {
			retValue_acc = __ste_Client_Keyring0_PublicKey0;
			return (retValue_acc);
		} else {
			if (userid == __ste_Client_Keyring0_User1) {
				retValue_acc = __ste_Client_Keyring0_PublicKey1;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	} else {
		if (handle == 2) {
			if (userid == __ste_Client_Keyring1_User0) {
				retValue_acc = __ste_Client_Keyring1_PublicKey0;
				return (retValue_acc);
			} else {
				if (userid == __ste_Client_Keyring1_User1) {
					retValue_acc = __ste_Client_Keyring1_PublicKey1;
					return (retValue_acc);
				} else {
					retValue_acc = 0;
					return (retValue_acc);
				}
			}
		} else {
			if (handle == 3) {
				if (userid == __ste_Client_Keyring2_User0) {
					retValue_acc = __ste_Client_Keyring2_PublicKey0;
					return (retValue_acc);
				} else {
					if (userid == __ste_Client_Keyring2_User1) {
						retValue_acc = __ste_Client_Keyring2_PublicKey1;
						return (retValue_acc);
					} else {
						retValue_acc = 0;
						return (retValue_acc);
					}
				}
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
}

void setClientKeyringPublicKey(int handle, int index, int value) {
	if (handle == 1) {
		if (index == 0) {
			__ste_Client_Keyring0_PublicKey0 = value;
		} else {
			if (index == 1) {
				__ste_Client_Keyring0_PublicKey1 = value;
			} else {

			}
		}
	} else {
		if (handle == 2) {
			if (index == 0) {
				__ste_Client_Keyring1_PublicKey0 = value;
			} else {
				if (index == 1) {
					__ste_Client_Keyring1_PublicKey1 = value;
				} else {

				}
			}
		} else {
			if (handle == 3) {
				if (index == 0) {
					__ste_Client_Keyring2_PublicKey0 = value;
				} else {
					if (index == 1) {
						__ste_Client_Keyring2_PublicKey1 = value;
					} else {

					}
				}
			} else {

			}
		}
	}
	return;
}

int __ste_client_forwardReceiver0 = 0;
int __ste_client_forwardReceiver1 = 0;
int __ste_client_forwardReceiver2 = 0;
int __ste_client_forwardReceiver3 = 0;

int getClientForwardReceiver(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_client_forwardReceiver0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_client_forwardReceiver1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_client_forwardReceiver2;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
	return (retValue_acc);
}

void setClientForwardReceiver(int handle, int value) {
	if (handle == 1) {
		__ste_client_forwardReceiver0 = value;
	} else {
		if (handle == 2) {
			__ste_client_forwardReceiver1 = value;
		} else {
			if (handle == 3) {
				__ste_client_forwardReceiver2 = value;
			} else {

			}
		}
	}
	return;
}

int __ste_client_idCounter0 = 0;
int __ste_client_idCounter1 = 0;
int __ste_client_idCounter2 = 0;
int getClientId(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_client_idCounter0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_client_idCounter1;
			return (retValue_acc);
		} else {
			if (handle == 3) {
				retValue_acc = __ste_client_idCounter2;
				return (retValue_acc);
			} else {
				retValue_acc = 0;
				return (retValue_acc);
			}
		}
	}
}

void setClientId(int handle, int value) {
	if (handle == 1) {
		__ste_client_idCounter0 = value;
	} else {
		if (handle == 2) {
			__ste_client_idCounter1 = value;
		} else {
			if (handle == 3) {
				__ste_client_idCounter2 = value;
			} else {

			}
		}
	}
	return;
}

//int initEmail(void);
//int getEmailId(int handle);
//void setEmailId(int handle, int value);
//char *getEmailSubject(int handle);
//void setEmailSubject(int handle, char *value);
//char *getEmailBody(int handle);
//void setEmailBody(int handle, char *value);
//int isVerified(int handle);

int __ste_Email_counter = 0;

int initEmail(void) {
	int retValue_acc;
	if (__ste_Email_counter < 2) {
		__ste_Email_counter = __ste_Email_counter + 1;
		retValue_acc = __ste_Email_counter;
		return (retValue_acc);
	} else {
		retValue_acc = -1;
		return (retValue_acc);
	}
}

int __ste_email_id0 = 0;
int __ste_email_id1 = 0;

int getEmailId(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_id0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_id1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
}

void setEmailId(int handle, int value) {
	if (handle == 1) {
		__ste_email_id0 = value;
	} else {
		if (handle == 2) {
			__ste_email_id1 = value;
		} else {

		}
	}
	return;
}

int __ste_email_from0 = 0;
int __ste_email_from1 = 0;

int getEmailFrom(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_from0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_from1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
}

void setEmailFrom(int handle, int value) {
	if (handle == 1) {
		__ste_email_from0 = value;
	} else {
		if (handle == 2) {
			__ste_email_from1 = value;
		} else {

		}
	}
	return;
}

int __ste_email_to0 = 0;
int __ste_email_to1 = 0;

int getEmailTo(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_to0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_to1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
}

void setEmailTo(int handle, int value) {
	if (handle == 1) {
		__ste_email_to0 = value;
	} else {
		if (handle == 2) {
			__ste_email_to1 = value;
		} else {

		}
	}
	return;
}

char *__ste_email_subject0;
char *__ste_email_subject1;

char *getEmailSubject(int handle) {
	char *retValue_acc;
	void *__cil_tmp3;
	if (handle == 1) {
		retValue_acc = __ste_email_subject0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_subject1;
			return (retValue_acc);
		} else {
			__cil_tmp3 = (void *) 0;
			retValue_acc = (char *) __cil_tmp3;
			return (retValue_acc);
		}
	}
}

void setEmailSubject(int handle, char *value) {
	if (handle == 1) {
		__ste_email_subject0 = value;
	} else {
		if (handle == 2) {
			__ste_email_subject1 = value;
		} else {

		}
	}
	return;
}

char *__ste_email_body0 = (char *) 0;
char *__ste_email_body1 = (char *) 0;

char *getEmailBody(int handle) {
	char *retValue_acc;
	void *__cil_tmp3;
	if (handle == 1) {
		retValue_acc = __ste_email_body0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_body1;
			return (retValue_acc);
		} else {
			__cil_tmp3 = (void *) 0;
			retValue_acc = (char *) __cil_tmp3;
			return (retValue_acc);
		}
	}
}

void setEmailBody(int handle, char *value) {
	if (handle == 1) {
		__ste_email_body0 = value;
	} else {
		if (handle == 2) {
			__ste_email_body1 = value;
		} else {

		}
	}
	return;
}

int __ste_email_isEncrypted0 = 0;
int __ste_email_isEncrypted1 = 0;

int isEncrypted(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_isEncrypted0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_isEncrypted1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
}

void setEmailIsEncrypted(int handle, int value) {
	if (handle == 1) {
		__ste_email_isEncrypted0 = value;
	} else {
		if (handle == 2) {
			__ste_email_isEncrypted1 = value;
		} else {

		}
	}
	return;
}

int __ste_email_encryptionKey0 = 0;
int __ste_email_encryptionKey1 = 0;

int getEmailEncryptionKey(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_encryptionKey0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_encryptionKey1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
}

void setEmailEncryptionKey(int handle, int value) {
	if (handle == 1) {
		__ste_email_encryptionKey0 = value;
	} else {
		if (handle == 2) {
			__ste_email_encryptionKey1 = value;
		} else {

		}
	}
	return;
}

int __ste_email_isSigned0 = 0;
int __ste_email_isSigned1 = 0;

int isSigned(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_isSigned0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_isSigned1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
}

void setEmailIsSigned(int handle, int value) {
	if (handle == 1) {
		__ste_email_isSigned0 = value;
	} else {
		if (handle == 2) {
			__ste_email_isSigned1 = value;
		} else {

		}
	}
	return;
}

int __ste_email_signKey0 = 0;
int __ste_email_signKey1 = 0;

int getEmailSignKey(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_signKey0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_signKey1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
	return (retValue_acc);
}

void setEmailSignKey(int handle, int value) {
	if (handle == 1) {
		__ste_email_signKey0 = value;
	} else {
		if (handle == 2) {
			__ste_email_signKey1 = value;
		} else {

		}
	}
	return;
}

int __ste_email_isSignatureVerified0;
int __ste_email_isSignatureVerified1;

int isVerified(int handle) {
	int retValue_acc;
	if (handle == 1) {
		retValue_acc = __ste_email_isSignatureVerified0;
		return (retValue_acc);
	} else {
		if (handle == 2) {
			retValue_acc = __ste_email_isSignatureVerified1;
			return (retValue_acc);
		} else {
			retValue_acc = 0;
			return (retValue_acc);
		}
	}
	return (retValue_acc);
}

void setEmailIsSignatureVerified(int handle, int value) {
	if (handle == 1) {
		__ste_email_isSignatureVerified0 = value;
	} else {
		if (handle == 2) {
			__ste_email_isSignatureVerified1 = value;
		} else {

		}
	}
	return;
}

int bob;
int rjh;
int chuck;

//void setup_bob(int bob___0);
//void setup_rjh(int rjh___0);
//void setup_chuck(int chuck___0);
//void bobToRjh(void);
//void rjhToBob(void);
//void setup(void);
//int main(void);
//void bobKeyAdd(void);
//void bobKeyAddChuck(void);
//void rjhKeyAdd(void);
//void rjhKeyAddChuck(void);
//void chuckKeyAdd(void);
//void bobKeyChange(void);
//void rjhKeyChange(void);
//void rjhDeletePrivateKey(void);
//void chuckKeyAddRjh(void);
//void rjhSetAutoRespond(void);
//void bobSetAddressBook(void);
//void rjhEnableForwarding(void);

void setup_bob__before__Keys(int bob___0) {
	setClientId(bob___0, bob___0);
	return;
}

void setup_bob__role__Keys(int bob___0) {
	setup_bob__before__Keys(bob___0);
	setClientPrivateKey(bob___0, 123);
	return;
}

void setup_bob(int bob___0) {
	if (__SELECTED_FEATURE_Keys) {
		{
			setup_bob__role__Keys(bob___0);
		}
		return;
	} else {
		{
			setup_bob__before__Keys(bob___0);
		}
		return;
	}
}

void setup_rjh__before__Keys(int rjh___0) {
	setClientId(rjh___0, rjh___0);
	return;
}

void setup_rjh__role__Keys(int rjh___0) {
	setup_rjh__before__Keys(rjh___0);
	setClientPrivateKey(rjh___0, 456);
	return;
}

void setup_rjh(int rjh___0) {
	if (__SELECTED_FEATURE_Keys) {
		{
			setup_rjh__role__Keys(rjh___0);
		}
		return;
	} else {
		{
			setup_rjh__before__Keys(rjh___0);
		}
		return;
	}
}

void setup_chuck__before__Keys(int chuck___0) {
	setClientId(chuck___0, chuck___0);
	return;
}

void setup_chuck__role__Keys(int chuck___0) {
	setup_chuck__before__Keys(chuck___0);
	setClientPrivateKey(chuck___0, 789);
	return;
}

void setup_chuck(int chuck___0) {
	if (__SELECTED_FEATURE_Keys) {
		{
			setup_chuck__role__Keys(chuck___0);
		}
		return;
	} else {
		{
			setup_chuck__before__Keys(chuck___0);
		}
		return;
	}
}

void bobToRjh(void) {
	//puts("Please enter a subject and a message body.\n");
	sendEmail(bob, rjh);
	if (is_queue_empty()) {

	} else {
		outgoing(get_queued_client(), get_queued_email());
	}
	return;
}

void rjhToBob(void) {
	sendEmail(rjh, bob);
	return;
}

/*int get_nondet(void)
 {
 int retValue_acc;
 int nd;
 retValue_acc = nd;
 return (retValue_acc);
 }*/

void setup(void) {
	bob = 1;
	setup_bob(bob);
	rjh = 2;
	setup_rjh(rjh);
	chuck = 3;
	setup_chuck(chuck);
	return;
}

int main(void) {
	select_helpers();
	select_features();
	if (valid_product()) {
		{
			setup();
			test();
		}
	} else {

	}
	return 0;
}

void bobKeyAdd(void) {
	createClientKeyringEntry(bob);
	setClientKeyringUser(bob, 0, 2);
	setClientKeyringPublicKey(bob, 0, 456);
#ifdef PRINTING
	puts("bob added rjhs key");
#endif // PRINTING
	return;
}

void rjhKeyAdd(void) {
	createClientKeyringEntry(rjh);
	setClientKeyringUser(rjh, 0, 1);
	setClientKeyringPublicKey(rjh, 0, 123);
	return;
}

void rjhKeyAddChuck(void) {
	createClientKeyringEntry(rjh);
	setClientKeyringUser(rjh, 0, 3);
	setClientKeyringPublicKey(rjh, 0, 789);
	return;
}

void bobKeyAddChuck(void) {
	createClientKeyringEntry(bob);
	setClientKeyringUser(bob, 1, 3);
	setClientKeyringPublicKey(bob, 1, 789);
	return;
}

void chuckKeyAdd(void) {
	createClientKeyringEntry(chuck);
	setClientKeyringUser(chuck, 0, 1);
	setClientKeyringPublicKey(chuck, 0, 123);
	return;
}

void chuckKeyAddRjh(void) {
	createClientKeyringEntry(chuck);
	setClientKeyringUser(chuck, 0, 2);
	setClientKeyringPublicKey(chuck, 0, 456);
	return;
}

void rjhDeletePrivateKey(void) {
	setClientPrivateKey(rjh, 0);
	return;
}

void bobKeyChange(void) {
	generateKeyPair(bob, 777);
	return;
}

void rjhKeyChange(void) {
	generateKeyPair(rjh, 666);
	return;
}

void rjhSetAutoRespond(void) {
	setClientAutoResponse(rjh, 1);
	return;
}

void bobSetAddressBook(void) {
	setClientAddressBookSize(bob, 1);
	setClientAddressBookAlias(bob, 0, rjh);
	setClientAddressBookAddress(bob, 0, rjh);
	setClientAddressBookAddress(bob, 1, chuck);
	return;
}

void rjhEnableForwarding(void) {
	setClientForwardReceiver(rjh, chuck);
	return;
}

void test(void) {
	if (__SELECTED_FEATURE_Keys) {
		if (__VERIFIER_nondet_int()) {
			bobKeyAdd();
		}
	}
	if (__SELECTED_FEATURE_AutoResponder) {
		if (__VERIFIER_nondet_int()) {
			rjhSetAutoRespond();
		}
	}
	if (__SELECTED_FEATURE_Keys) {
		if (__VERIFIER_nondet_int()) {
			rjhDeletePrivateKey();
		}
	}
	if (__SELECTED_FEATURE_Keys) {
		if (__VERIFIER_nondet_int()) {
			rjhKeyAdd();
		}
	}
	if (__SELECTED_FEATURE_Keys) {
		if (__VERIFIER_nondet_int()) {
			chuckKeyAddRjh();
		}
	}
	if (__SELECTED_FEATURE_Forward) {
		if (__VERIFIER_nondet_int()) {
			rjhEnableForwarding();
		}
	}
	if (__SELECTED_FEATURE_Keys) {
		if (__VERIFIER_nondet_int()) {
			rjhKeyChange();
		}
	}
	if (__SELECTED_FEATURE_AddressBook) {
		if (__VERIFIER_nondet_int()) {
			bobSetAddressBook();
		}
	}
	if (__SELECTED_FEATURE_Keys) {
		if (__VERIFIER_nondet_int()) {
			chuckKeyAdd();
		}
	}
	if (__SELECTED_FEATURE_Keys) {
		if (__VERIFIER_nondet_int()) {
			bobKeyChange();
		}
	}
	bobToRjh();
	return;
}

//DAS HIER �BERARBEITEN
// Die Idee ist wohl verscheiden instanzen des Problems zu erzeugen (y)
/*void test(void)
 {
 int op1;
 int op2;
 int op3;
 int op4;
 int op5;
 int op6;
 int op7;
 int op8;
 int op9;
 int op10;
 int op11;
 int splverifierCounter;
 int tmp;
 int tmp___0;
 int tmp___1;
 int tmp___2;
 int tmp___3;
 int tmp___4;
 int tmp___5;
 int tmp___6;
 int tmp___7;
 int tmp___8;
 int tmp___9;
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
 splverifierCounter = 0;
 while (1) {
 while_0_continue: /* CIL Label * /;
 if (splverifierCounter < 4) {

 }
 else {
 goto while_0_break;
 }
 splverifierCounter = splverifierCounter + 1;
 if (!op1) {
 tmp___9 = __VERIFIER_nondet_int();
 if (tmp___9) {
 if (__SELECTED_FEATURE_Keys) {
 bobKeyAdd();
 }
 else {

 }
 op1 = 1;
 }
 else {
 goto _L___8;
 }
 }
 else {
 _L___8: /* CIL Label * /
 if (!op2) {
 tmp___8 = __VERIFIER_nondet_int();
 if (tmp___8) {
 if (__SELECTED_FEATURE_AutoResponder) {
 rjhSetAutoRespond();
 }
 else {

 }
 op2 = 1;
 }
 else {
 goto _L___7;
 }
 }
 else {
 _L___7: /* CIL Label * /
 if (!op3) {
 tmp___7 = __VERIFIER_nondet_int();
 if (tmp___7) {
 if (__SELECTED_FEATURE_Keys) {
 rjhDeletePrivateKey();
 }
 else {

 }
 op3 = 1;
 }
 else {
 goto _L___6;
 }
 }
 else {
 _L___6: /* CIL Label * /
 if (!op4) {
 tmp___6 = __VERIFIER_nondet_int();
 if (tmp___6) {
 if (__SELECTED_FEATURE_Keys) {
 rjhKeyAdd();
 }
 else {

 }
 op4 = 1;
 }
 else {
 goto _L___5;
 }
 }
 else {
 _L___5: /* CIL Label * /
 if (!op5) {
 tmp___5 = __VERIFIER_nondet_int();
 if (tmp___5) {
 if (__SELECTED_FEATURE_Keys) {
 chuckKeyAddRjh();
 }
 else {

 }
 op5 = 1;
 }
 else {
 goto _L___4;
 }
 }
 else {
 _L___4: /* CIL Label * /
 if (!op6) {
 tmp___4 = __VERIFIER_nondet_int();
 if (tmp___4) {
 if (__SELECTED_FEATURE_Forward) {
 rjhEnableForwarding();
 }
 else {

 }
 op6 = 1;
 }
 else {
 goto _L___3;
 }
 }
 else {
 _L___3: /* CIL Label * /
 if (!op7) {
 tmp___3 = __VERIFIER_nondet_int();
 if (tmp___3) {
 if (__SELECTED_FEATURE_Keys) {
 rjhKeyChange();
 }
 else {

 }
 op7 = 1;
 }
 else {
 goto _L___2;
 }
 }
 else {
 _L___2: /* CIL Label * /
 if (!op8) {
 tmp___2 = __VERIFIER_nondet_int();
 if (tmp___2) {
 if (__SELECTED_FEATURE_AddressBook) {
 bobSetAddressBook();
 }
 else {

 }
 op8 = 1;
 }
 else {
 goto _L___1;
 }
 }
 else {
 _L___1: /* CIL Label * /
 if (!op9) {
 tmp___1 = __VERIFIER_nondet_int();
 if (tmp___1) {
 if (__SELECTED_FEATURE_Keys) {
 chuckKeyAdd();
 }
 else {

 }
 op9 = 1;
 }
 else {
 goto _L___0;
 }
 }
 else {
 _L___0: /* CIL Label * /
 if (!op10) {
 tmp___0 = __VERIFIER_nondet_int();
 if (tmp___0) {
 if (__SELECTED_FEATURE_Keys) {
 bobKeyChange();
 }
 else {

 }
 op10 = 1;
 }
 else {
 goto _L;
 }
 }
 else {
 _L: /* CIL Label * /
 if (!op11) {
 tmp = __VERIFIER_nondet_int();
 if (tmp) {
 if (__SELECTED_FEATURE_Keys) {
 chuckKeyAdd();
 }
 else {

 }
 op11 = 1;
 }
 else {
 goto while_0_break;
 }
 }
 else {
 goto while_0_break;
 }
 }
 }
 }
 }
 }
 }
 }
 }
 }
 }
 }
 while_0_break: /* CIL Label * /;
 bobToRjh();
 return;
 }*/

void printMail__before__Encrypt(int msg) {
	int tmp;
	int tmp___0;
	int tmp___1;
	int tmp___2;
	char const * __restrict __cil_tmp6;
	char const * __restrict __cil_tmp7;
	char const * __restrict __cil_tmp8;
	char const * __restrict __cil_tmp9;

	tmp = getEmailId(msg);
	__cil_tmp6 = (char const * __restrict) "ID:\n  %i\n";
	printf(__cil_tmp6, tmp);
	tmp___0 = getEmailFrom(msg);
	__cil_tmp7 = (char const * __restrict) "FROM:\n  %i\n";
	printf(__cil_tmp7, tmp___0);
	tmp___1 = getEmailTo(msg);
	__cil_tmp8 = (char const * __restrict) "TO:\n  %i\n";
	printf(__cil_tmp8, tmp___1);
	tmp___2 = isReadable(msg);
	__cil_tmp9 = (char const * __restrict) "IS_READABLE\n  %i\n";
	printf(__cil_tmp9, tmp___2);
	return;
}

void printMail__role__Encrypt(int msg) {
	int tmp;
	int tmp___0;
	char const * __restrict __cil_tmp4;
	char const * __restrict __cil_tmp5;

	printMail__before__Encrypt(msg);
	tmp = isEncrypted(msg);
	__cil_tmp4 = (char const * __restrict) "ENCRYPTED\n  %d\n";
	printf(__cil_tmp4, tmp);
	tmp___0 = getEmailEncryptionKey(msg);
	__cil_tmp5 = (char const * __restrict) "ENCRYPTION KEY\n  %d\n";
	printf(__cil_tmp5, tmp___0);
	return;
}

void printMail__before__Sign(int msg) {
	if (__SELECTED_FEATURE_Encrypt) {
		printMail__role__Encrypt(msg);
		return;
	} else {
		printMail__before__Encrypt(msg);
		return;
	}
}

void printMail__role__Sign(int msg) {
	int tmp;
	int tmp___0;
	char const * __restrict __cil_tmp4;
	char const * __restrict __cil_tmp5;

	printMail__before__Sign(msg);
	tmp = isSigned(msg);
	__cil_tmp4 = (char const * __restrict) "SIGNED\n  %i\n";
	printf(__cil_tmp4, tmp);
	tmp___0 = getEmailSignKey(msg);
	__cil_tmp5 = (char const * __restrict) "SIGNATURE\n  %i\n";
	printf(__cil_tmp5, tmp___0);
	return;
}

void printMail__before__Verify(int msg) {
	if (__SELECTED_FEATURE_Sign) {
		printMail__role__Sign(msg);
		return;
	} else {
		printMail__before__Sign(msg);
		return;
	}
}

void printMail__role__Verify(int msg) {
	int tmp;
	char const * __restrict __cil_tmp3;
	printMail__before__Verify(msg);
	tmp = isVerified(msg);
	__cil_tmp3 = (char const * __restrict) "SIGNATURE VERIFIED\n  %d\n";
	printf(__cil_tmp3, tmp);
	return;
}

void printMail(int msg) {
	if (__SELECTED_FEATURE_Verify) {
		printMail__role__Verify(msg);
		return;
	} else {
		printMail__before__Verify(msg);
		return;
	}
}

int isReadable__before__Encrypt(int msg) {
	int retValue_acc;

	retValue_acc = 1;
	return (retValue_acc);
}

int isReadable__role__Encrypt(int msg) {
	int retValue_acc;
	int tmp;
	tmp = isEncrypted(msg);
	if (tmp) {
		retValue_acc = 0;
		return (retValue_acc);
	} else {
		retValue_acc = isReadable__before__Encrypt(msg);
		return (retValue_acc);
	}
}

int isReadable(int msg) {
	int retValue_acc;
	if (__SELECTED_FEATURE_Encrypt) {
		retValue_acc = isReadable__role__Encrypt(msg);
		return (retValue_acc);
	} else {
		retValue_acc = isReadable__before__Encrypt(msg);
		return (retValue_acc);
	}
}

int cloneEmail(int msg) {
	int retValue_acc;

	retValue_acc = msg;
	return (retValue_acc);
}

int createEmail(int from, int to) {
	int retValue_acc;
	int msg;
	msg = 1;
	setEmailFrom(msg, from);
	setEmailTo(msg, to);
	retValue_acc = msg;
	return (retValue_acc);
}

/*extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion, char const   *__file, unsigned int __line, char const   *__function);
 extern  __attribute__((__nothrow__)) void *malloc(size_t __size)  __attribute__((__malloc__));
 extern  __attribute__((__nothrow__)) void free(void *__ptr);*/

/*void __utac__exception__cf_handler_set(void *exception, int(*cflow_func)(int,
 int),
 int val)
 {
 struct __UTAC__EXCEPTION *excep;
 struct __UTAC__CFLOW_FUNC *cf;
 void *tmp;
 unsigned long __cil_tmp7;
 unsigned long __cil_tmp8;
 unsigned long __cil_tmp9;
 unsigned long __cil_tmp10;
 unsigned long __cil_tmp11;
 unsigned long __cil_tmp12;
 unsigned long __cil_tmp13;
 unsigned long __cil_tmp14;
 int(**mem_15)(int, int);
 int *mem_16;
 struct __UTAC__CFLOW_FUNC **mem_17;
 struct __UTAC__CFLOW_FUNC **mem_18;
 struct __UTAC__CFLOW_FUNC **mem_19;

 excep = (struct __UTAC__EXCEPTION *)exception;
 tmp = malloc(24UL);
 cf = (struct __UTAC__CFLOW_FUNC *)tmp;
 mem_15 = (int(**)(int, int))cf;
 *mem_15 = cflow_func;
 __cil_tmp7 = (unsigned long)cf;
 __cil_tmp8 = __cil_tmp7 + 8;
 mem_16 = (int *)__cil_tmp8;
 *mem_16 = val;
 __cil_tmp9 = (unsigned long)cf;
 __cil_tmp10 = __cil_tmp9 + 16;
 __cil_tmp11 = (unsigned long)excep;
 __cil_tmp12 = __cil_tmp11 + 24;
 mem_17 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp10;
 mem_18 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp12;
 *mem_17 = *mem_18;
 __cil_tmp13 = (unsigned long)excep;
 __cil_tmp14 = __cil_tmp13 + 24;
 mem_19 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp14;
 *mem_19 = cf;
 return;
 }*/

/*void __utac__exception__cf_handler_free(void *exception)
 {
 struct __UTAC__EXCEPTION *excep;
 struct __UTAC__CFLOW_FUNC *cf;
 struct __UTAC__CFLOW_FUNC *tmp;
 unsigned long __cil_tmp5;
 unsigned long __cil_tmp6;
 struct __UTAC__CFLOW_FUNC *__cil_tmp7;
 unsigned long __cil_tmp8;
 unsigned long __cil_tmp9;
 unsigned long __cil_tmp10;
 unsigned long __cil_tmp11;
 void *__cil_tmp12;
 unsigned long __cil_tmp13;
 unsigned long __cil_tmp14;
 struct __UTAC__CFLOW_FUNC **mem_15;
 struct __UTAC__CFLOW_FUNC **mem_16;
 struct __UTAC__CFLOW_FUNC **mem_17;
 excep = (struct __UTAC__EXCEPTION *)exception;
 __cil_tmp5 = (unsigned long)excep;
 __cil_tmp6 = __cil_tmp5 + 24;
 mem_15 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp6;
 cf = *mem_15;
 while (1) {
 while_1_continue: /* CIL Label * /;
 __cil_tmp7 = (struct __UTAC__CFLOW_FUNC *)0;
 __cil_tmp8 = (unsigned long)__cil_tmp7;
 __cil_tmp9 = (unsigned long)cf;
 if (__cil_tmp9 != __cil_tmp8) {

 }
 else {
 goto while_1_break;
 }
 tmp = cf;
 __cil_tmp10 = (unsigned long)cf;
 __cil_tmp11 = __cil_tmp10 + 16;
 mem_16 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp11;
 cf = *mem_16;
 __cil_tmp12 = (void *)tmp;
 free(__cil_tmp12);
 }
 while_1_break: /* CIL Label * /;
 __cil_tmp13 = (unsigned long)excep;
 __cil_tmp14 = __cil_tmp13 + 24;
 mem_17 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp14;
 *mem_17 = (struct __UTAC__CFLOW_FUNC *)0;
 return;
 }*/

/*void __utac__exception__cf_handler_reset(void *exception)
 {
 struct __UTAC__EXCEPTION *excep;
 struct __UTAC__CFLOW_FUNC *cf;
 unsigned long __cil_tmp5;
 unsigned long __cil_tmp6;
 struct __UTAC__CFLOW_FUNC *__cil_tmp7;
 unsigned long __cil_tmp8;
 unsigned long __cil_tmp9;
 int(*__cil_tmp10)(int, int);
 unsigned long __cil_tmp11;
 unsigned long __cil_tmp12;
 int __cil_tmp13;
 unsigned long __cil_tmp14;
 unsigned long __cil_tmp15;
 struct __UTAC__CFLOW_FUNC **mem_16;
 int(**mem_17)(int, int);
 int *mem_18;
 struct __UTAC__CFLOW_FUNC **mem_19;

 excep = (struct __UTAC__EXCEPTION *)exception;
 __cil_tmp5 = (unsigned long)excep;
 __cil_tmp6 = __cil_tmp5 + 24;
 mem_16 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp6;
 cf = *mem_16;
 while (1) {
 while_2_continue: /* CIL Label * /;
 __cil_tmp7 = (struct __UTAC__CFLOW_FUNC *)0;
 __cil_tmp8 = (unsigned long)__cil_tmp7;
 __cil_tmp9 = (unsigned long)cf;
 if (__cil_tmp9 != __cil_tmp8) {

 }
 else {
 goto while_2_break;
 }
 mem_17 = (int(**)(int, int))cf;
 __cil_tmp10 = *mem_17;
 __cil_tmp11 = (unsigned long)cf;
 __cil_tmp12 = __cil_tmp11 + 8;
 mem_18 = (int *)__cil_tmp12;
 __cil_tmp13 = *mem_18;
 (*__cil_tmp10)(4, __cil_tmp13);
 __cil_tmp14 = (unsigned long)cf;
 __cil_tmp15 = __cil_tmp14 + 16;
 mem_19 = (struct __UTAC__CFLOW_FUNC **)__cil_tmp15;
 cf = *mem_19;
 }
 while_2_break: /* CIL Label * /;
 __utac__exception__cf_handler_free(exception);
 return;
 }*/

//void *__utac__error_stack_mgt(void *env, int mode, int count);
//static struct __ACC__ERR *head = (struct __ACC__ERR *)0;
/*void *__utac__error_stack_mgt(void *env, int mode, int count)
 {
 void *retValue_acc;
 struct __ACC__ERR *new;
 void *tmp;
 struct __ACC__ERR *temp;
 struct __ACC__ERR *next;
 void *excep;
 unsigned long __cil_tmp10;
 unsigned long __cil_tmp11;
 unsigned long __cil_tmp12;
 unsigned long __cil_tmp13;
 void *__cil_tmp14;
 unsigned long __cil_tmp15;
 unsigned long __cil_tmp16;
 void *__cil_tmp17;
 void **mem_18;
 struct __ACC__ERR **mem_19;
 struct __ACC__ERR **mem_20;
 void **mem_21;
 struct __ACC__ERR **mem_22;
 void **mem_23;
 void **mem_24;

 if (count == 0) {
 return (retValue_acc);
 }
 else {

 }
 if (mode == 0) {
 tmp = malloc(16UL);
 new = (struct __ACC__ERR *)tmp;
 mem_18 = (void **)new;
 *mem_18 = env;
 __cil_tmp10 = (unsigned long)new;
 __cil_tmp11 = __cil_tmp10 + 8;
 mem_19 = (struct __ACC__ERR **)__cil_tmp11;
 *mem_19 = head;
 head = new;
 retValue_acc = (void *)new;
 return (retValue_acc);
 }
 else {

 }
 if (mode == 1) {
 temp = head;
 while (1) {
 while_3_continue: /* CIL Label * /;
 if (count > 1) {

 }
 else {
 goto while_3_break;
 }
 __cil_tmp12 = (unsigned long)temp;
 __cil_tmp13 = __cil_tmp12 + 8;
 mem_20 = (struct __ACC__ERR **)__cil_tmp13;
 next = *mem_20;
 mem_21 = (void **)temp;
 excep = *mem_21;
 __cil_tmp14 = (void *)temp;
 free(__cil_tmp14);
 __utac__exception__cf_handler_reset(excep);
 temp = next;
 count = count - 1;
 }
 while_3_break: /* CIL Label * /;
 __cil_tmp15 = (unsigned long)temp;
 __cil_tmp16 = __cil_tmp15 + 8;
 mem_22 = (struct __ACC__ERR **)__cil_tmp16;
 head = *mem_22;
 mem_23 = (void **)temp;
 excep = *mem_23;
 __cil_tmp17 = (void *)temp;
 free(__cil_tmp17);
 __utac__exception__cf_handler_reset(excep);
 retValue_acc = excep;
 return (retValue_acc);
 }
 else {

 }
 if (mode == 2) {
 if (head) {
 mem_24 = (void **)head;
 retValue_acc = *mem_24;
 return (retValue_acc);
 }
 else {
 retValue_acc = (void *)0;
 return (retValue_acc);
 }
 }
 else {

 }
 return (retValue_acc);
 }*/

/*void *__utac__get_this_arg(int i, struct JoinPoint *this)
 {
 void *retValue_acc;
 unsigned long __cil_tmp4;
 unsigned long __cil_tmp5;
 int __cil_tmp6;
 int __cil_tmp7;
 unsigned long __cil_tmp8;
 unsigned long __cil_tmp9;
 void **__cil_tmp10;
 void **__cil_tmp11;
 int *mem_12;
 void ***mem_13;

 if (i > 0) {
 __cil_tmp4 = (unsigned long)this;
 __cil_tmp5 = __cil_tmp4 + 16;
 mem_12 = (int *)__cil_tmp5;
 __cil_tmp6 = *mem_12;
 if (i <= __cil_tmp6) {

 }
 else {
 __assert_fail("i > 0 && i <= this->argsCount", "/home/rhein/workspace/splverifier/splverifier/external/test/autofeature_tmp/libacc.c",
 123U, "__utac__get_this_arg");
 }
 }
 else {
 __assert_fail("i > 0 && i <= this->argsCount", "/home/rhein/workspace/splverifier/splverifier/external/test/autofeature_tmp/libacc.c",
 123U, "__utac__get_this_arg");
 }
 __cil_tmp7 = i - 1;
 __cil_tmp8 = (unsigned long)this;
 __cil_tmp9 = __cil_tmp8 + 8;
 mem_13 = (void ***)__cil_tmp9;
 __cil_tmp10 = *mem_13;
 __cil_tmp11 = __cil_tmp10 + __cil_tmp7;
 retValue_acc = *__cil_tmp11;
 return (retValue_acc);
 }*/

/*char const   *__utac__get_this_argtype(int i, struct JoinPoint *this)
 {
 char const   *retValue_acc;
 unsigned long __cil_tmp4;
 unsigned long __cil_tmp5;
 int __cil_tmp6;
 int __cil_tmp7;
 unsigned long __cil_tmp8;
 unsigned long __cil_tmp9;
 char const   **__cil_tmp10;
 char const   **__cil_tmp11;
 int *mem_12;
 char const   ***mem_13;

 if (i > 0) {
 __cil_tmp4 = (unsigned long)this;
 __cil_tmp5 = __cil_tmp4 + 16;
 mem_12 = (int *)__cil_tmp5;
 __cil_tmp6 = *mem_12;
 if (i <= __cil_tmp6) {

 }
 else {
 __assert_fail("i > 0 && i <= this->argsCount", "/home/rhein/workspace/splverifier/splverifier/external/test/autofeature_tmp/libacc.c",
 131U, "__utac__get_this_argtype");
 }
 }
 else {
 __assert_fail("i > 0 && i <= this->argsCount", "/home/rhein/workspace/splverifier/splverifier/external/test/autofeature_tmp/libacc.c",
 131U, "__utac__get_this_argtype");
 }
 __cil_tmp7 = i - 1;
 __cil_tmp8 = (unsigned long)this;
 __cil_tmp9 = __cil_tmp8 + 24;
 mem_13 = (char const   ***)__cil_tmp9;
 __cil_tmp10 = *mem_13;
 __cil_tmp11 = __cil_tmp10 + __cil_tmp7;
 retValue_acc = *__cil_tmp11;
 return (retValue_acc);
 }*/
