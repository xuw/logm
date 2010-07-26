package tests;


public class Test {
	static final String s = "the number is 5";
	private String string = "the number is 5";
	
//	public static void main(final String[] args) throws IOException {
//		constantTest();
//	}
	
	public static void constantTest() {
		System.out.println("the number is "+5);
	}
	
	public static void finalvarTest() {
		System.out.println(s);
	}
	
	public static void varTest() {
		String var = "the number is "+5;
		System.out.println(var);
	}
	
	public static void const_varTest() {
		int var = 5;
		System.out.println("the number is "+var);
	}
	
	public static void const_varTest2() {
		int var = 5;
		char var2 = 'b';
		System.out.println("the number is "+var
				+" and the letter is "+var2);
	}
	
	public static void arrayTest() {
		int[] array = {0,1,2,3};
		System.out.println("the number at 2 is "+array[2]);
	}
	
	public static void array_varTest() {
		int[] array = {0,1,2,3};
		int i = 2;
		System.out.println("the munber at "+i+" is "+array[i]);
	}
	
	public static void fieldTest() {
		SubTest i = new SubTest(5);
		System.out.println("the number is "+i.value);
	}
	
	public static void static_fieldTest() {
		System.out.println("the number is "+SubTest.svalue);
	}
	
	public static void field_array_varTest() {
		int i = 2;
		SubTest c = new SubTest(i);
		System.out.println("the number at "+i+" is "
				+c.array[c.value]);
	}
	
	public static void zero_argTest() {
		SubTest c = new SubTest(5);
		System.out.println("the function is "+ c.zeroArgs());
	}
	
	public static void one_argTest() {
		SubTest c = new SubTest(5);
		int i = 4;
		System.out.println("the function is "+ c.oneArgs(i));
	}
	
	public static void two_argTest() {
		SubTest c = new SubTest(5);
		int i = 0;
		System.out.println("the function is "+ c.twoArgs(i, 5));
	}
	
	public void thisTest() {
		System.out.println(this.string);
	}
	
	public void ifTest(boolean test) {	
		if(test)
			System.out.println("in an if, test is "+test);
		else
			System.out.println("in an else, test is "+test);
	}
	
	public void condTest(boolean test) {
		System.out.println("this is a conditional "+(test?"true":"or false"));
	}
	
	public void condTest2(boolean test, int trueValue, int falseValue) {
		System.out.println("cond: "+
				(test?"true "+trueValue:
					"false "+falseValue));
	}
	
	public void trycatchTest() {
		try{
			System.out.println("in try "+SubTest.svalue);
		}
		catch(Exception e) {
			System.out.println("in catch "+SubTest.svalue);
			throw new RuntimeException(e);
		}
		System.out.println("outside "+SubTest.svalue);
		return;
	}
	
	public void forTest() {
		for(int i = 0; i < 10; i++)
			System.out.println("inside for loop");
		System.out.println("outside for loop");
	}
	
	public void whileTest() {
		int i = 0;
		while(i < 10) {
			i++;
			System.out.println("inside while loop");
		}
		System.out.println("outside for loop");
	}
	
	public void intTest(int i) {
		System.out.println(i);
	}
	
	public void intCondTest(boolean b, int i, int j) {
		System.out.println(b?i:j);
	}
	
	private static class SubTest {
		public int value;
		public static int svalue = 5;
		public int[] array;
		
		public SubTest(int i) {
			value = i;
			array = new int[] {0,1,2,3,4};
		}
		
		public String zeroArgs() {
			return "no args";
		}
		
		public String oneArgs(int i) {
			return "one arg: "+i;
		}
		
		public String twoArgs(int i, int k) {
			return "two args: "+i+", "+k;
		}
	}
}
