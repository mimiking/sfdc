package sfws.client.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CSVFileWriter {

    public static final String RETURN_CODE = "\r\n";

    /** エンコード utf8 */
    public static final String CHAR_CODE_UTF = "UTF-8";

    /** エンコード ms932 */
    public static final String CHAR_CODE_MS932 = "ms932";

    /** 出力ファイル */
    private File outputFile = null;

    /** OutputStream */
    private BufferedOutputStream out = null;
    //private PrintWriter out = null;

    /** 出力バイト数 */
    private int writeBytes = 0;
    /** 入力文字コード */
    private String inCharCode;
    /** 出力文字コード */
    @SuppressWarnings("unused")
	private String outCharCode;
    /** 出力区切り文字 */
    private String separator;


    /**
     * コンストラクタ
     *
     * @param outputFile CSVファイル
     */
    public CSVFileWriter(final File outputFile) {
        this.outputFile = outputFile;
        this.separator =",";
        this.inCharCode=CHAR_CODE_MS932;
        this.outCharCode=CHAR_CODE_MS932;
    }

    public CSVFileWriter(final File outputFile, String separator) {
        this.outputFile = outputFile;
        this.separator =separator;
        this.inCharCode=CHAR_CODE_MS932;;
        this.outCharCode=CHAR_CODE_MS932;
    }

    /**
     * ファイルをオープンし、ヘッダ行を読み取ります
     *
     * @throws IOException 入出力例外
     */
    public final void open() throws IOException {
        close();

        out = new BufferedOutputStream(new FileOutputStream(outputFile));
    }

    /**
     * ファイルをクローズします.
     *
     */
    public final void close() {
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                out = null;
            }
        }
        writeBytes = 0;
    }


    /**
     * エスケープ処理
     * 文字列全体を"（ダブルクォーテーション）で囲む
     *
     * @param value カラム値
     * @return 処理後カラム値
     */
    private String escape(final String value) {
        if (value == null) {
            return "\"\"";
        }

        String ret = value.replaceAll("\"", "\"\"");
        return "\"" + ret + "\"";
    }

    /**
     * ファイルの出力
     *
     * @throws IOException ファイルの出力に失敗した場合
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * 出力バイト数の取得
     *
     */
    public int getWriteBytes() {
        return writeBytes;
    }


    /**
     * ファイル出力処理
     *
     * @param line 出力レコード
     * @throws IOException 入出力例外
     */
    public void writeLine(final List<String> line) throws IOException {
        StringBuffer sb = new StringBuffer();

        boolean b = false;
        for (String s : line) {
            if (b) {
               sb.append(separator);
            }
            sb.append(escape(s));

            b = true;
        }
        sb.append(RETURN_CODE);
        String p = sb.toString();

        byte[] bb = p.getBytes(inCharCode);

        out.write(bb);

        writeBytes += bb.length;
    }

    /**
     * ファイル出力処理
     *
     * @param line 出力レコード
     * @throws IOException 入出力例外
     */
    public void writeLineWithoutEscape(final List<String> line) throws IOException {
        StringBuffer sb = new StringBuffer();

        boolean b = false;
        for (String s : line) {
            if (b) {
                sb.append(separator);
            }
            sb.append(s);
            b = true;
        }
        sb.append(RETURN_CODE);
        String p = sb.toString();

        byte[] bb = p.getBytes(inCharCode);

        out.write(bb);

        writeBytes += bb.length;
    }

	/**
	 * 文字コード取得
	 * @return
	 */
	public String getInCharCode() {
		return inCharCode;
	}


	/**
	 * 文字コード設定
	 * @param charCode
	 */
	public void setInCharCode(String charCode) {
		this.inCharCode = charCode;
	}

	public void setOutCharCode(String outCharCode) {
		this.outCharCode = outCharCode;
	}

}