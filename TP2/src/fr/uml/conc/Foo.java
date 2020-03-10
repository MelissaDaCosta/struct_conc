package fr.uml.conc;

/**
 * Exercice 1 - Publication safe
 * 
 * @author mdacos05
 *
 */
public class Foo {
	private String value;

	public Foo(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	private static Foo f;

	public static void main(String[] args) {
		new Thread(() -> {
			System.out.println(f.value);
		}).start();
		
		f = new Foo("toto");

	}

}
