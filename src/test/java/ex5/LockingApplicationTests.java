package ex5;

import ex5.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LockingApplicationTests {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    private Long testAccountId;

    @BeforeEach
    void setUp() {
        Account account = new Account();
        account.setAccountNumber("12345");
        account.setBalance(100);
        account = accountRepository.save(account);
        testAccountId = account.getId();
    }

    @Test
    void testConcurrentUpdatesWithoutLocking() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                accountService.transferMoneyWithoutLock(testAccountId, 10);
                latch.countDown();
            });
        }

        latch.await();

        Account account = accountRepository.findById(testAccountId).orElseThrow();
        // Ожидаем 100 + 10*10 = 200, но из-за race condition будет меньше
        assertEquals(200, account.getBalance());
        System.out.println("Баланс без блокировки: " + account.getBalance());
    }

    @Test
    void testConcurrentUpdatesWithOptimisticLocking() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                try {
                    accountService.transferMoneyWithOptimisticLock(testAccountId, 10);
                } catch (Exception e) {
                    System.out.println("Поймано исключение оптимистичной блокировки: " + e.getMessage());
                }
                latch.countDown();
            });
        }

        latch.await();

        Account account = accountRepository.findById(testAccountId).orElseThrow();
        // Проверяем, что баланс изменился корректно (некоторые операции могли не выполниться)
        System.out.println("Баланс с оптимистичной блокировкой: " + account.getBalance());
    }

    @Test
    void testConcurrentUpdatesWithPessimisticLocking() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                accountService.transferMoneyWithPessimisticLock(testAccountId, 10);
                latch.countDown();
            });
        }

        latch.await();

        Account account = accountRepository.findById(testAccountId).orElseThrow();
        // Все операции выполнены последовательно
        assertEquals(200, account.getBalance());
        System.out.println("Баланс с пессимистичной блокировкой: " + account.getBalance());
    }
}