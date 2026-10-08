package com.banco.batch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
class BatchApplicationTests {

    @Test
    void contextLoads() {
    }

}
