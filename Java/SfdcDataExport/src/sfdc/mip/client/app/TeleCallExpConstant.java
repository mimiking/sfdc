package sfdc.mip.client.app;

import sfdc.client.util.Constant;

/**
 * SFDC-MIP連携情報を管理するクラス
 * 
 * @author M.Yi 2018/1/29
 *
 */
public class TeleCallExpConstant extends Constant {	
	// メッセージ
	/** メッセージ：%sに失敗しました。 */
	public static final String MESSAGE_E001 = "%sに失敗しました。";
	/** メッセージ：エラー発生データ = %d行目 */
	public static final String MESSAGE_E002 = "エラー発生データ = %d行目";
	/** メッセージ：出力項目 = %s */
	public static final String MESSAGE_E003 = "出力項目 = %s";
	/** メッセージ：引数が正しく入力されていません。 */
	public static final String MESSAGE_E004 = "引数が正しく入力されていません。";
	/** メッセージ：%sが不正です。 */
	public static final String MESSAGE_E005 = "%sが不正です。";
	/** メッセージ：連携方式が Query ではありません。 */
	public static final String MESSAGE_E006 = "連携方式が Query ではありません。";
	
	/** メッセージ：%sを開始します。 */
	public static final String MESSAGE_I001 = "%sを開始します。";
	/** メッセージ：%sが完了しました。 */
	public static final String MESSAGE_I002 = "%sが完了しました。";
	/** メッセージ：DBに接続しました。 */
	public static final String MESSAGE_I003 = "DBに接続しました。";
	/** メッセージ：DBへの接続を切断しました。 */
	public static final String MESSAGE_I004 = "DBへの接続を切断しました。";
	/** メッセージ：%sが存在しません。 */
	public static final String MESSAGE_I005 = "%sが存在しません。";
	/** メッセージ：SOQ L= %s */
	public static final String MESSAGE_I006 = "SOQL = %s";
	/** メッセージ：SFDCに接続します。 */
	public static final String MESSAGE_I007 = "SFDCに接続します。";
	/** メッセージ：%d秒待機後に再接続します。 */
	public static final String MESSAGE_I008 = "%d秒待機後に再接続します。";
	/** メッセージ：SOQLを実行します。 */
	public static final String MESSAGE_I009 = "SOQLを実行します。";
	/** メッセージ：SFDCへの接続を切断しました。 */
	public static final String MESSAGE_I010 = "SFDCへの接続を切断しました。";
	/** メッセージ：SOQLの実行が完了しました。（抽出件数 = %d件） */
	public static final String MESSAGE_I011 = "SOQLの実行が完了しました。（抽出件数 = %d件）";
	/** メッセージ：MIP連携用CSVファイルを作成します。 */
	public static final String MESSAGE_I012 = "MIP連携用CSVファイルを作成します。";
	/** メッセージ：MIP連携用CSVファイルの作成が完了しました。（連携ファイル = %s） */
	public static final String MESSAGE_I013 = "MIP連携用CSVファイルの作成が完了しました。（連携ファイル = %s）";
	/** メッセージ：SFDC-MIP連携が%sしました。[RETURNCODE=%d] */
	public static final String MESSAGE_I015 = "SFDC-MIP連携が%sしました。[RETURNCODE=%d]";
	/** メッセージ：退避ファイル=%s を削除しました。 */
	public static final String MESSAGE_I016 = "退避ファイル = %s を削除しました。";
	/** メッセージ：CSVファイルを退避しました。（%s） */
	public static final String MESSAGE_I017 = "CSVファイルを退避しました。（%s）";
	
	/** メッセージ：異常終了 */
	public static final String MESSAGE_R001 = "異常終了";
	/** メッセージ：正常終了 */
	public static final String MESSAGE_R002 = "正常終了";
	/** メッセージ：SFDC-MIP連携 */
	public static final String MESSAGE_R003 = "SFDC-MIP連携データ抽出処理";
	/** メッセージ：設定情報の取得 */
	public static final String MESSAGE_R004 = "設定情報の取得";
	/** メッセージ：DB接続 */
	public static final String MESSAGE_R005 = "DB接続";
	/** メッセージ：プロパティファイルの読込 */
	public static final String MESSAGE_R006 = "プロパティファイルの読込";
	/** メッセージ：SOQL文の生成 */
	public static final String MESSAGE_R007 = "SOQL文の生成";
	/** メッセージ：マッピング情報の取得 */
	public static final String MESSAGE_R008 = "マッピング情報の取得";
	/** メッセージ：設定情報 */
	public static final String MESSAGE_R009 = "設定情報";
	/** メッセージ：マッピング情報 */
	public static final String MESSAGE_R010 = "マッピング情報";
	/** メッセージ：連携方式 */
	public static final String MESSAGE_R011 = "連携方式";
	/** メッセージ：SFDC接続設定情報 */
	public static final String MESSAGE_R012 = "SFDC接続設定情報";
	/** メッセージ：SFDC接続 */
	public static final String MESSAGE_R013 = "SFDC接続";
	/** メッセージ：SOQL文の実行 */
	public static final String MESSAGE_R014 = "SOQL文の実行";
	/** メッセージ：データ変換情報の取得 */
	public static final String MESSAGE_R015 = "データ変換情報の取得";
	/** メッセージ：データ変換情報 */
	public static final String MESSAGE_R016 = "データ変換情報";
	/** メッセージ：CSVファイルを作成 */
	public static final String MESSAGE_R017 = "CSVファイル作成";
	
	// 定数文字列
	/** 文字列：Query */
	public static final String STRING_QUERY = "Query";
	/** 文字列：Perfect */
	public static final String STRING_PERFECT = "Perfect";
	/** 文字列：Partial */
	public static final String STRING_PARTIAL = "Partial";
	/** 文字列：Regex */
	public static final String STRING_REGEX = "Regex";
	
}
