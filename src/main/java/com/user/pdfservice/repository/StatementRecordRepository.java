package com.user.pdfservice.repository;

import com.user.pdfservice.model.entity.StatementRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatementRecordRepository extends MongoRepository<StatementRecord, String> {
    List<StatementRecord> findAllByOrderByGeneratedAtDesc();
    List<StatementRecord> findByUsernameOrderByGeneratedAtDesc(String username);
}

