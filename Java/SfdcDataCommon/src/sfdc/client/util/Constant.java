package sfdc.client.util;

public class Constant {
	// 戻り値
	/** 戻り値：OK */
	public static final int RETURN_OK = 0;
	/** 戻り値：OK */
	public static final int RETURN_NG = -1;
	
	public static final String MESSAGE_E001 = "%sに失敗しました。";
	public static final String MESSAGE_E002 = "データファイルが存在しません。（%s）";
	public static final String MESSAGE_E003 = "%sが存在しません。（%s）";
	public static final String MESSAGE_E004 = "マッピング情報が存在しません。（IFID: %s）";
	public static final String MESSAGE_E008 = "SFDCの接続に失敗しました。";
	
	public static final String MESSAGE_I003 = "設定チェックを開始します。";
    public static final String MESSAGE_I004 = "設定チェックが終了しました。[ %s ]";
	public static final String MESSAGE_I018 = "%s秒待機後、再接続します。";
	
	/** メッセージ：環境設定 */
	public static final String MESSAGE_E015 = "接続情報設定されていません。（IFID:%s）";
    public static final String MESSAGE_E016 = "設定情報存在していません。（IFID:%s）";
    
    /** メッセージ：異常終了 */
 	public static final String MESSAGE_R001 = "異常";
 	/** メッセージ：正常終了 */
 	public static final String MESSAGE_R002 = "正常";
 	/** メッセージ：SFDC-MIP連携 */
 	public static final String MESSAGE_R003 = "SFDC-MIP連携";
 	/** メッセージ：設定情報の取得 */
 	public static final String MESSAGE_R004 = "設定情報の取得";
 	/** メッセージ：DB接続 */
 	public static final String MESSAGE_R005 = "DB接続";
 	/** メッセージ：DB接続 */
 	public static final String MESSAGE_R006 = "プロパティファイルの読込";
 	/** メッセージ：マッピング情報の取得 **/
 	public static final String MESSAGE_R007 = "マッピング情報の取得";
 	/** メッセージ：データ変換の取得 **/
 	public static final String MESSAGE_R008 = "データ変換の取得";
 	/** メッセージ：CSVファイル接続 **/
 	public static final String MESSAGE_R009 = "CSVファイル接続";

}
