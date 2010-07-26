package tests;

public class SubSubTest11 extends SubTest1 {
	public String toString() {
		return "SubSubTest11";
	}
	
	public Object[] listPaths(Object src) {
		return new String[] {"1","2","3"};
	}
	
	public boolean isDirectory(Object cur) {
		return true;
	}
	
	public boolean getReplication(Object cur) {
		return true;
	}
	
	public int getLength(Object cur) {
		return 5;
	}
	
	public boolean reset(int bytes, int length) {
		return true;
	}
}
