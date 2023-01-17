package com.thelogicmaster.example;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Example {

	/*JNI
	// This is test code...
	 */

	private native static int nativeMethodExample(int a, int b, ByteBuffer b1, IntBuffer b2, int[] a1, String s); /*
		printf("%s\n", s);
		return a + b + b1[0] + b2[0] + a1[0];
	*/

	public static void main (String[] args) {
		System.out.println("Hello World!");
	}
}
