package sfdc.client.util;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class CommonUtils {
	
	/** ログ部品　*/
	private static Logger logger = Logger.getLogger(CommonUtils.class);
	
	public static boolean isFileExist(String filePath) {
		boolean isExist = false;
		if (!StringUtils.isEmpty(filePath)) {
			File file = new File(filePath);
			isExist = file.exists();
		}
		
		return isExist;
	}
	
	public static String dateFormat(String date, String format) {
		if (StringUtils.isEmpty(date) || StringUtils.isEmpty(format)) {
			return date;
		} else {
			try {
				String year = "", month = "", day = "";
				String hours = "08", minutes = "00", seconds = "00";
				if (date.matches("^[0-9]+$")) {
					// 全部数字の場合
					if (date.length() >= 8) {
						year = date.substring(0, 4);
						month = date.substring(4, 6);
						day = date.substring(6, 8);
					} 
					
					if (date.length() >= 10) {
						hours = date.substring(8, 10);
					} 
					
					if (date.length() >= 12) {
						hours = date.substring(8, 10);
						minutes = date.substring(10, 12);
					} 
					if (date.length() >= 14) {
						hours = date.substring(8, 10);
						minutes = date.substring(10, 12);
						seconds = date.substring(12, 14);
					}
					if (StringUtils.isEmpty(year) || StringUtils.isEmpty(month) || StringUtils.isEmpty(day)) {
						return date;
					}
				} else {
					date = date.replace("/", "-");
					String[] dateList = date.split(" ");
					
					if (dateList.length > 0) {
						String[] ymd = dateList[0].split("-");
						if (ymd.length != 3) {
							return date;
						} else {
							year = ymd[0];
							month = ymd[1];
							day = ymd[2];
						}
					}
					if (dateList.length > 1) {
						// 時分秒あり
						if (StringUtils.isNotEmpty(dateList[1])) {
							String[] hms = dateList[1].split(":");
							if (hms.length > 0) {
								hours = hms[0];
							}
							if (hms.length > 1) {
								minutes = hms[1];
							}
							if (hms.length > 2) {
								seconds = hms[2];
							}
						}
					}
					
				}
				
				date = String.format("%s-%s-%s %s:%s:%s", year, month, day, hours, minutes, seconds);
				
				Calendar c = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
				sdf.setLenient(false);
				c.setTime(sdf.parse(date));
				return CommonUtils.dateFormat(c.getTime(), format);
			} catch (ParseException e) {
				logger.error("日時フォーマット異常が発生しました。", e);
			}
		}
		
		return StringUtils.EMPTY;
	}
	
	public static String dateFormat(Date date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.JAPAN);
		sdf.setLenient(false);
		return sdf.format(date);
	}
	
    /**
     * 現在の日付を取得する。
     * @return
     */
    public static String getSysDate(){
		Date today = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		String ret = df.format(today);
		return ret;
    }
    
    /**
     * 現在の日付を取得する。
     * @return
     */
    public static String getSysTime(){
		Date today = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		String ret = df.format(today);
		return ret;
    }
    
    /**
     * 標準日時フォーマットへ変換する。
     * @param format 変更対象フォーマット
     * @return 変更後フォーマット
     */
    public static String getStandardFormat(String format) {
    	String standard ="";
    	if (format != null) {
    		String year = "", month = "", day = "";
			String hours = "", minutes = "", seconds = "", miliSec = "";
    		format = format.replaceAll("/", "-");
    		String[] fList = format.split(" ");
			
			if (fList.length > 0) {
				String[] ymd = fList[0].split("-");
				if (ymd.length != 3) {
					// フォーマット不正
					logger.error(String.format("日時フォーマットが不正です。（%s）", format));
					return "";
				} else {
					year = ymd[0].replaceAll("Y", "y");
					month = ymd[1].replaceAll("m", "M");
					day = ymd[2].replaceAll("D", "d");
				}
			}
			if (fList.length > 1) {
				// 時分秒あり
				if (StringUtils.isNotEmpty(fList[1])) {
					String [] hmsm = fList[1].split("\\.");
					if (hmsm.length > 0) {
						String[] hms = hmsm[0].split(":");
						if (hms.length > 0) {
							hours = hms[0].replaceAll("h", "H");
						}
						if (hms.length > 1) {
							minutes = hms[1].replaceAll("M", "m");
						}
						if (hms.length > 2) {
							seconds = hms[2].replaceAll("S", "s");
						}
					}
					
					if (hmsm.length > 1) {
						miliSec = hmsm[1];
					}
					
				}
			}
			
			StringBuilder builder = new StringBuilder(String.format("%s-%s-%s", year, month, day));
			if (!"".equals(hours)) {
				builder.append(" ").append(hours);
				if (!"".equals(minutes)) {
					builder.append(":").append(minutes);
					if (!"".equals(seconds)) {
						builder.append(":").append(seconds);
						if (!"".equals(miliSec)) {
							builder.append(".").append(miliSec);
						}
					}
				}
			}
			
			standard = builder.toString();
    	}
    	
    	return standard;
    }
}
