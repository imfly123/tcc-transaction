package org.mengyun.tcctransaction.utils;

import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleMailSender {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(SimpleMailSender.class.getSimpleName());

    private static final JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 获取系统错误邮件默认的接收者邮箱列表
     *
     * @return
     */
    private static String[] getSystemErrReceiver(String receivers) {
        String[] receiver = receivers.split(",");
        for (int i = 0; i < receiver.length; i++) {
            receiver[i] = receiver[i].trim() + "@sunlands.com";
        }
        return receiver;
    }

    public static void sendMail(MailParam mailParam,String subject ,String content ) throws MessagingException {

        if (mailParam == null || StringUtils.isEmpty(mailParam.getUsername()) ||StringUtils.isEmpty(mailParam.getPassword()) ||StringUtils.isEmpty(mailParam.getHost()) || mailParam.getPort() == null
                ||StringUtils.isEmpty(subject) ||StringUtils.isEmpty(content) ||StringUtils.isEmpty(mailParam.getReceivers())){
            logger.warn("邮件参数缺少，不发邮件！");
            return;
        }

        javaMailSender.setHost(mailParam.getHost());
        javaMailSender.setUsername(mailParam.getUsername());
        javaMailSender.setPassword(mailParam.getPassword());
        javaMailSender.setPort(mailParam.getPort());
        javaMailSender.setDefaultEncoding("UTF-8");
        Properties pro = new Properties();
        pro.setProperty("mail.smtp.auth", mailParam.getAuth());
        pro.setProperty("mail.smtp.timeout", mailParam.getTimeout());
        javaMailSender.setJavaMailProperties(pro);

        MimeMessage mine = javaMailSender.createMimeMessage();
        MimeMessageHelper mimehelper = new MimeMessageHelper(mine, true, "UTF-8");

        mimehelper.setSentDate(new Date());
        mimehelper.setFrom(mailParam.getFrom());
        String[] receiverArr = getSystemErrReceiver(mailParam.getReceivers());
        mimehelper.setTo(receiverArr);

        // 设置主题
        String realSub = StringUtils.isNotEmpty(mailParam.getSubjectPrefix())?"[" + mailParam.getSubjectPrefix() + "]" + subject:subject;
        mimehelper.setSubject(realSub);
        mimehelper.setText(content, true);

        javaMailSender.send(mine);
        logger.info("send mail success!");
    }

    public static void asyncSendMail(final MailParam mailParam, final String subject , final String content){
        logger.info("asyncSendMail has been submitted to ExecutorService! subject：{}",subject);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Long statTime = System.currentTimeMillis();
                    sendMail(mailParam,subject,content);
                    logger.debug("asyncSendMail cost time:" + (System.currentTimeMillis() - statTime));
                } catch (MessagingException e) {
                    logger.error("asyncSendMail failed!",e);
                }
            }
        });
    }
}