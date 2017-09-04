global class SfdcMailWebService {

    /**
     * 送信データを取得する。
     * @param limitSize 制限件数
     * @param param 最後送信データ情報
     * @retrun 送信データ情報
     */
    webService static List<ClientMailInfo> getMailInfoList(Integer limitSize, Id clientId) {
        List<MailReceiver__c> receiverList;
        List<ClientMailInfo> clientMailList = new List<ClientMailInfo>();

        info('送信データを取得します。[ clientId: ' + clientId + ', limitSize: ' + limitSize + ' ]');

        if(clientId == null) {
            // 初回の場合
            receiverList = getFirstPage(limitSize);
        } else {
            // 二回目以後
            receiverList = getNextPage(limitSize, clientId);
        }

        if (receiverList != null && receiverList.size() > 0) {
            // データ存在
            info('送信データを変換をします。');
            for(MailReceiver__c receiver : receiverList) {
                // データ変換
                clientMailList.add(getClientMailInfo(receiver));
            }
            info('送信データの変換が完了しました。');
        }

        info('送信データの取得完了しました。[ 件数: ' + clientMailList.size() + ' ]');

        return clientMailList;
    }

    /**
     * 宛先送信結果更新を行う。
     * @param successList 送信成功情報
     * @param failureList 送信失敗情報
     * @param paramList 最後送信データ情報
     * @retrun 処理結果（０：処理完了、－1：処理異常、その他：次回処理件数）
     */
     webService static Integer updateReceiverList(List<SfdcParam> successList, List<SfdcParam> failureList, List<SfdcParam> paramList) {
         
         Integer toDoCount = 0;
         info('宛先送信結果情報の更新が開始します。');
         if(paramList != null && paramList.size() > 0) {
         	 SfdcParam param = paramList[0];
             // 更新対象情報取得
             List<MailReceiver__c> receiverList = getReceiverList(param.id);
             // 対象データ更新
             toDoCount = updateReceiver(receiverList, successList, failureList);
             if(toDoCount != -1) {
                 // 異常発生しない場合、次回処理対象件数取得
                 toDoCount = getToDoCount(param.id);
             }
        }
        info('宛先送信結果情報の更新が終了しました。[ 処理結果：' + toDoCount + ' ]');

        return toDoCount;
    }

    /**
     * 送信管理結果更新を行う。
     * @param successList 送信成功情報
     * @param failureList 送信失敗情報
     * @param paramList 制限件数情報
     * @retrun 処理結果（０：正常　－1：異常）
     */
    webService static Integer updateManageList(List<SfdcParam> successList, List<SfdcParam> failureList, List<SfdcParam> paramList) {
        Integer result = 0;
        if(paramList != null && paramList.size() > 0) {
            Integer limitSize = paramList[0].limitSize;
            // 更新対象情報取得
            List<MailManage__c> manageList = getMailManageList(limitSize);
            if(manageList != null && manageList.size() > 0) {
                // 対象データ更新
                result = updateManage(manageList, successList, failureList);
            }
        }

        return result;
    }
    
    webService static Integer sendMail(SfdcParam param) {
        Integer result = 0;
        List<MailReceiver__c> receiverList;
        List<ClientMailInfo> clientMailList = new List<ClientMailInfo>();
        // 送信処理
        if(param != null) {
            if(param.id == null) {
                // 初回
                receiverList = getFirstPage(param.limitSize);
            } else {
                // ２回目以後
                receiverList = getNextPage(param.limitSize, param.id);
            }

            if(receiverList != null && receiverList.size() > 0) {
                Id lastMamageId;
                Id lastReceiverId;
                Boolean isAbort = false;
                SfdcResult receiverResult;
                SfdcResult manageResult;
                //List<SfdcParam> successList = new List<SfdcParam>();
                //List<SfdcParam> failureList = new List<SfdcParam>();
                Map<Id, SfdcResult> receiverMap = new Map<Id, SfdcResult>();
                Map<Id, SfdcResult> manageMap = new Map<Id, SfdcResult>();
                for(MailReceiver__c receiver: receiverList) {
                    // メール送信を行う
                    receiverResult = sendMail(receiver, param.retryCount, param.waitSeconds);
                    if(receiverResult != null) {
                        receiverMap.put(receiver.Id, receiverResult);
                        manageResult = manageMap.get(receiver.MAIL_SEQ_NUM__c);
                        if(manageResult != null) {
                            // 既に存在
                            if(manageResult.code < receiverResult.code) {
                                // 処理結果更新
                                manageMap.put(receiver.MAIL_SEQ_NUM__c, receiverResult);
                            }
                        } else {
                            manageMap.put(receiver.MAIL_SEQ_NUM__c, receiverResult);
                        }
                        lastMamageId = receiver.MAIL_SEQ_NUM__c;
                        lastReceiverId = receiver.Id;
                    } else {
                        // 異常発生
                        //isAbort = true;
                        result = -1;
                        error('フェータルエラーが発生しましたので、メール送信中止をしました。');
                        break;
                    }
                }

                // 送信結果更新を行う
                Integer status = updateReceiver(receiverList, receiverMap);
                if(status == -1) {
                    // 更新エラー発生
                    // TODO：処理中止
                    result = -1;
                } else {
                    // 管理テーブル更新
                    List<MailManage__c> manageList = getMailManageList(param.limitSize);
                    status = updateManage(manageList, manageMap, lastMamageId, lastReceiverId);
                    if(status == -1) {
                        // 更新エラー発生
                        // TODO:
                        result = -1;
                    }
                }


            } else {
                info('メール送信データがありません。');
            }
        }
        
        
        return result;
    }

    private static SfdcResult sendMail(MailReceiver__c receiver, Integer retryCount, Integer waitSeconds) {
        SfdcResult result = null;
        Messaging.SingleEmailMessage message = new Messaging.SingleEmailMessage();
        if(receiver.SEND_ADR__c != null 
           && (receiver.SEND_KBN__c == '1' || receiver.SEND_KBN__c == '2' || receiver.SEND_KBN__c == '3')) {
            if(receiver.SEND_KBN__c == '1') {
                // TO
                message.setToAddresses(new String[] { receiver.SEND_ADR__c });
            } else if(receiver.SEND_KBN__c == '2') {
                // CC
                message.setCcAddresses(new String[] { receiver.SEND_ADR__c });
            } else if(receiver.SEND_KBN__c == '3') {
                // BCC
                message.setBccAddresses(new String[] { receiver.SEND_ADR__c });
            }
            
            //message.optOutPolicy = 'FILTER';
            message.setSenderDisplayName(receiver.MAIL_SEQ_NUM__r.FROM_NAME__c);
            message.setReplyTo(receiver.MAIL_SEQ_NUM__r.REPLY_TO_ADDRESS__c);
            message.subject = receiver.MAIL_SEQ_NUM__r.MAIL_SUBJECT__c;
            message.plainTextBody = receiver.MAIL_SEQ_NUM__r.MAIL_BODY__c;
            Messaging.SingleEmailMessage[] messages = new List<Messaging.SingleEmailMessage>{message};
            Messaging.SendEmailResult[] results = Messaging.sendEmail(messages);
            result = new SfdcResult();
            if (results[0].success) {
                result.code = '1';
                info('送信成功しました。[ Id: ' + receiver.Id + ' ]');
            } else {
                result.code = '9';
                info('送信失敗しました。[ Id: ' + receiver.Id + ' ]');
            }
        }

        return result;
    }
    
    
    /**
     * 処理対象データ件数を取得する。
     */
    private static Integer getDataSize() {
        Integer size  = [
          Select
            Count()
          From
            MailReceiver__c
          Where
            SEND_RESULT__c != '1'
            And MAIL_SEQ_NUM__r.SEND_RESULT__c != '1' 
        ];
        
        return size;
        
        //Integer pageSize = 0;
        //String soql = 'SELECT Count(Id) FROM MailReceiver__c Where MAIL_SEQ_NUM__r.SEND_RESULT__c != \'1\'';
        //Integer dataSize = database.countQuery(soql);
        /**
        if(dataSize > 0) {
            pageSize = Math.mod(dataSize, limitSize) == 0 ? dataSize / limitSize : dataSize / limitSize + 1;
        }

**/
        
        //return database.countQuery(soql);
    }
    
    private static List<MailReceiver__c> getFirstPage(Integer limitSize) {
        List<MailReceiver__c> mailList = [
            SELECT Id,
              MAIL_SEQ_NUM__c,
              //MAIL_SEQ_NUM__r.Id,
              MAIL_SEQ_NUM__r.FROM_ADDRESS__c,
              MAIL_SEQ_NUM__r.ENVELOPE_FROM_ADDRESS__c,
              MAIL_SEQ_NUM__r.COMPANY_CODE__c,
              MAIL_SEQ_NUM__r.FROM_NAME__c,
              MAIL_SEQ_NUM__r.REPLY_TO_ADDRESS__c,
              MAIL_SEQ_NUM__r.REPLY_TO_NAME__c,
              MAIL_SEQ_NUM__r.MAIL_SUBJECT__c,
              MAIL_SEQ_NUM__r.MAIL_BODY__c,
              SEND_ADR__c,
              Name,
              SEND_KBN__c,
              SEND_SEQ__c
            From
              MailReceiver__c
            Where
              MAIL_SEQ_NUM__r.SEND_RESULT__c != '1'
            Order By
              Id, MAIL_SEQ_NUM__c, SEND_KBN__c, SEND_SEQ__c
            limit :limitSize
        ];
        
        return mailList;
    }
    
    private static List<MailReceiver__c> getNextPage(Integer limitSize, Id nextId) {
        List<MailReceiver__c> mailList = [
            SELECT Id,
              MAIL_SEQ_NUM__c,
              MAIL_SEQ_NUM__r.FROM_ADDRESS__c,
              MAIL_SEQ_NUM__r.ENVELOPE_FROM_ADDRESS__c,
              MAIL_SEQ_NUM__r.COMPANY_CODE__c,
              MAIL_SEQ_NUM__r.FROM_NAME__c,
              MAIL_SEQ_NUM__r.REPLY_TO_ADDRESS__c,
              MAIL_SEQ_NUM__r.REPLY_TO_NAME__c,
              MAIL_SEQ_NUM__r.MAIL_SUBJECT__c,
              MAIL_SEQ_NUM__r.MAIL_BODY__c,
              SEND_ADR__c,
              Name,
              SEND_KBN__c,
              SEND_SEQ__c
            From
              MailReceiver__c
            Where
              MAIL_SEQ_NUM__r.SEND_RESULT__c != '1'
              And Id > :nextId
            Order By
              Id, MAIL_SEQ_NUM__c, SEND_KBN__c, SEND_SEQ__c                                
            limit :limitSize
        ];
        
        return mailList;
    }
    
    private static ClientMailInfo getClientMailInfo(MailReceiver__c receiver) {
        ClientMailInfo info = new ClientMailInfo();
        // ID
        info.clientId = receiver.Id;
        // メール送信ｓｅｑ
        info.seqNo = receiver.MAIL_SEQ_NUM__c;
        // メール送信元アドレス（Ｆｒｏｍ）
        info.fromAddress = receiver.MAIL_SEQ_NUM__r.FROM_ADDRESS__c;
        // メール送信元アドレス（ｅｎｖｅｌｏｐｅ）
        info.envelopeFromAddress = receiver.MAIL_SEQ_NUM__r.ENVELOPE_FROM_ADDRESS__c;
        // 会社コード
        info.companyCode = receiver.MAIL_SEQ_NUM__r.COMPANY_CODE__c;
        // メール送信元名称（Ｆｒｏｍ）
        info.fromName = receiver.MAIL_SEQ_NUM__r.FROM_NAME__c;
        // メール送信元アドレス（Ｒｅｐｌｙ－Ｔｏ）
        info.replyToAddress = receiver.MAIL_SEQ_NUM__r.REPLY_TO_ADDRESS__c;
        // メール送信元名称（Ｒｅｐｌｙ－Ｔｏ）
        info.replayToName = receiver.MAIL_SEQ_NUM__r.REPLY_TO_NAME__c;
        // メール送信題名
        info.subject = receiver.MAIL_SEQ_NUM__r.MAIL_SUBJECT__c;
        // メール送信本文
        info.body = receiver.MAIL_SEQ_NUM__r.MAIL_BODY__c;
        // メール送信先
        info.toAddress = receiver.SEND_ADR__c;
        // メール送信先名称
        info.toName = receiver.Name;
        // メール送信先区分
        info.sendKbn = receiver.SEND_KBN__c;
        // メール送信先順序
        info.sendSeq = receiver.SEND_SEQ__c;
        
        return info;
    }
    
    /**
    private static void updateReceiver(List<SfdcParam> rSuccessList, List<SfdcParam> rFailureList) {
        List<MailReceiver__c> receiverList;
        Integer size = getDataSize();
        if(size < 50000) {
            // 一括更新
            System.debug('一括更新');
            receiverList = getReceiverList(null);
            updateReceiver(receiverList, rSuccessList, rFailureList);
        } else {
            // 複数回更新
            Integer times = Math.mod(size, 50000) == 0 ? size / 50000 : size / 50000 + 1;
            for(Integer i = 0; i < times; i++) {
                receiverList = getReceiverList(null);
                updateReceiver(receiverList, rSuccessList, rFailureList);
            }
        }
    }
    **/
    
    private static Integer updateReceiver(List<MailReceiver__c> receiverList, List<SfdcParam> rSuccessList, List<SfdcParam> rFailureList) {
        Integer result = 0;
        if (receiverList != null && receiverList.size() > 0) {
            for(MailReceiver__c receiver: receiverList) {
                //isExist = isReceiverExist(receiver, rSuccessList);
                SfdcParam param = getReveiverParam(receiver, rSuccessList);
                if(param != null) {
                    System.debug('送信成功更新');
                    // 成功
                    receiver.SEND_RESULT__c = '1';
                } else {
                    // 失敗
                    param = getReveiverParam(receiver, rFailureList);
                    if(param != null) {
                        receiver.SEND_RESULT__c = '9';
                        receiver.RETRY_CNT__c = param.retryCount;
                    }
                }
            }
            try {
                upsert receiverList;
            } catch (DmlException e) {
                // TODO:異常処理
                result = -1;
                System.debug(e.getMessage());
                System.debug(e.getCause());
            }
            
        }

        return result;
    }

    private static Integer updateReceiver(List<MailReceiver__c> receiverList, Map<Id, SfdcResult> resulMap) {
        Integer result = 0;
        if(receiverList != null && receiverList.size() > 0 && resulMap != null && !resulMap.isEmpty()) {
            SfdcResult sfdcResult;
            for(MailReceiver__c receiver: receiverList) {
                sfdcResult = resulMap.get(receiver.Id);
                if(sfdcResult != null) {
                    receiver.SEND_RESULT__c = '' + sfdcResult.code;
                    receiver.RETRY_CNT__c = sfdcResult.retryCount;
                }
            }

            try {
                upsert receiverList;
            } catch (DmlException e) {
                // 異常
                result = -1;
                System.debug(e.getMessage());
                System.debug(e.getCause());
            }
        }

        return result;
    }
    
    private static List<MailReceiver__c> getReceiverList(Id lastSendId) {
        List<MailReceiver__c> receiverList = [
            Select
                Id,
                SEND_RESULT__c
            From
                MailReceiver__c
            Where
                SEND_RESULT__c != '1'
                And MAIL_SEQ_NUM__r.SEND_RESULT__c != '1'
                And Id <= :lastSendId
        ];

        return receiverList;
    }

    private static Integer getToDoCount(Id lastSendId) {
        Integer count = [
            Select
              Count() 
            From
              MailReceiver__c
            Where
              SEND_RESULT__c != '1'
              And MAIL_SEQ_NUM__r.SEND_RESULT__c !='1'
              And Id > :lastSendId
        ];

        return count;
    }
    
    private static Integer getManageCount() {
        Integer count = [
            Select
              Count() 
            From
              MailManage__c
            Where
              SEND_RESULT__c != '1'
        ];

        return count;
    }

    private static List<MailManage__c> getMailManageList(Integer limitSize) {
        List<MailManage__c> manageList = [
            Select
              Id,
              SEND_RESULT__c,
              RETRY_CNT__c
            From
              MailManage__c
            Where
              SEND_RESULT__c != '1'
            Limit :limitSize
        ];

        return manageList;
    }

    private static Integer updateManage(List<MailManage__c> manageList, List<SfdcParam> successList, List<SfdcParam> failureList) {
        Integer result = 0;
        if (manageList != null && manageList.size() > 0) {
            for(MailManage__c manage: manageList) {
                SfdcParam param = getManageParam(manage, successList);
                if(param != null) {
                    System.debug('送信成功更新');
                    // 成功
                    manage.SEND_RESULT__c = '1';
                } else {
                    // 失敗
                    param = getManageParam(manage, failureList);
                    if(param != null) {
                        manage.SEND_RESULT__c = '9';
                        manage.RETRY_CNT__c = param.retryCount;
                    }
                }
            }

            try {
                upsert manageList;
            } catch (DmlException e) {
                // TODO:異常処理
                result = -1;
                error(e.getMessage());
                error(e.getCause());
            }
            
        }

        return result;
    }

    private static Integer updateManage(
        List<MailManage__c> manageList, Map<Id,SfdcResult> resultMap, Id lastMamageId, Id lastReceiverId) {
        Integer result = 0;
        if(manageList != null && manageList.size() > 0 && resultMap != null && !resultMap.isEmpty()) {
            SfdcResult sfdcResult;
            for(MailManage__c manage : manageList) {
                System.debug('manage.Id = ' + manage.Id + ', lastMamageId = ' + lastMamageId);
                sfdcResult = resultMap.get(manage.Id);
                if(sfdcResult != null) {
                    if(manage.Id == lastMamageId) {
                        // 処理完了しているかを判断する
                        if(isAllMailSent(lastMamageId, lastReceiverId)) {
                            manage.SEND_RESULT__c = '' + sfdcResult.code;
                            manage.RETRY_CNT__c = sfdcResult.retryCount;
                        }
                    } else {
                        manage.SEND_RESULT__c = '' + sfdcResult.code;
                        manage.RETRY_CNT__c = sfdcResult.retryCount;
                    }
                }
            }

            try {
                upsert manageList;
            } catch (DmlException e) {
                result = -1;
                error('送信管理情報更新失敗しましたので処理中止する。');
                error(e.getMessage());
                error(e.getCause());
            }
        } else {
            info('送信管理更新対象情報が存在しません。');
        }


        return result;
    }
    
    private static boolean isReceiverExist(MailReceiver__c receiver, List<SfdcParam> paramList) {
        boolean isExist = false;
        if(receiver != null && paramList != null && paramList.size() > 0) {
            for(SfdcParam param : paramList) {
                if (receiver.Id == param.Id) {
                    isExist = true;
                    break;
                }
            }
        }
        
        return isExist;
    }

    private static SfdcParam getReveiverParam(MailReceiver__c receiver, List<SfdcParam> paramList) {
        SfdcParam param = null;
        if(receiver != null && paramList != null && paramList.size() > 0) {
            for(SfdcParam sp : paramList) {
                if (receiver.Id == sp.Id) {
                    param = sp;
                    break;
                }
            }
        }
        
        return param;
    }

    private static SfdcParam getManageParam(MailManage__c manage, List<SfdcParam> paramList) {
        SfdcParam param = null;
        if(manage != null && paramList != null && paramList.size() > 0) {
            for(SfdcParam sp : paramList) {
                if(sp.Id == manage.Id) {
                    // 存在
                    param = sp;
                    break;
                }
            }
        }

        return param;
    }

    private static Boolean isAllMailSent(Id manageId, Id receiverId) {
        Integer count = [
            Select
              Count() 
            From
              MailReceiver__c
            Where
              MAIL_SEQ_NUM__c = :manageId
              And Id > :receiverId
              And SEND_RESULT__c != '1'
        ];

        return count == 0;
    }

    private static void info(Object message) {
        log(LoggingLevel.INFO, message);
    }
    
    private static void error(Object message) {
        log(LoggingLevel.ERROR, message);
    }

    private static void log(LoggingLevel level, Object message) {
        System.debug(level, message);
    }
}