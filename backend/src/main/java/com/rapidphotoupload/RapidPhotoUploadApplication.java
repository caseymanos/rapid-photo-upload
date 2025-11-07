  package com.rapidphotoupload;

  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
  import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
  import org.springframework.scheduling.annotation.EnableScheduling;

  /**
   * Main Spring Boot application for RapidPhotoUpload.
   * Supports 100 concurrent photo uploads with DDD, CQRS, and VSA architecture.
   */
  @SpringBootApplication
  @EnableJpaRepositories("com.rapidphotoupload.infrastructure.persistence.jpa")
  @EnableJpaAuditing
  @EnableScheduling
  public class RapidPhotoUploadApplication {

      public static void main(String[] args) {
          SpringApplication.run(RapidPhotoUploadApplication.class, args);
      }
  }