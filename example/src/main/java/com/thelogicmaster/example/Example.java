package com.thelogicmaster.example;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Example {

	/*JNI
	// This is test code...
	 */

//	private native static int nativeMethodExample(int a, int b, ByteBuffer b1, IntBuffer b2, int[] a1, String s); /*
//		printf("%s\n", s);
//		return a + b + b1[0] + b2[0] + a1[0];
//	*/

//	int test(int a, int b) {
//		return a + b;
//	}
//
//	class A {
//		void a() {
//
//		}
//	}
//
//	class B extends A {
//		void a() {
//
//		}
//
//		void b() {
//
//		}
//	}
//
//	void test(B b) {
//		b.a();
//		b.b();
//	}
//
//	static class C extends Example {
//		@Override
//		int test(int a, int b) {
//			System.out.println("...");
//			return 1;
//		}
//
//
//	}

	public static void main (String[] args) {
//		int a = 2;
//		int b = a + 2;
//		Example e = new Example();
//		C c = new C();
//		int d = c.test(a, b);
//		String s = "Hi";
		System.out.println("Hello World!");
		try {
			throw new Exception("Exception");
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}
}
