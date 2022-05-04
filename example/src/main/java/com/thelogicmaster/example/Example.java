package com.thelogicmaster.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Example {

	public static void main (String[] args) {
		try {
			System.out.println(new BufferedReader(new FileReader("HelloWorld.txt")).readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
