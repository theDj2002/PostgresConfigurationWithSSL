package com.secretmanager.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.protobuf.ByteString;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Map;

@Configuration
@EnableJpaRepositories
public class DevPGSqlConfig {

    @Value("${secretNamePostgre}")
    private String secretNamePostGre;

    private static final Logger logger = LogManager.getLogger(DevPGSqlConfig.class);

    @Bean(name = "postgresAdminDataSource")
    public DataSource postgresAdminDataSource() {
        Map<String, String> secretProperties = null;
        try (SecretManagerServiceClient secretManagerServiceClient = SecretManagerServiceClient.create()) {
            secretProperties = getStringStringMap(secretManagerServiceClient, secretNamePostGre);
        } catch (IOException e) {
            logger.error("Error accessing secret: " + e.getMessage());
        }

        assert secretProperties != null;
        String ip = secretProperties.get("ip");
        String port = secretProperties.get("port");
        String username = secretProperties.get("username");
        String password = secretProperties.get("password");

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://" + ip + ":" + port + "/");
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        String clientCertPath = "D:\\AllProjects\\SecretManager\\pgsqlclient-cert.pem";
        String clientKeyPath = "D:\\AllProjects\\SecretManager\\pgsqlclient-key.pk8";
        String serverCaCertPath = "D:\\AllProjects\\SecretManager\\pgsqlserver-ca.pem";
        String clientKeyPath2 = "D:\\AllProjects\\SecretManager\\pgsqlclient-key_pkcs8.pem";


        dataSource.addDataSourceProperty("sslcert", clientCertPath);
        dataSource.addDataSourceProperty("sslkey", clientKeyPath);
        dataSource.addDataSourceProperty("sslrootcert", serverCaCertPath);
        dataSource.addDataSourceProperty("clientKeyPath2", clientKeyPath2);

        dataSource.setMaximumPoolSize(30);
        logger.info("Connected to database successfully");

        return dataSource;
    }

    private Map<String, String> getStringStringMap(SecretManagerServiceClient secretManagerServiceClient,
                                                   String secretNamePostgre) {
        try {
            AccessSecretVersionResponse response = secretManagerServiceClient.accessSecretVersion(secretNamePostgre);
            ByteString secretData = response.getPayload().getData();
            String secretJson = secretData.toStringUtf8();

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(secretJson, new TypeReference<Map<String, String>>() {
            });

        } catch (IOException e) {
            logger.error("Error accessing secret: " + e.getMessage());
            return null;
        }
    }
}
