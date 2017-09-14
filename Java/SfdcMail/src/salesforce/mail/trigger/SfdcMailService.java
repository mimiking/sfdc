package salesforce.mail.trigger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import com.sforce.soap.ClientMailInfo.ClientMailInfo;
import com.sforce.soap.SfdcMailWebService.*;
import com.sforce.soap.SfdcParam.SfdcParam;
import com.sforce.soap.partner.PartnerConnection;

import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sun.mail.smtp.SMTPMessage;

import salesforce.mail.cmn.CmdAgent;
import salesforce.mail.cmn.ICmdService;
import salesforce.mail.util.CSVFileWriter;

public class SfdcMailService implements ICmdService {
	//エンコード指定
    private static final String ENCODE = "ISO-2022-JP";
    
    /** 送信区分：　TO **/
    private static final String SEND_KBN_TO = "1";
    /** 送信区分：　CC **/
    private static final String SEND_KBN_CC = "2";
    /** 送信区分：　BCC **/
    private static final String SEND_KBN_BCC = "3";
    /** 送信結果区分：　成功 **/
    private static final int SEND_RESULT_SUCCESS = 1;
    /** 送信結果区分：　失敗 **/
    private static final int SEND_RESULT_FAILURE = 9;
    /** 宛先送信結果情報 **/
    private Map<String, SfdcResult> receiverMap;
    /** 管理送信結果情報 **/
    private Map<String, SfdcResult> manageMap;
	
	/** ログイン接続　*/
	static PartnerConnection loginCon;
	/** サービス接続　*/
	static SoapConnection connection;

	/** ログ部品　*/
	static Logger logger =Logger.getLogger(SfdcMailService.class);

	@Override
	public boolean connect(ConnectorConfig config) {
		boolean ret = true;
		if(loginCon == null || connection == null){
			try {
				loginCon = new PartnerConnection(config);
				//Debug log 設定
				LogInfo[] logs = new LogInfo[1];
				logs[0] = new LogInfo();
				logs[0].setCategory(LogCategory.Apex_code);
				logs[0].setLevel(LogCategoryLevel.Debug);
				//WSサービスのコネクションを取得
				config.setServiceEndpoint(Connector.END_POINT);
				connection = Connector.newConnection(config);
				connection.setDebuggingHeader(logs, LogType.Profiling);
				
			} catch (ConnectionException e) {
				logger.error(e.getMessage());
				ret=false;
			}
		}

		return ret;
	}

	@Override
	public boolean execute(CmdAgent cmdAgent) {
		boolean isSuccess = true;
		SfdcMailAgent agent = (SfdcMailAgent)cmdAgent;
		if (SfdcMailAgent.MODE_LOCAL == agent.getMode()) {
			manageMap = new HashMap<String, SfdcResult>();
			
			// ローカル送信
			isSuccess = !this.sendMail(agent);
			
			// 送信管理情報更新
			isSuccess = isSuccess && this.updateManage(agent.getLimitSize()) == 0;
			
		} else if (SfdcMailAgent.MODE_CLOUD == agent.getMode()) {
			// クラウド送信
			try {
				logger.info("クラウド送信開始します。");
				SfdcParam params = new SfdcParam();
				// 契約より、クラウド送信は毎日送信できる件数が異なる
				params.setLimitSize(agent.getLimitSize());
				do {
					// 繰り返して
					SfdcParam ret = connection.sendMail(params);
					params.setResult(ret.getResult());
					params.setId(ret.getId());
					
					logger.info(String.format("クラウド送信結果： result: %s, id: %s", params.getResult(), params.getId()));
					
				} while(params.getResult() != -1 && params.getId() != null);
				
				if(params.getResult() == -1) {
					// 送信失敗
					isSuccess = false;
					logger.info("処理失敗しました、処理中止とする。");
				}
				
				logger.info("クラウド送信完了しました。");
			} catch (ConnectionException e) {
				logger.error("クラウド送信失敗しました。");
				logger.error(e.getMessage());
				logger.error(e.getCause());
			}
		}
		
		return isSuccess;
	}

	@Override
	public void getDebugLog() {
		// Salesforceログ出力
		DebuggingInfo_element info = connection.getDebuggingInfo();
		if(info != null) {
			logger.info(info.toString());
		}
		
	}
	
	/**
	 * クライアントでメール送信を行う。
	 * @param agent
	 * @return 処理結果
	 */
	private boolean sendMail(SfdcMailAgent agent) {
		boolean isAbort = false;
        int remains = 0;
        int retriedCount = 0;
        SfdcParam[] params = new SfdcParam[1];
        SfdcParam param;
        SfdcParam[] successList;
        SfdcParam[] failureList;
        String id = null;
        
        logger.info("ローカル送信開始します。");
        
		// メール送信設定情報取得
        final Properties properties = this.getMailProperties(agent);
		//propsに設定した情報を使用して、sessionの作成
        final Session session = this.getSession(properties, agent.getSender(), agent.getSendPassword());
        do {
        	try {
            	// データ取得
            	ClientMailInfo[] mailInfoList = connection.getMailInfoList(agent.getLimitSize(), id);
            	if(mailInfoList != null && mailInfoList.length > 0) {
            		receiverMap = new HashMap<String, SfdcResult>();
            		param = new SfdcParam();
            		// 送信
            		for(ClientMailInfo info : mailInfoList) {
            			isAbort = this.sendMail(session, info, agent.getRetryCount(), agent.getWaitSeconds());
                		param.setId(info.getClientId());
                		params[0] = param;
                		id = info.getClientId();
                		if (isAbort) {
                			logger.error(String.format("メール送信失敗しました。[ メール送信Seq：%s, 宛先ID： %s]", info.getSendSeq(), info.getClientId()));
                			break;
                		}
                	}
            		
            		// 送信成功情報取得
            		successList = getSfdcParam(receiverMap, SEND_RESULT_SUCCESS);
            		// 送信失敗情報取得
            		failureList = getSfdcParam(receiverMap, SEND_RESULT_FAILURE);
            		// 送信結果更新
            		remains = connection.updateReceiverList(successList, failureList, params);
            		if(remains == -1) {
            			// 更新失敗、CSVファイル出力して処理中止する。
            			logger.error("更新失敗、CSVファイル出力して処理中止する。");
            			isAbort = true;
            			this.writeCsv(successList, failureList, "receiver");
            		}
            	} else {
            		logger.info(String.format("送信対象データが存在しま。 [id=%s]", id));
            	}
    		} catch (ConnectionException e) {
    			retriedCount++;
    			// リトライ回数超えたら、処理中止
    			isAbort = retriedCount > agent.getRetryCount();
    			logger.error(e.getMessage());
    			logger.error(e.getCause());
    		}
        	
        } while(remains > 0 && !isAbort);
        
        logger.info(String.format("ローカル送信完了しました。[ result: %s ]", isAbort ? -1 : 0));
        
        return isAbort;
	}
	
	/**
	 * 送信セッションを取得する。
	 * @param properties 送信設定情報
	 * @param userName アカウント名
	 * @param password パスワード
	 * @return
	 */
	private Session getSession(Properties properties, String userName, String password) {
		return Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });
	}
	
	/**
	 * メール送信を行う。
	 * @param session セッション情報
	 * @param info 送信情報
	 * @param retryCount リトライ回数
	 * @param waitSeconds 待ち時間（秒）
	 * @return
	 */
	private boolean sendMail(Session session, ClientMailInfo info, int retryCount, int waitSeconds) {
		boolean isAbort = false;
		int retriedCount = 0;
		SfdcResult result = new SfdcResult(SEND_RESULT_FAILURE);
		while(result.getCode() == SEND_RESULT_FAILURE && result.getRetryCount() < retryCount) {
			result = this.sendMail(session, info);
    		if (result.getCode() == SEND_RESULT_SUCCESS) {
    			// 送信成功
    			retriedCount = 0;
    			this.updateResult(receiverMap, info.getClientId(), true);
    			this.updateResult(manageMap, info.getSeqNo(), true);
    		} else {
    			// 送信失敗
    			retriedCount++;
    			this.updateResult(receiverMap, info.getClientId(), false);
    			this.updateResult(manageMap, info.getSeqNo(), false);
    			try {
    				// Wait
					Thread.sleep(waitSeconds * 1000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
					logger.error(e.getCause());
				}
    		}
    		result = receiverMap.get(info.getClientId());
		}
		
		if(result.getRetryCount() == retriedCount && result.getCode() != 1) {
			// リトライ上限回数を超えたので、処理中止する。
			logger.info("処理回数はリトライ上限回数を超えたので、処理中止にしました。");
			isAbort = true;
		}
		
		return isAbort;
	}
	
	/**
	 * メール送信を行う。
	 * @param session セッション情報
	 * @param info 送信内容
	 * @return 送信結果
	 */
	private SfdcResult sendMail(Session session, ClientMailInfo info) {
		SfdcResult result = new SfdcResult(SEND_RESULT_FAILURE);
		
		final SMTPMessage message = new SMTPMessage(session);
		
		try {
            final Address from = getAddress(info.getFromName(), info.getFromAddress());
            final Address to = getAddress(info.getToName(), info.getToAddress());

            // 送信元
            message.setFrom(from);
            // 送信先設定
            switch(info.getSendKbn()) {
	            case SEND_KBN_TO:
	            	message.addRecipient(Message.RecipientType.TO, to);
	            	break;
	            case SEND_KBN_CC:
	            	message.addRecipient(Message.RecipientType.CC, to);
	            	break;
	            case SEND_KBN_BCC:
	            	message.addRecipient(Message.RecipientType.BCC, to);
	            	break;
            }
            
            if (isNotEmpty(info.getReplyToAddress())) {
            	final Address replyTo = getAddress(info.getReplayToName(), info.getReplyToAddress());
            	// 返信先
                message.setReplyTo(new Address[] { replyTo });
            }
            
            if (isNotEmpty(info.getEnvelopeFromAddress())) {
            	// 送信元（Envelope）
                message.setEnvelopeFrom(info.getEnvelopeFromAddress());
            }
            
            // 件名
            if (isNotEmpty(info.getSubject())) {
            	message.setSubject(info.getSubject(), ENCODE);
            }
            // 本文
            if (isNotEmpty(info.getBody())) {
            	message.setText(info.getBody(), ENCODE);
            } else {
            	message.setText("", ENCODE);
            }
            
            // 送信日時
            message.setSentDate(new Date());
            
            // メール送信
            Transport.send(message);
            
            result.setCode(SEND_RESULT_SUCCESS);
            
            logger.info(String.format("メール送信成功しました。[ メール送信Seq：%s, 宛先ID： %s]", info.getSendSeq(), info.getClientId()));
            
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (AuthenticationFailedException e) {
            // 認証失敗
        	logger.error(e.getMessage());
            e.printStackTrace();
        } catch (MessagingException e) {
        	// SMTPサーバへの接続失敗
        	logger.error(e.getMessage());
            e.printStackTrace();
        } 

		return result;
	}
	
	
	
	/**
	 * メール送信設定情報を取得する。
	 * @param agent 代理情報
	 * @return 設定情報
	 */
	private Properties getMailProperties(SfdcMailAgent agent) {
		final Properties props = new Properties();
		// SMTPサーバーの設定。
		props.setProperty("mail.smtp.host", agent.getSmtpHost());

		// SSL用にポート番号を変更。
		props.setProperty("mail.smtp.port", String.valueOf(agent.getPort()));

        // タイムアウト設定
        props.setProperty("mail.smtp.connectiontimeout", "60000");
        props.setProperty("mail.smtp.timeout", "60000");

        // 認証
        props.setProperty("mail.smtp.auth", "true");

        // SSLを使用するとこはこの設定が必要。
        if (agent.getSslFlag() == 1) {
        	props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "true");
            props.setProperty("mail.smtp.socketFactory.port", "587");
        }
        
        if(agent.getUseProxy() == 1) {
        	// プロクシー設定
        	props.setProperty("proxySet", "true");
        	props.setProperty("socksProxyHost", agent.getProxyHost());
        	props.setProperty("socksProxyPort", String.valueOf(agent.getProxyPort()));
        }
        
        props.setProperty("mail.debug", "true");
		
		return props;
	}
	
	/**
	 * 送信アドレスオブジェクトを取得する。
	 * @param name 表示名
	 * @param mail メール
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private Address getAddress(String name, String mail) throws UnsupportedEncodingException {
		return new InternetAddress(mail, name, ENCODE);
	}
	
	private boolean isNotEmpty(String input) {
		return input != null && input.trim().length() > 0;
	}
	
	/**
	 * 格納情報から情報を取得する。
	 * @param map 格納情報
	 * @param value 値
	 * @return 情報
	 */
	private SfdcParam[] getSfdcParam(Map<String, SfdcResult> map, int value) {
		List<SfdcParam> paramList = new ArrayList<SfdcParam>();
		if(map != null && map.size() > 0) {
			SfdcParam param;
			for(Map.Entry<String, SfdcResult> e : map.entrySet()) {
				if (value == e.getValue().getCode()) {
					param = new SfdcParam();
					param.setId(e.getKey());
					param.setRetryCount(e.getValue().getRetryCount());
					paramList.add(param);
				}
			}
		}
		
		SfdcParam[] params = new SfdcParam[paramList.size()];
		params = paramList.toArray(params);
		return params;
	}
	
	/**
	 * 処理結果更新を行う。
	 * @param map 処理結果情報
	 * @param key キー
	 * @param isSuccess 処理成功フラグ
	 */
	private void updateResult(Map<String, SfdcResult> map, String key, boolean isSuccess) {
		SfdcResult result;
		int value = isSuccess ? SEND_RESULT_SUCCESS : SEND_RESULT_FAILURE;
		if (map == null) {
			map = new HashMap<String, SfdcResult>();
			result = new SfdcResult(value);
		} else {
			result = map.get(key);
			if (result != null) {
				result.setCode(value);
			} else {
				result = new SfdcResult(value);
			}
		}
		
		if (!isSuccess) {
			result.add();
		}
		map.put(key, result);
	}
	
	/**
	 * 送信管理結果更新を行う。
	 * @param limitSize 制限件数
	 * @return 処理結果（-1: 異常が発生）
	 */
	private int updateManage(int limitSize) {
		
		logger.info("送信管理情報送信結果を更新します。");
		
		int result = 0;
		// 送信管理結果更新
    	SfdcParam[] successList = getSfdcParam(manageMap, SEND_RESULT_SUCCESS);
    	SfdcParam[] failureList = getSfdcParam(manageMap, SEND_RESULT_FAILURE);
    	int size = successList.length + failureList.length;
    	if(size > 0) {
    		SfdcParam limitParam = new SfdcParam();
    		SfdcParam[] param = new SfdcParam[1];
        	limitParam.setLimitSize(limitSize);
        	param[0] = limitParam;
        	int count = size % limitSize == 0 ? size / limitSize : size / limitSize + 1;
        	try {
        		for(int i = 0; i < count; i++) {
        			// 繰り返して更新する
					result = connection.updateManageList(successList, failureList, param);
        			if (result == -1) {
        				// 異常発生、処理中止
        				break;
        			}
        		}
        	} catch (ConnectionException e) {
        		result = -1;
        		logger.error("管理情報結果更新する時、異常が発生しました。");
    			logger.error(e.getMessage());
    			logger.error(e.getCause());
			}
        	
        	if(result == -1) {
    			// CSVファイル出力
    			writeCsv(successList, failureList, "manage");
    		}
    	} else {
    		logger.info("更新管理情報が存在しません。");
    	}
    	
    	logger.info(String.format("送信管理情報送信結果を更新しました。[ result = %d ]", result));
    	
    	return result;
		
	}
	
	/**
	 * 処理結果をCSVに出力する。
	 * 
	 * @param successList 送信成功情報
	 * @param failureList 送信失敗情報
	 */
	private void writeCsv(SfdcParam[] successList, SfdcParam[] failureList, String prefix) {
		if(successList != null || failureList != null) {
			logger.error("更新失敗データをCSVファイルに出力する。");
			File dir = new File("./output");
			if(!dir.exists()) {
				dir.mkdirs();
			}
			String fileName = String.format("./output/%s_update_failure_data_%s.csv", prefix, new Date().getTime());
			CSVFileWriter fileWritter = new CSVFileWriter(new File(fileName));
			writeCsv(fileWritter, successList);
			writeCsv(fileWritter, failureList);
		}
	}
	
	/**
	 * CSVファイル出力を行う。
	 * @param fileWritter 出力オブジェクト
	 * @param paramList 結果情報
	 */
	private void writeCsv(CSVFileWriter fileWritter, SfdcParam[] paramList) {
		try {
			List<String> itemList = null;
			if(paramList != null && paramList.length > 0) {
				fileWritter.open();
				for(SfdcParam param: paramList) {
					itemList = new ArrayList<String>();
					itemList.add(param.getId());
					itemList.add(String.valueOf(param.getRetryCount()));
					fileWritter.writeLine(itemList);
				}
				
				fileWritter.close();
			}
		} catch (IOException e) {
			logger.error("CSVファイル出力失敗しました。");
			logger.error(e.getMessage());
			logger.error(e.getStackTrace());
		}
	}

}
