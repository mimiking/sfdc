package salesforce.mail.cmn;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;

import com.sforce.ws.ConnectorConfig;

import salesforce.mail.util.IFSConfigReader;

/**
 * SalesForceサービス接続情報クラスクラス
 * @author m.yi 2017/03/08
 *
 */
public class ServiceConfig {
	/** ログ部品*/
	private static Logger logger =Logger.getLogger(ServiceConfig.class);
	
	/** 設定情報オブジェクト*/
	private static IFSConfigReader ifs;
	
	//外部から指定できるプロパティ
	/** 設定情報ファイル名*/
	public static String settingFile = "./config/ifs.ini";

	/**
	 * コンストラクタ
	 */
	public ServiceConfig() {
		// TODO 自動生成されたコンストラクター・スタブ
	}
	
	/**
	 * サービス接続配置情報取得
	 * @param needProxy
	 * @return
	 */
	public static ConnectorConfig getConnectorConfig(){
		//ログイン情報を取得する。
		if(ifs==null){
			ifs=new IFSConfigReader(settingFile);
		}
		if(ifs.validate()){
			//SalesForceの接続情報を作成する。
			ConnectorConfig config = new ConnectorConfig();
			config.setUsername(ifs.getUserID());
			config.setPassword(ifs.getPassword());
			config.setAuthEndpoint(ifs.getLoginURL());

			//Proxyの設定
			System.setProperty("java.net.useSystemProxies", "true");
			List<Proxy> proxyList = null;
			proxyList = ProxySelector.getDefault().select(URI.create(ifs.getLoginURL()));
			if (proxyList != null && proxyList.size()>0){
				Proxy proxy = proxyList.get(0);
				InetSocketAddress addr = (InetSocketAddress) proxy.address();
				
				if (addr != null) {
					logger.info("use system proxy: " + addr.getHostName() + ":" + addr.getPort());
					config.setProxy(addr.getHostName(), addr.getPort());
				}
				else{
					logger.info("No Proxy");
				}

			}

			return config;

		}
		return null;
	}
	
}
