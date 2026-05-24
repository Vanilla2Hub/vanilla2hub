plugins {
    java
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {
    group = "me.hjeon"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
