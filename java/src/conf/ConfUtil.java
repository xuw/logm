package conf;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationFactory;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class ConfUtil {
	
	private static Configuration config;
	
	static {
		init();
	}
	
	
	private static void init() {
		try {
			//config = new CompositeConfiguration();
			//((CompositeConfiguration)config).addConfiguration(new SystemConfiguration());
			//((CompositeConfiguration)config).addConfiguration(new XMLConfiguration("conf/config.xml"));
			
//			ConfigurationFactory factory = new ConfigurationFactory();
//			URL configURL = factory.getClass().getResource("/conf/config.xml");
//			System.err.println(configURL.toString());
//			factory.setConfigurationURL(configURL);
//			config = factory.getConfiguration();
			
			String s = System.getenv("LOGANA_BIN_DIR");
			File bindir;
			if (s==null || s.trim().length()==0) {
				bindir = new File(".");
			} else {
				bindir = new File(s);
			}
			
			File configfiledir = new File( new File(bindir, "conf"), "config.xml");
			
			ConfigurationFactory factory = new ConfigurationFactory(configfiledir.getAbsolutePath());
			config = factory.getConfiguration();
			
			if (s!=null && s.trim().length()!=0) {
				config.addProperty("LOGANA_BIN_DIR", s);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Configuration getConfig() {
		return config;
	}
	
	
	public static void main(String[] args) {
		System.err.println ( Arrays.deepToString( getConfig().getList("logformat").toArray()));
	}
	
}
