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
                 toDoCount = getRemains(param.id);
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
            Id minManageId = null;
            if (successList != null && successList.size() > 0 && successList.get(0) != null) {
                minManageId = successList.get(0).id;
                for(SfdcParam sp: successList) {
                    if (minManageId > sp.id) {
                        minManageId = sp.id;
                    }
                }
            }
            if (failureList != null && failureList.size() > 0 && failureList.get(0) != null) {
                minManageId = minManageId == null ? failureList.get(0).id : minManageId;
                for(SfdcParam sp: failureList) {
                    if (minManageId > sp.id) {
                        minManageId = sp.id;
                    }
                }
            }
            // 更新対象情報取得
            if (minManageId != null) {
                List<MailManage__c> manageList = getMailManageList(minManageId, limitSize);
                if(manageList != null && manageList.size() > 0) {
                    // 対象データ更新
                    result = updateManage(manageList, successList, failureList);
                }
            }
        }

        return result;
    }

    /**
     * 送信宛先更新対象情報を取得する。
     * @param lastSendId 処理された最後の宛先情報ID
     */
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
                And SEND_ADR__c != ''
        ];

        return receiverList;
    }

    /**
     * 送信宛先更新対象情報を更新する。
     * @param receiverList 送信宛先情報
     * @param rSuccessList 更新成功情報
     * @param rFailureList 更新失敗情報
     */
    private static Integer updateReceiver(List<MailReceiver__c> receiverList, List<SfdcParam> rSuccessList, List<SfdcParam> rFailureList) {
        Integer result = 0;
        if (receiverList != null && receiverList.size() > 0) {
            for(MailReceiver__c receiver: receiverList) {
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
                result = -1;
                error('送信宛先情報更新失敗しました。');
                error(e.getMessage());
                error(e.getCause());
            }
            
        }

        return result;
    }

    /**
     * 送信管理情報を更新する。
     * @param manageList 送信管理情報
     * @param successList 送信成功情報
     * @param failureList 送信失敗情報
     */
    private static Integer updateManage(List<MailManage__c> manageList, List<SfdcParam> successList, List<SfdcParam> failureList) {
        Integer result = 0;
        if (manageList != null && manageList.size() > 0) {
            info('管理情報更新開始します。[size: ' + manageList.size() + ' ]');
            for(MailManage__c manage: manageList) {
                SfdcParam param = getManageParam(manage, successList);
                if(param != null) {
                    info('送信成功管理情報更新：[ Id: ' + manage.Id + ', result: 1 ]');
                    // 成功
                    manage.SEND_RESULT__c = '1';
                    manage.PROCESSED_DATE__c = DateTime.now();
                } else {
                    // 失敗
                    param = getManageParam(manage, failureList);
                    if(param != null) {
                        info('送信失敗管理情報更新情報設定：[ Id: ' + manage.Id + ', result: 9 ]');
                        manage.SEND_RESULT__c = '9';
                        manage.RETRY_CNT__c = param.retryCount;
                        manage.PROCESSED_DATE__c = DateTime.now();
                    }
                }
            }

            try {
                upsert manageList;
            } catch (DmlException e) {
                // 異常
                result = -1;
                error(e.getMessage());
                error(e.getCause());
            }
            
        }
        info('管理情報更新完了しました。[ result: ' + result + ' ]');
        
	
        return result;
    }

    /**
     * 宛先更新情報を取得する。
     * @param receiver 宛先更新情報
     * @param paramList 更新情報
     */
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

    /**
     * 更新情報を取得する。
     * @param manage 管理情報
     * @param paramList 更新情報
     */
    private static SfdcParam getManageParam(MailManage__c manage, List<SfdcParam> paramList) {
        SfdcParam param = null;
        if(manage != null && paramList != null && paramList.size() > 0) {
            for(SfdcParam sp : paramList) {
                if(sp != null && sp.Id == manage.Id) {
                    // 存在
                    param = sp;
                    break;
                }
            }
        }

        return param;
    }

    //↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    //         　　　 クライアントメール送信処理
    //================================================================
    // 　　　　　　　　　クラウドメール送信処理
    //↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    
    /**
     * Saleforceでメール送信を行う。
     * @param param 送信パラメータ
     */
    webService static SfdcParam sendMail(SfdcParam param) {
        Integer result = 0;
        SfdcParam sfdcParam = new sfdcParam();
        sfdcParam.result = result;
        List<MailReceiver__c> receiverList;
        List<ClientMailInfo> clientMailList = new List<ClientMailInfo>();
        // 送信処理
        if(param != null) {
            // 送信対象データを取得する。
            if(param.id == null) {
                // 初回
                receiverList = getFirstPage(param.limitSize);
            } else {
                // ２回目以後
                receiverList = getNextPage(param.limitSize, param.id);
            }

            if(receiverList != null && receiverList.size() > 0) {
                Id minManageId = receiverList.get(0).MAIL_SEQ_NUM__c;
                Id lastMamageId;
                Id lastReceiverId;
                Boolean isAbort = false;
                SfdcResult receiverResult;
                SfdcResult manageResult;
                // 宛先送信結果情報
                Map<Id, SfdcResult> receiverMap = new Map<Id, SfdcResult>();
                // 管理送信結果情報
                Map<Id, SfdcResult> manageMap = new Map<Id, SfdcResult>();
                for(MailReceiver__c receiver: receiverList) {
                    // メール送信を行う
                    receiverResult = sendMail(receiver);
                    if(receiverResult != null) {
                        if(receiverResult.code != 0) {
                            result = receiverResult.code;
                        }
                        receiverMap.put(receiver.Id, receiverResult);
                        manageResult = manageMap.get(receiver.MAIL_SEQ_NUM__c);
                        if(manageResult != null) {
                            // 既に存在
                            if(manageResult.code < receiverResult.code) {
                                // 処理結果更新
                                info('処理結果：[ code: ' + receiverResult.code + ' ]');
                                manageMap.put(receiver.MAIL_SEQ_NUM__c, receiverResult);
                            }
                        } else {
                            manageMap.put(receiver.MAIL_SEQ_NUM__c, receiverResult);
                        }
                        lastMamageId = receiver.MAIL_SEQ_NUM__c;
                        lastReceiverId = receiver.Id;
                    } else {
                        // 異常発生
                        result = -1;
                        error('フェータルエラーが発生しましたので、メール送信中止をしました。');
                        break;
                    }
                }

                // 送信結果更新を行う
                Integer status = updateReceiver(receiverList, receiverMap);
                if(status == -1) {
                    // 更新エラー発生
                    result = -1;
                } else {
                    // 管理テーブル更新
                    List<MailManage__c> manageList = getMailManageList(minManageId, param.limitSize);
                    status = updateManage(manageList, manageMap, lastMamageId, lastReceiverId);
                    if(status == -1) {
                        // 更新エラー発生
                        result = -1;
                    }
                }

                // 残されたデータ取得
                sfdcParam.result = result;
                if(result != -1) {
                    // 処理成功の場合
                    if(getRemains(lastReceiverId) > 0) {
                        // 残されたデータあり
                        sfdcParam.id = lastReceiverId;
                    }
                }
            } else {
                info('メール送信データがありません。');
            }
        }
        
        return sfdcParam;
    }

    /**
     * メール送信を行う。
     * @param receiver 送信情報
     */
    private static SfdcResult sendMail(MailReceiver__c receiver) {
        SfdcResult result = null;
        Messaging.SingleEmailMessage message = new Messaging.SingleEmailMessage();
        
        if(checkSend(receiver)) {
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
            // 送信名
            if(receiver.MAIL_SEQ_NUM__r.FROM_NAME__c != null) {
                message.setSenderDisplayName(receiver.MAIL_SEQ_NUM__r.FROM_NAME__c);
            }
            // 返信先
            if(receiver.MAIL_SEQ_NUM__r.REPLY_TO_ADDRESS__c != null) {
                message.setReplyTo(receiver.MAIL_SEQ_NUM__r.REPLY_TO_ADDRESS__c);
            }
            // 件名
            message.subject = receiver.MAIL_SEQ_NUM__r.MAIL_SUBJECT__c;
            // 本文
            message.plainTextBody = receiver.MAIL_SEQ_NUM__r.MAIL_BODY__c;
            
            // メール送信
            try {
                Messaging.SingleEmailMessage[] messages = new List<Messaging.SingleEmailMessage>{message};
                Messaging.SendEmailResult[] results = Messaging.sendEmail(messages);
                result = new SfdcResult();
                if (results[0].success) {
                    // 送信成功
                    result.code = 0;
                    info('送信成功しました。[ Id: ' + receiver.Id + ' ]');
                } else {
                    // 送信失敗
                    result.code = 9;
                    info('送信失敗しました。[ Id: ' + receiver.Id + ' ]');
                }
            } catch(Exception e) {
                result = new SfdcResult();
                result.code = 9;
                info('異常が発生しましたので、メール送信失敗しました。[ Id: ' + receiver.Id + ' ]');
            }
        } else {
            result = new SfdcResult();
            result.code = 9;
        }

        return result;
    }
    
    private static boolean checkSend(MailReceiver__c receiver) {
        boolean isValid = true;
        
        if(receiver.SEND_ADR__c == null) {
            isValid = false;
            error('送信元アドレスが指定されないので、送信処理をスキップしました。');
        }
        
        if(isValid && receiver.SEND_KBN__c != '1' && receiver.SEND_KBN__c != '2' && receiver.SEND_KBN__c != '3') {
            isValid = false;
            error('送信区分が正しくないため、送信処理をスキップしました。');
        }
        
        // 件名
        if(isValid && receiver.MAIL_SEQ_NUM__r.MAIL_SUBJECT__c == null || receiver.MAIL_SEQ_NUM__r.MAIL_SUBJECT__c == '') {
            // 件名なし、送信不可にする
            isValid = false;
            error('件名が指定されないので、送信処理をスキップしました。');
        }
        // 本文
        if(isValid && receiver.MAIL_SEQ_NUM__r.MAIL_BODY__c == null || receiver.MAIL_SEQ_NUM__r.MAIL_BODY__c == '') {
            // 本文なし、送信不可にする
            isValid = false;
            error('本文が指定されないので、送信処理をスキップしました。');                                                                                         
        }
        
        return isValid;
    }

    /**
     * 宛先送信結果情報を更新する。
     * @param receiverList 宛先情報
     * @param resulMap 送信結果情報
     */
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
                error('送信宛先情報更新失敗しましたので処理中止する。');
                // 更新失敗データ出力
                error('下記宛先情報更新失敗しました。');
                for(MailReceiver__c receiver : receiverList) {
                    error('Id = ' + receiver.Id + ', SEND_RESULT__c = ' + receiver.SEND_RESULT__c);
                }
                error(e.getMessage());
                error(e.getCause());
            }
        }

        return result;
    }

    /**
     * 管理情報更新を行う。
     * @param manageList 管理情報
     * @param resultMap 送信結果情報
     * @param lastMamageId 処理された最後の管理情報ID
     * @param lastMamageId 処理された最後の宛先情報ID
     */
    private static Integer updateManage(
        List<MailManage__c> manageList, Map<Id,SfdcResult> resultMap, Id lastMamageId, Id lastReceiverId) {
        Integer result = 0;
        if(manageList != null && manageList.size() > 0 && resultMap != null && !resultMap.isEmpty()) {
            SfdcResult sfdcResult;
            info('更新対象管理情報：[ size: ' + manageList.size() + ' ]');
            info('処理結果情報：[ size: ' + resultMap.size() + ' ]');
            for(MailManage__c manage : manageList) {
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
                    manage.PROCESSED_DATE__c = DateTime.now();
                    info('管理情報更新：[ Id: ' + manage.Id + ', 送信結果: ' + manage.SEND_RESULT__c + ' ]');
                }
            }

            try {
                upsert manageList;
            } catch (DmlException e) {
                result = -1;
                error('送信管理情報更新失敗しましたので処理中止する。');
                // 更新失敗データ出力
                error('下記送信管理情報更新失敗しました。');
                for(MailManage__c manage : manageList) {
                    error('Id = ' + manage.Id + ', SEND_RESULT__c = ' + manage.SEND_RESULT__c);
                }
                error(e.getMessage());
                error(e.getCause());
            }
        } else {
            info('送信管理更新対象情報が存在しません。');
        }

        return result;
    }

    /**
     * メールを全部送信したかを判断する。
     * @param manageId 管理情報ID
     * @param receiverId 宛先情報ID
     */
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
              And SEND_ADR__c != ''
        ];

        return count == 0;
    }

    //================================================================
    //         　　　 メール送信処理（共通）
    //================================================================
    /**
     * 初回送信情報を取得する。
     * @param limitSize 制限件数
     */
    private static List<MailReceiver__c> getFirstPage(Integer limitSize) {
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
              SEND_ADR__c != ''
              And MAIL_SEQ_NUM__r.SEND_RESULT__c != '1'
            Order By
              Id, MAIL_SEQ_NUM__c, SEND_KBN__c, SEND_SEQ__c
            limit :limitSize
        ];
        
        return mailList;
    }
    
    /**
     * 二回目以後の送信情報を取得する。
     * @param limitSize 制限件数
     * @param nextId 次回処理開始ID
     */
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
              SEND_ADR__c != ''
              And MAIL_SEQ_NUM__r.SEND_RESULT__c != '1'
              And Id > :nextId
            Order By
              Id, MAIL_SEQ_NUM__c, SEND_KBN__c, SEND_SEQ__c                                
            limit :limitSize
        ];
        
        return mailList;
    }
    
    /**
     * メール送信情報を変換する。
     * @param receiver メール送信情報
     */
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
     * 送信管理情報を取得する。
     * @param limitSize 制限件数
     */
    private static List<MailManage__c> getMailManageList(Id startId, Integer limitSize) {
        List<MailManage__c> manageList = [
            Select
              Id,
              SEND_RESULT__c,
              RETRY_CNT__c
            From
              MailManage__c
            Where
              SEND_RESULT__c != '1'
              And Id >= :startId
            Limit :limitSize
        ];

        return manageList;
    }

    /**
     * 処理していないデータ件数を取得する。
     * @param lastSendId 前回送信情報ID
     */
    private static Integer getRemains(Id lastSendId) {
        Integer count = [
            Select
              Count() 
            From
              MailReceiver__c
            Where
              SEND_RESULT__c != '1'
              And MAIL_SEQ_NUM__r.SEND_RESULT__c !='1'
              And Id > :lastSendId
              And SEND_ADR__c != ''
        ];

        return count;
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