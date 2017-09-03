package sfws.client.trigger;

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

import sfws.client.cmn.CmdAgent;
import sfws.client.cmn.ICmdService;

public class SfdcMailService implements ICmdService {
	//エンコード指定
	// TODO:設定ファイル
    private static final String ENCODE = "ISO-2022-JP";
    
    private static final String SEND_KBN_TO = "1";
    private static final String SEND_KBN_CC = "2";
    private static final String SEND_KBN_BCC = "3";
    
    private static final int SEND_RESULT_SUCCESS = 1;
    private static final int SEND_RESULT_FAILURE = 9;
    
    private Map<String, SfdcResult> receiverMap;
    private Map<String, SfdcResult> manageMap;
//	Map<String, Integer> retryMap = new HashMap<String, Integer>();
	
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
			
			logger.info("ローカル送信開始します。");
			manageMap = new HashMap<String, SfdcResult>();
			// ローカル送信
			isSuccess = !this.sendMail(agent);
			
			logger.info("ローカル送信終了しました。");
			logger.info("ローカル送信結果更新開始します。");
			
			// 送信管理結果更新
        	try {
        		int result;
        		int limitSize = agent.getLimitSize();
        		SfdcParam limitParam = new SfdcParam();
        		SfdcParam[] param = new SfdcParam[1];
            	SfdcParam[] successList = getSfdcParam(manageMap, SEND_RESULT_SUCCESS);
            	SfdcParam[] failureList = getSfdcParam(manageMap, SEND_RESULT_FAILURE);
            	int size = successList.length + failureList.length;
            	if(size > 0) {
	            	limitParam.setLimitSize(limitSize);
	            	param[0] = limitParam;
	            	if (size <= agent.getLimitSize()) {
	            		// 一括更新
	            		result = connection.updateManageList(successList, failureList, param);
	            		if (result == -1) {
	        				// 異常発生
	        				// TODO:
	        			}
	            	} else {
	            		int count = size % limitSize == 0 ? size / limitSize : size / limitSize + 1;
	            		for(int i = 0; i < count; i++) {
	            			// 繰り返して更新する
	            			result = connection.updateManageList(successList, failureList, param);
	            			if (result == -1) {
	            				// 異常発生
	            				// TODO:
	            			}
	            		}
	            	}
            	} else {
            		logger.info("更新管理情報が存在しません。");
            	}
			} catch (ConnectionException e) {
				logger.error("管理情報結果更新失敗しました。");
				logger.error(e.getMessage());
				logger.error(e.getCause());
			}
			
        	logger.info("ローカル送信結果更新完了しました。");
		} else if (SfdcMailAgent.MODE_CLOUD == agent.getMode()) {
			// クラウド送信
			try {
				logger.info("クラウド送信開始します。");
				// 繰り返して
				connection.sendMail(agent.getLimitSize(), agent.getRetryCount(), agent.getWaitSeconds());
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
		logger.debug(connection.getDebuggingInfo().toString());
	}
	
	private boolean sendMail(SfdcMailAgent agent) {
		boolean isAbort = false;
        int remains = 0;
        int retriedCount = 0;
        SfdcParam[] params = new SfdcParam[1];
        SfdcParam param;
        SfdcParam[] successList;
        SfdcParam[] failureList;
        String id = null;
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
                			break;
                		}
                	}
            		
            		successList = getSfdcParam(receiverMap, SEND_RESULT_SUCCESS);
            		failureList = getSfdcParam(receiverMap, SEND_RESULT_FAILURE);
            		remains = connection.updateReceiverList(successList, failureList, params);
            		if(remains == -1) {
            			// 更新失敗
            			// TODO:
            		}
            	} else {
            		logger.info(String.format("送信対象データが存在しません。 [id=%s]", id));
            	}
    		} catch (ConnectionException e) {
    			retriedCount++;
    			isAbort = retriedCount > agent.getRetryCount();
    			logger.error(e.getMessage());
    			logger.error(e.getCause());
    		}
        	
        } while(remains > 0 && !isAbort);
        
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
    			if (retriedCount <= retryCount) {
	    			try {
	    				// Wait
						Thread.sleep(waitSeconds * 1000);
					} catch (InterruptedException e) {
						logger.error(e.getMessage());
						logger.error(e.getCause());
					}
    			} else {
    				// リトライ上限回数を超えたので、処理中止する。
    				logger.info("処理回数はリトライ上限回数を超えたので、処理中止にしました。");
    				isAbort = true;
    				break;
    			}
    		}
    		result = receiverMap.get(info.getClientId());
		}
		
		return isAbort;
	}
	
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
            message.setSubject(info.getSubject(), ENCODE);
            // 本文
            message.setText(info.getBody(), ENCODE);
            // 送信日時
            message.setSentDate(new Date());
            
            // メール送信
            Transport.send(message);
            
            result.setCode(SEND_RESULT_SUCCESS);
            
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
        

        props.setProperty("mail.debug", "true");
		
		return props;
	}
	
	private Address getAddress(String name, String mail) throws UnsupportedEncodingException {
		return new InternetAddress(mail, name, ENCODE);
	}
	
	private boolean isNotEmpty(String input) {
		return input != null && input.trim().length() > 0;
	}
	
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

}
