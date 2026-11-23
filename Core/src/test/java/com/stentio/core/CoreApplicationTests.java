package com.stentio.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/stentio_test",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoTemplateAutoConfiguration"
})
class CoreApplicationTests {

    @Test
    void contextLoads() {
    }

}
