package tests;

public class SubTest1 extends SuperTest {
	public int i;
	
	public SubTest1() {
		i = 1;
	}
	
	public String toString() {
		return "SubTest"+i;
	}
}
