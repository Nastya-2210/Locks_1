package ex5;


import ex5.Account;
import ex5.AccountRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transferMoneyWithoutLock(Long accountId, double amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);
    }

    @Transactional
    public void transferMoneyWithOptimisticLock(Long accountId, double amount) {
        // Получаем аккаунт с проверкой версии
        Account account = accountRepository.findById(accountId).orElseThrow();

        // Изменяем баланс
        account.setBalance(account.getBalance() + amount);

        try {
            // Сохраняем с проверкой версии
            accountRepository.save(account);
        } catch (ObjectOptimisticLockingFailureException e) {
            // В случае конфликта версий пробуем еще раз после паузы
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                transferMoneyWithOptimisticLock(accountId, amount);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Transfer interrupted", ex);
            }
        }
    }

    @Transactional
    public void transferMoneyWithPessimisticLock(Long accountId, double amount) {
    // Получаем аккаунт с пессимистичной блокировкой для записи
        Account account = accountRepository.findByIdWithPessimisticLock(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        // Изменяем баланс
        account.setBalance(account.getBalance() + amount);

        // Сохраняем изменения
        accountRepository.save(account);
    }
}