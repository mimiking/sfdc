package sfws.client.trigger;

import org.apache.log4j.Logger;

import sfws.client.cmn.CmdAgent;
import sfws.client.cmn.CmdProcess;
import sfws.client.util.XmlConfigReader;

/**
 * HotLeadReflect処理情報を管理するクラス
 * @author m.yi 2017/03/18
 *
 */
public class HotLeadReflectAgent extends CmdAgent {
	/** ログ部品*/
	private static Logger logger =Logger.getLogger(CmdProcess.class);

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

	
	/**
	 * コンストラクタ
	 * @param cmds
	 */
	public HotLeadReflectAgent(String configFile) {
		super();
		this.appConfig=new XmlConfigReader(configFile);
		this.appConfig.load();
	}
	
	

	/**
	 * 有効性チェック
	 * @return
	 */
	@Override
	protected boolean validate(){
		return appConfig!=null;
	}
	
	@Override
	protected  void dumpExecuteInfo(){
		String dump="\n下記のApexを実行しました。\n";
		
		
		logger.info("ExecuteInfo :\n" +dump);
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
