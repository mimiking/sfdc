package salesforce.mail.util;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Appの設定情報クラス
 * @author M.Yi 2017/03/10
 *
 */
public class XmlConfigReader {
	
//	/** 一回登録・更新制限件数キー*/
//	public final String KEY_UPSERT_LIMIT ="IURecordsPerTrans";
//	/** 一回削除制限件数キー*/
//	public final String KEY_DELETE_LIMIT = "DelRecordsPerTrans";
//	/** リード経過日数キー*/
//	public final String KEY_LEAD_ELAPSED_DAYS ="LeadElapsedDays";
//	/** タイプ名キー*/
//	public final String KEY_RECORD_TYPE_NAME ="RecordTypeName";
	
	/** ログ部品　*/
	private static Logger logger = Logger.getLogger(XmlConfigReader.class);
	
	/** SMTPサーバー **/
	private final String KEY_SMTP_HOST = "SMTP_HOST";
	/** 送信ポート **/
	private final String KEY_PORT = "PORT";
	/** 送信アカウント **/
	private final String KEY_SEND_ACCOUNT = "SEND_ACCOUNT";
	/** 送信パスワード **/
	private final String KEY_SEND_PWD = "SEND_PWD";
	/** SSLフラグ **/
	private final String KEY_SSL_FLAG = "SSL_FLAG";
	/** SalesforceログインID **/
	private final String KEY_SF_LOGIN_ID = "SF_LOGIN_ID";
	/** Salesforceログインパスワード **/
	private final String KEY_SF_PWD = "SF_LOGIN_PWD";
	/** 処理制限件数 **/
	private final String KEY_LIMIT_SIZE = "LIMIT_SIZE";
	/** 処理モード **/
	private final String KEY_MODE = "MODE";
	/** リトライ回数 **/
	private final String KEY_RETRY_COUNT = "RETRY_CNT";
	/**　待ち時間（秒） **/
	private final String KEY_WAIT_SECONDS = "WAIT_SECONDS";
	
	
//	/** 一回登録・更新制限件数(Default)*/
//	public final int DEFAULT_UPSERT_LIMIT =40;
//	/** 一回削除制限件数(Default)*/
//	public final int DEFAULT_DELETE_LIMIT = 1000;
//	/** リード経過日数(Default)*/
//	public final int DEFAULT_LEAD_ELAPSED_DAYS=90;
//	/** タイプ名(Default)*/
//	public final String DEFAULT_RECORD_TYPE_NAME="リード（ホットリード）";
		
	/** AppConfigから読み込んだSetting情報*/
	private static  Map<String, String> settings;
	
	/** AppConfigのファイル名*/
	private String xmlConfig;

	/**
	 * コンストラクタ
	 * @param xml
	 */
	public XmlConfigReader(String xml) {
		this.xmlConfig = xml;
	}

	/**
	 * AppConfigから情報を読み込む
	 */
	public void load()  {
		try {
			if(settings==null){
				//Mapを作成
				settings = new HashMap<String, String>();
				//DOM オブジェクトツリーをパースするインスタンスを生成するDocumentBuilderオブジェクトを生成
				DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = dbfactory.newDocumentBuilder();
				// パースを実行してDocumentオブジェクトを取得
				Document document = builder.parse(new File(xmlConfig));
				Element rootElement = document.getDocumentElement();
				// add要素取得
				NodeList nodeList = rootElement.getElementsByTagName("add");
				// add要素から、key、Valueを取得して、settingsに登録する。
				for (int i=0; i < nodeList.getLength(); i++) {
					Element add=(Element)nodeList.item(i);
					// Key属性の値を取得
					String key = add.getAttribute("key");
					String value= add.getAttribute("value");
					//System.out.println("name:" + name + ", key:" + key + ", value:" + value);
					settings.put(key, value);
				}
				nodeList = rootElement.getElementsByTagName("setting");
				for (int i=0; i < nodeList.getLength(); i++) {
					Element settingTag=(Element)nodeList.item(i);
					// id属性の値を取得
					String id = settingTag.getAttribute("name");
					String message = settingTag.getTextContent().trim();
					//System.out.println("id:" + id + ", message:" + message );
					settings.put(id, message);
				}
			}
		}
		catch (Exception e){
			logger.error(e.getStackTrace());
		}
	}
	
	/**
	 * SMTPサーバーを取得する。
	 * @return SMTPサーバー
	 */
	public String getSmtpHost() {
		return this.getValue(KEY_SMTP_HOST, "", "SMTPサーバー");
	}
	
	/**
	 * 送信ポートを取得する。
	 * @return 送信ポート
	 */
	public int getPort() {
		return this.getIntValue(KEY_PORT, 587, "ポート");
	}
	
	/**
	 * 送信アカウントを取得する。
	 * @return 送信アカウント
	 */
	public String getSender() {
		return this.getValue(KEY_SEND_ACCOUNT, "", "送信アカウント");
	}
	
	/**
	 * 送信パスワードを取得する。
	 * @return 送信パスワード
	 */
	public String getSendPassword() {
		return this.getValue(KEY_SEND_PWD, "", "送信パスワード");
	}
	
	/**
	 * SSLフラグを取得する。
	 * @return SSLフラグ
	 */
	public int getSslFlag() {
		return this.getIntValue(KEY_SSL_FLAG, 1, "SSLフラグ");
	}
	
	/**
	 * SaleforceログインIDを取得する。
	 * @return SaleforceログインID
	 */
	public String getSfLoginId() {
		return this.getValue(KEY_SF_LOGIN_ID, "", "SalesforceログインID");
	}
	
	/**
	 * Saleforceログインパスワードを取得する。
	 * @return Saleforceログインパスワード
	 */
	public String getSfPassword() {
		return this.getValue(KEY_SF_PWD, "", "Salesforceログインパスワード");
	}
	
	/**
	 * 制限件数を取得する。
	 * @return 制限件数
	 */
	public int getLimitSize() {
		return this.getIntValue(KEY_LIMIT_SIZE, 100, "Salesforceログインパスワード");
	}
	
	/**
	 * モードを取得する。
	 * @return モード
	 */
	public int getMode() {
		return this.getIntValue(KEY_MODE, 0, "処理モード");
	}
	
	/**
	 * リトライ回数を取得する。
	 * @return リトライ回数
	 */
	public int getRetryCount() {
		return this.getIntValue(KEY_RETRY_COUNT, 3, "リトライ回数");
	}
	
	/**
	 * 待ち時間を取得する。
	 * @return 待ち時間
	 */
	public int getWaitSeconds() {
		return this.getIntValue(KEY_WAIT_SECONDS, 60, "待ち時間");
	}
	
	/**
	 * メッセージを取得する
	 * @param id
	 * @param ps
	 * @return
	 */
	public String getMessage(String id, String... ps){
		String message = settings.get(id);
		if(message!=null && !"".equals(message)){
			MessageFormat format = new MessageFormat(message);
			return format.format(ps);
		}
		
		return id;
		
	}
	
	/**
	 * 文字列型設定値を取得する。
	 * 
	 * @param key 項目キー
	 * @param defaultValue デフォルト値
	 * @param name 項目名
	 * @return 値
	 */
	private String getValue(String key, String defaultValue, String name) {
		String value = defaultValue;
		if(settings != null){
			value = settings.get(key);
			if (value == null || value == "") {
				logger.warn(String.format("%sは未設定です。", name));
				value = defaultValue;
			}
		}
		
		return value;
	}
	
	/**
	 * 数字型設定値を取得する。
	 * 
	 * @param key 項目キー
	 * @param defaultValue デフォルト値
	 * @param name 項目名
	 * @return 値
	 */
	private int getIntValue(String key, int defaultValue, String name) {
		int value = defaultValue;
		if(settings != null){
			String val = settings.get(key);
			if (val != null && val.matches("^[0-9]+$")) {
				// 数字の場合
				value = Integer.valueOf(val);
			} else {
				logger.warn(String.format("%sは設定不正です。", name));
			}
		}
		
		return value;
	}
}