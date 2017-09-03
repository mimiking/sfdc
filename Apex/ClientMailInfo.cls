global class ClientMailInfo {
    // ID
    webService Id clientId;
    // メール送信ｓｅｑ
    webService String seqNo;
    // メール送信元アドレス（Ｆｒｏｍ）
    webService String fromAddress;
    // メール送信元アドレス（ｅｎｖｅｌｏｐｅ）
    webService String envelopeFromAddress;
    // 会社コード
    webService String companyCode;
    // メール送信元名称（Ｆｒｏｍ）
    webService String fromName;
    // メール送信元アドレス（Ｒｅｐｌｙ－Ｔｏ）
    webService String replyToAddress;
    // メール送信元名称（Ｒｅｐｌｙ－Ｔｏ）
    webService String replayToName;
    // メール送信題名
    webService String subject;
    // メール送信本文
    webService String body;
    // メール送信先
    webService String toAddress;
    // メール送信先名称
    webService String toName;
    // メール送信先区分
    webService String sendKbn;
    // メール送信先順序
    webService Decimal sendSeq;
}