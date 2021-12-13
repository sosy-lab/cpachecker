import phoneBook.Person;

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class PhoneBook {

    private String allPersons;
    private String bookName; 
    
    PhoneBook(String pName){
        this.bookName = pName; 
        allPersons = "";
    }
    
    PhoneBook (String pName, String pAllPersons){
        this.bookName = pName;
        allPersons = pAllPersons;
        
    }
    
//    PhoneBook(String pName, String[] pPersons) {
//        this.bookName = pName; 
//        allPersons = "";
//        for (int i =0;i <pPersons.length;i++) {
//            allPersons = allPersons +pPersons[i].toString();
//        }
//    }
    
    public void addPerson(String pPerson) {
        allPersons = allPersons + pPerson;
    }
    
    public void addPerson(Person pPerson) {
        addPerson(pPerson.toString());
    }
    
    public String getBookName() {
        return bookName;
    }
    
    public String toString() {
        return allPersons;
    }
}
