package test;

import java.io.Serializable;

public class SerialisationClass implements Serializable {
	private static final long serialVersionUID = -2362251348335469392L;
	private transient int foo = 56;

	public int getFoo() {
		return foo;
	}

	public void setFoo(int foo) {
		this.foo = foo;
	}
}