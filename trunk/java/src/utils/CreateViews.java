package utils;

public class CreateViews {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		DbUtils.runScript("src/utils/views.sql");
	}

}
