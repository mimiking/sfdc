package sfdc.db.entity;

/**
 * 設定データエンティティークラス
 *
 */
public class SettingEntity {
	/** 入出力ファイルヘッダ有無 　1: ヘッダなし **/
	public static final String FILE_HEADER_YES = "1";
	/** 連携方式： 登録 **/
	public static final String LINK_METHOD_CREATE = "create";
	/** 連携方式： 更新 **/
	public static final String LINK_METHOD_UPDATE = "update";
	/** 連携方式： 登録・更新 **/
	public static final String LINK_METHOD_UPSERT = "upsert";
	/** 連携方式： 削除 **/
	public static final String LINK_METHOD_DELETE = "delete";
	/** 連携方式： BULK登録 **/
	public static final String LINK_METHOD_BULK_CREATE = "bulkcreate";
	/** 連携方式： BULK更新 **/
	public static final String LINK_METHOD_BULK_UPDATE = "bulkupdate";
	/** 連携方式： BULK登録・更新 **/
	public static final String LINK_METHOD_BULK_UPSERT = "bulkupsert";
	/** 連携方式： BULK削除 **/
	public static final String LINK_METHOD_BULK_DELETE = "bulkdelete";
	
	// IF管理
	/** IDIF **/
	private String ifId;
	/** 接続先ID **/
	private int connectId;
	/** 入出力ファイルヘッダ有無 */ 
	private String fileHeader;
	/** 入出力ファイル文字コード */ 
	private String fileEncoding;
	/** 入出力ファイル区切り文字 */ 
	private String fileSplitter;
	/** 連携方式 */ 
	private String linkMethod;
	/** 連携オブジェクト名 */ 
	private String linkObj;
	/** 外部ID項目名 */ 
	private String externalIdCol;
	/** 抽出条件 */ 
	private String condition;
	
	// 接続先
	/** EndpointURL */ 
	private String endpointUrl ;
	/** ユーザID */ 
	private String userId;
	/** パスワード(暗号化済) */ 
	private String password;
	
	/**
	 * コンストラクタ
	 */
	public SettingEntity() {}

	/**
	 * Debug情報を出力する。
	 * @return
	 */
	public String dumpInfo(){
		StringBuilder info = new StringBuilder("\n");
		info.append("IF管理\n");
		info.append("IFID = ").append(this.ifId).append("\n");
		info.append("接続先ID = ").append(this.connectId).append("\n");
		info.append("入出力ファイルヘッダ有無 = ").append(this.fileHeader).append("\n");
		info.append("入出力ファイル文字コード = ").append(this.fileEncoding).append("\n");
		info.append("入出力ファイル区切り文字 = ").append(this.fileSplitter).append("\n");
		info.append("連携方式 = ").append(this.linkMethod).append("\n");
		info.append("連携オブジェクト名 = ").append(this.linkObj).append("\n");
		info.append("外部ID項目名 = ").append(this.externalIdCol).append("\n");
		info.append("抽出条件 = ").append(this.condition).append("\n");
		info.append("接続先\n");
		info.append("ユーザーID = ").append(this.userId).append("\n");
		info.append("パスワード（暗号化済） = ").append(this.password).append("\n");
		info.append("EndpointURL = ").append(this.endpointUrl).append("\n");
		return info.toString();
	}
	
	public String getIfId() {
		return ifId;
	}

	public void setIfId(String ifId) {
		this.ifId = ifId;
	}

	public int getConnectId() {
		return connectId;
	}

	public void setConnectId(int connectId) {
		this.connectId = connectId;
	}

	public String getEndpointUrl() {
		return endpointUrl;
	}

	public void setEndpointUrl(String endpointUrl) {
		this.endpointUrl = endpointUrl;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getFileHeader() {
		return fileHeader;
	}

	public void setFileHeader(String fileHeader) {
		this.fileHeader = fileHeader;
	}

	public String getFileEncoding() {
		return fileEncoding;
	}

	public void setFileEncoding(String fileEncoding) {
		this.fileEncoding = fileEncoding;
	}

	public String getFileSplitter() {
		return fileSplitter;
	}

	public void setFileSplitter(String fileSplitter) {
		this.fileSplitter = fileSplitter;
	}

	public String getLinkMethod() {
		return linkMethod;
	}

	public void setLinkMethod(String linkMethod) {
		this.linkMethod = linkMethod;
	}

	public String getLinkObj() {
		return linkObj;
	}

	public void setLinkObj(String linkObj) {
		this.linkObj = linkObj;
	}

	public String getExternalIdCol() {
		return externalIdCol;
	}

	public void setExternalIdCol(String externalIdCol) {
		this.externalIdCol = externalIdCol;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
	
}
