package sfdc.client.cmn;

import com.sforce.ws.ConnectorConfig;

public class SfdcConnectorConfig extends ConnectorConfig {
	
	private String endPointUrl;

	public String getEndPointUrl() {
		return endPointUrl;
	}

	public void setEndPointUrl(String endPointUrl) {
		this.endPointUrl = endPointUrl;
	}
}
