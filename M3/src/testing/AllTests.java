package testing;

import java.io.IOException;

import org.apache.log4j.Level;

import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;


public class AllTests {

	static {
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
//			new KVServer(50000, 10, "FIFO");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static Test suite() {
		TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
//		clientSuite.addTestSuite(testing.CacheTest.class);
//		clientSuite.addTestSuite(testing.ConnectionTest.class);
		clientSuite.addTestSuite(testing.InteractionTest.class);
		clientSuite.addTestSuite(testing.AdditionalTest.class);
		return clientSuite;
	}
	
}
