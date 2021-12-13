// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {

    public static void main(String[] args) {

        String p1FirstName = "Iron";
        String p1SurName = "Man"; 
        String p1Age = "42";
        String p1Gender = "Male"; 
        
        Person p1 = new Person(p1FirstName,p1SurName,p1Gender,p1Age);
        
        String p2FirstName = "Pepper";
        String p2SurName = "Potts";
        String p2Age = "35";
        String p2Gender ="Female";
        
        Person p2 = new Person(p2FirstName, p2SurName, p2Gender,p2Age);
        String allpersons = p1.toString() + p2.toString();
        assert allpersons.equals("(Full Name:Iron Man, Age:42, Gender:Male)(Full Name: Pepper Potts, Age: 35, Gender:Female)");
        
        PhoneBook book = new PhoneBook("Book", allpersons);
        book.addPerson(p2);
        assert allpersons.length() ==88;
        assert p1FirstName.startsWith("Iro");
    }

}
