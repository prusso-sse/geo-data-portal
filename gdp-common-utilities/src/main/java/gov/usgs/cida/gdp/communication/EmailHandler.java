package gov.usgs.cida.gdp.communication;

import gov.usgs.cida.gdp.constants.AppConstant;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.LoggerFactory;

public class EmailHandler {
	private static org.slf4j.Logger log = LoggerFactory.getLogger(EmailHandler.class);

	public static void sendMessage(EmailMessage message) throws AddressException, MessagingException {
		Properties properties = System.getProperties();

                properties.put("mail.smtp.host", AppConstant.EMAIL_HOST.getValue());
                properties.put("mail.smtp.port", AppConstant.EMAIL_PORT.getValue());

		Session session = Session.getInstance(properties, null);
		session.setDebug(true);

		Message msg = new MimeMessage(session);
                
		InternetAddress[] bccList = new InternetAddress[message.getBcc().size()];
		for (int counter = 0;counter < message.getBcc().size();counter++)  {
			InternetAddress email = new InternetAddress();
			email.setAddress(message.getBcc().get(counter));
			bccList[counter] = email;
		}

		InternetAddress[] ccList = new InternetAddress[message.getCc().size()];
		for (int counter = 0;counter < message.getCc().size();counter++)  {
			InternetAddress email = new InternetAddress();
			email.setAddress(message.getCc().get(counter));
			ccList[counter] = email;
		}

		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(message.getTo()));
		if (bccList.length > 0) {
			msg.setRecipients(Message.RecipientType.BCC, bccList);
		}
		if (ccList.length > 0) {
			msg.setRecipients(Message.RecipientType.CC, ccList);
		}
		msg.setFrom(new InternetAddress(message.getFrom()));
		msg.setSubject(message.getSubject());
		msg.setContent(message.getContent(), "text/plain");
                msg.setReplyTo(message.getReplyTo());
		
                Transport.send(msg);
		log.info(new StringBuilder("Sent E-Mail From: ")
                        .append(message.getFrom())
                        .append(" To: ")
                        .append(message.getTo())
                        .append(" Content: " )
                        .append(message.getContent()).toString()
                        );
	}
}