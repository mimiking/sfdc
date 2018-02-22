package sfdc.client.cmn;

import org.apache.log4j.xml.DOMConfigurator;

public abstract class BaseTrigger {
	protected static void logConfig(String logPath) {
		System.setProperty("output", logPath);
		DOMConfigurator.configure("./config/log4j.xml");
	}
}
