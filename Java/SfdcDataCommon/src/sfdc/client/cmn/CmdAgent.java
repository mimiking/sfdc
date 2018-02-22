package sfdc.client.cmn;

import org.apache.log4j.Logger;


/**
 * Apexの実行情報を記述するクラス
 * @author SDE
 *
 */
public abstract class CmdAgent {
	
	/** ログ部品　*/
	static Logger logger = Logger.getLogger(CmdAgent.class);
	
	/**
	 * Apex呼び出すコマンド配列
	 */
	protected String[] cmds;

	/**
	 * コンストラクタ
	 * @param cmds
	 */
	public CmdAgent(String[] cmds) {
		this.cmds = cmds;
	}
	
	/**
	 * コンストラクタ
	 * @param cmds
	 */
	public CmdAgent(String cmds) {
		this.cmds = parseCmd(cmds);
	}
	
	/**
	 * コンストラクタ
	 */
	public CmdAgent() {
		
	}

	protected abstract String[] parseCmd(String cmds);
	
	/**
	 * 有効性チェック
	 * @return
	 */
	protected abstract boolean validate();
	
	/**
	 * 実行情報をログ出力
	 * @param info
	 */
	protected abstract void dumpExecuteInfo();
	
	/**
	 * 文字列のNull判定
	 * @param val
	 * @return
	 */
	protected boolean isNull(String val){
		return null == val || "".equals(val);
	}
	

}
