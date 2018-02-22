package sfdc.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * ifs.iniファイルのWrapperクラス
 *
 */
public class ServiceConfig {
	/** ログ部品　*/
	private static Logger logger = Logger.getLogger(ServiceConfig.class);
	
	private static final String CONFIG_FILE = "./config/config.ini";
	
	/** DBユーザID キー */
	private static final String KEY_DB_USER_ID = "[MAGIC_LOGICAL_NAMES]DB_USER";
	/** DBパスワード キー */
	private static final String KEY_DB_PASSWORD = "[MAGIC_LOGICAL_NAMES]DB_PASS";
	/** JDBCドライバー キー */
	private static final String KEY_JDBC_DRIVER = "[MAGIC_LOGICAL_NAMES]JDBC_DRIVER";
	/** JDBC接続URL キー */
	private static final String KEY_JDBC_URL = "[MAGIC_LOGICAL_NAMES]JDBC_URL";
	/** データファイル **/
	private static final String KEY_DATA_FILE_PATH = "[MAGIC_LOGICAL_NAMES]DATA_FILE_PATH";
//	/** 退避フォルダ  **/
//	private static final String KEY_SAVE_FOLDER = "[MAGIC_LOGICAL_NAMES]SAVE_FOLDER";
//	/** 退避ファイル保持日数  **/
//	private static final String KEY_RESERVE_DAYS = "[MAGIC_LOGICAL_NAMES]RESERVE_DAYS";
	/** リトライ回数 キー */
	private static final String KEY_RETRY_COUNT = "[MAGIC_LOGICAL_NAMES]RETRY_CNT";
	/** DBリトライ回数 キー */
	private static final String KEY_DB_RETRY_COUNT = "[MAGIC_LOGICAL_NAMES]DB_RETRY_CNT";
	/** 待ち時間（秒）キー */
	private static final String KEY_WAIT_SECONDS = "[MAGIC_LOGICAL_NAMES]WAIT_SECONDS";
	/** バッチ毎最大バイト数キー **/
	private static final String KEY_MAX_BYTES_PER_BATCH = "[MAGIC_LOGICAL_NAMES]MAX_BYTES_PER_BATCH";
	/** バッチ毎最大レコード数キー **/
	private static final String KEY_MAX_ROWS_PER_BATCH = "[MAGIC_LOGICAL_NAMES]MAX_ROWS_PER_BATCH";
	
	/** 復号化ツール キー */
	private static final String KEY_DECRYPT_TOOL = "[MAGIC_LOGICAL_NAMES]TransAes256Bit";
	
	/** プロパティオブジェクト */
	private Properties conf = new Properties();
	
	// DB接続情報
	/** DBユーザID */
	private String dbUserID;
	/** DBパスワード */
	private String dbPassword;
	/** JDBCドライブ */
	private String jdbcDriver;
	/** JDBC接続URL */
	private String jdbcUrl;
	/** データファイルパス **/
	private String dataFilePath;
//	/** 退避フォルダ **/
//	private String saveFolder;
//	/** 退避ファイル保持日数 **/
//	private int reserveDays;
	
	// SFDC接続設定情報
	/** 処理制限件数 */
	private int limitSize;
	/** 処理モード */
	private int mode;
	/** リトライ回数 */
	private int retryCnt;
	/** DBリトライ回数 **/
	private int dbRetryCnt;
	/**　待ち時間（秒） */
	private int waitSeconds;
	/** バッチ毎最大バイト数  **/
	private int maxBytesPerBatch;
	/** バッチ毎最大レコード数 **/
	private int maxRowsPerBatch;
	
	private static ServiceConfig config = null;
	
	/** 復号化ツール名 */
	private String decryptTool;
	
	/**
	 * コンストラクタ
	 * @param ifsFile　プロパティファイル名
	 */
	private ServiceConfig() {
		readConfig();
	}
	
	public static ServiceConfig getConfig() {
		if (config == null) {
			config = new ServiceConfig();
		}
		
		return config;
	}
	
	/**
	 * プロパティファイルを読み込む処理
	 */
	private void readConfig(){
		
		logger.info("設定ファイル読込開始します。");
		
		conf = new Properties();
		try {
			// プロパティを読み込む
			conf.load(new FileInputStream(new File(CONFIG_FILE)));
			
			// 復号化ツールを取得する
			this.decryptTool = this.getConfigValue(KEY_DECRYPT_TOOL);
			// DBユーザIDを取得する
			this.dbUserID = this.getConfigValue(KEY_DB_USER_ID);
			// DBパスワードを取得する。
			
			//this.dbPassword = this.getConfigValue(KEY_DB_PASSWORD);
			// 暗号化された値を復号する
			this.dbPassword = this.decrypt(this.decryptTool, this.getConfigValue(KEY_DB_PASSWORD));
			
			// DBドライブを取得する。
			this.jdbcDriver = this.getConfigValue(KEY_JDBC_DRIVER);
			// DB接続URLを取得する。
			this.jdbcUrl = this.getConfigValue(KEY_JDBC_URL);			
			// データファイルパス
			this.dataFilePath = this.getConfigValue(KEY_DATA_FILE_PATH);
			// 退避フォルダ
//			this.saveFolder = this.getConfigValue(KEY_SAVE_FOLDER);
//			// 退避ファイル保留日数
//			this.reserveDays = this.getConfigValue(KEY_RESERVE_DAYS, 30);
			// リトライ回数を取得する。
			this.retryCnt = this.getConfigValue(KEY_RETRY_COUNT, 5);
			// (DB)リトライ回数を取得する
			this.dbRetryCnt = this.getConfigValue(KEY_DB_RETRY_COUNT, 5);
			// 待ち時間（秒）を取得する。
			this.waitSeconds = this.getConfigValue(KEY_WAIT_SECONDS, 60);
			// バッチ毎最大バイト数
			this.maxBytesPerBatch = this.getConfigValue(KEY_MAX_BYTES_PER_BATCH, 10000000);
			// バッチ毎最大レコード数
			this.maxRowsPerBatch = this.getConfigValue(KEY_MAX_ROWS_PER_BATCH, 10000);
		} catch (Exception e) {
			logger.error(String.format(Constant.MESSAGE_E001, Constant.MESSAGE_R006), e);
		}
		
		logger.info("設定ファイル読込完了しました。");
	}
	
	/**
	 * 有効かどうかをチェックする。
	 * @return
	 */
	public boolean validate(){
		//必須項目をチェックする。
		logger.info(Constant.MESSAGE_I003);

		boolean isValid = this.isEmpty(this.jdbcDriver, "JDBCドライバー");
		isValid = isValid && this.isEmpty(this.jdbcUrl, "JDBC接続文字列");
		
		logger.info(String.format(Constant.MESSAGE_I004, isValid));

		return isValid;
	}

	/**
	 * 復号化ツール名を取得する。
	 * @return 復号化ツール名
	 */
	public String getDecryptTool() {
		return this.decryptTool;
	}
	
	/**
	 * DBユーザID取得
	 * @return dbUserID
	 */
	public String getDbUserID() {
		return dbUserID;
	}

	/**
	 * DBパスワード取得
	 * @return dbPassword
	 */
	public String getDbPassword() {
		return dbPassword;
	}

	/**
	 * JDBCドライバークラス取得
	 * @return jdbcDriver
	 */
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	/**
	 * JDBC接続URL取得
	 * @return jdbcUrl
	 */
	public String getJdbcUrl() {
		return jdbcUrl;
	}
	
	/**
	 * 処理制限件数取得
	 * @return limitSize
	 */
	public int getLimitSize() {
		return limitSize;
	}
	
	/**
	 * 処理モード取得
	 * @return mode
	 */
	public int getMode() {
		return mode;
	}
	
	/**
	 * リトライ回数取得
	 * @return retryCnt
	 */
	public int getRetryCnt() {
		return retryCnt;
	}
	
	
	/**
	 * DBリトライ回数取得
	 * @return retryCnt
	 */
	public int getDbRetryCnt() {
		return dbRetryCnt;
	}

	/**
	 * 待ち時間（秒）取得
	 * @return waitSeconds
	 */
	public int getWaitSeconds() {
		return waitSeconds;
	}

	/**
	 * データフォルダを取得する。
	 * @return
	 */
	public String getDataFilePath() {
		return dataFilePath;
	}

//	/**
//	 * 退避フォルダを取得する。
//	 * @return　退避フォルダ
//	 */
//	public String getSaveFolder() {
//		return saveFolder;
//	}
//
//	/**
//	 * 退避ファイル保留日数を取得する。
//	 * @return　退避ファイル保留日数　
//	 */
//	public int getReserveDays() {
//		return reserveDays;
//	}
	
	
	/**
	 * バッチ毎最大バイト数を取得する。
	 * @return バッチ毎最大バイト数
	 */
	public int getMaxBytesPerBatch() {
		return maxBytesPerBatch;
	}

	/**
	 * バッチ毎最大レコード数を取得する。
	 * @return
	 */
	public int getMaxRowsPerBatch() {
		return maxRowsPerBatch;
	}

	/**
	 * 暗号化された値を復号する。
	 * @param tool　復号化ツール
	 * @param encodedWord　暗号化された値
	 * @return　復号された値
	 */
	public String decrypt(String tool, String encodedWord){
		//暗号化ツールが指定されない場合、そのまま返す。
		if(StringUtils.isEmpty(tool)){
			return encodedWord;
		}
		String result = "";
		try {
			Runtime rt = Runtime.getRuntime();
			String cmd = tool+" /r " + encodedWord;
			Process p = rt.exec(cmd);
					InputStream is = p.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					while ((result = br.readLine()) != null) {
						return result;
					}
		} catch (IOException e) {
			logger.error(e);
		}

		return result;
	}
	
	/**
	 * 設定値を取得する。
	 * @param key 設定キー
	 * @return 設定値
	 */
	private String getConfigValue(String key) {
		String value = "";
		if (this.conf.containsKey(key)) {
			value = conf.getProperty(key);
		}
		
		return value;
	}
	
	/**
	 * 設定値を取得する。
	 * @param key 設定キー
	 * @param defaultValue デフォルト値
	 * @return 設定値
	 */
	private int getConfigValue(String key, int defaultValue) {
		int value = defaultValue;
		if (this.conf.containsKey(key)) {
			value = Integer.valueOf(conf.getProperty(key));
		}
		
		return value;
	}

	/**
	 * 設定項目が設定されるかを判断する。
	 * @param value 値
	 * @param name 項目名
	 * @return 判断結果
	 */
	private boolean isEmpty(String value, String name) {
		boolean isValid = !StringUtils.isEmpty(value);
		if (!isValid) {
			logger.error(String.format(Constant.MESSAGE_E001, name));
		}
		return isValid;
	}
}
