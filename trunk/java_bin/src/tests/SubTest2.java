package tests;

import java.util.logging.Logger;

public class SubTest2 extends SuperTest {

	public void loggerTest() {
		Logger log = Logger.getLogger("test.tests");
		
		log.log(null, "this is a logger test");
	}
}
