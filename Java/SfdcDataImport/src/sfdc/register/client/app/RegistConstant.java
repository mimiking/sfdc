package sfdc.register.client.app;

import sfdc.client.util.Constant;

/**
 * 定数クラス
 *
 */
public class RegistConstant extends Constant {
	public static final String MESSAGE_I001 = "---▼SFDCデータ取込処理を開始します。                        ▼----------";
    public static final String MESSAGE_I002 = "---▲SFDCデータ取込処理が%s終了しました。[RETURN CODE：%s]▲----------";
//    public static final String MESSAGE_I003 = "設定チェックを開始します。";
//    public static final String MESSAGE_I004 = "設定チェックが終了しました。[ %s ]";
    public static final String MESSAGE_I005 = "マッピング情報取得を開始します。";
    public static final String MESSAGE_I006 = "マッピング情報取得が終了しました。[ %s ]";
    public static final String MESSAGE_I008 = "SFDCに接続しました。";
    public static final String MESSAGE_I009 = "データ%sが完了しました。[ IFユニークID：%s][ 登録件数：%s, エラー件数：%s ]";
    public static final String MESSAGE_I010 = "データファイルの読込が終了しました。[ 件数：%s件 ]";
    public static final String MESSAGE_I011 = "ファイル退避を開始します。";
    public static final String MESSAGE_I012 = "ファイル退避が終了しました。[ %s ]";
    public static final String MESSAGE_I013 = "退避ファイル削除を開始します。";
    public static final String MESSAGE_I014 = "退避ファイル削除が終了しました。";
    public static final String MESSAGE_I015 = "ファイルを削除しました。(ファイル名：%s)";
    public static final String MESSAGE_I016 = "取込履歴を削除しました。[ IF登録日付：%s以前 ][ 削除件数：%s件, エラー件数：%s件 ]";
    public static final String MESSAGE_I007 = "データ取込を開始します。";
    public static final String MESSAGE_I017 = "取込履歴削除を開始します。";
//    public static final String MESSAGE_I018 = "%s秒待機後、再接続します。";
    public static final String MESSAGE_I019 = "取込対象を特定しました。 [ %s ]";
	
//	public static final String MESSAGE_E001 = "%sに失敗しました。";
//    public static final String MESSAGE_E002 = "データファイルが存在しません。（%s）";
//    public static final String MESSAGE_E003 = "%sが存在しません。（%s）";
//    public static final String MESSAGE_E004 = "マッピング情報が存在しません。（IFID: %s）";
    public static final String MESSAGE_E005 = "データファイルの読込に失敗しました。（%s）";
    public static final String MESSAGE_E006 = "SOAP処理失敗しました。";
    public static final String MESSAGE_E007 = "マッピング定義ファイルの読込に失敗しました。（%s）";
//    public static final String MESSAGE_E008 = "SFDCの接続に失敗しました。";
    public static final String MESSAGE_E009 = "ファイル退避に失敗しました。（%s）";
    public static final String MESSAGE_E010 = "退避ファイル削除が失敗しました。（%s）";
    public static final String MESSAGE_E011 = "×取込失敗 [ StatusCode：%s, ErrorMessage：%s, ErrorField：%s ]";
    public static final String MESSAGE_E012 = "×削除失敗 [ Id：%s ][ StatusCode：%s, ErrorMessage：%s ]";
    public static final String MESSAGE_E013 = "マッピング定義が設定されていません。(Line: %d)";
    public static final String MESSAGE_E014 = "引数が設定されていません。[ 引数1：データファイル名(拡張子は除く) ]";
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
//	public static final String MESSAGE_R005 = "DB接続";
	/** メッセージ：DB接続 */
//	public static final String MESSAGE_R006 = "プロパティファイルの読込";
	/** メッセージ：マッピング情報の取得 **/
	public static final String MESSAGE_R007 = "マッピング情報の取得";
	/** メッセージ：データ変換の取得 **/
	public static final String MESSAGE_R008 = "データ変換の取得";
	  
}
