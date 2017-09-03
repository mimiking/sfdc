package salesforce.mail;

import java.io.UnsupportedEncodingException;
import java.util.Date;
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

import com.sun.mail.smtp.SMTPMessage;


public class SendMail {
	private static Logger logger =Logger.getLogger(SendMail.class);

	//エンコード指定
    private static final String ENCODE = "ISO-2022-JP";

    public static void main(final String[] args) {
        //メール送付
        new SendMail().send();
    }

//ここからメール送付に必要なSMTP,SSL認証などの設定

    public void send() {
        final Properties props = new Properties();

        // SMTPサーバーの設定。
//        props.setProperty("mail.smtp.host", "floboard.bizmw.com");
        props.setProperty("mail.smtp.host", "60.43.186.164");


        // SSL用にポート番号を変更。
        props.setProperty("mail.smtp.port", "587");

        // タイムアウト設定
        props.setProperty("mail.smtp.connectiontimeout", "60000");
        props.setProperty("mail.smtp.timeout", "60000");

        // 認証
        props.setProperty("mail.smtp.auth", "true");

        // SSLを使用するとこはこの設定が必要。
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.smtp.socketFactory.fallback", "true");
        props.setProperty("mail.smtp.socketFactory.port", "587");

        props.setProperty("mail.debug", "true");

       //propsに設定した情報を使用して、sessionの作成
        final Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("sfdev1", "P@ssw0rd");
            }
        });



        // ここからメッセージ内容の設定。上記で作成したsessionを引数に渡す。SMTPMessage
        // final MimeMessage message = new MimeMessage(session);
        final SMTPMessage message = new SMTPMessage(session);

        try {
            final Address addrFrom = new InternetAddress(
                    "sfdev1@floboard.bizmw.com", "test", ENCODE);
            message.setFrom(addrFrom);

//            final Address addrTo = new InternetAddress("sfdev2@floboard.co.jp",
//                    "test2", ENCODE);
//            message.addRecipient(Message.RecipientType.TO, addrTo);
            message.addRecipients(
            		Message.RecipientType.BCC,
            		new Address[] { 
            			new InternetAddress("sfdev1@floboard.co.jp", "TO:1", ENCODE),
            			new InternetAddress("shenxuan@outlook.com", "TO:2", ENCODE)
            		});

            // メールのSubject
            message.setSubject("ありがとうメッセージ受信しました！", ENCODE);

            // メール本文。
            message.setText("こんにちは。envelope + BCC test.", ENCODE);
            
            message.setReplyTo(new Address[] { new InternetAddress("sfdev1@floboard.co.jp", "Reply")});
            message.setEnvelopeFrom("sfdev@floboard.co.jp");

            // その他の付加情報。
//            message.addHeader("X-Mailer", "blancoMail 0.1");
            message.setSentDate(new Date());
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // メール送信。
        try {
            Transport.send(message);
        } catch (AuthenticationFailedException e) {
            // 認証失敗
                 e.printStackTrace();
        } catch (MessagingException e) {
            // smtpサーバへの接続失敗
           e.printStackTrace();

        }
    }
}
