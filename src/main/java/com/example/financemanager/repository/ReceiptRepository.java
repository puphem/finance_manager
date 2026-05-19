package com.example.financemanager.repository;

import com.example.financemanager.entity.Receipt;
import com.example.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    boolean existsByReceiptKeyAndUser(String receiptKey, User user);
    List<Receipt> findAllByUser(User user);
    void deleteAllByUser(User user);
}
