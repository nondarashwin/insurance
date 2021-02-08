package com.manipal.mail.mail


import com.manipal.mail.route.Route
import es.atrujillo.mjml.service.auth.MjmlAuth
import es.atrujillo.mjml.service.auth.MjmlAuthFactory
import es.atrujillo.mjml.service.definition.MjmlService
import es.atrujillo.mjml.service.impl.MjmlRestService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Component

class SendMail {
    @Autowired
    var env:Environment?=null

    fun sendMail(to: List<String>, subjects: String, messages: String) {

        if (to.isNotEmpty()) {
            Transport.send(plainMail(to, subjects, mjml(messages)))
        }

    }

    private fun mjml(mjmlTemplate: String): String {

        val memoryAuthConf: MjmlAuth = MjmlAuthFactory.builder()
                .withMemoryCredentials()
                .mjmlCredentials("c49481d5-fe08-4819-bab8-26db50b9d01a", "dc2f0033-679d-4fb6-9dc6-5784bd5d6f2c")
                .build()

        val mjmlService: MjmlService = MjmlRestService(memoryAuthConf)


        return mjmlService.transpileMjmlToHtml(mjmlTemplate)
    }

    private fun plainMail(tos: List<String>, subjects: String, messages: String): MimeMessage {


        val from =env?.getProperty("spring.mail.username")
        val pass =env?.getProperty("spring.mail.password")
        val encryption = Encryption()
        val properties = System.getProperties()

        with(properties) {
            put("mail.smtp.host", env?.getProperty("spring.mail.host"))
            put("mail.smtp.port", env?.getProperty("spring.mail.port"))
            put("mail.smtp.auth", env?.getProperty("spring.mail.properties.smtp.auth"))
            put("mail.smtp.socketFactory.port",env?.getProperty("spring.mail.properties.smtp.socketFactory.port") )
            put("mail.smtp.socketFactory.class", env?.getProperty("spring.mail.properties.smtp.socketFactory.class"))
        }

        val auth = object : Authenticator() {
            override fun getPasswordAuthentication() =
                    PasswordAuthentication(from, pass?.let { encryption.decrypt(it) })
        }

        val session = Session.getDefaultInstance(properties, auth)

        val message = MimeMessage(session)

        with(message) {
            setFrom(InternetAddress(from))
            for (to in tos) {
                addRecipient(Message.RecipientType.TO, InternetAddress(to))
                subject = subjects
                setContent(messages, "text/html; charset=utf-8")
            }
        }

        return message
    }
}