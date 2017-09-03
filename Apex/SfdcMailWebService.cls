global class SfdcMailWebService {

    /**
     * 送信データを取得する。
     * @param limitSize 制限件数
     * @param param 最後送信データ情報
     */
    webService static List<ClientMailInfo> getMailInfoList(Integer limitSize, Id clientId) {
        List<MailReceiver__c> receiverList;
        List<ClientMailInfo> clientMailList = new List<ClientMailInfo>(); 
        if(clientId == null) {
            // 初回の場合
            System.debug('★★★★★★★★★★★★★★★★★★初回の場合 ');
            receiverList = getFirstPage(limitSize);
        } else {
            // 二回目以後
            System.debug('★★★★★★★★★★★★★★★★★★二回目以後 ' + clientId);
            receiverList = getNextPage(limitSize, clientId);
        }

        if (receiverList != null && receiverList.size() > 0) {
            // データ存在
            for(MailReceiver__c receiver : receiverList) {
                // データ変換
                clientMailList.add(getClientMailInfo(receiver));
            }
        }

        return clientMailList;
    }

    /**
     * 宛先送信結果更新を行う。
     * @param successList 送信成功情報
     * @param failureList 送信失敗情報
     * @param param 最後送信データ情報
     */
     webService static Integer updateReceiverList(List<SfdcParam> successList, List<SfdcParam> failureList, List<SfdcParam> paramList) {
         
         Integer toDoCount = 0;
         System.debug('★★★★★');
       
         if(paramList != null && paramList.size() > 0) {
             System.debug('宛先情報取得。。。');
         	 SfdcParam param = paramList[0];
             List<MailReceiver__c> receiverList = getReceiverList(param.id);
             System.debug('宛先送信結果更新開始');
             toDoCount = updateReceiver(receiverList, successList, failureList);
	
             System.debug('TODO情報設定');
             if(toDoCount != -1) {
                 // 異常発生しない場合
                 toDoCount = getToDoCount(param.id);
             }
        }

        return toDoCount;
    }

    webService static Integer updateManageList(List<SfdcParam> successList, List<SfdcParam> failureList, List<SfdcParam> paramList) {
        Integer result = 0;
        if(paramList != null && paramList.size() > 0) {
            Integer limitSize = paramList[0].limitSize;
            List<MailManage__c> manageList = getMailManageList(limitSize);
            if(manageList != null && manageList.size() > 0) {
                result = updateManage(manageList, successList, failureList);
            }
        }

        return result;
    }
    
    /**
    webService static List<ClientMailInfo> getClientMailInfo(Integer limitSize) {
        List<ClientMailInfo> clientMailList = new List<ClientMailInfo>();        
        // 件数を取得する。
        Integer size = getDataSize();
        limitSize = limitSize < 50000 ? limitSize : 50000;
        if (size < 50000) {
            // 一括取得
            List<MailReceiver__c> receiverList = getFirstPage(50000);
            if (receiverList != null && receiverList.size() > 0) {
                for(MailReceiver__c receiver : receiverList) {
                   // データ変換
                   clientMailList.add(getClientMailInfo(receiver));
                }
            }
        } else {
            // 繰り返してデータを取得する。
            List<MailReceiver__c> receiverList = null;
            Integer times = Math.mod(size, limitSize) == 0 ? size / limitSize : size / limitSize + 1;
            //ApexPages.StandardSetController controller = getController();
            Id nextId;
            Decimal sendSeq;
            for(Integer i = 0; i < times; i++) {
                if (i == 0) {
                    receiverList = getFirstPage(limitSize);
                } else {
                    receiverList = getNextPage(nextId, sendSeq);
                }
                
                if (receiverList != null && receiverList.size() > 0) {
                    MailReceiver__c lastRecord = receiverList[receiverList.size() - 1];
                    nextId = lastRecord.Id;
                    sendSeq = lastRecord.SEND_SEQ__c;
                } else {
                    break;
                }
            }
        
        }
        
        return clientMailList;
    }
    
    webService static void updateResult(List<SfdcParam> mSuccessList,
      List<SfdcParam> mFailureList, List<SfdcParam> rSuccessList, List<SfdcParam> rFailureList) {
        updateReceiver(rSuccessList, rFailureList);
        updateManage(mSuccessList, mFailureList);
    }
    */

    
    webService static Integer sendMail(Integer limitSize, Integer retryCount, Integer waitSeconds) {
        Integer result = 0;
        // 送信処理
        // TODO:
        
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
                System.debug(e.getMessage());
                System.debug(e.getCause());
            }
            
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
}