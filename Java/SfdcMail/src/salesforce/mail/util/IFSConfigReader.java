package salesforce.mail.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * ifs.iniファイルのWrapperクラス
 * @author M.Yi　2017/03/08
 *
 */
public class IFSConfigReader {
	
	/** ユーザID キー*/
	private static String KEY_USER_ID="[MAGIC_LOGICAL_NAMES]IFS_SFDC_USER";
	/** パスワード キー*/
	private static String KEY_PASSWORD="[MAGIC_LOGICAL_NAMES]IFS_SFDC_PASS";
	/** 復号化ツール キー*/
	private static String KEY_DECRYPT_TOOL="[MAGIC_LOGICAL_NAMES]TransAes256Bit";
	
	/** プロパティファイル*/
	private String configFile;
	/** プロパティオブジェクト*/
	private Properties conf = new Properties();
	/**
	 * ユーザID
	 */
	private String userID;
	/**
	 * パスワード
	 */
	private String password;
	
	/**
	 * 復号化ツール名
	 */
	private String decryptTool;
	
	/**
	 * SalesForceのWebserviceへのログインURL
	 */
	private String loginURL="https://login.salesforce.com/services/Soap/u/38.0";
	
	/**
	 * コンストラクタ
	 * @param ifsFile　プロパティファイル名
	 */
	public IFSConfigReader(String ifsFile) {
		this.configFile=ifsFile;
		prepare();
	}
	
	/**
	 * プロパティファイルを読み込む処理
	 */
	private void prepare(){
		conf = new Properties();
		try {
			//プロパティを読み込む
			conf.load(new FileInputStream(new File(configFile)));
			//復号化ツールを取得する
			decryptTool = conf.getProperty(KEY_DECRYPT_TOOL);
			//ユーザIDを取得する
			if(conf.containsKey(KEY_USER_ID)){
				userID = conf.getProperty(KEY_USER_ID);

			}
			//パスワードを取得する。
			if(conf.containsKey(KEY_PASSWORD)){
//				String tempword = conf.getProperty(KEY_PASSWORD);
//				if(null != decryptTool && !"".equals(decryptTool)){
//					password = decrypt(decryptTool, tempword);
//				}
				password = conf.getProperty(KEY_PASSWORD);
			}
			
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} 

	}
	
	/**
	 * 有効かどうかをチェックする。
	 * @return
	 */
	public boolean validate(){
		//ユーザID、パスワードがあれば、OK
		return (this.userID!=null && !"".equals(this.userID) &&
				this.password!=null && !"".equals(this.password));
	}
	
	/**
	 * プロパティの値を取得する。
	 * @param key
	 * @return
	 */
	public String getProperty(String key){
		return conf.getProperty(key);
	}
	

	/**
	 * ユーザIDを取得する。
	 * @return
	 */
	public String getUserID() {
		return userID;
	}

	/**
	 * パスワードを取得する
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * ログインURLを取得する
	 * @return loginURL
	 */
	public String getLoginURL() {
		return loginURL;
	}

	/**
	 * 暗号化された値を復号する。
	 * @param tool　復号化ツール
	 * @param encodedWord　暗号化された値
	 * @return　復号された値
	 */
	protected String decrypt(String tool, String encodedWord){
		String result="";
		try {
			Runtime rt = Runtime.getRuntime();
			String cmd=tool+" /r " + encodedWord;
			Process p = rt.exec(cmd);
					InputStream is = p.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					while ((result = br.readLine()) != null) {
						return result;
					}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return result;
	}

}
