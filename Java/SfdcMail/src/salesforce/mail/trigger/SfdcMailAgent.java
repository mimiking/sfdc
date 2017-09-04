package salesforce.mail.trigger;

import org.apache.log4j.Logger;

import salesforce.mail.cmn.CmdAgent;
import salesforce.mail.util.XmlConfigReader;

public class SfdcMailAgent extends CmdAgent {
	/** ログ部品*/
	private static Logger logger = Logger.getLogger(SfdcMailAgent.class);

	/**
	 * AppConfigのオブジェクト
	 */
	private XmlConfigReader appConfig;
	
	/** 処理回数　*/
	private int processTimes;
	
	/** 処理状態コード */
	private int statusCode;
	
 
    /** 成功 */
    public static final  int SUCCESS = 0;
    /** 警告 */
    public static final int WARNING = 9;
    /** 失敗 */
    public static final int FAILURE = -1;
    /** モード：　ローカル **/
    public static final int MODE_LOCAL = 0;
    /** モード：　クラウド **/
    public static final int MODE_CLOUD = 1;
    
    /**
	 * コンストラクタ
	 * @param cmds
	 */
	public SfdcMailAgent(String configFile) {
		super();
		this.appConfig = new XmlConfigReader(configFile);
		this.appConfig.load();
	}

	/**
	 * 有効性チェック
	 * @return
	 */
	@Override
	protected boolean validate() {
		return this.getSmtpHost() != "" 
		&& this.getSender() != ""
		&& this.getSendPassword() != ""
		&& this.getSfLoginId() != ""
		&& this.getSfPassword() != "";
	}
	
	@Override
	protected  void dumpExecuteInfo(){
		String dump="\n下記のApexを実行しました。\n";
		logger.info("ExecuteInfo :\n" + dump);
	}


	@Override
	protected String[] parseCmd(String cmds) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}
	
	/**
	 * 処理回数を取得する。
	 * @return processTimes
	 */
	public int getProcessTimes() {
		return processTimes;
	}


	/**
	 * 処理回数を設定する。
	 * @param processTimes 設定する processTimes
	 */
	public void setProcessTimes(int processTimes) {
		this.processTimes = processTimes;
	}


	/**
	 * 結果コードを取得する。
	 * @return statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}


	/**
	 * 結果コードを設定する
	 * @param statusCode 設定する statusCode
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	
	
	/**
	 * SMTPサーバーを取得する。
	 * @return SMTPサーバー
	 */
	public String getSmtpHost() {
		return this.appConfig.getSmtpHost();
	}
	
	/**
	 * 送信ポートを取得する。
	 * @return 送信ポート
	 */
	public int getPort() {
		return this.appConfig.getPort();
	}
	
	/**
	 * 送信アカウントを取得する。
	 * @return 送信アカウント
	 */
	public String getSender() {
		return this.appConfig.getSender();
	}
	
	/**
	 * 送信パスワードを取得する。
	 * @return 送信パスワード
	 */
	public String getSendPassword() {
		return this.appConfig.getSendPassword();
	}
	
	/**
	 * SSLフラグを取得する。
	 * @return SSLフラグ
	 */
	public int getSslFlag() {
		return this.appConfig.getSslFlag();
	}
	
	/**
	 * SaleforceログインIDを取得する。
	 * @return SaleforceログインID
	 */
	public String getSfLoginId() {
		return this.appConfig.getSfLoginId();
	}
	
	/**
	 * Saleforceログインパスワードを取得する。
	 * @return Saleforceログインパスワード
	 */
	public String getSfPassword() {
		return this.appConfig.getSfPassword();
	}
	
	/**
	 * 制限件数を取得する。
	 * @return 制限件数
	 */
	public int getLimitSize() {
		return this.appConfig.getLimitSize();
	}
	
	/**
	 * 実行モードを取得する。
	 * @return 実行モード
	 */
	public int getMode() {
		return this.appConfig.getMode();
	}
	
	/**
	 * リトライ回数を取得する。
	 * @return リトライ回数
	 */
	public int getRetryCount() {
		return this.appConfig.getRetryCount();
	}
	
	/**
	 * 待ち時間を取得する。
	 * @return 待ち時間
	 */
	public int getWaitSeconds() {
		return this.appConfig.getWaitSeconds();
	}
	
	
//	/**
//	 * 削除時の処理単位を取得
//	 * @return
//	 */
//	public int getDelRecordUnit(){
//		return this.appConfig.getDeleteLimit();
//	}
//	
//	/**
//	 * 更新、新規時の処理単位を取得
//	 * @return
//	 */
//	public int getUpSertRecordUnit(){
//		return this.appConfig.getUpSertLimit();
//	}
//
//	/**
//	 * メッセージを取得する
//	 * @param id　メッセージID
//	 * @param ps　メッセージの引数
//	 * @return　　メッセージ文字列
//	 */
//	public String getMessage(String id, String...ps){
//		return this.appConfig.getMessage(id, ps);
//	}
//	
//	public String getLeadRecordType(){
//		return this.appConfig.getLeadRecordType();
//	}

}
