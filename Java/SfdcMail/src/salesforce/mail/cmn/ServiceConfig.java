package salesforce.mail.cmn;

import com.sforce.ws.ConnectorConfig;

import salesforce.mail.util.IFSConfigReader;

/**
 * SalesForceサービス接続情報クラスクラス
 * @author m.yi 2017/03/08
 *
 */
public class ServiceConfig {
	
	/** 設定情報オブジェクト*/
	private static IFSConfigReader ifs;
	
	//外部から指定できるプロパティ
	/** 設定情報ファイル名*/
	public static String settingFile = "./config/ifs.ini";
	/** プロクシー*/
	public static String proxy="proxygate2.nic.nec.co.jp";
	/** ポート*/
	public static int port=8080;
	
	public static boolean useProxy=false;
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

			if(useProxy){
				config.setProxy(proxy, port);
			}
			return config;

		}
		return null;
	}
	
}
