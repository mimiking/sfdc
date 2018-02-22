package sfdc.db.entity;

public class ConvertEntity {
	/** IDIF **/
	private String ifId;
	/** 順序No. **/
	private int no;
	/** データ変換通番 **/
	private int convNo;
	/** 入力項目論理名 **/
	private String iColL;
	/** 入力項目物理名 **/
	private String iColP;
	/** 変換前 **/
	private String before;
	/** 変換後 **/
	private String after;
	/** 変換条件 **/
	private String fixedVal;
	/** メモ **/
	private String memo;
	/** 削除フラグ **/
	private String delFlg;
	
	public ConvertEntity() {}
	
	/**
	 * Debug情報を出力する。
	 * @return
	 */
	public String dumpInfo(){
		StringBuilder info = new StringBuilder("\n");
		info.append("データ変換\n");
		info.append("IFID = ").append(this.ifId).append("\n");
		info.append("順序No. = ").append(this.no).append("\n");
		info.append("データ変換通番 = ").append(this.convNo).append("\n");
		info.append("入力項目論理名 = ").append(this.iColL).append("\n");
		info.append("入力項目物理名 = ").append(this.iColP).append("\n");
		info.append("変換前 = ").append(this.before).append("\n");
		info.append("変換後 = ").append(this.after).append("\n");
		info.append("変換条件 = ").append(this.fixedVal).append("\n");
		info.append("備考 = ").append(this.memo).append("\n");
		info.append("削除フラグ = ").append(this.delFlg).append("\n");
		return info.toString();
	}
	
	public String getIfId() {
		return ifId;
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
	
	public int getConvNo() {
		return convNo;
	}
	
	public void setConvNo(int convNo) {
		this.convNo = convNo;
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
	
	public String getBefore() {
		return before;
	}
	
	public void setBefore(String before) {
		this.before = before;
	}
	
	public String getAfter() {
		return after;
	}
	
	public void setAfter(String after) {
		this.after = after;
	}
	
	public String getFixedVal() {
		return fixedVal;
	}
	
	public void setFixedVal(String fixedVal) {
		this.fixedVal = fixedVal;
	}
	
	public String getMemo() {
		return memo;
	}
	
	public void setMemo(String memo) {
		this.memo = memo;
	}
	
	public String getDelFlg() {
		return delFlg;
	}
	
	public void setDelFlg(String delFlg) {
		this.delFlg = delFlg;
	}
	
}
