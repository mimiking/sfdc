package sfdc.client.cmn;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.sforce.ws.ConnectorConfig;

import sfdc.client.util.Constant;
import sfdc.client.util.ServiceConfig;


/**
 * 各種サービス接続情報クラス
 *
 */
public class ConnectionManager {
	/** ログ部品 */
	private static Logger logger = Logger.getLogger(ConnectionManager.class);
	
	/**
	 * コンストラクタ
	 */
	public ConnectionManager() {}
	
	/**
	 * SFDCサービス接続配置情報取得
	 * @param sfdcUserID SFDCユーザID
	 * @param sfdcPassword SFDCパスワード
	 * @param sfdcLoginURL SalesForceのWebserviceへのログインURL
	 * @return
	 */
	public static ConnectorConfig getSfdcServiceConfig(String sfdcUserID, String sfdcPassword, String sfdcLoginURL){
		//ログイン情報を取得する。
		if (!StringUtils.isEmpty(sfdcUserID) && !StringUtils.isEmpty(sfdcPassword) &&
				!StringUtils.isEmpty(sfdcLoginURL)) {
			//SalesForceの接続情報を作成する。
			ConnectorConfig config = new ConnectorConfig();
			config.setUsername(sfdcUserID);
			config.setPassword(sfdcPassword);
			config.setAuthEndpoint(sfdcLoginURL);

			//Proxyの設定
			System.setProperty("java.net.useSystemProxies", "true");
			List<Proxy> proxyList = null;
			proxyList = ProxySelector.getDefault().select(URI.create(sfdcLoginURL));
			if (proxyList != null && proxyList.size() > 0) {
				Proxy proxy = proxyList.get(0);
				InetSocketAddress addr = (InetSocketAddress) proxy.address();

				if (addr != null) {
					logger.debug("use system proxy: " + addr.getHostName() + ":" + addr.getPort());
					config.setProxy(addr.getHostName(), addr.getPort());
				} else {
					logger.debug("No Proxy");
				}
			}
			return config;
		}
		return null;
	}

	/**
	 * DB接続を取得する。
	 * @return　DB接続オブジェクト
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static Connection getDBConnection() throws Exception {
		//DB接続情報を取得する。
		Connection conn = null;
		ServiceConfig config = ServiceConfig.getConfig();
		for (int i = 0; i <= config.getDbRetryCnt(); i++) {
			try {
				Class.forName(config.getJdbcDriver());
				String url = config.getJdbcUrl();
				String id = config.getDbUserID();
				String pw = config.getDbPassword();
				if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(pw)) {
					conn = DriverManager.getConnection(url, id, pw);
				} else{
					conn = DriverManager.getConnection(url);
				}
				return conn;
			} catch(Exception e) {
				logger.error(String.format(Constant.MESSAGE_E001, Constant.MESSAGE_R005), e);
				if (i < config.getDbRetryCnt()) {
					logger.info(String.format(Constant.MESSAGE_I018, config.getWaitSeconds()));
					try {
						Thread.sleep(config.getWaitSeconds() * 1000L);
					} catch (InterruptedException ie) {
						logger.error(ie.getMessage());
					}
				}
				
				if (i == config.getDbRetryCnt()) {
					throw e;
				}
			}
		}
		return conn;
	}

	public static Connection getCsvConnection(String src, Properties properties) throws Exception {
		Connection conn = null;
		try {
			Class.forName("org.relique.jdbc.csv.CsvDriver");
			conn = DriverManager.getConnection(String.format("jdbc:relique:csv:%s", src), properties);
		} catch (SQLException | ClassNotFoundException e) {
			logger.error(String.format(Constant.MESSAGE_E001, Constant.MESSAGE_R009), e);
			throw e;
		}
		
		return conn;
	}
}
