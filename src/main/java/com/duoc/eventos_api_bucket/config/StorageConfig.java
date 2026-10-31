package com.duoc.eventos_api_bucket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * Le dice al SDK de AWS CON QUÉ CREDENCIALES conectarse a S3.
 *
 * ¿Por qué hace falta esta clase?
 *  - AWS Academy entrega credenciales TEMPORALES de 3 partes (key, secret y session token).
 *    Las propiedades estándar de Spring Cloud AWS solo aceptan key y secret, sin token.
 *  - Tu .env lo carga Spring, no el sistema operativo, así que el SDK no lo vería por su cuenta.
 *
 * Spring Cloud AWS crea su propio AwsCredentialsProvider SOLO si no existe uno
 * (@ConditionalOnMissingBean). Al definir este @Bean, el S3Template usa el nuestro.
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
            @Value("${app.aws.access-key:}") String accessKey,
            @Value("${app.aws.secret-key:}") String secretKey,
            @Value("${app.aws.session-token:}") String sessionToken) {

        // Sin llaves configuradas: cadena por defecto de AWS (variables de entorno del sistema
        // o IAM Role de la EC2, que es la práctica recomendada en la industria: cero llaves).
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            log.warn("No hay credenciales AWS en la configuración; se usa la cadena por defecto del SDK");
            return DefaultCredentialsProvider.builder().build();
        }

        // AWS Academy (Learner Lab): credenciales temporales -> necesitan el session token.
        // Con AwsBasicCredentials (solo 2 partes) AWS responde "The security token included in the request is invalid".
        if (StringUtils.hasText(sessionToken)) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKey.trim(), secretKey.trim(), sessionToken.trim()));
        }

        // Cuenta AWS normal con llaves permanentes (sin token).
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
    }
}
