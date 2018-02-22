package sfdc.db.entity;

/**
 * マッピングデータエンティティークラス
 *
 */
public class MappingEntity {
	/** IFID **/
	private String ifId;
	/** 順序No. */ 
	private int no;
	/** 入力項目論理名 */ 
	private String iColL;
	/** 入力項目物理名 */ 
	private String iColP;
	/** 出力項目論理名 */ 
	private String oColL;
	/** 出力項目物理名 */ 
	private String oColP;
	/** 固定値 */ 
	private String fixedVal;
	/** 複数選択 */ 
	private int multiSelect;
	/** 切出し位置 */ 
	private int midStart;
	/** 切出し文字数 */ 
	private int midCount;
	/** 日時フォーマット */ 
	private String dtFormat;
	
	/**
	 * コンストラクタ
	 */
	public MappingEntity() {}

	/**
	 * Debug情報を出力する。
	 * @return
	 */
	public String dumpInfo(){ 
		StringBuilder info = new StringBuilder("\n");
		info.append("マッピング定義\n");
		info.append("IFID = ").append(this.ifId).append("\n");
		info.append("順序No. = ").append(this.no).append("\n");
		info.append("入力項目論理名 = ").append(this.iColL).append("\n");
		info.append("入力項目物理名 = ").append(this.iColP).append("\n");
		info.append("出力項目論理名 = ").append(this.oColL).append("\n");
		info.append("出力項目物理名 = ").append(this.oColP).append("\n");
		info.append("固定値 = ").append(this.fixedVal).append("\n");
		info.append("複数選択 = ").append(this.multiSelect).append("\n");
		info.append("切出し位置 = ").append(this.midStart).append("\n");
		info.append("切出し文字数 = ").append(this.midCount).append("\n");
		info.append("日時フォーマット = ").append(this.dtFormat).append("\n");
		return info.toString();
	}  	
	
	public String getIfId() {
		return this.ifId;
	}

	public void setIfId(String ifId) {
		this.ifId = ifId;
	}
	
	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public String getiColL() {
		return iColL;
	}

	public void setiColL(String iColL) {
		this.iColL = iColL;
	}

	public String getiColP() {
		return iColP;
	}

	public void setiColP(String iColP) {
		this.iColP = iColP;
	}

	public String getoColL() {
		return oColL;
	}

	public void setoColL(String oColL) {
		this.oColL = oColL;
	}

	public String getoColP() {
		return oColP;
	}

	public void setoColP(String oColP) {
		this.oColP = oColP;
	}

	public String getFixedVal() {
		return fixedVal;
	}

	public void setFixedVal(String fixedVal) {
		this.fixedVal = fixedVal;
	}

	public int getMultiSelect() {
		return multiSelect;
	}

	public void setMultiSelect(int multiSelect) {
		this.multiSelect = multiSelect;
	}

	public int getMidStart() {
		return midStart;
	}

	public void setMidStart(int midStart) {
		this.midStart = midStart;
	}

	public int getMidCount() {
		return midCount;
	}

	public void setMidCount(int midCount) {
		this.midCount = midCount;
	}

	public String getDtFormat() {
		return dtFormat;
	}

	public void setDtFormat(String dtFormat) {
		this.dtFormat = dtFormat;
	}
	
}
